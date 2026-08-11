package com.dyshop.api.config;

import com.dyshop.api.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 待支付订单超时自动取消定时任务。
 * <p>
 * 后台兜底（前端 15 分钟倒计时仅是 UX 层）：每 60s 扫描一次，
 * 将「待支付且超过 payDeadline（创建时间 + 15 分钟）」的订单置为已取消并回补库存。
 * 单实例部署用 @Scheduled 足够；多实例部署需加分布式锁（如 Redis）防重复处理，
 * 且 {@code expireTimeoutOrders} 内部已用条件更新保证幂等（重复触发不双倍回补）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderServiceImpl orderService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void expireTimeoutOrders() {
        int count = orderService.expireTimeoutOrders();
        if (count > 0) {
            log.info("超时订单自动取消完成，共处理 {} 单（库存已回补）", count);
        }
    }
}
