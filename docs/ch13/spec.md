# 积分商城模块 — 规格说明（Spec）

> 项目：dyshop 购物程序 · 模块：积分商城（ch13）
> 状态：v1.0 草案
> 关联：`docs/ch09/spec.md`（会员/积分发放）、`docs/ch11/spec.md`（优惠券）、`docs/ch12/`（退款、定时任务先例）
> 前置：积分发放链路（ch09）、优惠券体系（ch11）、订单与安全（ch07/ch08）

## 1. 目标

在既有「订单完成赠送积分」的只进不出体系上，打通**积分的消耗出口**：C 端提供**积分商城**，用户可用积分兑换**虚拟商品**（优惠券 / 兑换码），并引入**积分 12 个月有效期**（批次过期）。后台提供兑换商品管理与兑换记录查询。不新建积分获取途径，不改动结算/抵扣流程。

## 2. 术语

| 术语 | 含义 |
|---|---|
| 积分批次 batch | 一次积分入账形成的一个批次（默认=一笔订单确认收货发放），含独立到期时间 |
| 兑换商品 goods | 积分商城在售的虚拟商品，类型为 COUPON（发券到券包）或 CODE（发放兑换码） |
| 兑换记录 exchange | 用户兑换某商品的一次落库凭证，快照商品名/积分价 |

## 3. 范围

### 3.1 本期（In Scope）

| 编号 | 内容 | 归属 |
|---|---|---|
| S1 | 积分有效期：自到账日起固定 12 个月，批次过期清零并写流水 | 数据/后端 |
| S2 | 积分商城商品：积分价、库存（-1 不限）、每人限兑、上下架 | 数据/后台 |
| S3 | C 端商城列表、兑换校验（上架/库存/限兑/积分充足）、FIFO 扣批次 | 后端 |
| S4 | COUPON 类兑换 → 发放到 `user_coupon`（source=POINTS）；CODE 类 → 生成唯一 16 位兑换码 | 后端 |
| S5 | 我的兑换记录（分页） | 后端/C 端 |
| S6 | C 端商城页面 + 个人中心入口改造（积分卡片可跳转）、积分明细页补齐 | 前端 |
| S7 | 每日积分过期定时任务 | 后端 |
| S8 | 文档：spec / plan / tasks / manual-test | 文档 |

### 3.2 本期外（Out of Scope）

- 结算/订单抵扣积分（用户已确认：**仅积分商城**，不做下单抵扣；50% 上限、与优惠券二选一规则不适用）
- 新增积分获取途径（签到、评价返积分等）
- 实物兑换、兑换后物流发货
- 积分转赠、合成、抽奖等运营玩法
- 手工调整用户积分（后台加/扣减积分，可后续扩展）

## 4. 数据模型

### 4.1 表变更（新增 3 张表）

```sql
-- 积分批次表（ch13；一次发放一条，expire_at=到账+12个月）
CREATE TABLE IF NOT EXISTS `point_batch` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT        NOT NULL COMMENT '用户ID',
    `source_type` VARCHAR(16)   NOT NULL COMMENT '来源: ORDER=订单发放 EXCHANGE=兑换扣减',
    `source_id`   BIGINT        NOT NULL COMMENT '来源单号(订单ID；兑换流水仅作记录)',
    `points`      INT           NOT NULL COMMENT '本批次积分',
    `remaining`   INT           NOT NULL COMMENT '剩余可抵扣积分',
    `expire_at`   DATETIME      NOT NULL COMMENT '到期时间(到账+12个月)',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`, `expire_at`),
    KEY `idx_expire` (`expire_at`)
) ENGINE=InnoDB COMMENT='积分批次表';

-- 积分商城商品表（ch13；后台配置，只售虚拟商品）
CREATE TABLE IF NOT EXISTS `points_goods` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           VARCHAR(64)   NOT NULL COMMENT '商品名',
    `cover_image`    VARCHAR(500)  DEFAULT NULL COMMENT '封面图',
    `description`    VARCHAR(500)  DEFAULT NULL COMMENT '描述',
    `goods_type`     VARCHAR(16)   NOT NULL COMMENT '类型: COUPON=发券 / CODE=兑换码',
    `point_cost`     INT           NOT NULL COMMENT '兑换所需积分(>0)',
    `stock`          INT           NOT NULL DEFAULT -1 COMMENT '库存, -1=不限',
    `limit_per_user` INT           NOT NULL DEFAULT 0 COMMENT '每人限兑次数, 0=不限(COUPON类受券唯一键约束=1)',
    `coupon_template_id` BIGINT    DEFAULT NULL COMMENT 'COUPON类关联优惠券模板',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 1上架 0下架',
    `sort`           INT           NOT NULL DEFAULT 0 COMMENT '排序(升序)',
    `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB COMMENT='积分商城商品表';

-- 兑换记录表（ch13；user_id+goods_id 组合可查，COUPON 类兼防超限）
CREATE TABLE IF NOT EXISTS `points_exchange` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `exchange_no`    VARCHAR(32)   NOT NULL COMMENT '兑换单号(业务唯一)',
    `user_id`        BIGINT        NOT NULL COMMENT '用户ID',
    `goods_id`       BIGINT        NOT NULL COMMENT '商品ID',
    `goods_name`     VARCHAR(64)   NOT NULL COMMENT '商品名快照',
    `goods_type`     VARCHAR(16)   NOT NULL COMMENT '快照类型 COUPON/CODE',
    `point_cost`     INT           NOT NULL COMMENT '快照消耗积分',
    `code`           VARCHAR(64)   DEFAULT NULL COMMENT 'CODE类型兑换码(唯一)',
    `coupon_id`      BIGINT        DEFAULT NULL COMMENT 'COUPON类型发放的 user_coupon.id',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exchange_no` (`exchange_no`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB COMMENT='积分兑换记录表';
```

### 4.2 现有表扩展

- `user_coupon.source` 新增值 `'POINTS'`（注释含义扩展：积分商城兑换）；**不加列、不加索引**——沿用唯一键 `(user_id, template_id, source)`。因 source 参与唯一键，同模板同人经商城兑换**限 1 张**；若运营需要多次，以不同的 `points_goods`（各自关联同一模板）仍会被唯一键拦截，因此设计上 **COUPON 类商品限兑以 1 次/模板封顶**。

> 说明：若后续需要「同模板可兑换多次」，需将唯一键扩展为 `(user_id, template_id, source, points_exchange_id)`（保留备用，本期不做）。

### 4.3 默认种子（可选，验收用）

```sql
-- 示例：全场可用无门槛 ¥5 券，500 积分；兑换码商品《会员周卡》，800 积分
INSERT INTO `points_goods` (id, name, description, goods_type, point_cost, stock, coupon_template_id, status, sort)
VALUES (1,'满 30 减 5 全场券','积分专享优惠券','COUPON',500,100,<模板ID>,1,1),
       (2,'会员周卡兑换码','周卡卡密兑换','CODE',800,-1,NULL,1,2);
```

## 5. 接口设计

### 5.1 C 端（需登录）

| 方法 | 路径 | 说明 | 响应 data |
|---|---|---|---|
| GET | `/api/user/points/goods?page&size` | 商城在售商品列表（含每人剩可兑数/库存/我的查询余额） | `PageResult<PointsGoodsVO>` |
| POST | `/api/user/points/exchange` | 兑换 `{goodsId}` | `{exchangeNo, goodsType, code?, couponId?, pointCost}` |
| GET | `/api/user/points/exchanges?page&size` | 我的兑换记录分页 | `PageResult<PointsExchangeVO>` |
| GET | `/api/user/member/points` | （复用 ch09）积分明细分页 | `PageResult<PointLogVO>` |

> 抵扣/兑换规则：`user.points` 为**可用余额**（=未过期 batch 的 `remaining` 之和）；兑换需 `可用余额 >= point_cost`。积分不足返回 `POINTS_NOT_ENOUGH`。

### 5.2 后台（ROLE_ADMIN）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/points/goods?keyword&page&size` | 商品分页查询（含已删除筛选） |
| POST | `/api/admin/points/goods` | 新建商品 |
| PUT | `/api/admin/points/goods/{id}` | 编辑（名称/图/描述/积分价/库存限兑/排序/状态；已产生兑换记录的 COUPON 商品不可改关联模板） |
| DELETE | `/api/admin/points/goods/{id}` | 逻辑删除（仅下架商品） |
| PATCH | `/api/admin/points/goods/{id}/status` | 上/下架 |
| GET | `/api/admin/points/exchanges?goodsId&keyword&page&size` | 兑换记录分页查询 |

### 5.3 错误码（扩展 `ResultCode`）

| 码 | 场景 | 备注（HTTP 语义） |
|---|---|---|
| `POINTS_GOODS_NOT_FOUND` | 商品不存在/已删除/已下架 | 404 |
| `POINTS_GOODS_SOLD_OUT` | 库存不足（限库商品） | 409 |
| `POINTS_NOT_ENOUGH` | 积分余额不足 | 409 |
| `POINTS_EXCHANGE_FROZEN` | 重复兑换/超限（用户+商品已存在） | 409 |
| `POINTS_EXCHANGE_FAIL` | 兑换内部失败（发券/生成码异常回滚） | 500 |

## 6. 关键实现点

### 6.1 积分批次创建（改造 `MemberLevelServiceImpl.grantPoints`）

- 现有逻辑不变（仍写 `point_log` 一条 + `user.points` 累加）；
- **新增**：同事务写入 `point_batch`（`source_type=ORDER`, `source_id=order.id`, `points=remaining=本次积分`, `expire_at=now+12个月`）。
- 幂等：沿用 `point_log.order_id` 唯一索引兜底（批次与流水同事务，重复发放整体回滚）。

### 6.2 兑换事务（`PointsService.exchange`）

```text
1. 校验商品（存在/未删除/上架/（若是COUPON未停用模板））
2. 校验限兑：points_exchange 无 (user_id, goods_id) → 否则 409（COUPON 券唯一边沿化）
3. SELECT user FOR UPDATE（锁用户行，串行化余额变动）
4. 校验可用积分：SUM(point_batch.remaining WHERE user_id=? AND expire_at>NOW) >= point_cost → 否则 POINTS_NOT_ENOUGH
5. FIFO 扣减：按 expire_at ASC,id ASC 逐批 UPDATE remaining（同事务写 point_log 一条 -point_cost）
6. 生成单号（业务前缀，如 PX + 时间戳+随机）→ 库存乐观锁：stock>0 时 UPDATE WHERE stock-1>=0 行数=0 → SOLD_OUT
7. COUPON：发落到 user_coupon（复用 ch11 发放逻辑：issued_count+1 乐观锁 + expire_at 计算）；写 points_exchange.coupon_id
8. CODE：生成唯一 16 位大写卡密（如 DQ-XXXX-... 三组加中画线）→ points_exchange.code（uk_code 冲突则重试）
9. INSERT points_exchange → 返回结果
```

> 幂等与并发：所有写操作在用户行锁之后，天然串行；`uk_exchange_no` / `uk_code` 兜底重复提交。

### 6.3 每日过期任务（`PointsExpireScheduler`，参考 `OrderTimeoutScheduler`）

```text
@Scheduled（如每日 02:00）
1. 分批查询：SELECT DISTINCT user_id FROM point_batch WHERE remaining>0 AND expire_at<NOW() LIMIT 批次
2. 对每用户：行锁 → 汇总该用户全部过期批次 remaining → 置 0 → user.points 扣减 → 写「积分过期」point_log
3. 幂等：remaining=0 的批次不会再扣；重复执行 0 副作用
```

### 6.4 前端

- `/user/points-mall`：商品卡片（封面/名称/积分价/库存/限兑）、兑换点击弹确认框（未登录 401、积分不足置灰并提示差多少）、成功展示：COUPON 跳「我的优惠券」、CODE 展示卡密 + 一键复制。
- `/user/points`：积分明细页（复用 `fetchMemberPoints`，页面底部含余额）。
- `frontend/src/config/userMenu.js` 新增「积分商城」「积分明细」两个入口；`AccountOverview.vue` 积分项文案改为「商城兑换 >」（点击跳商城）。

## 7. 库存/兑换码/过期口径汇总

- **兑换消耗**：只考虑本**未过期**批次；`user.points` 本身=全部 `remaining`，过期任务保证二者一致（同事务扣减）。
- **限兑**：CODE 类用 `limit_per_user`；COUPON 类用唯一键=1（实现时再判断）。
- **库存**：stock=-1 不限；>0 时乐观扣减。

## 8. 验收标准

- 商城列表上架商品可见、下架不可见
- 兑换成功：`user.points` 同步减少、`point_batch` 剩余相应扣减、`point_log` 记消耗流水、`points_exchange` 落账
- 兑换 COUPON：券包出现该券（来源=积分兑换），可使用；重复兑换同模板 → 409
- 兑换 CODE：返回唯一卡密，重复兑换新商品不同码、同商品 409
- 积分不足兑换 → 409 且页面置灰
- 插入 `expire_at` 已过的批次 → 过期任务执行后，`user.points` = 有效批次和、写「积分过期」流水、再次运行 0 副作用
- 积分丢失守恒：发放 + 兑换 + 过期后 `user.points` 与 `point_log` 平衡一致
- 权限：未登录系列代码 → 401；普通用户访问 admin → 403

## 9. 文档结构

```text
docs/ch13/
├── plan.md                    # 实施计划（阶段划分/里程碑/风险）
├── spec.md                    # 本文件（规格说明）
├── tasks.md                   # 任务清单（可按勾选）
└── manual-test/points-mall.md # 手工测试清单
```