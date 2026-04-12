package com.yue.infrastructure.dao;

import com.yue.infrastructure.dao.po.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 商品查询
 * @create 2024-12-21 10:48
 */
@Mapper
public interface ISkuDao {

    Sku querySkuByGoodsId(String goodsId);

    List<Sku> querySkuList();

    /** 按类目统计数量，categoryId 为 null 时查全部 */
    int countByCategoryId(@Param("categoryId") Integer categoryId);

    /** 按类目分页查询，categoryId 为 null 时查全部，按 id 降序 */
    List<Sku> querySkuPageByCategoryId(@Param("categoryId") Integer categoryId,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    int insert(Sku po);

    int update(Sku po);

    int deleteByGoodsId(String goodsId);

    /**
     * 按商品ID加悲观锁查询（FOR UPDATE），用于锁单时保证可售库存计算一致
     */
    Sku selectByGoodsIdForUpdate(@Param("goodsId") String goodsId);

    /**
     * 增加锁单量（在悲观锁事务内调用）
     *
     * @param goodsId 商品ID
     * @param count   增加数量，通常为1
     * @return 更新行数
     */
    int addLockedStock(@Param("goodsId") String goodsId, @Param("count") int count);

    /**
     * 扣减锁单量（结算完成时调用，锁单转成交）
     *
     * @param goodsId 商品ID
     * @param count   扣减数量，通常为1
     * @return 更新行数
     */
    int subLockedStock(@Param("goodsId") String goodsId, @Param("count") int count);
}
