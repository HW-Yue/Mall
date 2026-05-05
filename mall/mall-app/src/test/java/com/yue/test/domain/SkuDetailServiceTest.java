package com.yue.test.domain;

import com.yue.domain.noMarket.detail.adapter.repository.ISkuDetailRepository;
import com.yue.domain.noMarket.detail.model.entity.SkuDetailEntity;
import com.yue.domain.noMarket.detail.service.SkuDetailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkuDetailServiceTest {

    @Mock
    private ISkuDetailRepository repository;

    @InjectMocks
    private SkuDetailService skuDetailService;

    @Test
    void querySkuDetailReturnsRepositoryResult() {
        SkuDetailEntity detail = SkuDetailEntity.builder()
                .id("g1")
                .name("phone")
                .price(new BigDecimal("1999"))
                .build();
        when(repository.querySkuDetail("g1")).thenReturn(detail);

        SkuDetailEntity result = skuDetailService.querySkuDetail("g1");

        assertThat(result).isSameAs(detail);
        verify(repository).querySkuDetail("g1");
    }

    @Test
    void querySkuDetailReturnsNullWhenRepositoryMisses() {
        when(repository.querySkuDetail("g2")).thenReturn(null);

        assertThat(skuDetailService.querySkuDetail("g2")).isNull();
    }
}
