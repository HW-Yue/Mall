package cn.bugstack.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 按策略返回的客户端槽位（client_type）列表，供前端二级联动选择。
 *
 * @author bugstack.cn
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientStrategySlotsResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 策略标识，如 flowAgentExecuteStrategy */
    private String strategy;
    /** 必选 client_type 列表 */
    private List<String> requiredSlots;
    /** 可选 client_type 列表 */
    private List<String> optionalSlots;
}
