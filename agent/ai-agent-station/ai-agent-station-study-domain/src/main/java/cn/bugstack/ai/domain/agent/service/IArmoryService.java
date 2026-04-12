package cn.bugstack.ai.domain.agent.service;

import cn.bugstack.ai.domain.agent.model.valobj.AiAgentVO;

import java.util.List;

/**
 * 装配接口
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/10/3 12:48
 */
public interface IArmoryService {

    List<AiAgentVO> acceptArmoryAllAvailableAgents();

    void acceptArmoryAgent(String agentId);

    /**
     * 按 clientId 进行运行时局部装配，仅装配该客户端及其依赖的 API、Model、Prompt 等 Bean。
     * 用于延迟加载场景：执行时若容器中不存在 ai_client_xxx，则先调用本方法再获取 Bean。
     *
     * @param clientId 客户端 ID
     */
    void armoryByClientId(String clientId);

    /**
     * 按 apiId 装配 API 配置到运行环境（模型 API 管理页「加载 API」）
     *
     * @param apiId API 唯一标识
     */
    void armoryByApiId(String apiId);

    List<AiAgentVO> queryAvailableAgents();

}
