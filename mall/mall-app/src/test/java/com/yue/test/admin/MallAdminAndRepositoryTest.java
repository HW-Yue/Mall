package com.yue.test.admin;

import com.yue.api.dto.ActivityTypeDTO;
import com.yue.api.dto.CategoryDTO;
import com.yue.api.dto.SkuDTO;
import com.yue.api.response.Response;
import com.yue.infrastructure.adapter.repository.ConfigRepository;
import com.yue.infrastructure.adapter.repository.SkuDetailRepository;
import com.yue.infrastructure.dao.IActivityTypeDao;
import com.yue.infrastructure.dao.ICategoryDao;
import com.yue.infrastructure.dao.ISkuDao;
import com.yue.infrastructure.dao.ISkuResourceDetailDao;
import com.yue.infrastructure.dao.po.ActivityType;
import com.yue.infrastructure.dao.po.Category;
import com.yue.infrastructure.dao.po.Sku;
import com.yue.trigger.http.admin.BackendConfigController;
import com.yue.trigger.service.admin.BackendConfigAppServiceImpl;
import com.yue.trigger.service.admin.DCCAppServiceImpl;
import com.yue.types.enums.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MallAdminAndRepositoryTest {

    @Mock
    private ISkuDao skuDao;
    @Mock
    private ISkuResourceDetailDao skuResourceDetailDao;
    @Mock
    private IActivityTypeDao activityTypeDao;
    @Mock
    private ICategoryDao categoryDao;
    @Mock
    private DCCAppServiceImpl dccAppService;

    private ConfigRepository configRepository;
    private SkuDetailRepository skuDetailRepository;
    private BackendConfigAppServiceImpl backendService;
    private BackendConfigController backendController;

    @BeforeEach
    void setUp() {
        configRepository = new ConfigRepository();
        ReflectionTestUtils.setField(configRepository, "skuDao", skuDao);
        ReflectionTestUtils.setField(configRepository, "skuResourceDetailDao", skuResourceDetailDao);
        ReflectionTestUtils.setField(configRepository, "activityTypeDao", activityTypeDao);
        ReflectionTestUtils.setField(configRepository, "categoryDao", categoryDao);

        skuDetailRepository = new SkuDetailRepository();
        ReflectionTestUtils.setField(skuDetailRepository, "skuDao", skuDao);

        backendService = new BackendConfigAppServiceImpl();
        ReflectionTestUtils.setField(backendService, "configRepository", configRepository);
        ReflectionTestUtils.setField(backendService, "dccAppService", dccAppService);

        backendController = new BackendConfigController();
        ReflectionTestUtils.setField(backendController, "backendConfigAppService", backendService);
    }

    @Test
    void repositoriesMapAndDelegateToDaos() {
        when(skuDao.querySkuByGoodsId("g1")).thenReturn(Sku.builder()
                .goodsId("g1").goodsName("phone").goodsImageUrl("img").originalPrice(new BigDecimal("99")).goodsDetail("detail").build());
        assertThat(skuDetailRepository.querySkuDetail("g1").getName()).isEqualTo("phone");

        when(activityTypeDao.queryActivityTypeListAll()).thenReturn(List.of(new ActivityType()));
        when(categoryDao.queryCategoryListAll()).thenReturn(List.of(new Category()));
        when(skuDao.querySkuList()).thenReturn(List.of(new Sku()));
        assertThat(configRepository.listActivityTypes()).hasSize(1);
        assertThat(configRepository.listCategories()).hasSize(1);
        assertThat(configRepository.listSkus()).hasSize(1);
    }

    @Test
    void backendServiceHandlesCrudAndValidation() {
        when(activityTypeDao.queryActivityTypeListAll()).thenReturn(List.of(new ActivityType()));
        assertThat(backendService.listActivityTypes().getData()).hasSize(1);

        when(activityTypeDao.queryActivityTypeById(1)).thenReturn(null);
        assertThat(backendService.getActivityType(1).getCode()).isEqualTo(ResponseCode.ILLEGAL_PARAMETER.getCode());

        ActivityTypeDTO badType = new ActivityTypeDTO();
        assertThat(backendService.saveActivityType(badType).getCode()).isEqualTo(ResponseCode.ILLEGAL_PARAMETER.getCode());

        ActivityTypeDTO typeDTO = new ActivityTypeDTO(null, "秒杀", "seckill", 1);
        when(activityTypeDao.insert(any())).thenReturn(1);
        assertThat(backendService.saveActivityType(typeDTO).getData()).isTrue();

        CategoryDTO categoryDTO = new CategoryDTO(null, "数码", "digital", "icon", 1, 1);
        when(categoryDao.insert(any())).thenReturn(1);
        assertThat(backendService.saveCategory(categoryDTO).getData()).isTrue();

        when(skuDao.querySkuByGoodsId("g1")).thenReturn(null);
        when(skuDao.insert(any())).thenReturn(1);
        SkuDTO skuDTO = new SkuDTO();
        skuDTO.setGoodsId("g1");
        skuDTO.setGoodsName("phone");
        skuDTO.setOriginalPrice(new BigDecimal("99"));
        assertThat(backendService.saveSku(skuDTO).getData()).isTrue();

        when(dccAppService.updateConfig("k1", "v1")).thenReturn(Response.<Boolean>builder().code("0000").info("成功").data(true).build());
        assertThat(backendService.updateDccConfig("k1", "v1").getData()).isTrue();
    }

    @Test
    void backendControllerDelegatesEndpoints() {
        when(activityTypeDao.queryActivityTypeListAll()).thenReturn(List.of(new ActivityType()));
        assertThat(backendController.listActivityTypes().getData()).hasSize(1);

        when(dccAppService.updateConfig("k1", "v1")).thenReturn(Response.<Boolean>builder().code("0000").info("成功").data(true).build());
        assertThat(backendController.updateDccConfig("k1", "v1").getData()).isTrue();
    }
}
