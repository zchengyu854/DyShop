package com.dyshop.api.config;

import com.dyshop.api.service.impl.PointsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 积分过期定时任务（ch13）。
 * <p>
 * 积分自到账日起 12 个月过期：每日 02:00 扫描所有 remaining&gt;0 且已到期的
 * 积分批次，逐用户清零并扣减 user.points、写「积分过期」流水。
 * 幂等：仅 remaining&gt;0 的批次被处理，重复执行 0 副作用；
 * 单实例 @Scheduled 足够，多实例需加分布式锁。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpireScheduler {

    private final PointsServiceImpl pointsService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void expireOverduePoints() {
        int count = pointsService.expireOverdueBatches();
        if (count > 0) {
            log.info("积分过期清零完成，共处理 {} 个用户（已写过期流水）", count);
        }
    }
}