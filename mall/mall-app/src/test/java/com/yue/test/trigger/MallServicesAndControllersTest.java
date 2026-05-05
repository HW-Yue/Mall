package com.yue.test.trigger;

import cn.bugstack.wrench.dynamic.config.center.domain.model.valobj.AttributeVO;
import com.yue.api.dto.*;
import com.yue.api.response.Response;
import com.yue.domain.noMarket.detail.model.entity.SkuDetailEntity;
import com.yue.domain.noMarket.detail.service.ISkuDetailService;
import com.yue.infrastructure.dao.ICategoryDao;
import com.yue.infrastructure.dao.ISkuDao;
import com.yue.infrastructure.dao.po.Category;
import com.yue.infrastructure.dao.po.Sku;
import com.yue.trigger.http.IndexController;
import com.yue.trigger.http.SkuController;
import com.yue.trigger.http.admin.DCCController;
import com.yue.trigger.service.admin.DCCAppServiceImpl;
import com.yue.trigger.service.mall.IndexAppServiceImpl;
import com.yue.trigger.service.sku.SkuStockAppService;
import com.yue.types.enums.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RTopic;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MallServicesAndControllersTest {

    @Mock
    private ICategoryDao categoryDao;
    @Mock
    private ISkuDao skuDao;
    @Mock
    private ISkuDetailService skuDetailService;
    @Mock
    private RTopic dccTopic;

    private IndexAppServiceImpl indexAppService;
    private IndexController indexController;
    private SkuStockAppService skuStockAppService;
    private SkuController skuController;
    private DCCAppServiceImpl dccAppService;
    private DCCController dccController;

    @BeforeEach
    void setUp() {
        indexAppService = new IndexAppServiceImpl();
        ReflectionTestUtils.setField(indexAppService, "categoryDao", categoryDao);
        ReflectionTestUtils.setField(indexAppService, "skuDao", skuDao);
        ReflectionTestUtils.setField(indexAppService, "skuDetailService", skuDetailService);

        indexController = new IndexController();
        ReflectionTestUtils.setField(indexController, "indexAppService", indexAppService);

        skuStockAppService = new SkuStockAppService();
        ReflectionTestUtils.setField(skuStockAppService, "skuDao", skuDao);

        skuController = new SkuController();
        ReflectionTestUtils.setField(skuController, "skuStockAppService", skuStockAppService);

        dccAppService = new DCCAppServiceImpl();
        ReflectionTestUtils.setField(dccAppService, "dccTopic", dccTopic);

        dccController = new DCCController();
        ReflectionTestUtils.setField(dccController, "dccAppService", dccAppService);
    }

    @Test
    void indexAppServiceBuildsCategoryGoodsAndDetailResponses() {
        when(categoryDao.queryCategoryList()).thenReturn(List.of(Category.builder().id(1).name("数码").iconUrl("icon").sortOrder(2).build()));
        when(skuDao.countByCategoryId(1)).thenReturn(1);
        when(skuDao.querySkuPageByCategoryId(1, 0, 5)).thenReturn(List.of(
                Sku.builder().goodsId("g1").goodsName("phone").goodsImageUrl("img").originalPrice(new BigDecimal("99")).build()));
        when(skuDetailService.querySkuDetail("g1")).thenReturn(SkuDetailEntity.builder()
                .id("g1").name("phone").imageUrl("img").price(new BigDecimal("99")).goodsDetail("detail").build());

        assertThat(indexAppService.queryCategoryTypeList().getData()).hasSize(1);
        GoodsPageRequestDTO pageRequest = GoodsPageRequestDTO.builder().categoryId(1).pageNum(1).pageSize(5).build();
        assertThat(indexAppService.queryGoodsPage(pageRequest).getData().getList()).hasSize(1);
        GoodsDetailRequestDTO detailRequest = new GoodsDetailRequestDTO();
        detailRequest.setGoodsId("g1");
        assertThat(indexAppService.querySkuDetail(detailRequest).getData().getGoodsDetail()).isEqualTo("detail");
        assertThat(indexAppService.queryActivityGoods().getData().getGroupBuyList()).isEmpty();
    }

    @Test
    void indexAppServiceHandlesDetailErrorsAndControllerDelegates() {
        assertThat(indexAppService.querySkuDetail(null).getInfo()).contains("商品ID不能为空");
        GoodsDetailRequestDTO request = new GoodsDetailRequestDTO();
        request.setGoodsId("g2");
        when(skuDetailService.querySkuDetail("g2")).thenReturn(null);
        assertThat(indexAppService.querySkuDetail(request).getInfo()).contains("商品不存在");

        when(categoryDao.queryCategoryList()).thenThrow(new IllegalStateException("db down"));
        assertThat(indexController.queryCategoryTypeList().getCode()).isEqualTo(ResponseCode.UN_ERROR.getCode());
    }

    @Test
    void skuStockAppServiceAndControllerHandleLockAndUnlock() {
        assertThat(skuStockAppService.lockStock(new SkuStockRequestDTO("g1", 0)).getCode()).isEqualTo(ResponseCode.ILLEGAL_PARAMETER.getCode());

        when(skuDao.selectByGoodsIdForUpdate("g1")).thenReturn(Sku.builder().goodsId("g1").totalStock(5).lockedStock(1).build());
        when(skuDao.addLockedStock("g1", 2)).thenReturn(1);
        Response<Boolean> lockResp = skuController.lockStock(new SkuStockRequestDTO("g1", 2));
        assertThat(lockResp.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());

        when(skuDao.selectByGoodsIdForUpdate("g2")).thenReturn(Sku.builder().goodsId("g2").totalStock(1).lockedStock(1).build());
        assertThat(skuStockAppService.lockStock(new SkuStockRequestDTO("g2", 1)).getInfo()).contains("库存不足");

        when(skuDao.subLockedStock("g1", 1)).thenReturn(1);
        assertThat(skuController.unlockStock(new SkuStockRequestDTO("g1", 1)).getData()).isTrue();
    }

    @Test
    void dccServicePublishesAttributeAndControllerDelegates() {
        Response<Boolean> response = dccController.updateConfig("k1", "v1");

        assertThat(response.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        ArgumentCaptor<AttributeVO> captor = ArgumentCaptor.forClass(AttributeVO.class);
        verify(dccTopic).publish(captor.capture());
        assertThat(captor.getValue().getAttribute()).isEqualTo("k1");
        assertThat(captor.getValue().getValue()).isEqualTo("v1");
    }
}
