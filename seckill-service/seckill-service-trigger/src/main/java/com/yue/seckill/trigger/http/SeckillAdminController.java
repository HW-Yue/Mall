package com.yue.seckill.trigger.http;

import com.yue.seckill.api.ISeckillAdminController;
import com.yue.seckill.api.dto.PreheatRequestDTO;
import com.yue.seckill.api.dto.PreheatResponseDTO;
import com.yue.seckill.api.dto.QueryActivitiesResponseDTO;
import com.yue.seckill.api.response.Response;
import com.yue.seckill.domain.activity.model.valobj.SeckillActivityWithGoodsVO;
import com.yue.seckill.domain.activity.service.ISeckillAdminService;
import com.yue.seckill.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 秒杀管理后台 Controller（预热管理）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/seckill/admin")
public class SeckillAdminController implements ISeckillAdminController {

    private static final long DEFAULT_EXPIRE_SECONDS = 7200L;

    @Resource
    private ISeckillAdminService seckillAdminService;

    @Override
    @GetMapping("/query_activities")
    public Response<QueryActivitiesResponseDTO> queryActivities() {
        try {
            log.info("管理后台查询秒杀活动列表");
            List<SeckillActivityWithGoodsVO> voList = seckillAdminService.queryActivitiesWithGoods();

            List<QueryActivitiesResponseDTO.ActivityWithGoodsDTO> activities = new ArrayList<>();
            for (SeckillActivityWithGoodsVO vo : voList) {
                List<QueryActivitiesResponseDTO.GoodsItemDTO> goodsItems = new ArrayList<>();
                for (SeckillActivityWithGoodsVO.GoodsItem item : vo.getGoodsList()) {
                    String currentStock = seckillAdminService.getPreheatStatus(vo.getActivityId(), item.getGoodsId());
                    goodsItems.add(QueryActivitiesResponseDTO.GoodsItemDTO.builder()
                            .goodsId(item.getGoodsId())
                            .goodsName(item.getGoodsName())
                            .currentStock(currentStock)
                            .build());
                }
                activities.add(QueryActivitiesResponseDTO.ActivityWithGoodsDTO.builder()
                        .activityId(vo.getActivityId())
                        .activityName(vo.getActivityName())
                        .remainCount(vo.getRemainCount())
                        .goodsList(goodsItems)
                        .build());
            }

            return Response.<QueryActivitiesResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(QueryActivitiesResponseDTO.builder().activities(activities).build())
                    .build();
        } catch (Exception e) {
            log.error("管理后台查询秒杀活动列表异常", e);
            return Response.<QueryActivitiesResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @Override
    @PostMapping("/preheat")
    public Response<PreheatResponseDTO> preheat(@RequestBody PreheatRequestDTO request) {
        try {
            if (request.getActivityId() == null) {
                return Response.<PreheatResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("activityId 不能为空")
                        .build();
            }

            long expireSeconds = (request.getExpireSeconds() != null && request.getExpireSeconds() > 0)
                    ? request.getExpireSeconds() : DEFAULT_EXPIRE_SECONDS;

            int successCount = 0;
            List<String> failList = new ArrayList<>();

            if ("all".equalsIgnoreCase(request.getGoodsId())) {
                // 预热该活动下所有商品
                List<SeckillActivityWithGoodsVO> voList = seckillAdminService.queryActivitiesWithGoods();
                for (SeckillActivityWithGoodsVO vo : voList) {
                    if (!request.getActivityId().equals(vo.getActivityId())) continue;
                    for (SeckillActivityWithGoodsVO.GoodsItem item : vo.getGoodsList()) {
                        try {
                            seckillAdminService.preheatStock(vo.getActivityId(), item.getGoodsId(), request.getStock(), expireSeconds);
                            successCount++;
                        } catch (Exception e) {
                            log.error("预热失败 activityId:{} goodsId:{}", vo.getActivityId(), item.getGoodsId(), e);
                            failList.add("goodsId=" + item.getGoodsId() + " 预热失败: " + e.getMessage());
                        }
                    }
                    break;
                }
            } else {
                // 预热单个商品
                if (request.getGoodsId() == null || request.getGoodsId().isEmpty()) {
                    return Response.<PreheatResponseDTO>builder()
                            .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                            .info("goodsId 不能为空，如需预热全部请传 \"all\"")
                            .build();
                }
                try {
                    seckillAdminService.preheatStock(request.getActivityId(), request.getGoodsId(), request.getStock(), expireSeconds);
                    successCount = 1;
                } catch (Exception e) {
                    log.error("预热失败 activityId:{} goodsId:{}", request.getActivityId(), request.getGoodsId(), e);
                    failList.add("goodsId=" + request.getGoodsId() + " 预热失败: " + e.getMessage());
                }
            }

            log.info("管理后台预热完成 activityId:{} successCount:{} failCount:{}", request.getActivityId(), successCount, failList.size());
            return Response.<PreheatResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(PreheatResponseDTO.builder()
                            .successCount(successCount)
                            .failList(failList)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("管理后台预热异常", e);
            return Response.<PreheatResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
