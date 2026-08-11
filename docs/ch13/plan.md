# 积分商城模块 — 实施计划（Plan）

> 模块：ch13 积分商城 · 基于 `docs/ch13/spec.md` v1.0
> 本计划仅文档排期与依赖，不预估工时；每阶段完成后需过手工验收（`docs/ch13/manual-test/points-mall.md`）。

## 0. 依赖与前置

- 依赖 ch09 会员体系：积分发放链路（`MemberLevelServiceImpl.grantPoints` + `point_log` 流水 + `user.points` 余额）已就绪，本期不新增获取途径。
- 依赖 ch11 优惠券体系：COUPON 类兑换商品复用 `coupon_template` / `user_coupon`（source 扩展值 `'POINTS'`）与发放乐观锁模式。
- 依赖 ch12 定时任务先例：`OrderTimeoutScheduler`（`backend/dyshop-api/src/main/java/com/dyshop/api/config/`）作为每日积分过期任务的实现范本。
- 存量约束：`point_log.order_id` 唯一（幂等兜底）；`user_coupon` 唯一键 `(user_id, template_id, source)` 决定「COUPON 类每模板每人限兑 1 次」上限。
- 若联调发现积分/发放逻辑位置不在预期，先用 grep 定位实际服务类再动工。

## 1. 阶段划分

### P0 准备与表结构（P0-ready）

- 产出：幂等 DDL 迁移脚本 `backend/sql/points-mall.sql`（`point_batch` / `points_goods` / `points_exchange` 三表 + `user_coupon.source` 注释扩展）；后端实体与 Mapper（沿用项目 ORM 风格）。
- 关键：`point_batch` 以 `expire_at` 记录批次到期（到账 + 12 个月）；`points_exchange.goods_id` 业务唯一防重复兑换（对 COUPON 类兼防超限）。
- 验收：迁移脚本可重复执行（幂等）；实体/表字段对齐 spec §4。

### P1 后台：兑换商品管理 + 兑换记录查询

- 产出：后台 API（spec §5.2）与后台前端页面（商品列表/新建/编辑、上下架、兑换记录列表）。
- 关键规则：编辑已上架商品仅改文案/积分价/库存/限兑等，关联模板不可改（防历史记录错位）；下架不影响已兑换记录；删除仅限下架商品（逻辑删除）。
- 验收：后台建商品 → C 端商城可见；下架后 C 端不可见。

### P2 C 端：积分商城 + 兑换流程

- 产出：C 端 API（spec §5.1 三项）与页面（商城列表、兑换确认弹窗、成功反馈）；`frontend/src/config/userMenu.js` 新增「积分商城」「积分明细」入口。
- 关键规则：兑换事务（spec §6.2）——用户行锁 + FIFO 批次扣减 + 发券/生成码 + 库存乐观锁；积分不足置灰提示；重复兑换 409。
- 验收：兑换 COUPON → 券包可见；兑换 CODE → 兑换码展示可复制；积分余额/流水同步。

### P3 过期任务与展示联动

- 产出：每日积分过期定时任务（spec §6.3）；个人中心积分卡片入口改造（AccountOverview 积分项可点击跳积分明细/商城）。
- 关键规则：过期任务按批次清零 `remaining`、汇总扣减 `user.points`、写「积分过期」流水；与兑换并发时以批次行锁保证不重复扣减。
- 验收：手工构造过期批次 → 任务执行后余额/流水/可兑换额度一致。

### P4 联调与手工验收

- 产出：前后端联调修复；按 `manual-test/points-mall.md` 全量过一遍；回归 ch09 积分发放、ch11 券发放/结算、ch12 退款扣消费。
- 验收：spec §8 验收标准全绿。

## 2. 风险与对策（文档层）

| 风险 | 对策 |
|---|---|
| 服务/表位置与文档假设不符 | P0 先 grep 定位（grantPoints / OrderTimeoutScheduler / user_coupon 发放逻辑） |
| 积分与优惠券并发扣减（同用户同刻兑换 + 过期） | 兑换与过期均以用户行锁串行化（spec §6.2/§6.3） |
| 库存 -1 与实际发放数不一致 | 库存扣减与发券同事务；stock=-1 不限时不扣减 |
| 前段路由/菜单结构变化（ch10 重构过） | P2 先读 userMenu.js 与现有页面结构，最小侵入接入 |

## 3. 里程碑

- M1：P0+P1 完成（后台可配置商品，C 端可见）
- M2：P2 完成（C 端可兑换虚拟商品）
- M3：P3 完成（过期任务 + 入口联动）
- M4：P4 完成（验收全绿，交付）

## 4. 验收依赖确认

- 手工测试清单：`docs/ch13/manual-test/points-mall.md`
- 回归清单：ch09 积分发放/会员卡片、ch11 优惠券、ch12 退款

## 5. 进度记录

| 里程碑 | 状态 | 说明 |
|---|---|---|
| M1 P0+P1 | 🟢 | 代码完成（DDL/实体/后台 API/后台页面），待手工验收 |
| M2 P2 | 🟢 | 代码完成（C 端三 API + 商城页 + 明细页 + 菜单入口），待手工验收 |
| M3 P3 | 🟢 | 代码完成（过期任务 + 积分卡片入口），待手工验收 |
| M4 P4 | ⬜ | 未开始（联调 / 手工验收 / 回归） |
