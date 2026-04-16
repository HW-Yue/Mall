package com.yue.groupbuy.trigger.http;

import com.yue.groupbuy.api.IGroupBuyMarketController;
import com.yue.groupbuy.api.dto.*;
import com.yue.groupbuy.api.response.Response;
import com.yue.groupbuy.domain.activity.model.entity.GroupBuyGoodsEntity;
import com.yue.groupbuy.domain.activity.model.entity.MarketProductEntity;
import com.yue.groupbuy.domain.activity.model.entity.TrialBalanceEntity;
import com.yue.groupbuy.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.yue.groupbuy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.yue.groupbuy.domain.activity.model.valobj.TeamStatisticVO;
import com.yue.groupbuy.domain.activity.service.IIndexGroupBuyMarketService;
import com.yue.groupbuy.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/group-buy/market/")
public class GroupBuyMarketController implements IGroupBuyMarketController {

    @Resource
    private IIndexGroupBuyMarketService indexGroupBuyMarketService;


    @Override
    @RequestMapping(value = "query_goods_list", method = RequestMethod.GET)
    public Response<QueryGroupBuyGoodsListResponseDTO> queryGroupBuyGoodsList() {
        try {
            log.info("查询拼团商品列表");
            List<GroupBuyGoodsEntity> goodsList = indexGroupBuyMarketService.queryGroupBuyGoodsList();

            List<GroupBuyGoodsDTO> dtoList = new ArrayList<>();
            if (goodsList != null && !goodsList.isEmpty()) {
                for (GroupBuyGoodsEntity goods : goodsList) {
                    dtoList.add(GroupBuyGoodsDTO.builder()
                            .goodsId(goods.getGoodsId())
                            .goodsName(goods.getGoodsName())
                            .goodsImageUrl(goods.getGoodsImageUrl())
                            .originalPrice(goods.getOriginalPrice())
                            .payPrice(goods.getPayPrice())
                            .source(goods.getSource())
                            .channel(goods.getChannel())
                            .activityId(goods.getActivityId())
                            .build());
                }
            }

            return Response.<QueryGroupBuyGoodsListResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(QueryGroupBuyGoodsListResponseDTO.builder()
                            .groupBuyGoodsList(dtoList)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("查询拼团商品列表异常", e);
            return Response.<QueryGroupBuyGoodsListResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @Override
    @RequestMapping(value = "query_group_buy_market_config", method = RequestMethod.POST)
    public Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfig(@RequestBody GoodsMarketRequestDTO requestDTO) {
        try {
            log.info("查询拼团营销配置开始:{} goodsId:{}", requestDTO.getUserId(), requestDTO.getGoodsId());

            if (StringUtils.isBlank(requestDTO.getUserId()) || StringUtils.isBlank(requestDTO.getSource())
                    || StringUtils.isBlank(requestDTO.getChannel()) || StringUtils.isBlank(requestDTO.getGoodsId())) {
                return Response.<GoodsMarketResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }

            // 1. 试算
            MarketProductEntity marketProductEntity = MarketProductEntity.builder()
                    .userId(requestDTO.getUserId())
                    .source(requestDTO.getSource())
                    .channel(requestDTO.getChannel())
                    .goodsId(requestDTO.getGoodsId())
                    .build();

            TrialBalanceEntity trialBalance = indexGroupBuyMarketService.indexMarketTrial(marketProductEntity);
            Long activityId = trialBalance.getGroupBuyActivityDiscountVO() != null
                    ? trialBalance.getGroupBuyActivityDiscountVO().getActivityId() : null;

            // 2. 查询参团记录
            List<UserGroupBuyOrderDetailEntity> teamList = indexGroupBuyMarketService
                    .queryInProgressUserGroupBuyOrderDetailList(requestDTO.getGoodsId(), requestDTO.getUserId(), 1, 2);

            // 3. 查询统计
            TeamStatisticVO statistic = indexGroupBuyMarketService.queryTeamStatisticByGoodsId(requestDTO.getGoodsId());

            // 组装响应
            GoodsMarketResponseDTO.Goods goods = GoodsMarketResponseDTO.Goods.builder()
                    .goodsId(trialBalance.getGoodsId())
                    .goodsImageUrl(trialBalance.getGoodsImageUrl())
                    .originalPrice(trialBalance.getOriginalPrice())
                    .deductionPrice(trialBalance.getDeductionPrice())
                    .payPrice(trialBalance.getPayPrice())
                    .build();

            List<GoodsMarketResponseDTO.Team> teams = new ArrayList<>();
            if (teamList != null) {
                for (UserGroupBuyOrderDetailEntity entity : teamList) {
                    teams.add(GoodsMarketResponseDTO.Team.builder()
                            .userId(entity.getUserId())
                            .teamId(entity.getTeamId())
                            .activityId(entity.getActivityId())
                            .targetCount(entity.getTargetCount())
                            .completeCount(entity.getCompleteCount())
                            .lockCount(entity.getLockCount())
                            .validStartTime(entity.getValidStartTime())
                            .validEndTime(entity.getValidEndTime())
                            .validTimeCountdown(GoodsMarketResponseDTO.Team.differenceDateTime2Str(
                                    new Date(), entity.getValidEndTime()))
                            .outTradeNo(entity.getOutTradeNo())
                            .build());
                }
            }

            GoodsMarketResponseDTO.TeamStatistic teamStatistic = GoodsMarketResponseDTO.TeamStatistic.builder()
                    .allTeamCount(statistic.getAllTeamCount())
                    .allTeamCompleteCount(statistic.getAllTeamCompleteCount())
                    .allTeamUserCount(statistic.getAllTeamUserCount())
                    .build();

            GoodsMarketResponseDTO result = GoodsMarketResponseDTO.builder()
                    .activityId(activityId)
                    .goods(goods)
                    .teamList(teams)
                    .teamStatistic(teamStatistic)
                    .build();

            log.info("查询拼团营销配置完成:{} goodsId:{}", requestDTO.getUserId(), requestDTO.getGoodsId());

            return Response.<GoodsMarketResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();

        } catch (Exception e) {
            log.error("查询拼团营销配置失败", e);
            return Response.<GoodsMarketResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
