# 会员模块 — 规格说明（Spec）

> 项目：dyshop 购物程序 · 模块：会员（ch09）
> 状态：v1.0 定稿（开发完成：后端 curl 联调全过 + 前端 build 通过 + Playwright 冒烟通过）
> 关联：`docs/ch08/spec.md`（后台管理）、`docs/ch07/`（订单结算）、`docs/ch04/spec-sku-selector.md`（SKU 价格来源）
> 前置：C 端登录体系（ch01）、订单结算（ch07）、后台管理与双 token 隔离（ch08）

## 1. 目标

为 C 端用户提供完整的会员体系：**按近 12 个月消费实时计算的 4 级等级（可升可降）+ 等级折扣 + 商品会员专享价 + 消费积分**；个人中心会员卡片从写死占位改为真实数据；后台提供会员列表与等级配置管理。

## 2. 范围

### 2.1 本期（In Scope）

| 编号 | 内容 | 归属 |
|---|---|---|
| S1 | 等级体系：普通 / 银卡 / 金卡 / 钻石 4 级，门槛按**近 12 个月**累计消费判定（0 / ¥2000 / ¥5000 / ¥10000） | 数据/后端 |
| S2 | 等级权益：银卡 98 折、金卡 95 折、钻石 9 折；折扣在下单结算时应用（`order_item.price` 写入折扣后价） | 后端 |
| S3 | 商品会员专享价 `product.vip_price`：有专享价优先用专享价，否则走等级折扣 | 数据/后端 |
| S4 | 积分：每消费 ¥1 积 1 分 × 等级积分倍率（普通 1x / 银卡 1x / 金卡 1.5x / 钻石 2x），订单完成后发放；本期仅记录余额+流水展示，不提供兑换 | 后端 |
| S5 | 个人中心会员卡片：当前等级 / 积分 / 近 12 月消费 / 距下一级差额与进度条，全部实时接口渲染（移除写死占位） | 前端 |
| S6 | 商品详情会员价展示（有专享价时显示会员专享价与划线） | 前端 |
| S7 | 后台会员管理：等级配置编辑（门槛/折扣/积分倍率）+ 会员分页列表（搜索） | 前后端 |
| S8 | 文档：spec / plan / tasks / manual-test | 文档 |

### 2.2 本期外（Out of Scope）

- 积分兑换 / 抵扣 / 过期
- 优惠券（表结构无对应）
- 等级权益对历史订单追溯（仅新订单生效）
- 会员等级徽章体系、会员日等运营玩法
- 自动降级通知（前端展示为准）

## 3. 架构决策

### D1 接口归属：C 端 `/api/user/member/**`（需登录）+ 后台 `/api/admin/member/**`（ROLE_ADMIN）

沿用 ch08 的权限体系：`SecurityConfig` 已统一 `/api/admin/** → hasRole('ADMIN')`，C 端 `/api/user/**` 走 JWT 认证。会员接口不新增安全链路。

### D2 等级实时计算，不落库到 user 表

等级随消费变化（可升级也可降级），落库会导致字段过期不同步。采用**查询时实时计算**：`近 12 个月已支付订单（pay_time 非空）pay_amount 之和（退款单扣减）` 匹配等级表最高门槛。等级表可编辑，改配置立即生效。

> 口径修订（2026-08-06）：由「仅已完成(status=3)」改为「支付即计入」——已支付未收货订单同样计入消费，避免大额在途订单被排除；退款仍从消费中扣减（ch12）。

### D3 专享价优先级：`vip_price` 优先，其次等级折扣

下单结算时对每行：`unitPrice = vip_price != null ? vip_price : price`；若用户等级折扣 < 1.0 且该行无专享价 → `unitPrice = price × discountRate`（保留两位小数）。普通会员（rate=1.0）与无专享价商品走原价路径，不回归历史行为。

### D4 积分发放时机：订单「已完成」（status=3）

积分仅对已完成(status=3)订单发放，避免支付后取消造成的积分错配（消费口径已改为支付计入，积分发放时机保持完成时）。倍率取订单完成时刻的当前等级。幂等：已完成订单通过前置状态判断（status=2 → 3 唯一路径）+ `point_log.order_id` 唯一索引兜底。

## 4. 数据模型

### 4.1 表变更

```sql
-- 会员等级配置表（可后台编辑）
CREATE TABLE IF NOT EXISTS `member_level` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`          VARCHAR(20)  NOT NULL COMMENT '等级标识: NORMAL/SILVER/GOLD/DIAMOND',
    `name`          VARCHAR(20)  NOT NULL COMMENT '等级名称',
    `threshold`     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '近12个月消费门槛',
    `discount_rate` DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT '订单折扣率: 0.98=98折',
    `point_rate`    DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT '积分倍率',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序(升序=等级从低到高)',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB COMMENT='会员等级配置表';

-- 积分流水表（预留，本期记录发放流水）
CREATE TABLE IF NOT EXISTS `point_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `order_id`    BIGINT       DEFAULT NULL COMMENT '来源订单ID',
    `points`      INT          NOT NULL COMMENT '变动积分(正=获得)',
    `balance`     INT          NOT NULL COMMENT '变动后余额',
    `remark`      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order` (`order_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='积分流水表';
```

- `user` 表新增列：`points INT NOT NULL DEFAULT 0 COMMENT '积分余额'`
- `product` 表新增列：`vip_price DECIMAL(10,2) DEFAULT NULL COMMENT '会员专享价'`
- 种子：`member_level` 4 行（见 P0.2）

### 4.2 默认等级配置

| code | name | threshold | discount | point_rate |
|---|---|---|---|---|
| NORMAL | 普通 | 0 | 1.00 | 1.0 |
| SILVER | 银卡 | 2000 | 0.98 | 1.0 |
| GOLD | 金卡 | 5000 | 0.95 | 1.5 |
| DIAMOND | 钻石 | 10000 | 0.90 | 2.0 |

## 5. 接口设计

### 5.1 C 端（需登录）

| 方法 | 路径 | 说明 | 响应 data |
|---|---|---|---|
| GET | `/api/user/member/overview` | 会员全景 | `{ level:{code,name,threshold,discountRate,pointRate}, totalConsumption, annualConsumption, nextLevel:{code,name,threshold,needAmount}｜null, progressPct, points }` |
| GET | `/api/user/member/points?page&size` | 积分流水分页 | `PageResult<{points,balance,remark,createTime}>` |
| POST | `/api/user/member/price-preview` | 结算价格预览（会员价展示） | 请求 `{rows:[{productId, skuId}]}`；响应 `{ userLevel:{name,discountRate}, rows:[{productId, skuId, basePrice, memberPrice}] }` |

> 说明：`totalConsumption`（累计消费，复用 ch08.4 overview 口径）与 `annualConsumption`（近 12 个月，等级判定口径）均为「已支付订单合计、退款扣减」；`progressPct = annualConsumption / 下一级threshold`（已满级返回 100）。
>
> 说明2（price-preview）：结算/详情前端为体现「会员已成交价」追加该预览接口，复用下单 `resolvePrice` 规则（vip_price 优先 else 价格×折扣率；普通等级无优惠），不重复落价逻辑；未登录 401。

### 5.2 后台（ROLE_ADMIN）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/member/levels` | 等级配置列表（含 id，可编辑字段） |
| PUT | `/api/admin/member/levels/{id}` | 更新 `{threshold, discountRate, pointRate}`（名称/code 不可改） |
| GET | `/api/admin/member/users?keyword&page&size` | 会员分页列表：用户名/昵称/手机号模糊；每行含实时等级 code/name、annualConsumption、totalConsumption、points |

## 6. 关键实现点

### 6.1 下单价格计算（OrderServiceImpl）

- 结算构建 `order_item` 时对每行调 `MemberLevelService.resolvePrice(userId, product, sku)`：取当前用户等级 → 专享价/折扣价（D3）→ 写入 `price`/`subtotal`；订单 `total_amount/pay_amount` = 折扣后合计。
- SKU 规格商品：`vip_price` 仅对无规格商品生效；规格商品按 SKU price × 折扣。

### 6.2 积分发放（OrderServiceImpl.confirm）

- 前置状态 2 → 3 通过后：`points = pay_amount 向下取整 × level.pointRate`（向下取整到整数）；`user.points += points`；写 `point_log`（balance = 变动后余额）。

### 6.3 前端会员卡片（AccountOverview.vue）

- 移除写死「银卡会员 / 72% / 再消费¥800」；改为 `fetchMemberOverview()` 渲染：等级名 + 积分 + 进度条（progressPct）+ 「距 {nextLevel.name} 还差 ¥{needAmount}」（已满级显示「已达最高等级」）。

## 7. 验收标准

- 普通用户下单：`order_item.price` = 原价；银卡用户：= 98 折价（无专享价商品）；有 `vip_price` 商品：= vip_price
- 订单完成（确认收货）后：`points` 增加 = floor(pay_amount × 倍率)，流水一条，重复确认不重复加
- 近 12 月消费跨越门槛：等级自动升级；`GET /api/user/member/overview` 反映变化；后台改门槛/折扣后前端与结算立即生效
- 个人中心会员卡片、商品详情会员价、后台会员列表/等级配置均正常
- 权限：未登录访问 `/api/user/member/**` → 401；普通用户访问 `/api/admin/member/**` → 403