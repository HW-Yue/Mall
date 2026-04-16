package com.yue.groupbuy.domain.activity.service.discount;

import com.yue.groupbuy.domain.activity.adapter.repository.IActivityRepository;
import com.yue.groupbuy.domain.activity.model.valobj.DiscountTypeEnum;
import com.yue.groupbuy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
public abstract class AbstractDiscountCalculateService implements IDiscountCalculateService {

    @Resource
    protected IActivityRepository repository;

    @Override
    public BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        // 1. 人群标签过滤
        if (groupBuyDiscount.getDiscountType() != null && DiscountTypeEnum.TAG.equals(groupBuyDiscount.getDiscountType())) {
            boolean isCrowdRange = repository.isTagCrowdRange(groupBuyDiscount.getTagId(), userId);
            if (!isCrowdRange) {
                log.info("折扣优惠计算拦截，用户不在优惠人群标签范围内 userId:{}", userId);
                return originalPrice;
            }
        }
        // 2. 折扣优惠计算
        return doCalculate(originalPrice, groupBuyDiscount);
    }

    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);

}
