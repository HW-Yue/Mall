package cn.bugstack.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 客户端 API 配置请求 DTO（模型 API 管理）
 *
 * @author bugstack虫洞栈
 * @description 模型 API 创建/更新请求数据传输对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientApiRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID（更新时使用）
     */
    private Long id;

    /**
     * API 唯一标识
     */
    private String apiId;

    /**
     * 基础 URL
     */
    private String baseUrl;

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 补全接口路径
     */
    private String completionsPath;

    /**
     * 向量接口路径
     */
    private String embeddingsPath;

    /**
     * 状态（0 禁用，1 启用）
     */
    private Integer status;
}
