package cn.bugstack.infrastructure.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品详情请求
 */
@Data
@Builder
public class GoodsDetailRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private String goodsId;

}

