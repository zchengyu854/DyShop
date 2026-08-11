-- ============================================================
-- dyshop 积分商城模块 —— 开发库增量迁移（一次性执行）
-- 前置：既有开发库（含 user.points / point_log / user_coupon）
-- 执行：docker exec -i mysql-dev mysql -uroot -proot --default-character-set=utf8mb4 dyshop < sql/points-mall.sql
-- 注意：必须带 --default-character-set=utf8mb4（中文注释/数据防 latin1 二次编码）
-- 全新部署走 schema.sql（本文件仅用于既有开发库升级）
-- ============================================================

USE dyshop;

-- ---------- 1. point_batch 积分批次表（12 个月有效期按批次核算） ----------
CREATE TABLE IF NOT EXISTS `point_batch` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `source_type` VARCHAR(16) NOT NULL COMMENT '来源: ORDER=订单发放 / LEGACY=存量迁移',
    `source_id`   BIGINT      DEFAULT NULL COMMENT '来源单号(订单ID)；LEGACY 存量批次为空',
    `points`      INT         NOT NULL COMMENT '本批次积分',
    `remaining`   INT         NOT NULL COMMENT '剩余可用积分',
    `expire_at`   DATETIME    NOT NULL COMMENT '到期时间(到账+12个月)',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_expire` (`user_id`, `expire_at`),
    KEY `idx_expire` (`expire_at`)
) ENGINE = InnoDB COMMENT = '积分批次表(ch13)';

-- ---------- 2. points_goods 积分商城商品表 ----------
CREATE TABLE IF NOT EXISTS `points_goods` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`               VARCHAR(64)   NOT NULL COMMENT '商品名',
    `cover_image`        VARCHAR(500)  DEFAULT NULL COMMENT '封面图',
    `description`        VARCHAR(500)  DEFAULT NULL COMMENT '描述',
    `goods_type`         VARCHAR(16)   NOT NULL COMMENT '类型: COUPON=发券 / CODE=兑换码',
    `point_cost`         INT           NOT NULL COMMENT '兑换所需积分(>0)',
    `stock`              INT           NOT NULL DEFAULT -1 COMMENT '库存, -1=不限',
    `limit_per_user`     INT           NOT NULL DEFAULT 0 COMMENT '每人限兑次数, 0=不限',
    `coupon_template_id` BIGINT        DEFAULT NULL COMMENT 'COUPON类关联优惠券模板',
    `status`             TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 1上架 0下架',
    `sort`               INT           NOT NULL DEFAULT 0 COMMENT '排序(升序)',
    `deleted`            TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    `create_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_sort` (`status`, `sort`)
) ENGINE = InnoDB COMMENT = '积分商城商品表(ch13)';

-- ---------- 3. points_exchange 兑换记录表 ----------
CREATE TABLE IF NOT EXISTS `points_exchange` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `exchange_no` VARCHAR(32)  NOT NULL COMMENT '兑换单号(业务唯一)',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `goods_id`    BIGINT       NOT NULL COMMENT '商品ID',
    `goods_name`  VARCHAR(64)  NOT NULL COMMENT '商品名快照',
    `goods_type`  VARCHAR(16)  NOT NULL COMMENT '快照类型 COUPON/CODE',
    `point_cost`  INT          NOT NULL COMMENT '快照消耗积分',
    `code`        VARCHAR(64)  DEFAULT NULL COMMENT 'CODE类型兑换码(唯一)',
    `coupon_id`   BIGINT       DEFAULT NULL COMMENT 'COUPON类型发放的 user_coupon.id',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exchange_no` (`exchange_no`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE = InnoDB COMMENT = '积分兑换记录表(ch13)';
