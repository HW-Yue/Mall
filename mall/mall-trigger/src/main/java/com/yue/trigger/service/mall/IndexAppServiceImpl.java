package com.yue.trigger.service.mall;

import com.yue.api.dto.*;
import com.yue.api.response.Response;
import com.yue.domain.noMarket.detail.model.entity.SkuDetailEntity;
import com.yue.domain.noMarket.detail.service.ISkuDetailService;
import com.yue.infrastructure.dao.ICategoryDao;
import com.yue.infrastructure.dao.ISkuDao;
import com.yue.infrastructure.dao.po.Category;
import com.yue.infrastructure.dao.po.Sku;
import com.yue.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IndexAppServiceImpl {

    @Resource
    private ICategoryDao categoryDao;
    @Resource
    private ISkuDao skuDao;
    @Resource
    private ISkuDetailService skuDetailService;


    public Response<List<CategoryTypeDTO>> queryCategoryTypeList() {
        try {
            log.info("查询商品分类列表");
            List<Category> categories = categoryDao.queryCategoryList();
            List<CategoryTypeDTO> list = categories.stream()
                    .map(c -> CategoryTypeDTO.builder()
                            .id(c.getId())
                            .name(c.getName())
                            .iconUrl(c.getIconUrl())
                            .sortOrder(c.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
            log.info("查询商品分类列表完成 size:{}", list.size());
            return Response.<List<CategoryTypeDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(list)
                    .build();
        } catch (Exception e) {
            log.error("查询商品类型列表失败", e);
            return Response.<List<CategoryTypeDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }


    public Response<GoodsPageResponseDTO> queryGoodsPage(GoodsPageRequestDTO request) {
        try {
            int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
            int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : Math.min(request.getPageSize(), 100);
            int offset = (pageNum - 1) * pageSize;
            log.info("分页查询商品 categoryId:{} pageNum:{} pageSize:{}", request.getCategoryId(), pageNum, pageSize);

            int total = skuDao.countByCategoryId(request.getCategoryId());
            List<Sku> skuList = skuDao.querySkuPageByCategoryId(request.getCategoryId(), offset, pageSize);

            List<GoodsItemDTO> list = skuList.stream()
                    .map(s -> GoodsItemDTO.builder()
                            .id(s.getGoodsId())
                            .imageUrl(s.getGoodsImageUrl())
                            .name(s.getGoodsName())
                            .price(s.getOriginalPrice())
                            .build())
                    .collect(Collectors.toList());

            GoodsPageResponseDTO data = GoodsPageResponseDTO.builder()
                    .total((long) total)
                    .list(list)
                    .build();
            log.info("分页查询商品完成 categoryId:{} total:{} 本页条数:{}", request.getCategoryId(), total, list.size());
            return Response.<GoodsPageResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(data)
                    .build();
        } catch (Exception e) {
            log.error("分页查询商品失败 request:{}", request, e);
            return Response.<GoodsPageResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    public Response<GoodsDetailResponseDTO> querySkuDetail(GoodsDetailRequestDTO requestDTO) {
        try {
            if (requestDTO == null || requestDTO.getGoodsId() == null) {
                log.info("查询商品详情 参数非法 goodsId为空");
                return Response.<GoodsDetailResponseDTO>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("商品ID不能为空")
                        .build();
            }
            log.info("查询商品详情 goodsId:{}", requestDTO.getGoodsId());
            SkuDetailEntity entity = skuDetailService.querySkuDetail(requestDTO.getGoodsId());
            if (entity == null) {
                log.info("查询商品详情 商品不存在 goodsId:{}", requestDTO.getGoodsId());
                return Response.<GoodsDetailResponseDTO>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("商品不存在")
                        .build();
            }

            GoodsDetailResponseDTO data = new GoodsDetailResponseDTO();
            data.setId(entity.getId());
            data.setImageUrl(entity.getImageUrl());
            data.setName(entity.getName());
            data.setPrice(entity.getPrice());
            data.setGoodsDetail(entity.getGoodsDetail());

            log.info("查询商品详情成功 goodsId:{}", requestDTO.getGoodsId());
            return Response.<GoodsDetailResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(data)
                    .build();
        } catch (Exception e) {
            log.error("查询普通商品详情失败 request:{}", requestDTO, e);
            return Response.<GoodsDetailResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }


    public Response<ActivityGoodsDTO> queryActivityGoods() {
        // TODO: sc_sku_activity / group_buy_activity / seckill_activity 已迁移至各自微服务 DB
        // 此处暂返回空列表，后续通过 Feign 调用 group-buy-service / seckill-service 获取活动商品
        log.info("查询活动商品 当前返回空列表(待对接拼团/秒杀服务)");
        ActivityGoodsDTO data = ActivityGoodsDTO.builder()
                .groupBuyList(new ArrayList<>())
                .seckillList(new ArrayList<>())
                .build();
        return Response.<ActivityGoodsDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }
}
