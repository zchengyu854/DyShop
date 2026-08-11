# 客户端购物车模块 — 规格说明（Spec）

> 项目：dyshop 购物程序 · 模块：客户端购物车（C 端 `/cart`，需登录）
> 状态：v1.0 定稿
> 关联：`backend/dyshop-api`（cart_item 表已建，接口本期新增）、`frontend/src/views/shop/Cart.vue`（占位改实现）、`frontend/src/stores/cart.js`（占位改实现）

## 1. 目标

实现**购物车**：加购、数量调整、移除、汇总与结算入口；页面风格参考**苹果官网购物袋（Bag）**——白底左商品列表 + 右侧灰底摘要卡。未登录加购引导登录（与收藏一致，redirect 回跳）。

## 2. 范围

### 2.1 本期（In Scope）

| 编号 | 内容 | 归属 |
|---|---|---|
| S1 | 后端购物车接口：列表 / 加购（累加）/ 改量 / 移除 / 清空 | 后端 |
| S2 | 前端数据层：`api/cart.js` + `stores/cart.js`（含购物车角标数量与合计） | 前端 |
| S3 | 详情页「加入购物车」接入：未登录跳登录（redirect 回跳）；已登录加购成功提示 | 前端 |
| S4 | `Cart.vue` 页面：左列表（图/名称/单价/数量步进器/行小计/移除）+ 右摘要（商品数/合计/结算按钮）+ 空态 | 前端 |
| S5 | `HomeHeader` 购物车入口数量角标（联动 cart store） | 前端 |

### 2.2 本期外（Out of Scope）

- 勾选结算（`cart_item.checked` 字段保留，结算模块启用）
- 结算页 / 收货地址 / 运费 / 优惠（`Checkout.vue` 占位，结算按钮提示"结算模块开发中"）
- 未登录本地购物车与登录合并
- 限购、活动价、批量删除

## 3. 接口设计

全部接口**需认证**（`/api/cart/**` 非白名单，SecurityConfig 自动拦截），principal=userId。

| 方法 | 路径 | 请求体 | 返回 |
|---|---|---|---|
| GET | `/api/cart` | - | `List<CartItemVO>` |
| POST | `/api/cart/items` | `{ productId, quantity }` | `void` |
| PUT | `/api/cart/items/{productId}` | `{ quantity }` | `void` |
| DELETE | `/api/cart/items/{productId}` | - | `void` |
| DELETE | `/api/cart` | - | `void`（清空，预留） |

`CartItemVO`：`cartItemId, productId, name, subtitle, mainImage, price, originalPrice, stock, sales, quantity`

### 业务规则

| 场景 | 行为 |
|---|---|
| 加购商品不存在/已下架 | 404「商品不存在」 |
| 已存在同商品 | 数量累加（幂等不报错） |
| 加购/改量超过库存 | 400「库存不足（剩余 N 件）」 |
| 数量上限 | 1 ~ `min(99, stock)`，越界 400「数量超出范围」 |
| 改量/移除不存在的条目 | 幂等成功 |
| 删除商品后购物车残留条目 | 列表接口跳过失效商品（不作为错误） |

## 4. UI 规范（苹果官网风格）

| 元素 | 规范 |
|---|---|
| 页面容器 | `max-width 980px` 居中，白底，标题「购物车」28px 600 |
| 列表行 | 白底，行间 `1px solid #e8e8ed` 分隔；缩略图 88px 圆角 12px；名称 17px 600；副标题/单价 13px 灰 |
| 数量步进器 | 胶囊白底 `1px solid var(--border)`：`− 数量 +`；`-` 到 1 置灰，`+` 到库存上限置灰 |
| 摘要卡 | 灰底 `var(--bg-gray)` 圆角 `var(--radius-lg)` 内边距 22px；商品数/合计行；结算按钮 `var(--blue)` 胶囊全宽（空车禁用） |
| 移除 | 行内红色小字「移除」，直接删除不弹确认（苹果风格） |
| 空态 | 居中：「购物车是空的」+ 蓝色「去逛逛」链接回首页 |
| Header 角标 | 购物车入口右侧 18px 红色圆点数字（总数 >0 显示，未登录不显示） |

## 5. 交互细节

- 加购成功：详情页提示「已加入购物车」，Header 角标同步
- 数量步进：本地先行更新，请求失败回滚并提示
- 结算按钮：点击提示「结算模块开发中，敬请期待」（与 ch04 购买按钮一致）
- 页面加载：进入 `/cart` 拉取列表；401 由 request.js 统一处理跳登录

## 6. 验收标准

- 后端：编译通过；curl 覆盖 列表/加购累加/超库存 400/未登录 401/改量/移除/404 商品
- 前端：`npm run build` 通过；浏览器手测全流程通过（详见 docs/ch05/manual-test/cart.md）
