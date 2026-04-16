package com.yue.seckill.trigger.http;

import com.yue.seckill.api.ISeckillMarketController;
import com.yue.seckill.api.dto.QuerySeckillGoodsListResponseDTO;
import com.yue.seckill.api.dto.SeckillGoodsDTO;
import com.yue.seckill.api.response.Response;
import com.yue.seckill.domain.activity.model.entity.SeckillGoodsEntity;
import com.yue.seckill.domain.activity.service.ISeckillMarketService;
import com.yue.seckill.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 秒杀市场 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/seckill/market")
public class SeckillMarketController implements ISeckillMarketController {

    @Resource
    private ISeckillMarketService seckillMarketService;

    @Override
    @RequestMapping(value = "/query_goods_list", method = RequestMethod.GET)
    public Response<QuerySeckillGoodsListResponseDTO> querySeckillGoodsList() {
        try {
            log.info("查询秒杀商品列表");
            List<SeckillGoodsEntity> goodsList = seckillMarketService.querySeckillGoodsList();

            List<SeckillGoodsDTO> dtoList = new ArrayList<>();
            if (goodsList != null && !goodsList.isEmpty()) {
                for (SeckillGoodsEntity goods : goodsList) {
                    dtoList.add(SeckillGoodsDTO.builder()
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

                        return Response.<QuerySeckillGoodsListResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(QuerySeckillGoodsListResponseDTO.builder()
                            .seckillGoodsList(dtoList)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("查询秒杀商品列表异常", e);
            return Response.<QuerySeckillGoodsListResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
