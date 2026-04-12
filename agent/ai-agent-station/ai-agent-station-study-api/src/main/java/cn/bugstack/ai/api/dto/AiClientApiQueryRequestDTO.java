package cn.bugstack.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 客户端 API 配置查询请求 DTO（模型 API 管理列表）
 *
 * @author bugstack虫洞栈
 * @description 模型 API 分页/条件查询请求数据传输对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientApiQueryRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * API ID 筛选
     */
    private String apiId;

    /**
     * 状态筛选（0 禁用，1 启用）
     */
    private Integer status;

    /**
     * 页码，默认 1
     */
    private Integer pageNum;

    /**
     * 每页条数，默认 10
     */
    private Integer pageSize;
}
