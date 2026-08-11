-- ============================================================
-- dyshop 种子数据 —— 客户端首页模块手测用
-- 前置：先执行 schema.sql 建库建表
-- 执行：mysql -u root -p dyshop < sql/data.sql
-- ============================================================

USE dyshop;

-- ---------- 分类（3 个，均启用） ----------
INSERT INTO `category` (`id`, `parent_id`, `name`, `sort`, `status`) VALUES
(1, 0, '手机数码', 1, 1),
(2, 0, '家居生活', 2, 1),
(3, 0, '食品生鲜', 3, 1);

-- ---------- 商品（10 个：8 上架 status=1，2 下架 status=0） ----------
INSERT INTO `product`
(`id`, `category_id`, `name`, `subtitle`, `main_image`, `images`, `detail`, `price`, `original_price`, `stock`, `sales`, `status`)
VALUES
(1, 1, '降噪无线耳机', '主动降噪 30 小时续航', 'https://picsum.photos/seed/product1/600/600', 'https://picsum.photos/seed/product1a/600/600,https://picsum.photos/seed/product1b/600/600',
 '<p>轻量佩戴，ANC 主动降噪，蓝牙 5.3 稳定连接。</p>', 399.00, 499.00, 200, 156, 1),
(2, 1, '智能运动手表', '血氧心率监测 14 天长续航', 'https://picsum.photos/seed/product2/600/600', 'https://picsum.photos/seed/product2a/600/600,https://picsum.photos/seed/product2b/600/600',
 '<p>1.43 英寸 AMOLED 屏幕，100+ 运动模式。</p>', 899.00, 1099.00, 80, 89, 1),
(3, 1, '便携蓝牙音箱', 'IPX7 防水 360° 环绕声', 'https://picsum.photos/seed/product3/600/600', 'https://picsum.photos/seed/product3a/600/600',
 '<p>小巧便携，户外露营好伴侣。</p>', 259.00, 299.00, 150, 210, 1),
(4, 2, '316L 不锈钢保温杯', '12 小时长效保温', 'https://picsum.photos/seed/product4/600/600', 'https://picsum.photos/seed/product4a/600/600',
 '<p>食品级 316L 不锈钢内胆，一键弹盖。</p>', 129.00, 159.00, 500, 342, 1),
(5, 2, '天然大豆香薰蜡烛', '雪松木香 40 小时燃烧', 'https://picsum.photos/seed/product5/600/600', 'https://picsum.photos/seed/product5a/600/600',
 '<p>手工灌装，纯棉烛芯，低烟无黑。</p>', 89.00, 119.00, 300, 178, 1),
(6, 2, '奶油风懒人沙发', '可拆洗 加厚填充', 'https://picsum.photos/seed/product6/600/600', 'https://picsum.photos/seed/product6a/600/600,https://picsum.photos/seed/product6b/600/600',
 '<p>高回弹海绵，云朵般包裹感，客厅卧室皆宜。</p>', 599.00, 799.00, 60, 45, 1),
(7, 3, '埃塞俄比亚手冲咖啡豆', '日晒耶加雪菲 250g', 'https://picsum.photos/seed/product7/600/600', 'https://picsum.photos/seed/product7a/600/600',
 '<p>浅度烘焙，花香与柑橘风味，现磨更佳。</p>', 79.00, 99.00, 400, 267, 1),
(8, 3, '每日坚果混合礼盒', '30 袋独立小包', 'https://picsum.photos/seed/product8/600/600', 'https://picsum.photos/seed/product8a/600/600',
 '<p>六种坚果三种果干，科学配比，锁鲜小包。</p>', 109.00, 139.00, 350, 523, 1),
(9, 2, '复古黄铜台灯', '暖光三档调光（已下架示例）', 'https://picsum.photos/seed/product9/600/600', 'https://picsum.photos/seed/product9a/600/600',
 '<p>复古工业风，书房卧室氛围灯。</p>', 199.00, 249.00, 0, 30, 0),
(10, 1, '智能运动手环', '防水彩屏（已下架示例）', 'https://picsum.photos/seed/product10/600/600', 'https://picsum.photos/seed/product10a/600/600',
 '<p>1.1 英寸彩屏，50 米防水，14 天续航。</p>', 159.00, 199.00, 0, 88, 0);

-- ---------- 规格/SKU 维度（docs/ch04/spec-sku-selector.md） ----------
-- 1 降噪无线耳机：颜色(3) × 版本(2)；「白色/无线充电版」售罄、「蓝色/无线充电版」空缺（无效组合置灰）
UPDATE `product` SET
    `price` = 399.00, `original_price` = 549.00,
    `specs` = '[{"name":"颜色","values":["黑色","白色","蓝色"]},{"name":"版本","values":["标准版","无线充电版"]}]',
    `skus`  = '[{"id":101,"specs":{"颜色":"黑色","版本":"标准版"},"price":399.00,"originalPrice":499.00,"stock":70,"image":null},
                {"id":102,"specs":{"颜色":"黑色","版本":"无线充电版"},"price":449.00,"originalPrice":549.00,"stock":30,"image":null},
                {"id":103,"specs":{"颜色":"白色","版本":"标准版"},"price":399.00,"originalPrice":499.00,"stock":60,"image":null},
                {"id":104,"specs":{"颜色":"白色","版本":"无线充电版"},"price":449.00,"originalPrice":549.00,"stock":0,"image":null},
                {"id":105,"specs":{"颜色":"蓝色","版本":"标准版"},"price":399.00,"originalPrice":499.00,"stock":40,"image":null}]'
  WHERE `id` = 1;

-- 2 智能运动手表：表带(3) × 尺寸(2)，价格随规格变化（真皮/不锈钢加价）
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
--   Air+32G 无效（显存 32GB 仅 Pro）；Pro/深空灰/1TB 售罄；颜色维度 SKU 带图（主图联动）
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

-- ---------- 管理员账号（ch08 后台管理；BCrypt 密码 admin123，验签已确认） ----------
-- 密码哈希：$2y$10$O8lrwhiRIzxU8LT9A49Gve4VeaLR0Oah.Yp.NgI/mkOLwZfRhq1y2
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
('admin', '$2y$10$O8lrwhiRIzxU8LT9A49Gve4VeaLR0Oah.Yp.NgI/mkOLwZfRhq1y2', '管理员', 1, 0);
