# 客户端结算模块 — 规格说明（Spec）

> 项目：dyshop 购物程序 · 模块：结算闭环（结算页 → 下单 → 模拟支付 → 订单中心 / 后台发货）
> 状态：v1.0 开放稿（范围已与用户确认，待开发前定稿）
> 关联：`orders` / `order_item` / `payment` 三表已建（schema.sql 预留，本期无 DDL）；`cart_item.checked` 字段已建（ch05 预留，本期启用）；后端 `controller/order` 空目录待填充

## 1. 目标

打通 **勾选购物车（或立即购买）→ 结算页（选地址 / 清单 / 备注）→ 创建订单 → 模拟支付 → 我的订单（列表 / 详情 / 取消 / 确认收货）** 全链路；后台支持订单查看与发货。页面风格参考**苹果官网**：结算页白底左主列 + 右侧灰底 sticky 摘要卡；订单页白底卡片列表。

## 2. 范围

### 2.1 本期（In Scope）

| 编号 | 内容 | 归属 |
|---|---|---|
| S1 | 购物车勾选启用（`cart_item.checked`）：勾选接口 + 前端复选框 / 全选 | 前后端 |
| S2 | 「立即购买」入口：商品详情页直达结算（绕过购物车） | 前端 |
| S3 | 结算页 `Checkout.vue`：地址选择、商品清单、备注、摘要、提交 | 前端 |
| S4 | 创建订单：事务下单、库存扣减、价格快照、购物车清理 | 后端 |
| S5 | 模拟支付（MOCK）：`payment` 流水 + 订单状态流转 | 后端 |
| S6 | 我的订单：列表（状态 tab）/ 详情 / 取消（待支付）/ 确认收货 | 前后端 |
| S7 | 后台订单管理：列表 / 详情 / 发货 | 前后端 |
| S8 | 文档：spec / plan / tasks / manual-test | 文档 |

### 2.2 本期外（Out of Scope）

- 超时未支付自动取消（定时任务；用户手动取消兜底）
- 真实支付渠道（仅 MOCK 模拟，`channel='MOCK'`）
- 运费 / 优惠券 / 积分 / 退款售后（表结构无对应字段）
- 待发货取消申请（仅待支付可取消）
- 后台商品管理（后续章节）
- 订单导出

## 3. 接口设计

全部接口需认证；后台接口需 `ROLE_ADMIN`（`SecurityConfig` 新增 `/api/admin/**` 规则，`hasRole('ADMIN')`；JwtAuthFilter 已注入 ROLE_ADMIN/ROLE_USER 权限）。DTO/VO 放 `dyshop-api/dto`、`dyshop-api/vo`（与 cart 一致）。

### 3.1 购物车勾选（CartController 新增）

| 方法 | 路径 | 请求体 | 返回 |
|---|---|---|---|
| PUT | `/api/cart/items/{productId}/checked` | `CartCheckedDTO {checked: 0/1}` | `void` |

- `CartService.listCart` 返回的 `CartItemVO` 增加 `checked` 字段（ch05 预留未启用，本期启用）

### 3.2 结算（C 端，需登录，principal=userId）

| 方法 | 路径 | 请求体 | 返回 |
|---|---|---|---|
| POST | `/api/orders` | `CreateOrderDTO` | `OrderVO` |
| GET | `/api/orders` | `?status=`（0~4，缺省=全部） | `List<OrderVO>` |
| GET | `/api/orders/{id}` | - | `OrderVO` |
| POST | `/api/orders/{id}/cancel` | - | `void` |
| POST | `/api/orders/{id}/pay` | - | `void` |
| POST | `/api/orders/{id}/confirm` | - | `void` |
| DELETE | `/api/orders/{id}` | - | `void`（仅已完成/已取消，逻辑删除） |

`CreateOrderDTO`：

| 字段 | 校验 | 规则 |
|---|---|---|
| source | 必填 | `cart`（购物车勾选结算）\| `buyNow`（立即购买） |
| addressId | 必填 | 必须是本人地址 |
| remark | 选填 | ≤200 字符（订单备注） |
| productId | 必填 | `buyNow` 模式下必传 |
| quantity | 必填 | `buyNow` 模式下 1~99 |

### 3.3 后台订单（需 ROLE_ADMIN）

| 方法 | 路径 | 参数 | 返回 |
|---|---|---|---|
| GET | `/api/admin/orders` | `?status=&page=&size=` | 分页 `AdminOrderVO` |
| GET | `/api/admin/orders/{id}` | - | `AdminOrderVO` |
| POST | `/api/admin/orders/{id}/ship` | - | `void`（发货） |

### 3.4 业务规则

| 场景 | 行为 |
|---|---|
| 结算数据来源 | `cart`：取当前用户 `checked=1` 的购物车条目；`buyNow`：按 `productId + quantity` 直接构造 |
| 地址校验 | `addressId` 非本人 → 404「地址不存在」；无可用地址由前端引导，后端不兜底 |
| 商品校验 | 逐条：商品存在、`status=1` 上架、`stock ≥ quantity`；任一失败 → 400「{name} 库存不足」/「{name} 已下架」 |
| 价格快照 | `order_item.price/subtotal` 取商品当前 `price`（快照），改价不影响历史订单 |
| 金额 | 无运费字段 → 运费 ¥0；`total_amount = pay_amount = Σ(price×quantity)` |
| 订单号 | `yyyyMMddHHmmss` + 8 位随机数字，唯一冲突重试（≤3 次） |
| 下单事务 | 插入 `orders`(status=0) → 批量插入 `order_item` → 条件扣库存 `UPDATE product SET stock=stock-#{q} WHERE id=#{id} AND stock>=#{q}`（影响行数 0 → 400 回滚）→ `cart` 模式删除已结算购物车条目 |
| 取消订单 | 仅待支付可取消（否则 400「订单状态不允许取消」）；事务内回补库存、置 4 已取消、`cancel_time`。**幂等（ch10）**：已是 4 → 直接返回成功（不回补库存） |
| 模拟支付 | 仅待支付可支付（否则 400「订单状态不允许支付」）；插入 `payment`（channel=MOCK、status=1、payment_no 唯一、paid_at=now、amount=pay_amount）→ 订单置 1 待发货、`pay_time=now`；支付成功 `product.sales += quantity`。**幂等（ch10）**：已是 1 → 直接返回成功（不重复插支付单/加销量） |
| 确认收货 | 仅待收货可确认（否则 400）；置 3 已完成、`finish_time`。**幂等（ch10）**：已是 3 → 直接返回成功（不重复发积分） |
| 删除订单 | 仅已完成(3)/已取消(4)可删（否则 400「仅已完成或已取消的订单可删除」）；`@TableLogic` 逻辑删除（deleted=1），列表/详情/后台自动过滤，`order_item` 保留作历史。**幂等（ch10）**：订单不存在（已删）→ 直接返回成功 |
| 超时自动取消 | `OrderTimeoutScheduler` 每 60s 扫描：`status=0 且 create_time ≤ now-15min` → 条件更新（`WHERE id=? AND status=0`，影响行数 0 即并发支付/重复触发时跳过）置 4 + `cancel_time` + 回补库存；单实例 @Scheduled 即可，多实例需分布式锁 |
| 后台发货 | 仅待发货可发货（否则 400）；置 2 待收货、`ship_time` |
| 越权访问 | C 端订单非本人 → 404「订单不存在」（不暴露存在性） |
| 状态机 | `0 待支付 →(支付) 1 待发货 →(admin 发货) 2 待收货 →(确认) 3 已完成`；`0 →(取消) 4 已取消` |

### 3.5 VO 设计

`CartItemVO`：+ `checked`（Boolean）

`OrderVO`：`id, orderNo, status, statusText, totalAmount, payAmount, remark, receiverName, receiverPhone, receiverAddr, payTime, shipTime, finishTime, cancelTime, createTime, items[]`
- `items[] = OrderItemVO`：`productId, productName, productImage, price, quantity, subtotal`
- `statusText` 由后端映射：待支付 / 待发货 / 待收货 / 已完成 / 已取消

`AdminOrderVO`：`OrderVO` 字段 + `userId, userName`（列表页展示下单人；详情复用同结构）

## 4. UI 规范（苹果官网风格）

### 4.1 购物车勾选（Cart.vue 改造）

| 元素 | 规范 |
|---|---|
| 复选框 | 24px 圆形 checkbox（选中 `var(--blue)`，苹果风）；行首左对齐 |
| 全选 | 列表头部「全选」+ 三态（全选/部分/无）；与购物车表头同行 |
| 摘要 | 按**勾选项**计算商品数量与合计（`cartStore` 增加 checked 维度） |
| 结算按钮 | 勾选为空时禁用置灰；点击跳转 `/checkout`（替换原「结算模块开发中」toast） |
| 勾选交互 | 乐观更新 + 失败回滚 + toast.error |

### 4.2 结算页（Checkout.vue 重写）

| 元素 | 规范 |
|---|---|
| 布局 | Grid 双栏：左主内容 `1.62fr`（约 62%）+ 右摘要 `minmax(300px, 1fr)`（约 38%）；右栏 `position: sticky; top: 24px` 防来回滚动；页面 `max-width 1140px` |
| 呼吸感 | 卡片垂直间距 `36px`（2.25rem）；卡片内边距 `32px`；标题与内容间距 `≥16px`；间距全程 rem 相对单位 |
| 卡片样式 | **去厚重阴影**：白底 + `1px var(--border-line)` 细边框 + 圆角 16px；摘要卡为淡灰底 `var(--bg-gray)` 无边框 |
| 地址默认态 | 地址模块高亮展示当前选中地址（姓名 17px 600 / 手机号脱敏 / 完整地址）+「默认」胶囊徽标；右侧「更改」蓝色文字按钮（`aria-haspopup="dialog"` + `aria-expanded` + `aria-controls`） |
| 地址选择 | 点「更改」从右侧滑入 **Slide-over 抽屉**（宽 `min(26rem,90vw)`，`transition 0.2s ease` + 遮罩淡入）；列表每项单选（radio 圆点 + 选中态蓝色边框 + ✓ 勾，`role="radiogroup/radio"` + `aria-checked`）；底部「＋ 新增收货地址」→ `/user/addresses` |
| 地址空态 | 无地址时展示虚线引导卡：定位图标 +「还没有收货地址」+「＋ 添加收货地址」按钮（非纯文本） |
| 商品清单卡 | 标题行右侧「共 N 件」/「立即购买」胶囊徽标；行：缩略图 72px / 名称 / `¥单价 × 数量` / 行小计，行间 `1px var(--border-line)` 分隔 |
| 备注卡 | 「订单备注（选填）」textarea ≤200 字，右下角 `N/200` 计数，聚焦蓝色描边 |
| 摘要卡 | 商品数量 / 商品总额 / 运费 免运费 / 应付合计（大号加粗）；「提交订单」蓝色胶囊全宽 44px；下方「演示项目 · 模拟支付」说明 |
| 提交流程 | 提交订单 → 创建成功 → 弹**模拟支付 Modal**（金额 +「确认支付」蓝按钮 +「稍后支付」）→ 确认后调 `pay` → toast + 跳 `/orders/{id}`；关闭跳详情（保持待支付，可稍后支付） |
| 响应式 | `≤900px` 单栏：左/右合并为一列，摘要随 DOM 顺序沉底，卡片间距缩为 `24px`、内边距 `20px` |

### 4.3 我的订单（OrderList.vue 重写）

| 元素 | 规范 |
|---|---|
| 页头 | 标题「我的订单」30px 700 + 胶囊 tab 行：全部 / 待支付 / 待发货 / 待收货 / 已完成 / 已取消（当前项蓝底白字） |
| 订单卡片 | 白底卡片列表：首行订单号 + 下单时间（灰）+ 状态文字（蓝/橙按状态）；商品缩略图横排（60px，点击跳商品）；合计行「共 N 件 合计 ¥xx」；底部操作区右对齐 |
| 操作按钮 | 待支付：去支付（蓝）/ 取消订单（文字红）；待收货：确认收货（蓝）；其余状态仅「查看详情」 |
| 取消/确认收货 | 自绘确认弹窗（取消提示「取消后将恢复商品库存」）→ toast 反馈 |
| 空态 | 各 tab 独立空态文案（如「暂无待支付的订单」+ 去逛逛链接） |

### 4.4 订单详情（OrderDetail.vue 重写）

- 顶部状态区：大号状态文字 + 金额；状态色与列表一致
- 信息卡顺序：收货信息（姓名/手机/地址）→ 商品列表（同结算页只读样式）→ 金额卡（商品总额 / 应付）→ 订单信息（订单号 / 下单时间 / 支付时间 / 发货时间 / 完成时间，无值显示 `-`）
- 底部操作按钮与订单列表规则一致（去支付 / 取消 / 确认收货）

### 4.5 后台订单（OrderManage.vue 重写，AdminLayout 内）

- 简洁表格（后台不追求苹果风）：订单号 / 用户 / 收货手机 / 金额 / 状态（带色标签）/ 下单时间 / 操作
- 操作：详情（侧拉或新卡片区显示订单明细）、发货（仅待发货可点，确认弹窗）

## 5. 交互细节

- 结算页进入：`cart` 模式复用 `cartStore` 勾选数据（实时展示）；提交时后端兜底校验（库存/下架可能变化）
- 立即购买：`/checkout?buyNow=1&productId=X&quantity=N`；结算页读 `route.query` 进入单品只读模式，提交 `source=buyNow`
- 下单成功后：后端已删除对应购物车条目 → 前端 `cartStore` 刷新（重新拉取）
- 支付 Modal 关闭不取消订单：仅关闭，订单保持待支付
- 401 / 400 / 404 错误由 `request.js` 统一处理（HTTP 200 + code≠0）
- 订单页 401 跳登录由路由 `meta.requiresAuth` 兜底

## 6. 验收标准

- 后端：`./mvnw install -pl dyshop-common && ./mvnw compile -pl dyshop-api` 通过；curl 覆盖：勾选切换、cart 下单、buyNow 下单、库存不足 400、无勾选 400、越权 404、取消回补库存、重复支付 400、状态流转全链、后台发货、非 admin 403
- 前端：`npm run build` 通过；浏览器手测全流程（详见 `docs/ch07/manual-test/order.md`）
