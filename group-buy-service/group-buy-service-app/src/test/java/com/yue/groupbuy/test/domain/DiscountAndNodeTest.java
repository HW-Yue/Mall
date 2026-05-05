package com.yue.groupbuy.test.domain;

import com.yue.groupbuy.domain.activity.adapter.repository.IActivityRepository;
import com.yue.groupbuy.domain.activity.model.entity.MarketProductEntity;
import com.yue.groupbuy.domain.activity.model.entity.TrialBalanceEntity;
import com.yue.groupbuy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.yue.groupbuy.domain.activity.model.valobj.SkuVO;
import com.yue.groupbuy.domain.activity.service.discount.IDiscountCalculateService;
import com.yue.groupbuy.domain.activity.service.discount.impl.MJCalculateService;
import com.yue.groupbuy.domain.activity.service.discount.impl.NCalculateService;
import com.yue.groupbuy.domain.activity.service.discount.impl.ZJCalculateService;
import com.yue.groupbuy.domain.activity.service.discount.impl.ZKCalculateService;
import com.yue.groupbuy.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.yue.groupbuy.domain.activity.service.trial.node.EndNode;
import com.yue.groupbuy.domain.activity.service.trial.node.RootNode;
import com.yue.groupbuy.domain.activity.service.trial.node.SwitchNode;
import com.yue.groupbuy.domain.activity.service.trial.node.TagNode;
import com.yue.groupbuy.types.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DiscountAndNodeTest {

    @Test
    void discountCalculatorsComputeExpectedPayPrice() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount mj = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder().marketExpr("100,10").build();
        GroupBuyActivityDiscountVO.GroupBuyDiscount n = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder().marketExpr("49.9").build();
        GroupBuyActivityDiscountVO.GroupBuyDiscount zj = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder().marketExpr("10").build();
        GroupBuyActivityDiscountVO.GroupBuyDiscount zk = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder().marketExpr("0.8").build();

        assertThat(new MJCalculateService().doCalculate(new BigDecimal("120"), mj)).isEqualByComparingTo("110");
        assertThat(new NCalculateService().doCalculate(new BigDecimal("120"), n)).isEqualByComparingTo("49.9");
        assertThat(new ZJCalculateService().doCalculate(new BigDecimal("120"), zj)).isEqualByComparingTo("110");
        assertThat(new ZKCalculateService().doCalculate(new BigDecimal("99"), zk)).isEqualByComparingTo("79");
    }

    @Test
    void factoryExposesInjectedRootNode() {
        RootNode rootNode = mock(RootNode.class);
        DefaultActivityStrategyFactory factory = new DefaultActivityStrategyFactory(rootNode);
        assertThat(factory.strategyHandler()).isSameAs(rootNode);
    }

    @Test
    void rootNodeRejectsMissingRequiredFields() {
        RootNode rootNode = new RootNode();
        assertThatThrownBy(() -> rootNode.apply(MarketProductEntity.builder().build(), new DefaultActivityStrategyFactory.DynamicContext()))
                .isInstanceOf(AppException.class);
    }

    @Test
    void switchNodeRejectsDowngradeAndCutRangeFailures() {
        IActivityRepository repository = mock(IActivityRepository.class);
        SwitchNode switchNode = new SwitchNode();
        ReflectionTestUtils.setField(switchNode, "repository", repository);
        when(repository.downgradeSwitch()).thenReturn(true);
        assertThatThrownBy(() -> switchNode.doApply(MarketProductEntity.builder().userId("u1").build(), new DefaultActivityStrategyFactory.DynamicContext()))
                .isInstanceOf(AppException.class);

        when(repository.downgradeSwitch()).thenReturn(false);
        when(repository.cutRange("u1")).thenReturn(false);
        assertThatThrownBy(() -> switchNode.doApply(MarketProductEntity.builder().userId("u1").build(), new DefaultActivityStrategyFactory.DynamicContext()))
                .isInstanceOf(AppException.class);
    }

    @Test
    void tagNodeSetsVisibilityAndEnableFlags() throws Exception {
        IActivityRepository repository = mock(IActivityRepository.class);
        EndNode endNode = mock(EndNode.class);
        TagNode tagNode = new TagNode();
        ReflectionTestUtils.setField(tagNode, "repository", repository);
        ReflectionTestUtils.setField(tagNode, "endNode", endNode);
        when(repository.isTagCrowdRange("tag-1", "u1")).thenReturn(false);
        when(endNode.apply(any(), any())).thenReturn(TrialBalanceEntity.builder().goodsId("g1").build());

        DefaultActivityStrategyFactory.DynamicContext context = DefaultActivityStrategyFactory.DynamicContext.builder()
                .groupBuyActivityDiscountVO(GroupBuyActivityDiscountVO.builder().tagId("tag-1").tagScope("1,2").build())
                .build();
        TrialBalanceEntity result = tagNode.apply(MarketProductEntity.builder().userId("u1").build(), context);

        assertThat(result.getGoodsId()).isEqualTo("g1");
        assertThat(context.isVisible()).isFalse();
        assertThat(context.isEnable()).isFalse();
    }

    @Test
    void endNodeBuildsTrialBalance() throws Exception {
        EndNode endNode = new EndNode();
        DefaultActivityStrategyFactory.DynamicContext context = DefaultActivityStrategyFactory.DynamicContext.builder()
                .groupBuyActivityDiscountVO(GroupBuyActivityDiscountVO.builder().target(3).build())
                .skuVO(SkuVO.builder().goodsId("g1").goodsName("goods").originalPrice(new BigDecimal("99")).build())
                .deductionPrice(new BigDecimal("10"))
                .payPrice(new BigDecimal("89"))
                .visible(true)
                .enable(true)
                .build();

        TrialBalanceEntity result = endNode.doApply(MarketProductEntity.builder().userId("u1").build(), context);

        assertThat(result.getGoodsId()).isEqualTo("g1");
        assertThat(result.getPayPrice()).isEqualByComparingTo("89");
        assertThat(result.getTargetCount()).isEqualTo(3);
    }
}
