-- ============================================================
-- dyshop 规格/SKU 模块 —— 开发库增量迁移（一次性执行）
-- 前置：后端已用过 schema.sql 初始化的库（含旧结构 cart_item/order_item）
-- 执行：docker exec -i mysql-dev mysql -uroot -proot --default-character-set=utf8mb4 dyshop < sql/alter-sku.sql
-- 注意：必须带 --default-character-set=utf8mb4 且经文件重定向执行，否则中文会被 latin1 二次编码入库（2026-08-05 踩坑记录）
-- 全新部署走 schema.sql，本文件仅用于既有开发库升级。
-- ============================================================

USE dyshop;

-- ---------- 1. product：新增规格维度定义 + SKU 列表 ----------
ALTER TABLE `product`
    ADD COLUMN `specs` TEXT NULL COMMENT '规格维度定义(JSON数组)，NULL=无规格商品' AFTER `detail`,
    ADD COLUMN `skus`  TEXT NULL COMMENT 'SKU列表(JSON数组)' AFTER `specs`;

-- ---------- 2. cart_item：SKU 穿透 + 唯一键升级 ----------
ALTER TABLE `cart_item`
    ADD COLUMN `sku_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '规格SKU ID: 0=无规格' AFTER `product_id`,
    ADD COLUMN `spec_text` VARCHAR(200) DEFAULT NULL COMMENT '规格展示快照' AFTER `sku_id`,
    DROP INDEX `uk_user_product`,
    ADD UNIQUE KEY `uk_user_product_sku` (`user_id`, `product_id`, `sku_id`);

-- ---------- 3. order_item：下单规格快照 ----------
ALTER TABLE `order_item`
    ADD COLUMN `sku_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '规格SKU ID: 0=无规格' AFTER `product_id`,
    ADD COLUMN `spec_text` VARCHAR(200) DEFAULT NULL COMMENT '规格快照' AFTER `sku_id`;

-- ============================================================
-- 种子数据：耳机/手表按规格重写，新增笔记本（4 维联动演示）
-- ============================================================

-- 1 降噪无线耳机：颜色(3) × 版本(2) = 6 组合；
--   「白色/无线充电版」售罄(stock=0 置灰)、「蓝色/无线充电版」空缺(无效组合置灰)
UPDATE `product` SET
    `price` = 399.00, `original_price` = 549.00,
    `specs` = '[{"name":"颜色","values":["黑色","白色","蓝色"]},{"name":"版本","values":["标准版","无线充电版"]}]',
    `skus`  = '[{"id":101,"specs":{"颜色":"黑色","版本":"标准版"},"price":399.00,"originalPrice":499.00,"stock":70,"image":null},
                {"id":102,"specs":{"颜色":"黑色","版本":"无线充电版"},"price":449.00,"originalPrice":549.00,"stock":30,"image":null},
                {"id":103,"specs":{"颜色":"白色","版本":"标准版"},"price":399.00,"originalPrice":499.00,"stock":60,"image":null},
                {"id":104,"specs":{"颜色":"白色","版本":"无线充电版"},"price":449.00,"originalPrice":549.00,"stock":0,"image":null},
                {"id":105,"specs":{"颜色":"蓝色","版本":"标准版"},"price":399.00,"originalPrice":499.00,"stock":40,"image":null}]'
  WHERE `id` = 1;

-- 2 智能运动手表：表带(3) × 尺寸(2) = 6 组合，价格随规格变化（真皮/不锈钢加价）
UPDATE `product` SET
    `price` = 899.00, `original_price` = 1549.00,
    `specs` = '[{"name":"表带","values":["硅胶","真皮","不锈钢"]},{"name":"尺寸","values":["40mm","44mm"]}]',
    `skus`  = '[{"id":201,"specs":{"表带":"硅胶","尺寸":"40mm"},"price":899.00,"originalPrice":1099.00,"stock":20,"image":null},
                {"id":202,"specs":{"表带":"硅胶","尺寸":"44mm"},"price":949.00,"originalPrice":1149.00,"stock":15,"image":null},
                {"id":203,"specs":{"表带":"真皮","尺寸":"40mm"},"price":1199.00,"originalPrice":1399.00,"stock":12,"image":null},
                {"id":204,"specs":{"表带":"真皮","尺寸":"44mm"},"price":1249.00,"originalPrice":1449.00,"stock":10,"image":null},
                {"id":205,"specs":{"表带":"不锈钢","尺寸":"40mm"},"price":1299.00,"originalPrice":1499.00,"stock":13,"image":null},
                {"id":206,"specs":{"表带":"不锈钢","尺寸":"44mm"},"price":1349.00,"originalPrice":1549.00,"stock":10,"image":null}]'
  WHERE `id` = 2;

-- 11 轻薄笔记本电脑（新增）：型号(2) × 显存(2) × 内存(2) × 颜色(2) = 16 组合
--   演示点：Air+32G 无效（显存 32GB 仅 Pro 可选）；Pro/深空灰/1TB 售罄(stock=0)；
--   颜色维度 SKU 带图（主图联动）。
INSERT INTO `product` (`id`, `category_id`, `name`, `subtitle`, `main_image`, `images`, `detail`,
                       `specs`, `skus`, `price`, `original_price`, `stock`, `sales`, `status`)
VALUES (11, 1, '轻薄笔记本电脑', 'M 系列芯片 视网膜屏幕 全天候续航',
        'https://picsum.photos/seed/product11/600/600',
        'https://picsum.photos/seed/product11a/600/600,https://picsum.photos/seed/product11b/600/600',
        '<p>M 系列芯片，视网膜屏幕，静音无风扇设计，全天候续航。</p>',
        '[{"name":"型号","values":["MacBook Air","MacBook Pro"]},{"name":"显存","values":["16GB","32GB"]},{"name":"内存","values":["512GB","1TB"]},{"name":"颜色","values":["深空灰","银色"]}]',
        '[{"id":1101,"specs":{"型号":"MacBook Air","显存":"16GB","内存":"512GB","颜色":"深空灰"},"price":7999.00,"originalPrice":9999.00,"stock":6,"image":"https://picsum.photos/seed/mbp-gray/600/600"},
          {"id":1102,"specs":{"型号":"MacBook Air","显存":"16GB","内存":"512GB","颜色":"银色"},"price":7999.00,"originalPrice":9999.00,"stock":6,"image":"https://picsum.photos/seed/mbp-silver/600/600"},
          {"id":1103,"specs":{"型号":"MacBook Air","显存":"16GB","内存":"1TB","颜色":"深空灰"},"price":8999.00,"originalPrice":10999.00,"stock":5,"image":"https://picsum.photos/seed/mbp-gray/600/600"},
          {"id":1104,"specs":{"型号":"MacBook Air","显存":"16GB","内存":"1TB","颜色":"银色"},"price":8999.00,"originalPrice":10999.00,"stock":5,"image":"https://picsum.photos/seed/mbp-silver/600/600"},
          {"id":1105,"specs":{"型号":"MacBook Pro","显存":"16GB","内存":"512GB","颜色":"深空灰"},"price":11999.00,"originalPrice":13999.00,"stock":4,"image":"https://picsum.photos/seed/mbp-gray/600/600"},
          {"id":1106,"specs":{"型号":"MacBook Pro","显存":"16GB","内存":"512GB","颜色":"银色"},"price":11999.00,"originalPrice":13999.00,"stock":4,"image":"https://picsum.photos/seed/mbp-silver/600/600"},
          {"id":1107,"specs":{"型号":"MacBook Pro","显存":"16GB","内存":"1TB","颜色":"深空灰"},"price":12999.00,"originalPrice":14999.00,"stock":3,"image":"https://picsum.photos/seed/mbp-gray/600/600"},
          {"id":1108,"specs":{"型号":"MacBook Pro","显存":"16GB","内存":"1TB","颜色":"银色"},"price":12999.00,"originalPrice":14999.00,"stock":3,"image":"https://picsum.photos/seed/mbp-silver/600/600"},
          {"id":1109,"specs":{"型号":"MacBook Pro","显存":"32GB","内存":"512GB","颜色":"深空灰"},"price":13999.00,"originalPrice":15999.00,"stock":3,"image":"https://picsum.photos/seed/mbp-gray/600/600"},
          {"id":1110,"specs":{"型号":"MacBook Pro","显存":"32GB","内存":"512GB","颜色":"银色"},"price":13999.00,"originalPrice":15999.00,"stock":3,"image":"https://picsum.photos/seed/mbp-silver/600/600"},
          {"id":1111,"specs":{"型号":"MacBook Pro","显存":"32GB","内存":"1TB","颜色":"深空灰"},"price":14999.00,"originalPrice":16999.00,"stock":0,"image":"https://picsum.photos/seed/mbp-gray/600/600"},
          {"id":1112,"specs":{"型号":"MacBook Pro","显存":"32GB","内存":"1TB","颜色":"银色"},"price":14999.00,"originalPrice":16999.00,"stock":2,"image":"https://picsum.photos/seed/mbp-silver/600/600"}]',
        7999.00, 16999.00, 44, 0, 1);