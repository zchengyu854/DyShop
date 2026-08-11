# 客户端购物车模块 — 任务拆解（Tasks）

> 前置：docs/ch05/spec.md。勾选状态随开发进度更新（☐ 未开始 / ☑ 完成 / ⚠️ 部分完成-待环境验证）。

## T 后端 — 接口

- [x] **T1** `CartItem` 实体（`dyshop-common/entity`，@TableName("cart_item")）+ `CartItemMapper`（继承 BaseMapper）
- [x] **T2** `CartItemVO`（含商品信息）+ `CartAddDTO` / `CartQuantityDTO`（jakarta 校验）
- [x] **T3** `CartService` / `CartServiceImpl`：`listCart / addItem / updateQuantity / removeItem / clear`（库存上限 1~min(99,stock)、幂等累加、404 商品校验、列表跳过失效商品）
- [x] **T4** `CartController`：`/api/cart` 五个接口（principal=userId）
- [x] **T5** 后端编译 + 重启 + curl 手测（列表/加购/累加/超库存 400/未登录 401/改量/移除/商品 404）

## T 前端 — 数据层

- [x] **T6** `api/cart.js`：`fetchCart / addCartItem / updateCartItem / removeCartItem / clearCart`
- [x] **T7** `stores/cart.js`：`items / totalQuantity / totalPrice / fetchCart / addToCart / updateQuantity / removeItem / clear`

## T 前端 — 页面

- [x] **T8** `ProductInfoPanel.vue`：加入购物车接入（未登录跳登录 redirect 回跳；已登录加购 + 成功提示；失败提示回滚）
- [x] **T9** `Cart.vue`：左列表（缩略图/名称/单价/数量步进器/行小计/移除）+ 右摘要卡（商品数/合计/结算按钮占位）+ 空态 + Header/Footer
- [x] **T10** `HomeHeader.vue`：购物车入口数量角标（store 联动，未登录隐藏）

## T 验证与文档

- [x] **T11** 前端 `npm run build` 构建验证
- [x] **T12** 浏览器手测 + 记录 `docs/ch05/manual-test/cart.md` + tasks/plan 回填
