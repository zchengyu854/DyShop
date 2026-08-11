# 优惠券模块 — 任务清单（Tasks）

> 模块：ch11 优惠券 · 对应 `docs/ch11/spec.md` v1.0 · 阶段划分见 `docs/ch11/plan.md`
> 勾选规则：实现 → 代码评审 → 该功能手工用例过完，方可打勾。

## P0 数据层

- [x] 定位项目实际结算/下单服务与超时任务类（grep OrderServiceImpl / Schedule）
- [x] 编写幂等迁移脚本：`coupon_template` / `user_coupon` / `order_coupon` 建表
- [x] 迁移：`orders` 增加 `discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0`
- [x] 索引与约束：`user_coupon` UNIQUE(user_id, template_id, source)、`user_coupon.used_order_id` 索引、`order_coupon.order_id` UNIQUE
- [x] 新增实体 + Mapper/DAO（CouponTemplate / UserCoupon / OrderCoupon），沿用项目 ORM 风格

## P1 后台（模板 + 发放 + 用户券）

- [x] 模板分页/搜索 API（GET /api/admin/coupon/templates）
- [x] 新建/编辑 API（POST/PUT，编辑限制 amount/scope/分类/商品列表/valid 仅新领取生效；LIMITED 至少 category_ids/product_ids 一项非空）
- [x] 启用/停用 API（PATCH status）
- [x] 逻辑删除 API（DELETE，仅停用模板）
- [x] 发放 API（POST /api/admin/coupon/grants：指定用户 / 全员；幂等 409）
- [x] 用户券分页/搜索 API（GET /api/admin/coupon/user-coupons）
- [x] 作废 API（PATCH void，仅未使用）
- [x] 后台模板列表页 + 新建/编辑表单页（范围支持指定分类 + 指定商品并行多选）
- [x] 后台发放弹窗（选模板/范围/用户）
- [x] 后台用户券管理页（筛选 + 作废确认）

## P2 C 端（领券 + 我的券）

- [x] 领券中心列表 API（GET /api/user/coupon/center：含每人领取情况与剩余量）
- [x] 领取 API（POST claim：事务 + 乐观总量 + 每人限 1 + 有效期计算，重复 409）
- [x] 我的优惠券 API（GET /api/user/coupon/mine?status=&page=）
- [x] 领券中心页面（列表、领取按钮态、剩余量展示）
- [x] 我的优惠券页面（未使用/已使用/已过期分态 + 下拉刷新/分页）
- [x] `frontend/src/config/userMenu.js`「我的优惠券」占位项改指向新路由

## P3 结算集成（预览 + 下单扣券）

- [x] `/api/orders/preview` 支持可选 couponId（复用会员折扣逻辑 + 券抵扣 + 门槛/范围[分类∪商品]/叠加校验）
- [x] `POST /api/orders` 支持 couponId：事务内乐观扣券（spec §5.3）+ 写 orders.discount_amount + order_coupon 快照
- [x] 后端错误码约定：券已使用 / 已过期 / 未满门槛 / 已抢光 / 已领取（409 系）
- [x] 结算页读可用券列表 + 门槛提示（「还差 ¥X」）/ 不可用原因渲染
- [x] 结算页单选券、实时重算应付（节流调用 preview）、展示「-¥Y」
- [x] 下单成功清空选券缓存；订单列表/详情展示优惠快照（读 order_coupon）

## P4 回退与定时任务

- [x] 手动取消订单 → 同事务回退券（幂等 SQL，保留原有效期，过期置 status=2）
- [x] 超时任务扩展：取消时一并回退券
- [x] 可选：批量过期券清扫（status=0 → status=2）
- [x] 回归：库存回补 / 积分回补不受影响

## P5 联调与验收

- [x] 前后端联调（领券 → 我的券 → 结算选券 → 下单 → 取消回退全链路）
- [x] 并发用例：同券双下单只成一单；领取最后一券不超发
- [x] 手工验收：`docs/ch11/manual-test/coupon.md` 全过
- [x] 回归：ch07 结算/超时、ch09 会员价、ch10 订单列表快照与菜单
- [x] `npm run build` 前端构建通过
