package cn.bugstack.ai.trigger.http.admin;

import cn.bugstack.ai.api.IAiClientApiAdminService;
import cn.bugstack.ai.api.dto.AiClientApiQueryRequestDTO;
import cn.bugstack.ai.api.dto.AiClientApiRequestDTO;
import cn.bugstack.ai.api.dto.AiClientApiResponseDTO;
import cn.bugstack.ai.api.response.Response;
import cn.bugstack.ai.infrastructure.dao.IAiClientApiDao;
import cn.bugstack.ai.infrastructure.dao.po.AiClientApi;
import cn.bugstack.ai.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 客户端 API 配置管理控制器（模型 API 管理）
 *
 * @author bugstack虫洞栈
 * @description 模型 API 管理 CRUD 及查询
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ai-client-api")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AiClientApiAdminController implements IAiClientApiAdminService {

    @Resource
    private IAiClientApiDao aiClientApiDao;

    @Override
    @PostMapping("/create")
    public Response<Boolean> createAiClientApi(@RequestBody AiClientApiRequestDTO request) {
        try {
            log.info("创建 API 配置请求：{}", request);
            if (!StringUtils.hasText(request.getApiId()) || !StringUtils.hasText(request.getBaseUrl())) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("apiId 与 baseUrl 必填")
                        .data(false)
                        .build();
            }
            AiClientApi po = convertToPo(request);
            po.setCreateTime(LocalDateTime.now());
            po.setUpdateTime(LocalDateTime.now());
            int result = aiClientApiDao.insert(po);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result > 0)
                    .build();
        } catch (Exception e) {
            log.error("创建 API 配置失败", e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }

    @Override
    @PostMapping("/update-by-id")
    public Response<Boolean> updateAiClientApiById(@RequestBody AiClientApiRequestDTO request) {
        try {
            log.info("根据 ID 更新 API 配置请求：{}", request);
            if (request.getId() == null) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("ID 不能为空")
                        .data(false)
                        .build();
            }
            AiClientApi po = convertToPo(request);
            po.setUpdateTime(LocalDateTime.now());
            int result = aiClientApiDao.updateById(po);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result > 0)
                    .build();
        } catch (Exception e) {
            log.error("根据 ID 更新 API 配置失败", e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }

    @Override
    @PostMapping("/update-by-api-id")
    public Response<Boolean> updateAiClientApiByApiId(@RequestBody AiClientApiRequestDTO request) {
        try {
            log.info("根据 API ID 更新 API 配置请求：{}", request);
            if (!StringUtils.hasText(request.getApiId())) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("apiId 不能为空")
                        .data(false)
                        .build();
            }
            AiClientApi po = convertToPo(request);
            po.setUpdateTime(LocalDateTime.now());
            int result = aiClientApiDao.updateByApiId(po);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result > 0)
                    .build();
        } catch (Exception e) {
            log.error("根据 API ID 更新 API 配置失败", e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }

    @Override
    @DeleteMapping("/delete-by-id/{id}")
    public Response<Boolean> deleteAiClientApiById(@PathVariable Long id) {
        try {
            log.info("根据 ID 删除 API 配置请求：{}", id);
            int result = aiClientApiDao.deleteById(id);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result > 0)
                    .build();
        } catch (Exception e) {
            log.error("根据 ID 删除 API 配置失败", e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }

    @Override
    @DeleteMapping("/delete-by-api-id/{apiId}")
    public Response<Boolean> deleteAiClientApiByApiId(@PathVariable String apiId) {
        try {
            log.info("根据 API ID 删除 API 配置请求：{}", apiId);
            int result = aiClientApiDao.deleteByApiId(apiId);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result > 0)
                    .build();
        } catch (Exception e) {
            log.error("根据 API ID 删除 API 配置失败", e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }

    @Override
    @GetMapping("/query-by-id/{id}")
    public Response<AiClientApiResponseDTO> queryAiClientApiById(@PathVariable Long id) {
        try {
            log.info("根据 ID 查询 API 配置请求：{}", id);
            AiClientApi po = aiClientApiDao.queryById(id);
            if (po == null) {
                return Response.<AiClientApiResponseDTO>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("未找到对应配置")
                        .data(null)
                        .build();
            }
            return Response.<AiClientApiResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(convertToResponseDto(po))
                    .build();
        } catch (Exception e) {
            log.error("根据 ID 查询 API 配置失败", e);
            return Response.<AiClientApiResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(null)
                    .build();
        }
    }

    @Override
    @GetMapping("/query-by-api-id/{apiId}")
    public Response<AiClientApiResponseDTO> queryAiClientApiByApiId(@PathVariable String apiId) {
        try {
            log.info("根据 API ID 查询 API 配置请求：{}", apiId);
            AiClientApi po = aiClientApiDao.queryByApiId(apiId);
            if (po == null) {
                return Response.<AiClientApiResponseDTO>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("未找到对应配置")
                        .data(null)
                        .build();
            }
            return Response.<AiClientApiResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(convertToResponseDto(po))
                    .build();
        } catch (Exception e) {
            log.error("根据 API ID 查询 API 配置失败", e);
            return Response.<AiClientApiResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(null)
                    .build();
        }
    }

    @Override
    @GetMapping("/query-enabled")
    public Response<List<AiClientApiResponseDTO>> queryEnabledAiClientApis() {
        try {
            log.info("查询所有启用的 API 配置");
            List<AiClientApi> list = aiClientApiDao.queryEnabledApis();
            List<AiClientApiResponseDTO> dtos = list.stream().map(this::convertToResponseDto).collect(Collectors.toList());
            return Response.<List<AiClientApiResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(dtos)
                    .build();
        } catch (Exception e) {
            log.error("查询启用的 API 配置失败", e);
            return Response.<List<AiClientApiResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(null)
                    .build();
        }
    }

    @Override
    @PostMapping("/query-list")
    public Response<List<AiClientApiResponseDTO>> queryAiClientApiList(@RequestBody AiClientApiQueryRequestDTO request) {
        try {
            log.info("分页/条件查询 API 配置列表请求：{}", request);
            List<AiClientApi> list;
            if (StringUtils.hasText(request.getApiId())) {
                AiClientApi one = aiClientApiDao.queryByApiId(request.getApiId());
                list = one != null ? List.of(one) : List.of();
            } else {
                list = aiClientApiDao.queryAll();
            }
            if (request.getStatus() != null) {
                list = list.stream()
                        .filter(api -> request.getStatus().equals(api.getStatus()))
                        .collect(Collectors.toList());
            }
            int pageNum = request.getPageNum() != null && request.getPageNum() > 0 ? request.getPageNum() : 1;
            int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 10;
            int start = (pageNum - 1) * pageSize;
            int end = Math.min(start + pageSize, list.size());
            if (start < list.size()) {
                list = list.subList(start, end);
            } else {
                list = List.of();
            }
            List<AiClientApiResponseDTO> dtos = list.stream().map(this::convertToResponseDto).collect(Collectors.toList());
            return Response.<List<AiClientApiResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(dtos)
                    .build();
        } catch (Exception e) {
            log.error("查询 API 配置列表失败", e);
            return Response.<List<AiClientApiResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(Collections.emptyList())
                    .build();
        }
    }

    @Override
    @GetMapping("/query-all")
    public Response<List<AiClientApiResponseDTO>> queryAllAiClientApis() {
        try {
            log.info("查询全部 API 配置");
            List<AiClientApi> list = aiClientApiDao.queryAll();
            List<AiClientApiResponseDTO> dtos = list.stream().map(this::convertToResponseDto).collect(Collectors.toList());
            return Response.<List<AiClientApiResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(dtos)
                    .build();
        } catch (Exception e) {
            log.error("查询全部 API 配置失败", e);
            return Response.<List<AiClientApiResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(null)
                    .build();
        }
    }

    private AiClientApi convertToPo(AiClientApiRequestDTO dto) {
        AiClientApi po = new AiClientApi();
        BeanUtils.copyProperties(dto, po);
        return po;
    }

    private AiClientApiResponseDTO convertToResponseDto(AiClientApi po) {
        AiClientApiResponseDTO dto = new AiClientApiResponseDTO();
        BeanUtils.copyProperties(po, dto);
        return dto;
    }
}
