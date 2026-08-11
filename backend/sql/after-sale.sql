-- ============================================================
-- dyshop 售后/退款模块 —— 开发库增量迁移（一次性执行）
-- 前置：既有开发库（含 orders / order_item 表）
-- 执行：docker exec -i mysql-dev mysql -uroot -proot --default-character-set=utf8mb4 dyshop < sql/after-sale.sql
-- 全新部署走 schema.sql
-- ============================================================

USE dyshop;

CREATE TABLE IF NOT EXISTS `after_sale` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `after_sale_no` VARCHAR(32)   NOT NULL COMMENT '售后单号(业务唯一)',
    `order_id`      BIGINT        NOT NULL COMMENT '来源订单ID',
    `order_item_id` BIGINT        NOT NULL COMMENT '商品行ID(唯一=同行仅可申请一次)',
    `user_id`       BIGINT        NOT NULL COMMENT '申请人',
    `product_id`    BIGINT        NOT NULL COMMENT '商品ID',
    `product_name`  VARCHAR(100)  NOT NULL COMMENT '商品名称快照',
    `product_image` VARCHAR(500)  DEFAULT NULL COMMENT '商品主图快照',
    `spec_text`     VARCHAR(200)  DEFAULT NULL COMMENT '规格快照',
    `quantity`      INT           NOT NULL DEFAULT 1 COMMENT '退款数量(=行数量)',
    `refund_amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额=成交单价×数量(自动)',
    `reason`        VARCHAR(200)  NOT NULL COMMENT '申请原因',
    `type`          VARCHAR(16)   NOT NULL DEFAULT 'ONLY_REFUND' COMMENT '售后类型(本期仅仅退款)',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0待处理 1退款中 2已退款 3已拒绝 4已取消',
    `reject_reason` VARCHAR(200)  DEFAULT NULL COMMENT '拒绝理由',
    `handle_time`   DATETIME      DEFAULT NULL COMMENT '审核时刻',
    `cancel_time`   DATETIME      DEFAULT NULL COMMENT '取消时刻',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_item_id` (`order_item_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT = '售后单表';
