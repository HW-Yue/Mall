package cn.bugstack.ai.api;

import cn.bugstack.ai.api.dto.AiClientQueryRequestDTO;
import cn.bugstack.ai.api.dto.AiClientRequestDTO;
import cn.bugstack.ai.api.dto.AiClientResponseDTO;
import cn.bugstack.ai.api.dto.AiClientSimpleItemDTO;
import cn.bugstack.ai.api.dto.AiClientStrategySlotsResponseDTO;
import cn.bugstack.ai.api.response.Response;

import java.util.List;

/**
 * AI客户端管理服务接口
 *
 * @author bugstack虫洞栈
 * @description AI客户端配置管理服务接口
 */
public interface IAiClientAdminService {

    /**
     * 创建AI客户端配置
     * @param request AI客户端配置请求对象
     * @return 操作结果
     */
    Response<Boolean> createAiClient(AiClientRequestDTO request);

    /**
     * 根据ID更新AI客户端配置
     * @param request AI客户端配置请求对象
     * @return 操作结果
     */
    Response<Boolean> updateAiClientById(AiClientRequestDTO request);

    /**
     * 根据客户端ID更新AI客户端配置
     * @param request AI客户端配置请求对象
     * @return 操作结果
     */
    Response<Boolean> updateAiClientByClientId(AiClientRequestDTO request);

    /**
     * 根据ID删除AI客户端配置
     * @param id 主键ID
     * @return 操作结果
     */
    Response<Boolean> deleteAiClientById(Long id);

    /**
     * 根据客户端ID删除AI客户端配置
     * @param clientId 客户端ID
     * @return 操作结果
     */
    Response<Boolean> deleteAiClientByClientId(String clientId);

    /**
     * 根据ID查询AI客户端配置
     * @param id 主键ID
     * @return AI客户端配置对象
     */
    Response<AiClientResponseDTO> queryAiClientById(Long id);

    /**
     * 根据客户端ID查询AI客户端配置
     * @param clientId 客户端ID
     * @return AI客户端配置对象
     */
    Response<AiClientResponseDTO> queryAiClientByClientId(String clientId);

    /**
     * 查询所有启用的AI客户端配置
     * @return AI客户端配置列表
     */
    Response<List<AiClientResponseDTO>> queryEnabledAiClients();

    /**
     * 根据条件查询AI客户端配置列表
     * @param request 查询条件
     * @return AI客户端配置列表
     */
    Response<List<AiClientResponseDTO>> queryAiClientList(AiClientQueryRequestDTO request);

    /**
     * 查询所有AI客户端配置
     * @return AI客户端配置列表
     */
    Response<List<AiClientResponseDTO>> queryAllAiClients();

    /**
     * 按策略返回必选/可选 client_type 槽位列表，供前端二级联动选择
     * @param strategy 策略标识（如 flowAgentExecuteStrategy）
     * @return 该策略的 requiredSlots、optionalSlots
     */
    Response<AiClientStrategySlotsResponseDTO> getStrategySlots(String strategy);

    /**
     * 按 clientType 高效返回该类型下所有客户端实例（clientId、clientName），走 client_type 索引
     * @param clientType 客户端类型
     * @return 客户端简要列表
     */
    Response<List<AiClientSimpleItemDTO>> listByType(String clientType);

    /**
     * 按 client_type 查询该类型下客户端完整列表（含 clientType 等），供前端二级联动第二步使用
     * @param clientType 客户端类型编码，如 TASK_ANALYZER_CLIENT、PRECISION_EXECUTOR_CLIENT
     * @return 该类型下的 AiClientResponseDTO 列表
     */
    Response<List<AiClientResponseDTO>> queryByClientType(String clientType);

}