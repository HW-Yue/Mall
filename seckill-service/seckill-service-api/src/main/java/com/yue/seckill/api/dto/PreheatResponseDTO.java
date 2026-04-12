package com.yue.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理后台手动预热库存响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreheatResponseDTO {

    /** 预热成功的商品数量 */
    private int successCount;

    /** 预热失败的描述列表 */
    private List<String> failList;

}
