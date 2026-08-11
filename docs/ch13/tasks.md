# 积分商城模块 — 任务清单（Tasks）

> 模块：ch13 积分商城 · 对应 `docs/ch13/spec.md` v1.0 · 阶段划分见 `docs/ch13/plan.md`
> 勾选规则：实现 → 代码评审 → 该功能手工用例过完，方可打勾。

## P0 数据层

- [ ] 定位实际积分发放/定时/发券服务类（grep grantPoints / OrderTimeoutScheduler / source=CENTER）
- [x] 编写幂等迁移脚本 `backend/sql/points-mall.sql`：`point_batch` / `points_goods` / `points_exchange` 建表
- [x] 约束：`point_batch(user_id, expire_at)` 索引、`points_exchange` 唯一键 `uk_exchange_no`、`uk_code`、`(user_id, goods_id)` 联查索引
- [x] 迁移：`user_coupon.source` 注释扩展 `POINTS`（无 DDL，文档注释即可）
- [x] 新增实体 + Mapper/DAO（PointBatch / PointsGoods / PointsExchange），沿用项目 ORM 风格

## P1 后台（商品管理 + 兑换记录）

- [x] 商品分页/搜索 API（GET /api/admin/points/goods）
- [x] 新建商品 API（POST，校验积分为正、类型、COUPON 必须关联模板）
- [x] 编辑 API（PUT；已产生兑换记录的 COUPON 商品禁止改关联模板）
- [x] 上/下架 API（PATCH status）
- [x] 逻辑删除 API（DELETE，仅下架商品）
- [x] 兑换记录分页 API（GET /api/admin/points/exchanges?goodsId&keyword）
- [x] 后台商品列表页 + 新建/编辑表单页 + 上下架/删除交互
- [x] 后台兑换记录列表页（按商品/用户关键字筛选）

## P2 C 端（商城 + 兑换）

- [x] 商城列表 API（GET /api/user/points/goods：在售 + 每人可兑数 + 我的积分余额）
- [x] 兑换 API（POST /api/user/points/exchange：idempotent，事务，见 spec §6.2）
- [x] 我的兑换记录 API（GET /api/user/points/exchanges）
- [x] 错误码接入：`ResultCode` 扩展 5 个 points 码（§5.3）
- [x] 积分批次写入改造：`MemberLevelServiceImpl.grantPoints` 同事务写 point_batch
- [x] `frontend/src/api/user.js` 新增 fetchPointsGoods / exchangePoints / fetchPointsExchanges
- [x] 积分商城页面（商品卡片、积分不足置灰、兑换确认弹窗、成功反馈：券→跳券包 / 码→复制）
- [x] 积分明细页 `/user/points`（复用 fetchMemberPoints + 余额）
- [x] `frontend/src/config/userMenu.js` 新增「积分商城」「积分明细」入口
- [x] `AccountOverview.vue` 积分卡片文案改「商城兑换 >」可点击跳转

## P3 过期任务

- [x] 每日积分过期任务 `PointsExpireScheduler`（行锁 + 批次清零 + user.points 扣减 + 「积分过期」流水）
- [ ] 幂等验证：重复执行无副作用；与兑换并发不超扣

## P4 联调与验收

- [ ] 前后端联调（发放 → 商城兑换券/码 → 余额与流水 → 我的兑换记录 → 过期）
- [ ] 并发用例：同用户同商品重复点击只成一单；库存 1 时并发兑换不超发；FIFO 扣到期批次
- [ ] 手工验收：`manual-test/points-mall.md` 全过
- [ ] 回归：ch09 会员价/积分发放、ch11 优惠券（发放/结算/回退）、ch12 退款扣消费
- [ ] `npm run build` 前端构建通过；后端 mvn package 通过