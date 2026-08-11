-- ============================================================
-- dyshop 购物程序 —— 数据库初始化脚本（架构/数据模型设计阶段）
-- 目标：MySQL 8.0+
-- 执行：mysql -u root -p < sql/schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS dyshop
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE dyshop;

-- ------------------------------------------------------------
-- 用户表
-- role: 0=普通用户(买家)  1=管理员
-- status: 0=正常  1=禁用
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`      VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`      VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    `nickname`      VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`        VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`         VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `role`          TINYINT      NOT NULL DEFAULT 0 COMMENT '角色: 0普通用户 1管理员',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1禁用',
    `points`        INT          NOT NULL DEFAULT 0 COMMENT '积分余额(ch09)',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT = '用户表';

-- ------------------------------------------------------------
-- 商品分类表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类ID(0为顶级)',
    `name`        VARCHAR(50)  NOT NULL COMMENT '分类名称',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序值(越小越靠前)',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB COMMENT = '商品分类表';

-- ------------------------------------------------------------
-- 商品表
-- status: 0=下架  1=上架
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_id`    BIGINT        NOT NULL COMMENT '分类ID',
    `name`           VARCHAR(100)  NOT NULL COMMENT '商品名称',
    `subtitle`       VARCHAR(200)  DEFAULT NULL COMMENT '副标题/卖点',
    `main_image`     VARCHAR(500)  DEFAULT NULL COMMENT '主图URL',
    `images`         VARCHAR(2000) DEFAULT NULL COMMENT '轮播图URL(逗号分隔)',
    `detail`         TEXT          COMMENT '商品详情(富文本/HTML)',
    `specs`          TEXT          COMMENT '规格维度定义(JSON数组)，NULL=无规格商品，结构见 docs/ch04/spec-sku-selector.md',
    `skus`           TEXT          COMMENT 'SKU列表(JSON数组)',
    `price`          DECIMAL(10,2) NOT NULL COMMENT '售价',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价(划线价)',
    `vip_price`      DECIMAL(10,2) DEFAULT NULL COMMENT '会员专享价(ch09)',
    `stock`          INT           NOT NULL DEFAULT 0 COMMENT '库存',
    `sales`          INT           NOT NULL DEFAULT 0 COMMENT '销量',
    `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0下架 1上架',
    `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT = '商品表';

-- ------------------------------------------------------------
-- 购物车表（唯一键保证同一商品的同一 SKU 只存一行，加入时累加数量；
-- sku_id=0 表示无规格商品）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cart_item` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT        NOT NULL COMMENT '用户ID',
    `product_id`  BIGINT        NOT NULL COMMENT '商品ID',
    `sku_id`      BIGINT        NOT NULL DEFAULT 0 COMMENT '规格SKU ID: 0=无规格',
    `spec_text`   VARCHAR(200)  DEFAULT NULL COMMENT '规格展示快照',
    `quantity`    INT           NOT NULL DEFAULT 1 COMMENT '数量',
    `checked`     TINYINT       NOT NULL DEFAULT 1 COMMENT '是否勾选: 0否 1是',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product_sku` (`user_id`, `product_id`, `sku_id`)
) ENGINE = InnoDB COMMENT = '购物车表';

-- ------------------------------------------------------------
-- 收藏表（unique 键防重复，ch03 新增）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `favorite` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `product_id`  BIGINT   NOT NULL COMMENT '商品ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE = InnoDB COMMENT = '收藏表';

-- ------------------------------------------------------------
-- 收货地址表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `address` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT      NOT NULL COMMENT '用户ID',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    `province`      VARCHAR(50) NOT NULL COMMENT '省',
    `city`          VARCHAR(50) NOT NULL COMMENT '市',
    `district`      VARCHAR(50) DEFAULT NULL COMMENT '区/县',
    `detail`        VARCHAR(200) NOT NULL COMMENT '详细地址',
    `is_default`    TINYINT     NOT NULL DEFAULT 0 COMMENT '是否默认: 0否 1是',
    `deleted`       TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '收货地址表';

-- ------------------------------------------------------------
-- 订单表（orders：order 为保留字故加 s）
-- status: 0=待支付  1=待发货  2=待收货  3=已完成  4=已取消
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`       VARCHAR(32)   NOT NULL COMMENT '订单号(业务唯一)',
    `user_id`        BIGINT        NOT NULL COMMENT '用户ID',
    `total_amount`   DECIMAL(10,2) NOT NULL COMMENT '订单总额',
    `pay_amount`     DECIMAL(10,2) NOT NULL COMMENT '应付金额',
    `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠总额(会员/券自动取优,ch11)',
    `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0待支付 1待发货 2待收货 3已完成 4已取消',
    -- 收货信息快照（下单时固化，防止地址后续变更影响订单）
    `receiver_name`  VARCHAR(50)   NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20)   NOT NULL COMMENT '收货人手机号',
    `receiver_addr`  VARCHAR(300)  NOT NULL COMMENT '收货地址',
    `remark`         VARCHAR(200)  DEFAULT NULL COMMENT '买家备注',
    `pay_time`       DATETIME      DEFAULT NULL COMMENT '支付时间',
    `ship_time`      DATETIME      DEFAULT NULL COMMENT '发货时间',
    `finish_time`    DATETIME      DEFAULT NULL COMMENT '完成时间',
    `cancel_time`    DATETIME      DEFAULT NULL COMMENT '取消时间',
    `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id_status` (`user_id`, `status`)
) ENGINE = InnoDB COMMENT = '订单表';

-- ------------------------------------------------------------
-- 订单明细表（商品信息快照，下单后商品改价/改名不影响历史订单）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_item` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`     BIGINT        NOT NULL COMMENT '订单ID',
    `product_id`   BIGINT        NOT NULL COMMENT '商品ID',
    `sku_id`       BIGINT        NOT NULL DEFAULT 0 COMMENT '规格SKU ID: 0=无规格',
    `spec_text`    VARCHAR(200)  DEFAULT NULL COMMENT '规格快照',
    `product_name` VARCHAR(100)  NOT NULL COMMENT '商品名称(快照)',
    `product_image` VARCHAR(500) DEFAULT NULL COMMENT '商品主图(快照)',
    `price`        DECIMAL(10,2) NOT NULL COMMENT '成交单价(快照)',
    `quantity`     INT           NOT NULL COMMENT '数量',
    `subtotal`     DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE = InnoDB COMMENT = '订单明细表';

-- ------------------------------------------------------------
-- 支付流水表（预留：模拟支付通道 MOCK）
-- status: 0=处理中  1=成功  2=失败/关闭
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `payment` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `payment_no`  VARCHAR(32)   NOT NULL COMMENT '支付流水号(业务唯一)',
    `order_id`    BIGINT        NOT NULL COMMENT '订单ID',
    `user_id`     BIGINT        NOT NULL COMMENT '用户ID',
    `amount`      DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `channel`     VARCHAR(20)   NOT NULL DEFAULT 'MOCK' COMMENT '支付渠道(当前为模拟)',
    `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0处理中 1成功 2失败',
    `paid_at`     DATETIME      DEFAULT NULL COMMENT '支付成功时间',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    KEY `idx_order_id` (`order_id`)
) ENGINE = InnoDB COMMENT = '支付流水表';

-- ------------------------------------------------------------
-- 会员等级配置表（ch09；后台可编辑门槛/折扣率/积分倍率，
-- 等级实时计算不落库：近12个月已完成订单 pay_amount 之和匹配门槛）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `member_level` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`          VARCHAR(20)   NOT NULL COMMENT '等级标识: NORMAL/SILVER/GOLD/DIAMOND',
    `name`          VARCHAR(20)   NOT NULL COMMENT '等级名称',
    `threshold`     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '近12个月消费门槛',
    `discount_rate` DECIMAL(4,2)  NOT NULL DEFAULT 1.00 COMMENT '订单折扣率: 0.98=98折',
    `point_rate`    DECIMAL(4,2)  NOT NULL DEFAULT 1.00 COMMENT '积分倍率',
    `sort`          INT           NOT NULL DEFAULT 0 COMMENT '排序(升序=等级从低到高)',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB COMMENT = '会员等级配置表';

-- ------------------------------------------------------------
-- 积分流水表（ch09；order_id 唯一索引兜底幂等，同订单只记一条）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `point_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `order_id`    BIGINT       DEFAULT NULL COMMENT '来源订单ID',
    `points`      INT          NOT NULL COMMENT '变动积分(正=获得)',
    `balance`     INT          NOT NULL COMMENT '变动后余额',
    `remark`      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order` (`order_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '积分流水表';

-- ------------------------------------------------------------
-- 优惠券模板表（ch11；后台配置，1 对 N 实例化 user_coupon）
-- type: REDUCE=立减型(满减/无门槛)   scope: ALL=全场 LIMITED=有限定
-- issue_type: CENTER=可领取 / MANUAL_ONLY=仅后台发放
-- valid_type: FIXED=固定起止 / AFTER_DAYS=领取后N天
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 用户持有券表（ch11）
-- status: 0=未使用 1=已使用 2=已过期  source: CENTER领取 / MANUAL发放
-- UNIQUE(user_id, template_id, source)：同款同人对同一来源最多 1 张
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 订单券快照表（ch11；order_id 唯一=一单一券，模板后改不影响历史订单）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 售后单表（ch12；按 order_item 行申请，order_item_id 唯一=防重复退款）
-- status: 0待处理 1退款中 2已退款 3已拒绝 4已取消
-- ------------------------------------------------------------
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
