package cn.bugstack.ai.api;

import cn.bugstack.ai.api.dto.AiAgentResponseDTO;
import cn.bugstack.ai.api.dto.ArmoryAgentRequestDTO;
import cn.bugstack.ai.api.dto.ArmoryApiRequestDTO;
import cn.bugstack.ai.api.dto.AutoAgentRequestDTO;
import cn.bugstack.ai.api.response.Response;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * Ai Agent 服务接口
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/8/7 17:52
 */
public interface IAiAgentService {

    /**
     * 智能体对话（SSE 流式），成功返回 ResponseBodyEmitter，参数错误返回 ResponseEntity(400)
     */
    Object autoAgent(AutoAgentRequestDTO request, HttpServletResponse response);

    /**
     * 装配智能体
     */
    Response<Boolean> armoryAgent(ArmoryAgentRequestDTO request);

    /**
     * 装配 API（加载 API）：将指定 apiId 的 API 配置装配到运行环境
     */
    Response<Boolean> armoryApi(ArmoryApiRequestDTO request);

    /**
     * 查询可用的智能体列表
     */
    Response<List<AiAgentResponseDTO>> queryAvailableAgents();

}
