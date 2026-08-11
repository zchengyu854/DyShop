# 售后/退款模块 — 规格说明（Spec）

> 项目：dyshop 购物程序 · 模块：售后/退款（ch12）
> 状态：v1.0
> 关联：`docs/ch07/`（订单状态机）、`docs/ch11/`（优惠券——售后不做比例退券，本项目无部分退款）
> 前置：C 端登录、订单已完成（status=3）

## 1. 目标

为已完成订单提供**按商品行申请售后**的闭环：C 端申请（自动计算退款金额）→ 后台审核（同意=模拟退款完成 / 拒绝并填理由）→ 我的售后单列表与状态跟踪；同商品行仅可申请一次（防重复退款）。

## 2. 范围

### 2.1 本期（In Scope）

| 编号 | 内容 | 归属 |
|---|---|---|
| S1 | 售后单模型：按 order_item 行申请，自动退款金额（成交单价×数量） | 后端 |
| S2 | C 端申请（仅已完成订单）、我的售后列表/详情、取消申请 | 前后端 |
| S3 | 后台售后列表（状态/关键词筛选）、同意（模拟退款）、拒绝（填理由） | 前后端 |
| S4 | 同商品行唯一售后（防重复退款） | 后端 |
| S5 | 文档：spec / tasks / plan / manual-test | 文档 |

### 2.2 本期不做（Out of Scope）

- 退货物流（回填物流单号）、仅退款与退货退款双类型拆分（本期统一 `ONLY_REFUND` 仅退款）
- 退款比例退优惠券/积分回补（ch11 明确无部分退款；积分回补另计）
- 部分退款、售后超时自动处理、纠纷介入

## 3. 数据模型（新增 `after_sale`）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| after_sale_no | varchar(32) | 售后单号（业务唯一） |
| order_id | bigint | 来源订单 |
| order_item_id | bigint | 商品行（UNIQUE：同一行仅可申请一次） |
| user_id | bigint | 申请人 |
| product_id / product_name / product_image / spec_text | | 商品快照 |
| quantity | int | 退款数量（=行数量，本期不支持部分退） |
| refund_amount | decimal(10,2) | 退款金额 = order_item.price × quantity（自动） |
| reason | varchar(200) | 申请原因（必填） |
| type | varchar(16) | 本期固定 `ONLY_REFUND` |
| status | tinyint | `0` 待处理 / `1` 退款中 / `2` 已退款完成 / `3` 已拒绝 / `4` 已取消 |
| reject_reason | varchar(200) | 拒绝理由（status=3 时） |
| handle_time / cancel_time | datetime | 审核时刻 / 取消时刻 |
| create_time / update_time | datetime | 审计 |

> 状态流转：`0 待处理 →(后台同意·模拟退款)→ 2 已退款完成`；`0 →(后台拒绝)→ 3`；`0 →(用户取消)→ 4`。
> 退款后**不改动原订单**（订单保持已完成，售后单独立记账）。

## 4. 接口设计

> 权限沿用：C 端 `/api/user/**`（需登录）、后台 `/api/admin/**`（ROLE_ADMIN）。

### 4.1 C 端

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/user/after-sales` | 申请售后 `{ orderItemId, reason }`（校验：订单已完成、行归属本人、未申请过） |
| GET | `/api/user/after-sales?status=&page=` | 我的售后列表（分页，状态筛选） |
| GET | `/api/user/after-sales/{id}` | 售后详情（越权 404） |
| POST | `/api/user/after-sales/{id}/cancel` | 取消申请（仅待处理） |

### 4.2 后台

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/after-sales?status=&keyword=&page=` | 售后列表（keyword=订单号/用户名/商品名） |
| POST | `/api/admin/after-sales/{id}/approve` | 同意：模拟退款 → status=2 |
| POST | `/api/admin/after-sales/{id}/reject` | 拒绝 `{ reason }` → status=3 |

## 5. 业务规则

- R1 申请条件：订单 `status=3`（已完成）且订单属于当前用户。
- R2 退款金额：`order_item.price × order_item.quantity`（成交单价口径，自动计算，前端只读展示）。
- R3 防重复：`after_sale.order_item_id` 唯一索引；已存在申请（任意状态）则拒绝再次申请。
- R4 取消：仅 `status=0` 可取消；审核后不可取消。
- R5 审核：仅 `status=0` 可同意/拒绝；同意直接置 `2 已退款完成`（模拟支付环境，无真实打款）；拒绝必填理由。
- R6 幂等：重复取消/重复审核按状态条件更新（影响行数 0 则忽略或报错）。

## 6. 非功能

- N1 所有金额以后端计算为准（前端只展示）。
- N2 售后列表分页，size ≤ 50。
- N3 统一错误码：参数 400 / 资源不存在 404 / 重复申请 409「该商品已申请售后」。

## 7. 验收标准

1. 已完成订单可申请售后；未完成/待收货订单不可申请
2. 同一商品行不可重复申请（409）
3. 后台同意 → 状态「已退款完成」，金额正确；拒绝 → 展示拒绝理由
4. 用户可取消待处理申请
5. C 端「我的售后」列表/详情与后台「售后管理」列表数据一致
6. 前端 `npm run build` 通过；手测记录见 docs/ch12/manual-test/after-sale.md
