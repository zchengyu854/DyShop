-- ============================================================
-- dyshop 优惠券模块 —— 开发库增量迁移（一次性执行）
-- 前置：既有开发库（含 orders 表旧结构）
-- 执行：docker exec -i mysql-dev mysql -uroot -proot --default-character-set=utf8mb4 dyshop < sql/coupon.sql
-- 注意：必须带 --default-character-set=utf8mb4（中文注释/数据防 latin1 二次编码）
-- 全新部署走 schema.sql（本文件仅用于既有开发库升级）
-- ============================================================

USE dyshop;

-- ---------- 1. coupon_template 券模板 ----------
CREATE TABLE IF NOT EXISTS `coupon_template` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`            VARCHAR(64)   NOT NULL COMMENT '模板名(展示)',
    `type`            VARCHAR(16)   NOT NULL DEFAULT 'REDUCE' COMMENT '券类型: REDUCE=立减型(满减/无门槛)，预留 DISCOUNT',
    `min_amount`      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '满减门槛, 0=无门槛',
    `discount_amount` DECIMAL(10,2) NOT NULL COMMENT '立减金额(>0)',
    `scope`           VARCHAR(16)   NOT NULL DEFAULT 'ALL' COMMENT '适用范围: ALL=全场 / LIMITED=有限定',
    `category_ids`    JSON          DEFAULT NULL COMMENT '指定分类id数组(LIMITED时与product_ids并集生效)',
    `product_ids`     JSON          DEFAULT NULL COMMENT '指定商品id数组',
    `allow_stack`     TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否可与会员折扣叠加: 1是 0否',
    `issue_type`      VARCHAR(16)   NOT NULL DEFAULT 'CENTER' COMMENT '发放渠道: CENTER=领券中心 / MANUAL_ONLY=仅后台发放',
    `valid_type`      VARCHAR(16)   NOT NULL DEFAULT 'FIXED' COMMENT '有效期类型: FIXED=固定起止 / AFTER_DAYS=领取后N天',
    `start_at`        DATETIME      DEFAULT NULL COMMENT 'FIXED生效开始(可空=长期)',
    `end_at`          DATETIME      DEFAULT NULL COMMENT 'FIXED生效结束(可空=长期)',
    `valid_days`      INT           NOT NULL DEFAULT 0 COMMENT 'AFTER_DAYS领取后有效天数(0=长期)',
    `total_quantity`  INT           NOT NULL DEFAULT -1 COMMENT '可发放总量, -1=不限',
    `per_user`        INT           NOT NULL DEFAULT 1 COMMENT '每人限领张数',
    `issued_count`    INT           NOT NULL DEFAULT 0 COMMENT '已发放量(领取+发放)',
    `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    `create_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT = '优惠券模板表';

-- ---------- 2. user_coupon 用户持有券 ----------
CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT      NOT NULL COMMENT '持券人',
    `template_id`   BIGINT      NOT NULL COMMENT '来源模板',
    `status`        TINYINT     NOT NULL DEFAULT 0 COMMENT '状态: 0未使用 1已使用 2已过期',
    `source`        VARCHAR(16) NOT NULL DEFAULT 'CENTER' COMMENT '来源: CENTER领取 / MANUAL发放',
    `used_order_id` BIGINT      DEFAULT NULL COMMENT '占用订单ID(下单写入, 回退置空)',
    `received_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取/发放时刻',
    `expire_at`     DATETIME    DEFAULT NULL COMMENT '有效期到期(FIXED复制end_at / AFTER_DAYS=领取+valid_days)',
    `used_at`       DATETIME    DEFAULT NULL COMMENT '使用时刻',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_template_source` (`user_id`, `template_id`, `source`),
    KEY `idx_used_order_id` (`used_order_id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE = InnoDB COMMENT = '用户持有券表';

-- ---------- 3. order_coupon 订单券快照 ----------
CREATE TABLE IF NOT EXISTS `order_coupon` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`        BIGINT        NOT NULL COMMENT '订单ID(唯一=一单一券)',
    `user_coupon_id`  BIGINT        NOT NULL COMMENT '消费的持有券实例',
    `template_id`     BIGINT        NOT NULL COMMENT '模板冗余',
    `template_name`   VARCHAR(64)   NOT NULL COMMENT '名称快照(模板后改不影响历史)',
    `scope`           VARCHAR(16)   NOT NULL DEFAULT 'ALL' COMMENT '范围快照',
    `category_ids`    JSON          DEFAULT NULL COMMENT '分类范围快照',
    `product_ids`     JSON          DEFAULT NULL COMMENT '商品范围快照',
    `discount_amount` DECIMAL(10,2) NOT NULL COMMENT '实际抵扣额',
    `used_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单扣券时刻',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_user_coupon_id` (`user_coupon_id`)
) ENGINE = InnoDB COMMENT = '订单券快照表';

-- ---------- 4. orders 增加 discount_amount（幂等：已存在则跳过） ----------
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = 'dyshop' AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'discount_amount');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT ''券优惠总额'' AFTER pay_amount',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
