package cn.bugstack.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户端简要信息（clientId + clientName），用于按类型列表、二级联动下拉等。
 *
 * @author bugstack.cn
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientSimpleItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String clientId;
    private String clientName;
}
