# 客户端结算模块 — 开发计划（Plan）

> 状态：P1-P8 完成（后端联调全过 + 前端 build 通过），P9 手工验证待用户执行
> 顺序：后端先行（可独立 curl 联调）→ 前端页面 → 后台 → 手工验证

## P1 后端：购物车勾选
- [x] P1.1 `CartCheckedDTO`（checked 必填 0/1）
- [x] P1.2 `CartService.updateChecked(userId, productId, checked)` + 实现（条目不存在 404）
- [x] P1.3 `CartItemVO` 增加 `checked` 字段；`listCart` 填充
- [x] P1.4 `CartController` 新增 `PUT /api/cart/items/{productId}/checked`
- 验证：`./mvnw install -pl dyshop-common`（如涉 common）→ `./mvnw compile -pl dyshop-api`

## P2 后端：订单域（C 端）
- [x] P2.1 实体：`Order`（orders 表）、`OrderItem`（order_item 表）、`Payment`（payment 表）放入 `dyshop-common/entity`，`@TableName` + `@TableLogic(deleted)`，映射 schema.sql 现有列
- [x] P2.2 `OrderMapper` / `OrderItemMapper` / `PaymentMapper`（`dyshop-api/mapper`）
- [x] P2.3 `CreateOrderDTO`（source/addressId/remark/productId/quantity + jakarta 校验）
- [x] P2.4 `OrderVO` / `OrderItemVO` / `AdminOrderVO`（含 statusText 映射）
- [x] P2.5 `OrderService` 接口 + `OrderServiceImpl`：
  - 下单 `createOrder(userId, dto)`：地址归属校验 → cart/buyNow 数据源 → 商品校验（存在/上架/库存）→ 订单号生成（唯一重试 ≤3）→ 事务插入 orders + order_item + 条件扣库存 + 删除已结算购物车条目
  - 列表 `listOrders(userId, status)`：按 create_time 倒序
  - 详情 `getOrder(userId, id)`：越权 404
  - 取消 `cancel(userId, id)`：仅待支付，事务回补库存 + 置 4 + cancel_time
  - 支付 `pay(userId, id)`：仅待支付，插入 payment 流水（MOCK、成功态、payment_no 唯一）+ 订单置 1 + pay_time + `product.sales` 累加
  - 确认收货 `confirm(userId, id)`：仅待收货，置 3 + finish_time
- [x] P2.6 `OrderController`（`/api/orders`）：POST / GET / GET {id} / POST {id}/cancel / POST {id}/pay / POST {id}/confirm
- 验证：`./mvnw compile -pl dyshop-api`

## P3 后端：后台订单接口 + 鉴权
- [x] P3.1 `SecurityConfig` 新增 `/api/admin/**` → `hasRole('ADMIN')`（需声明 `requestMatchers("/api/admin/**").hasRole("ADMIN")` 于 anyRequest 之前）
- [x] P3.2 `AdminOrderService`（或并入 OrderService）：分页列表（`status`/`page`/`size`，JOIN user 取 userName）、详情、发货 `ship`（仅待发货，置 2 + ship_time）
- [x] P3.3 `AdminOrderController`（`/api/admin/orders`）：GET 分页 / GET {id} / POST {id}/ship
- 验证：`./mvnw compile -pl dyshop-api`；重启后端（日志 `/tmp/dyshop-api.log`）

## P4 后端：curl 联调（M 用例，覆盖 spec 6 全部）
- [x] M1-M17 全部通过（详见 manual-test/order.md M 段；期间创建管理员账号 admin_07）

## P5 前端：购物车勾选
- [x] P5.1 `api/cart.js` 新增 `updateChecked(productId, checked)`
- [x] P5.2 `stores/cart.js`：getters `checkedItems / checkedTotalQuantity / checkedTotalPrice / allChecked`；actions `toggleChecked / setAllChecked`（乐观更新 + 回滚）
- [x] P5.3 `Cart.vue`：行首复选框（苹果风圆形）+ 头部全选（三态）；摘要按勾选计算；结算按钮改跳转 `/checkout`，勾选为空禁用
- 验证：`npm run build` ✅

## P6 前端：结算页 + 立即购买
- [x] P6.1 `api/order.js`：`createOrder / fetchOrders / fetchOrder / cancelOrder / payOrder / confirmOrder`
- [x] P6.2 `Checkout.vue` 重写：地址卡（选择弹层 + 管理地址入口 + 无地址引导）、商品清单（cart 勾选 / buyNow 单品的只读列表 +「立即购买」标签）、备注（≤200 计数）、摘要卡（免运费 + 应付）、提交订单
- [x] P6.3 模拟支付 Modal（`PayModal.vue` 公共组件：金额 + 确认支付 + 稍后支付）→ 支付成功 toast + 跳详情
- [x] P6.4 `ProductInfoPanel.vue`：「立即购买」→ `/checkout?buyNow=1&productId=X&quantity=N`（替换「结算模块开发中」toast）；下单成功后 cartStore 刷新
- 验证：`npm run build` ✅

## P7 前端：我的订单
- [x] P7.1 `OrderList.vue`：状态 tab（全部/待支付/待发货/待收货/已完成/已取消）+ 订单卡片（缩略图横排/金额/状态）+ 操作按钮（去支付/取消/确认收货 + 自绘确认弹窗）+ 空态
- [x] P7.2 `OrderDetail.vue`：状态区 + 收货信息 + 商品列表 + 金额 + 订单信息（时间线字段 `-` 兜底）+ 操作按钮
- 验证：`npm run build` ✅

## P8 前端：后台订单管理
- [x] P8.1 `api/admin/order.js`：`fetchOrders / fetchOrder / shipOrder`
- [x] P8.2 `OrderManage.vue`：表格列表（状态标签/分页）+ 详情抽屉 + 发货确认弹窗
- 验证：`npm run build` ✅

## P9 手工验证
- [ ] P9.1 浏览器全流程手测（N 用例，回填 `manual-test/order.md`）
- [ ] P9.2 回归：购物车/地址/首页 build 无回归（build ✅；手测 N6 回归用例待用户）
