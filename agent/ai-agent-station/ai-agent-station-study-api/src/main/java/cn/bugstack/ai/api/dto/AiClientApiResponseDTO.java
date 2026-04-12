package cn.bugstack.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 客户端 API 配置响应 DTO（模型 API 管理）
 *
 * @author bugstack虫洞栈
 * @description 模型 API 查询响应数据传输对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientApiResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * API 标识
     */
    private String apiId;

    /**
     * 基础 URL
     */
    private String baseUrl;

    /**
     * API 密钥（可选）
     */
    private String apiKey;

    /**
     * 补全路径（可选）
     */
    private String completionsPath;

    /**
     * 向量路径（可选）
     */
    private String embeddingsPath;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
