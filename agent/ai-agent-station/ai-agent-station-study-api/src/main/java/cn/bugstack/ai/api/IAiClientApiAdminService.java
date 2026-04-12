package cn.bugstack.ai.api;

import cn.bugstack.ai.api.dto.AiClientApiQueryRequestDTO;
import cn.bugstack.ai.api.dto.AiClientApiRequestDTO;
import cn.bugstack.ai.api.dto.AiClientApiResponseDTO;
import cn.bugstack.ai.api.response.Response;

import java.util.List;

/**
 * AI 客户端 API 配置管理服务接口（模型 API 管理）
 *
 * @author bugstack虫洞栈
 * @description 模型 API 管理后台 CRUD 及查询
 */
public interface IAiClientApiAdminService {

    /**
     * 创建 API 配置
     */
    Response<Boolean> createAiClientApi(AiClientApiRequestDTO request);

    /**
     * 根据 ID 更新 API 配置
     */
    Response<Boolean> updateAiClientApiById(AiClientApiRequestDTO request);

    /**
     * 根据 API ID 更新 API 配置
     */
    Response<Boolean> updateAiClientApiByApiId(AiClientApiRequestDTO request);

    /**
     * 根据 ID 删除
     */
    Response<Boolean> deleteAiClientApiById(Long id);

    /**
     * 根据 API ID 删除
     */
    Response<Boolean> deleteAiClientApiByApiId(String apiId);

    /**
     * 根据 ID 查询
     */
    Response<AiClientApiResponseDTO> queryAiClientApiById(Long id);

    /**
     * 根据 API ID 查询
     */
    Response<AiClientApiResponseDTO> queryAiClientApiByApiId(String apiId);

    /**
     * 查询所有启用的 API 配置
     */
    Response<List<AiClientApiResponseDTO>> queryEnabledAiClientApis();

    /**
     * 分页/条件查询列表（列表页主接口）
     */
    Response<List<AiClientApiResponseDTO>> queryAiClientApiList(AiClientApiQueryRequestDTO request);

    /**
     * 查询全部 API 配置
     */
    Response<List<AiClientApiResponseDTO>> queryAllAiClientApis();
}
