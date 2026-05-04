package cn.bugstack.test.domain;

import cn.bugstack.domain.order.model.entity.CreateOrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import cn.bugstack.domain.order.service.IOrderService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

import cn.bugstack.test.config.RocketMqMockTestConfig;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.profiles.active=test-mock")
@Import(RocketMqMockTestConfig.class)
public class OrderServiceTest {

    @Resource
    private IOrderService orderService;

    @Test
    public void test_createOrder_NO_MARKET() throws Exception {
        CreateOrderEntity req = CreateOrderEntity.builder()
                .userId("xiaofuge01")
                .productId("9890001")
                .teamId(null)
                .activityId(100123L)
                .marketTypeVO(MarketTypeVO.Normal)
                .orderId("ORDER_TEST_NO_MARKET_" + System.currentTimeMillis())
                .productName("测试商品")
                .originalPrice(new BigDecimal("99.00"))
                .deductionPrice(BigDecimal.ZERO)
                .payPrice(new BigDecimal("99.00"))
                .build();

        PayOrderEntity payOrderEntity = orderService.createOrder(req);

        log.info("请求参数:{}", JSON.toJSONString(req));
        log.info("测试结果:{}", JSON.toJSONString(payOrderEntity));
    }

    @Test
    public void test_createOrder_GROUP_BUY_MARKET() throws Exception {
        CreateOrderEntity req = CreateOrderEntity.builder()
                .userId("xfg03")
                .productId("9890001")
                .teamId("96646112")
                .activityId(100123L)
                .marketTypeVO(MarketTypeVO.GroupBuyMarket)
                .orderId("ORDER_TEST_GROUP_" + System.currentTimeMillis())
                .productName("测试商品")
                .originalPrice(new BigDecimal("99.00"))
                .deductionPrice(BigDecimal.ZERO)
                .payPrice(new BigDecimal("99.00"))
                .build();

        PayOrderEntity payOrderEntity = orderService.createOrder(req);

        log.info("请求参数:{}", JSON.toJSONString(req));
        log.info("测试结果:{}", JSON.toJSONString(payOrderEntity));
    }

}
