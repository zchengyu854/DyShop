# 后台管理模块 — 规格说明（Spec）

> 项目：dyshop 购物程序 · 模块：后台管理（ch08）
> 状态：v1.0 定稿（开发完成：后端 curl 联调全过 + 前端 build 通过 + Playwright 冒烟通过）
> 关联：`docs/ch07/`（订单结算 + 后台订单发货雏形）、`docs/ch07/notes-admin-module.md`（待办备忘）、`docs/ch04/spec-sku-selector.md`（specs/skus JSON 结构）
> 前置：ch07 已完成 `/api/admin/**` hasRole('ADMIN') 安全规则与后台订单发货（`AdminOrderController` 位于 dyshop-api:8081）

## 1. 目标

将后台管理从「订单发货占位」扩展为完整管理后台：**管理员登录分流 → 仪表盘（订单/交易额统计曲线）→ 商品管理 → 分类管理 → 用户管理 → 订单管理增强**。解决 ch07 备忘 P3（管理员提权无入口：SQL 预置管理员 + 后台授权入口）。

## 2. 范围

### 2.1 本期（In Scope）

| 编号 | 内容 | 归属 |
|---|---|---|
| S1 | 管理员账号：`sql/data.sql` 预置 `admin`（BCrypt 密码 `admin123`）；登录共用 `POST /api/auth/login`（返回 role），前端按 role 分流 | 数据/前端 |
| S2 | 权限守卫修复：`meta.role` 目前未生效（守卫仅查 token 不查角色）；**独立后台登录页 `/admin/login`**（管理员独立登录窗口，买家/游客也可访问做校验）；未登录/非管理员访问 `/admin/**` 一律引导到该页并提示 | 前端 |
| S3 | 仪表盘：概览数字卡片（今日订单数 / 今日交易额 / 待发货订单 / 商品总数 / 用户总数）+ 订单数 & 交易额趋势曲线（近 7 天 / 近 30 天 / 全部历史切换） | 前后端 |
| S4 | 订单管理增强：ch07 已有列表/详情/发货，本期补搜索（订单号 / 收货手机号模糊）与状态/分页保留 | 前后端 |
| S5 | 商品管理：分页列表（关键词/分类/上下架筛选）+ 新增 + 编辑（基本信息 + 规格 SKU JSON 编辑器）+ 上下架 + 删除（引用检查） | 前后端 |
| S6 | 分类管理：列表 + 新增 / 改名 / 排序 / 启停 / 删除（有商品引用禁止删除） | 前后端 |
| S7 | 用户管理：分页列表（关键词搜索）+ 禁用/启用 + 设为管理员/取消管理员（解决 ch07 P3 提权入口） | 前后端 |
| S8 | 文档：spec / plan / tasks / manual-test | 文档 |

### 2.2 本期外（Out of Scope）

- 真实支付 / 退款 / 售后 / 优惠券管理（表结构无对应）
- 商品批量导入导出、订单导出
- 角色权限细分（RBAC，仅 管理员/买家 两级）
- 图表实时推送（页面进入/刷新时拉取即可）
- 管理员操作审计日志
- `dyshop-admin`（8082）空壳模块开发（决策见 D1）

## 3. 架构决策

### D1 后台接口归属：继续用 dyshop-api（8081）`/api/admin/**`

ch07 已在 dyshop-api 建立 `/api/admin/**` → `hasRole('ADMIN')` 与 `JwtAuthFilter`（ROLE_USER/ROLE_ADMIN）体系，`AdminOrderController`、前端 `api/admin/order.js`（baseURL `/api`）均已在此。**本期继续沿用**，不动 dyshop-admin（8082，当前空壳）。避免双服务双鉴权重复建设。

### D2 图表方案：ECharts 按需引入

前端当前零图表依赖。新增 `echarts`（`echarts/core` + `LineChart/BarChart/GridComponent/TooltipComponent/LegendComponent` + Canvas 渲染器按需注册），曲线图轻量打包。

### D3 管理员账号：SQL 预置 + 后台授权，注册接口不动

- 注册接口 `role` 固定 0（保持买家只能自助注册）
- `data.sql` 追加管理员种子：`username=admin`、密码 BCrypt（`admin123`），`role=1`
- 后台「用户管理」提供设管理员/取消管理员，形成后续管理员入口（ch07 P3 解决）

### D4 商品库存同步规则（有规格商品）

`product.stock` 为扣减真源（ch07 下单条件更新），`skus[].stock` 为尽力同步展示。后台保存规则：
- 有规格（specs/skus 非空且解析成功）：表单不直接编辑总库存，**保存时自动 `product.stock = Σ sku.stock`**（防不一致）
- 无规格：直接编辑 `product.stock`

### D5 删除保护（引用检查）

| 对象 | 规则 |
|---|---|
| 商品删除 | `cart_item` / `favorite` / `order_item` 任一有引用 → 400「该商品存在交易/收藏引用，请改为下架」；无引用 → 逻辑删除（`deleted=1`） |
| 分类删除 | 分类下存在未删除商品（含子分类）→ 400「分类下存在商品」；否则逻辑删除 |
| 分类启停 | 停用后 C 端该分类商品隐藏（C 端查询按分类 status 过滤，本期实现） |

### D6 统计口径

| 指标 | 口径 |
|---|---|
| 今日订单数 | `orders.create_time` 为今日（含待支付/已取消，即今日新下单总数） |
| 今日交易额 | `pay_time` 为今日 且 订单已支付成功（status ∈ 1/2/3，不含 4 已取消）Σ `pay_amount` |
| 待发货数 | 全量 status=1 |
| 商品总数 | 未删除商品数（不分上下架）；用户总数 = 未删除用户数 |
| 趋势-订单数 | 按日 `create_time` 聚合订单数 |
| 趋势-交易额 | 按日 `pay_time` 聚合已支付金额（status ∈ 1/2/3） |
| 全部历史 | 自最早订单日起逐日；数据为空的天补 0 |

## 4. 接口设计

全部接口需认证且 `ROLE_ADMIN`（现有 SecurityConfig `/api/admin/**` 已覆盖）；返回统一 `Result` 包裹（HTTP 200 + `code`）。分页沿用 ch07 的 `IPage` 风格（`records/total`）。统计类接口需加入 `CacheControlFilter` no-store 名单（防浏览器缓存）。

### 4.1 仪表盘统计

| 方法 | 路径 | 参数 | 返回 |
|---|---|---|---|
| GET | `/api/admin/stats/overview` | - | `OverviewVO` |
| GET | `/api/admin/stats/trend` | `days=7\|30\|all`（缺省 7） | `TrendVO` |

`OverviewVO`：`todayOrderCount, todayPaidAmount, waitPayCount, waitShipCount, productCount, userCount`（ch08.2 新增 `waitPayCount` 待支付订单数）

`TrendVO`：`dates[]（yyyy-MM-dd）, orderCounts[], paidAmounts[]`（长度 = 天数；`all` 为全历史长度）

### 4.2 订单管理（增强，ch07 已有接口 + 搜索参数）

| 方法 | 路径 | 参数 | 返回 |
|---|---|---|---|
| GET | `/api/admin/orders` | `?status=&keyword=&page=&size=`（新增 `keyword`：订单号模糊 或 收货手机号精确，二选一匹配） | 分页 `AdminOrderVO` |
| GET | `/api/admin/orders/{id}` | - | `AdminOrderVO`（已有） |
| POST | `/api/admin/orders/{id}/ship` | - | `void`（已有） |

### 4.3 商品管理（新增）

| 方法 | 路径 | 参数/请求体 | 返回 |
|---|---|---|---|
| GET | `/api/admin/products` | `?keyword=&categoryId=&status=&page=&size=` | 分页 `AdminProductVO` |
| GET | `/api/admin/products/{id}` | - | `AdminProductVO`（含 specs/skus 原文） |
| POST | `/api/admin/products` | `ProductDTO` | `void` |
| PUT | `/api/admin/products/{id}` | `ProductDTO` | `void` |
| PUT | `/api/admin/products/{id}/status` | `?status=0\|1` | `void`（上下架） |
| DELETE | `/api/admin/products/{id}` | - | `void`（引用检查见 D5） |

`ProductDTO`：

| 字段 | 校验 | 规则 |
|---|---|---|
| categoryId | 必填 | 分类必须存在且启用 |
| name | 必填 ≤100 | 商品名 |
| subtitle | 选填 ≤200 | 副标题 |
| mainImage / images | 必填/选填 | 主图 URL；images 逗号分隔 |
| detail | 选填 | 富文本 HTML |
| price | 必填 >0 | 售价（无规格商品生效） |
| originalPrice | 选填 ≥price | 划线价 |
| stock | 有规格时忽略 | 无规格商品库存，≥0 |
| status | 必填 0/1 | 上下架 |
| specs / skus | 选填 | JSON 字符串，结构见 ch04；空 = 无规格；保存前服务端 `parse` 校验（非法 JSON 或结构不符 → 400「规格数据格式错误」）；有规格时 `product.stock = Σ sku.stock` |

### 4.4 分类管理（新增）

| 方法 | 路径 | 参数/请求体 | 返回 |
|---|---|---|---|
| GET | `/api/admin/categories` | - | `List<CategoryVO>`（平铺，含 sort） |
| POST | `/api/admin/categories` | `CategoryDTO{parentId,name,sort}` | `void` |
| PUT | `/api/admin/categories/{id}` | `CategoryDTO{name,sort}` | `void` |
| PUT | `/api/admin/categories/{id}/status` | `?status=0\|1` | `void`（启停，停用 C 端隐藏） |
| DELETE | `/api/admin/categories/{id}` | - | `void`（引用检查见 D5） |

### 4.5 用户管理（新增）

| 方法 | 路径 | 参数 | 返回 |
|---|---|---|---|
| GET | `/api/admin/users` | `?keyword=&page=&size=` | 分页 `AdminUserVO` |
| PUT | `/api/admin/users/{id}/status` | `?status=0\|1` | `void`（禁用后 JwtAuthFilter 已拒绝其后续请求） |
| PUT | `/api/admin/users/{id}/role` | `?role=0\|1` | `void`（授权/取消管理员；目标为本人时 400 防自降级） |

`AdminUserVO`：`id, username, nickname, phone, email, role, status, createTime`

### 4.6 业务规则

| 场景 | 行为 |
|---|---|
| 非管理员访问 `/api/admin/**` | 403 FORBIDDEN（SecurityConfig 已处理，返回统一 Result） |
| 禁用用户 | `status=1`：JwtAuthFilter 已拦截（`user.getStatus()==0` 才放行），立即失效 |
| 本人操作限制 | 用户管理接口不可对**自己**执行禁用/降级（400「不能操作当前登录账号」） |
| 商品保存 | 有规格 → 总库存自动 = Σ sku.stock；规格 JSON 解析失败 → 400 |
| 商品改价 | 历史订单不受影响（订单为价格快照） |
| 分类停用 | C 端 `CategoryController`/商品查询按 `status=1` 过滤（本期补） |

## 5. UI 规范（后台，简洁表格风，沿用 OrderManage.vue 现有风格）

### 5.1 布局（AdminLayout.vue 重写）

- 左侧固定侧边栏（宽 208px，白底）：**仪表盘 / 订单管理 / 商品管理 / 分类管理 / 用户管理**；底部「返回商城」
- 顶栏：当前管理员昵称（`admin`）+ 退出登录（清 token → 跳 `/`）
- 内容区：顶栏下页面级滚动，各页自行控制宽度（统一 `max-width 1360px` 居中，`padding 1.5rem`）
- **共享样式 `assets/admin.css`**（ch08.2）：所有后台页面以 `.admin-page` 包装类复用同一套规范——页头（h1 1.375rem / 700）、表格卡片化（`--card-radius` 8px 圆角 + `--card-shadow` + 边框 + 末行去分隔线）、筛选/操作按钮、分页、抽屉（右侧贴齐）/弹窗（居中）/确认框、表单字段、状态胶囊（on 绿 / off 灰 / warn 橙 / admin 蓝 / buyer 灰）；间距统一 rem（`--sp-sm/md/lg`）；`.admin-page` 作用域隔离，零泄漏到 C 端；各页 scoped 仅保留独有样式（订单 tab/明细、商品规格区、用户「当前账号」等）
- `/admin` 默认重定向 → `/admin/dashboard`（原 `/admin/products` 改）

### 5.2 仪表盘（Dashboard.vue 重构，ch08.2 企业级布局）

**布局架构**（自上而下，Flex column + gap，禁手动 margin-left/right）：

```
┌──────────────────────────────────────────────────────────────┐
│ DashboardHeader: h1 仪表盘 · 实时业务概览 · 更新于 HH:MM   [刷新][导出数据] │
├──────────────────────────────────────────────────────────────┤
│ metric-grid  (3 列 × 2 行 = 6 张 MetricCard，104px 高)             │
│   今日订单数 | 今日交易额 | 待支付订单                              │
│   待发货订单 | 商品总数   | 用户总数                                │
├──────────────────────────────────────────────────────────────┤
│ 时间范围 [近7天|近30天|全部历史]  ← 居中共享切换器（单一状态驱动双图）      │
├──────────────────────────────────────────────────────────────┤
│ chart-row:  ChartSection(订单数趋势·柱状)  |  ChartSection(交易金额趋势·折线) │
└──────────────────────────────────────────────────────────────┘
```

| 元素 | 规范 |
|---|---|
| DashboardHeader.vue | h1（1.375rem/700）+ 副标题「实时业务概览 · 更新于 HH:MM」（刷新后更新）；右侧胶囊按钮：刷新（ghost，loading 禁用）/ 导出数据（primary 蓝底白字，无数据禁用） |
| MetricCard.vue | 统一 `height 6.5rem`（104px）、`border-radius 8px`、`shadow 0 2px 8px rgba(0,0,0,0.04)`、`padding 1.25rem`；顶部 label + 色点（blue/green/orange/purple/teal/ink 六色），中部大数字（1.5rem/700，tabular-nums，超长省略 + title tooltip），底部 hint（0.75rem 灰）——三行固定布局保证 6 卡对齐 |
| ChartSection.vue | 图表卡：标题 + ECharts 容器（高 280px）；props 收 `title` / `option`，watch option 自动 setOption（notMerge）；容器统一卡片规范 |
| 共享时间切换器 | 两图之间**唯一**一组胶囊（近7天/近30天/全部历史），居中于图表区上方；切换只发一次 `fetchTrend`，同时刷新两图 |
| 导出数据 | 前端生成 CSV（BOM 头保证 Excel 中文）：概览 6 指标 + 当前范围趋势明细（日期/订单数/交易金额），文件名 `dyshop-仪表盘-YYYYMMDDHHmm.csv` |
| 响应式 | ≥1200px：卡片 3 列 + 双图并排；768–1199px：卡片 2 列 + 图表堆叠；≤767px：卡片 1 列 + 页边距收紧（筛选器恒在图表上方） |
| 间距 | rem 体系：`--sp-sm 0.5rem / --sp-md 1rem / --sp-lg 1.5rem`；页内纵向排布用 Flex gap 1rem，卡片间距 gap 1rem |
| 趋势图 | ECharts，`#0071e3` 柱状 / `#34c759` 折线+渐变面积；Tooltip 显示数值；空数据显示空态文案（「暂无交易数据」） |
| 刷新 | 进入页拉取一次；右上角手动刷新（概览+趋势并行，成功后 toast「数据已刷新」）；不轮询（本期外） |

### 5.3 商品管理（ProductManage.vue 重写）

- 表格列：主图缩略（48px）/ 名称 / 分类 / 售价 / 原价 / 库存 / 销量 / 状态标签（上架绿 / 下架灰）/ 更新时间 / 操作
- 顶部：搜索框（名称模糊）+ 分类下拉 + 状态筛选 + 「＋ 新增商品」
- 操作：编辑（抽屉）、上下架切换（`status` 即时，确认弹窗）、删除（确认弹窗 + 引用 400 toast）
- 编辑抽屉（宽 640px，可滚动）：
  - 基本信息：分类（下拉）/ 名称 / 副标题 / 主图 URL / 轮播图 URL（逗号分隔）/ 详情（textarea）/ 售价 / 原价 / 库存 / 上架状态
  - 有规格时「库存」输入禁用并提示「有规格商品库存由 SKU 自动汇总」
  - **规格 SKU（JSON）高级区**：折叠面板；无规格商品展示「＋ 配置规格」→ 展开两个 textarea（specs / skus）+ 格式说明（引用 ch04 文档示例）+ 「设为无规格」清空按钮；保存时前端先 `JSON.parse` 校验，失败 toast 提示
- 新增商品：同一抽屉空表单（默认 status=0 下架，保存后手动上架）

### 5.4 分类管理（CategoryManage.vue 新建）

- 表格：分类名 / 排序 / 状态标签 / 创建时间 / 操作（编辑 / 启停 / 删除）
- 「＋ 新增分类」弹窗：名称 + 排序（parentId 本期固定顶级 0，平铺列表）
- 删除引用 400 toast

### 5.5 用户管理（UserManage.vue 新建）

- 表格：用户名 / 昵称 / 手机号 / 角色标签（管理员蓝 / 买家灰）/ 状态标签（正常 / 已禁用）/ 注册时间 / 操作
- 操作：设为管理员（或 取消管理员）、禁用（或 启用），均确认弹窗；对自己操作按钮隐藏
- 顶部：搜索框（用户名/昵称/手机号模糊）

### 5.6 登录分流与权限（AdminLogin.vue / Login.vue / router 改造）

- **后台独立登录态（ch08.3 双 token 隔离）**：后台会话与 C 端会话完全解耦——后台 token 存独立 key `dyshop_admin_token`（`utils/auth.js` 提供 get/set/remove），C 端沿用 `dyshop_token`；`request.js` 拦截器按路径取 token（`/admin/**` → 后台 token，其余 → C 端 token），401 时清除对应域 token 并跳对应登录页（后台 401 → `/admin/login`）
- **独立后台登录页 `/admin/login`**（AdminLogin.vue，ch08 补充需求）：
  - 白底居中卡片：「dyshop 管理后台」+「请使用管理员账号登录」+ 用户名/密码 + 登录后台按钮 +「← 返回商城」+ 演示账号提示（admin / admin123）
  - 提交走共用 `POST /api/auth/login`：返回 `user.role===1` 才写入后台 token 放行（成功回跳 `redirect` 参数或 `/admin/dashboard`）；非管理员 → 抛「该账号无后台权限」留在页内，**不写任何 token，C 端会话不受影响**
  - 已有后台 token 访问该页 → 补拉 `/admin/users/me` 校验后直接进后台；token 无效/非管理员 → 清除留页
  - 顶栏管理员昵称/用户管理「当前账号」识别均基于后台会话（`stores/admin.js`：login/fetchAdminInfo/adopt/logout）
- **后台守卫**（`meta.role==='admin'`）：无后台 token → `/admin/login?redirect=…`；刷新后补拉 `adminStore.fetchAdminInfo()`（`GET /api/admin/users/me`，失败清会话回登录页）；`role!==1` → 清会话引导登录页。**与 C 端是否登录完全无关**——管理员登录后台后回到商城首页仍显示游客态
- C 端登录页（Login.vue）：管理员账号登录 → 会话仅写入后台 token（`adminStore.adopt`）并直接进 `/admin/dashboard`，C 端不建立会话；普通用户正常 C 端登录。页尾「管理员？前往后台登录」入口
- 个人中心侧边栏：管理员额外显示「后台管理」入口（跳 `/admin/dashboard`）；后台顶栏「返回商城」跳 `/`（退出后台仅清后台 token）

## 6. 交互细节

- 所有写操作（上下架/删除/发货/授权/禁用）均需自绘确认弹窗（沿用 OrderManage 确认样式）
- 列表加载态「加载中…」、空态文案（沿用现有风格）
- 401/403 由 `request.js` 统一处理（403 时提示「无权限」并回首页）
- 数据变更后仅刷新本页数据，不全局广播（订单页沿用 `ORDER_NS` 不变）
- 图表 resize：侧边栏折叠/窗口 resize 时 `chart.resize()`

## 7. 验收标准

- 后端：`./mvnw compile -pl dyshop-api` 通过；curl 覆盖：admin 登录、非 admin 访问 403（含 `/admin/users/me`）、overview/trend 口径、商品 CRUD + 上下架 + 引用删除 400、规格 JSON 校验 400、库存自动汇总、分类 CRUD + 引用 400、用户搜索/禁用（禁用后旧 token 403）/授权（自操作 400）
- 前端：`npm run build` 通过；浏览器手测全流程（详见 `docs/ch08/manual-test/admin.md`）
- **会话隔离（ch08.3）**：管理员后台登录后回 C 端首页显示游客态；C 端买家登录不影响后台会话；后台退出不影响 C 端；双 token 共存不互覆盖
