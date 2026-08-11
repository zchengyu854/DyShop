-- ============================================================
-- dyshop 会员模块种子数据（ch09）
-- 前置：先执行 schema.sql（member_level 表已建）
-- 执行：mysql --default-character-set=utf8mb4 -u root -p dyshop < sql/seed-member.sql
-- 注意：重复执行前先 TRUNCATE member_level
-- ============================================================

USE dyshop;

-- ---------- 会员等级（默认 4 级，后台可改门槛/折扣率/积分倍率） ----------
TRUNCATE TABLE `member_level`;
INSERT INTO `member_level` (`code`, `name`, `threshold`, `discount_rate`, `point_rate`, `sort`) VALUES
('NORMAL',  '普通',  0,       1.00, 1.0, 1),
('SILVER',  '银卡',  2000.00, 0.98, 1.0, 2),
('GOLD',    '金卡',  5000.00, 0.95, 1.5, 3),
('DIAMOND', '钻石',  10000.00, 0.90, 2.0, 4);

-- ---------- 商品会员专享价（ch09 演示数据） ----------
-- 保温杯/音箱 设置会员专享价（普通价 129/259 → 会员价 109/219）
UPDATE `product` SET `vip_price` = 109.00 WHERE `id` = 4;
UPDATE `product` SET `vip_price` = 219.00 WHERE `id` = 3;
