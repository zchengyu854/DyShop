-- ============================================================
-- dyshop 积分商城 —— 存量积分补批次迁移（幂等，可重复执行）
-- 背景：ch13 之前的存量积分只写 user.points / point_log，无 point_batch；
--       兑换按批次 FIFO 扣减，无批次时余额充足也会提示「积分余额不足」（事务回滚）。
-- 方案：对每个用户补一条 LEGACY 批次 = user.points - 已有批次 remaining 之和
--       （差额 > 0 才补），expire_at = 现在 + 12 个月；
--       重复执行先删 LEGACY 再重建，结果收敛。
-- 执行：docker exec -i mysql-dev mysql -uroot -proot --default-character-set=utf8mb4 dyshop < sql/points-mall-legacy-batch.sql
-- 验收：修复后任意用户 user.points == SUM(未过期批次 remaining)
-- ============================================================

USE dyshop;

-- 1. point_batch.source_id 放开可空（LEGACY 批次无来源单号；已执行过则跳过）
ALTER TABLE `point_batch`
    MODIFY COLUMN `source_id` BIGINT DEFAULT NULL COMMENT '来源单号(订单ID)；LEGACY 存量批次为空';

-- 2. 幂等重建：先清 LEGACY，再按差额补
DELETE FROM `point_batch` WHERE `source_type` = 'LEGACY';

INSERT INTO `point_batch` (`user_id`, `source_type`, `source_id`, `points`, `remaining`, `expire_at`)
SELECT u.`id`, 'LEGACY', NULL,
       u.`points` - COALESCE(b.`total_remaining`, 0),
       u.`points` - COALESCE(b.`total_remaining`, 0),
       DATE_ADD(NOW(), INTERVAL 12 MONTH)
FROM `user` u
LEFT JOIN (
    SELECT `user_id`, SUM(`remaining`) AS `total_remaining`
    FROM `point_batch`
    GROUP BY `user_id`
) b ON b.`user_id` = u.`id`
WHERE u.`points` > 0
  AND u.`points` - COALESCE(b.`total_remaining`, 0) > 0;

-- 3. 一致性核对（应输出 0 行：无「余额 > 批次和」的用户）
SELECT u.`id`, u.`username`, u.`points` AS user_points, COALESCE(b.`s`, 0) AS batch_remaining
FROM `user` u
LEFT JOIN (
    SELECT `user_id`, SUM(`remaining`) AS `s`
    FROM `point_batch`
    GROUP BY `user_id`
) b ON b.`user_id` = u.`id`
WHERE u.`points` > COALESCE(b.`s`, 0);