# 客户端结算模块 — 任务清单（Tasks）

> 状态：开发完成，待浏览器手测
> 图例：[x] 完成　[ ] 待做

## 后端
- [x] **T1** `CartCheckedDTO` + `CartService.updateChecked` + `CartItemVO.checked` + `CartController PUT /api/cart/items/{productId}/checked`
- [x] **T2** 实体 `Order` / `OrderItem` / `Payment`（dyshop-common/entity，映射既有表）
- [x] **T3** `OrderMapper` / `OrderItemMapper` / `PaymentMapper`
- [x] **T4** `CreateOrderDTO` + `OrderVO` / `OrderItemVO` / `AdminOrderVO`
- [x] **T5** `OrderServiceImpl`：`createOrder`（事务下单：地址校验/商品校验/订单号/插入/扣库存/清购物车）
- [x] **T6** `OrderServiceImpl`：`listOrders` / `getOrder`（越权 404）
- [x] **T7** `OrderServiceImpl`：`cancel`（仅待支付 + 回补库存）/ `pay`（MOCK 流水 + 加销量）/ `confirm`（仅待收货）
- [x] **T8** `OrderController`：POST `/api/orders`、GET `/api/orders?status=`、GET `/api/orders/{id}`、POST `{id}/cancel`、POST `{id}/pay`、POST `{id}/confirm`
- [x] **T9** `SecurityConfig` 新增 `/api/admin/**` hasRole('ADMIN')
- [x] **T10** 后台：分页列表（JOIN user）+ 详情 + `ship` 发货 + `AdminOrderController`
- [x] **T11** 后端编译 + 重启（PID 75041，日志 /tmp/dyshop-api.log）
- [x] **T12** curl 联调 M1-M17 全部通过

## 前端
- [x] **T13** `api/cart.js` 勾选接口 + `stores/cart.js` 勾选状态/选择器/乐观更新
- [x] **T14** `Cart.vue`：圆形复选框 + 全选三态 + 摘要按勾选 + 结算跳 `/checkout`
- [x] **T15** `api/order.js` 六个请求方法 + `api/admin/order.js`
- [x] **T16** `Checkout.vue` 重写：地址卡（选择弹层/管理地址/无地址引导）+ 商品清单（cart/buyNow）+ 备注计数 + 摘要（免运费）+ 提交
- [x] **T17** 模拟支付 Modal（PayModal 组件）：金额 + 确认支付 + 稍后支付；成功后 toast + 跳详情
- [x] **T18** `ProductInfoPanel.vue`「立即购买」→ `/checkout?buyNow=1&productId=&quantity=`
- [x] **T19** `OrderList.vue`：tab 筛选 + 订单卡片 + 操作（去支付/取消/确认收货/详情）+ 确认弹窗 + 空态
- [x] **T20** `OrderDetail.vue`：状态区/收货信息/商品列表/金额/订单信息/操作
- [x] **T21** 后台 `OrderManage.vue`：表格/分页/详情抽屉/发货确认
- [x] **T22 `npm run build` 通过；浏览器手测 N 用例**（build ✅，N 用例待用户手测回填）

## 文档
- [x] **T23** spec/plan/tasks 按开发结果回填（spec 定稿、plan P1-P8 完成、M 用例回填）

## 2026-08-05 补充（超时自动取消 + 订单删除）
- [x] **T24** 后端 `OrderServiceImpl.expireTimeoutOrders()`：扫描「待支付且超 15 分钟」订单 → 条件更新置已取消 + 回补库存（幂等，并发支付/多实例不重复回补）
- [x] **T25** 后端 `OrderTimeoutScheduler`（@Scheduled 60s）+ 主类 `@EnableScheduling`
- [x] **T26** 后端 `OrderService.remove`：仅已完成/已取消可删（逻辑删除 deleted=1），交易中订单 400；`OrderController` `DELETE /api/orders/{id}`
- [x] **T27** 前端 `api/order.js` `deleteOrder`；`OrderList.vue`/`OrderDetail.vue` 已完成/已取消显示删除按钮（小号 ghost）+ 确认弹窗 + 删除后广播 `ORDER_NS`
- [x] **T28** curl 验证：待收货删 400 / 待支付删 400 / 已取消删成功 / 列表消失 / 详情 404 / 越权 404 / 超时自动取消（16 分钟前下单 → 8s 内被定时任务取消，库存 149→150 回补）✅

## 2026-08-05 补充（导航入口修复）
- [x] **T29** 根因：个人中心快捷入口（待付款/待收货/待评价/售后）此前仅 toast「订单模块开发中」无跳转；订单页 tab 为纯本地状态，外部入口无法直达筛选
- [x] **T30** `OrderList.vue`：tab 与 `/orders?status=` 双向同步（切 tab `router.replace` 写 URL、`watch(route.query.status)` 统一驱动 load，`immediate` 首载，无重复请求；replace 不污染历史栈）
- [x] **T31** `RecentActivity.vue`：快捷入口改 `router-link` 声明式导航 → `/orders?status=0/2/3`（售后退款无模块保留 toast）；44px 触控高度
- [x] **T32** `UserCenter.vue`：侧边栏交易记录组新增 待支付/待发货/待收货/已完成 快捷入口；窄屏菜单项 44px 触控
- [x] **T33** 编程式导航补 try-catch（openDetail 等）；移动端 Playwright（iPhone 13 视口）全链路验证：快捷入口直达筛选 ✅ / 侧边入口 ✅ / tab↔URL 同步 ✅ / 后退离开订单页（replace 语义）✅ / 直达链接 /orders?status=0 激活对应 tab ✅ / 详情跳转与返回 ✅

## 2026-08-05 补充（订单模块内嵌个人中心重构）
- [x] **T34** 选型：方案A变体 —— 订单列表迁入 `/user/orders` 子路由，与个人资料/地址/收藏同构（侧边栏 + 内容区），状态筛选保留为列表内 Segmented Tab；未选 Overlay/手风琴（与现有导航模式冲突）
- [x] **T35** 新建 `views/user/UserOrders.vue`（OrderList 内嵌版）：去 page-shell、移动端左右滑动手势切 tab、keep-alive 适配（defineOptions name）
- [x] **T36** 路由：`/user/orders` 子路由（lazy import）；`/orders` 保留为兼容重定向；`/orders/:id` 详情保留独立路由（聚焦阅读 + back 回列表）
- [x] **T37** 状态保持：UserCenter router-view 加 `<keep-alive :include="['UserOrders']">`（滚动/DOM 保留）+ 新建 `stores/userOrders.js`（tab 记忆，URL 无 query 时兜底，跨模块往返不丢筛选）；「我的订单」菜单改 button 接管导航（router-link+@click.prevent 存在监听器顺序竞态）
- [x] **T38** 入口迁移：RecentActivity 快捷入口、侧边栏 待支付/待发货/待收货/已完成、OrderDetail 返回列表目标 → `/user/orders*`；OrderList.vue 标记 DEPRECATED（不再路由引用）
- [x] **T39** Playwright 验证（iPhone13 触控 + 桌面）：内嵌布局 ✅ / 筛选跨模块往返保持 ✅ / 重复点击不重置 ✅ / 左右滑动切 tab ✅ / 详情进出保持筛选 ✅ / 旧链接 /orders 重定向 ✅ / 未登录鉴权 ✅ / 桌面端全链路 ✅

## 备忘（未开发）
- [ ] **T40** 后台管理模块备忘见 `notes-admin-module.md`：个人中心订单统计（`/api/user/overview` 接口缺失）、发货状态实时推送、管理员提权入口 —— **仅记录，未开发**