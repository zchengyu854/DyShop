# 后台管理模块 — 验证备忘与待办（Notes）

> 状态：**仅记录备忘，未开发**（2026-08-05）
> 归属：ch07 结算闭环的后台订单管理（后台商品管理等后续章节另行规划）

## 1. 发货链路验证结论（2026-08-05 已实测通过）

全链路 curl 实测（服务：dyshop-api:8081 + dyshop-admin:8082 + MySQL(docker mysql-dev) + 前端:5173）：

| 步骤 | 操作 | 结果 |
|---|---|---|
| 1 | 注册买家 `buyer01` / 管理员 `admin01`（注册接口默认 role=0） | ✅ |
| 2 | 管理员提权：`UPDATE user SET role=1 WHERE username='admin01'` | ✅ |
| 3 | 买家建地址 → `POST /api/orders`（buyNow, productId=1, qty=1）→ 订单 11「待支付」 | ✅ |
| 4 | `POST /api/orders/11/pay` → 「待发货」，`pay_time` 写入 | ✅ |
| 5 | 管理员 `POST /api/admin/orders/11/ship` → 「待收货」，`ship_time` 写入 | ✅ |
| 6 | 买家 `GET /api/orders`、`GET /api/orders/11` → 状态 2「待收货」，`shipTime` 有值 | ✅ |

要点：
- 后台接口路径为 `/api/admin/orders*`（**在 dyshop-api:8081**，`AdminOrderController`，`SecurityConfig` `/api/admin/**` hasRole('ADMIN')）；前端 `api/admin/order.js` 用 `request`（baseURL `/api`）→ 正确，**不是** `adminRequest`（`/admin-api` → 8082 仅 dyshop-admin 模块用）
- 订单接口响应头 `Cache-Control: no-store`（后端 `CacheControlFilter`），前端 request 层亦显式携带 no-store 请求头，无缓存风险
- C 端订单列表/详情已接 `ORDER_NS` 事件总线 + pageshow/visibilitychange 自动刷新，后台发货后买家侧刷新可见新状态

## 2. 发现的问题（待办，未开发）

### P1 个人中心无订单数据展示（AccountOverview / RecentActivity 为静态占位）
- `AccountOverview.vue`：订单总数/待收货数**写死 0**，注释「数据源为 GET /api/user/overview（订单/优惠券模块后接入）」
- `RecentActivity.vue`：「待付款/待收货/待评价/售后退款」count 写死 0，点击 toast「订单模块开发中」
- 后端 `GET /api/user/overview` **接口不存在**（实测 404，`CacheControlFilter` 已预留该路径）
- 待办（若做个人中心订单统计）：
  1. 后端新增 `GET /api/user/overview` → `{ orderTotal, waitPay, waitShip, waitReceive, ... }`（或直接返回各状态计数）
  2. 前端 `AccountOverview`/`RecentActivity` 接该接口，并接入 `ORDER_NS` 订阅 + pageshow/visibilitychange 刷新（复用 `useDataRefresh`），使「管理员发货 → 个人中心待收货数即时 +1」

### P2 发货状态无实时推送（跨窗口边界）
- 管理员窗口发货后，买家窗口**无 WebSocket/SSE/轮询**，需切回前台（visibilitychange）、手动刷新或重新进入页面才同步
- 当前为可接受降级方案；生产建议 WebSocket/SSE 推送订单状态变更

### P3 管理员账号提权无入口 —— ✅ 已解决（ch08 后台管理模块，2026-08-05）
- `sql/data.sql` 已预置管理员 `admin / admin123`（BCrypt，role=1）
- 后台「用户管理」提供 设管理员/取消管理员 入口（`PUT /api/admin/users/{id}/role`）
- 注册接口 role 保持固定 0（买家只能自助注册，无法自提权）

## 3. 建议的后端配合事项（开发时落实）
- 订单/支付/取消接口返回**完整 OrderVO**（含 items/payDeadline/statusText），避免前端二次查询与状态文案漂移（现已在做）
- 支付/取消/发货接口返回幂等结果，配合前端乐观更新回滚
- `GET /api/user/overview` 接口同属交易类接口，需加入 `CacheControlFilter` no-store 名单
