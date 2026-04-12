package com.yue.seckill.trigger.job;

import com.yue.seckill.domain.activity.adapter.repository.ISeckillActivityRepository;
import com.yue.seckill.domain.activity.model.valobj.SeckillStockVO;
import com.yue.seckill.domain.trade.adapter.port.ISeckillStockPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 服务启动时预热秒杀库存到 Redis（已禁用，改为通过管理后台手动触发预热）
 * 保留此类供参考，如需恢复自动预热可重新添加 @Component
 */
@Slf4j
// @Component  -- 已禁用：启动时自动预热改为管理后台手动触发
public class SeckillStockPreheatJob implements CommandLineRunner {

    @Resource
    private ISeckillActivityRepository seckillActivityRepository;
    @Resource
    private ISeckillStockPort seckillStockPort;

    @Override
    public void run(String... args) {
        try {
            List<SeckillStockVO> stockList = seckillActivityRepository.querySeckillStockList();
            if (stockList == null || stockList.isEmpty()) {
                log.info("秒杀库存预热完成，无可预热活动");
                return;
            }

            int preheatCount = 0;
            for (SeckillStockVO stock : stockList) {
                Integer remainCount = stock.getRemainCount() == null ? 0 : stock.getRemainCount();
                seckillStockPort.preloadStock(stock.getActivityId(), stock.getGoodsId(), remainCount);
                preheatCount++;
            }

            log.info("秒杀库存预热完成，总条数:{}", preheatCount);
        } catch (Exception e) {
            log.error("秒杀库存预热失败，应用继续启动，请检查 Redis 连接", e);
        }
    }

}
