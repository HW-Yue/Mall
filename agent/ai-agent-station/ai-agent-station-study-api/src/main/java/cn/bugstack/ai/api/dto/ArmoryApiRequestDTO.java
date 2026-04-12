package cn.bugstack.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 装配 API 请求 DTO（加载 API）
 *
 * @author bugstack虫洞栈
 * @description 模型 API 管理页「加载 API」按钮请求体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArmoryApiRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 要装配的 API ID
     */
    private String apiId;
}
