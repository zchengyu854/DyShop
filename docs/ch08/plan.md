# 后台管理模块 — 开发计划（Plan）

> 状态：P0–P8 完成（后端联调全过 + 前端 build 通过 + Playwright 冒烟通过），P9 手工验证待用户执行
> 顺序：数据 + 后端接口（可独立 curl 联调）→ 前端布局与页面 → 权限与分流 → 手工验证
> 约定：后端全部走 dyshop-api:8081 `/api/admin/**`（D1）；编译用 `./mvnw install -pl dyshop-common`（如涉 common）+ `./mvnw compile -pl dyshop-api`

## P0 基建：管理员账号 + 图表依赖
- [x] P0.1 `sql/data.sql` 追加管理员种子：`admin / admin123（BCrypt）`、`role=1、status=0`（开发库已执行，user id=7）
- [x] P0.2 前端 `npm i echarts`；按需注册（echarts/core + LineChart/BarChart/Grid/Tooltip/Legend + CanvasRenderer），封装复用 `useEChart`
- 验证：后端启动 SQL 无报错；`npm run build` 通过 ✅

## P1 后端：仪表盘统计接口
- [x] P1.1 `OverviewVO` / `TrendVO`（dyshop-api/vo）
- [x] P1.2 `AdminStatsService` + 实现：overview 计数（今日订单 / 今日交易额（`pay_time` 口径 status∈1/2/3）/ 待发货 / 商品数 / 用户数）；trend `days=7|30|all` 按日聚合订单数与已支付金额，空天补 0
- [x] P1.3 `AdminStatsController`（`/api/admin/stats/*`）
- [x] P1.4 `CacheControlFilter` 新增 `/api/admin/stats/**` no-store
- 验证：compile ✅；curl M1/M3/M4 ✅

## P2 后端：商品管理
- [x] P2.1 `ProductDTO`（categoryId/name/subtitle/mainImage/images/detail/price/originalPrice/stock/status/specs/skus + 校验）
- [x] P2.2 `AdminProductService.list`：分页（key name 模糊 / categoryId / status 过滤；JOIN category 取 categoryName）
- [x] P2.3 `detail`：含 specs/skus 原文
- [x] P2.4 `create/update`：有规格 → JSON 解析校验（失败 400）+ `stock = Σ sku.stock` 自动同步；无规格 → 直接 stock
- [x] P2.5 `changeStatus` 上下架；`delete`：cart_item/favorite/order_item 引用检测（有引用 400）
- [x] P2.6 `AdminProductController` + VO
- 验证：compile ✅；curl M6–M12 ✅

## P3 后端：分类 + 用户管理
- [x] P3.1 `CategoryDTO` + `AdminCategoryService`：列表/新增/改名+排序/启停/删除（含子分类商品引用 400）
- [x] P3.2 C 端 `ProductServiceImpl` 列表/详情按 `category.status=1` 过滤（分类停用隐藏）
- [x] P3.3 `AdminUserService`：list（keyword 用户名/昵称/手机号模糊）+ status 禁用/启用 + role 授权/取消（自操作 400）
- [x] P3.4 `AdminCategoryController` / `AdminUserController` + VO
- 验证：compile ✅；curl M13–M18 ✅

## P4 前端：后台骨架 + 权限分流
- [x] P4.1 `api/admin/` 统一后台 api（stats.js / product.js / category.js / user.js，复用现有 `request`）
- [x] P4.2 `AdminLayout.vue` 重写：左侧边栏（仪表盘/订单/商品/分类/用户）+ 顶栏（管理员名 + 返回商城 + 退出登录）
- [x] P4.3 路由：`/admin` 重定向 → `/admin/dashboard`；注册 5 个 admin 子路由
- [x] P4.4 权限：`Login.vue` 登录成功 role===1 跳 `/admin/dashboard`；`router.beforeEach` 校验 `meta.role=='admin'`（未登录跳登录；无角色信息补拉 `fetchUserInfo`；非管理员重定向 `/`）
- [x] P4.5 个人中心侧边栏管理员专属「后台管理」入口
- 验证：build ✅；Playwright：买家被拦 / admin 分流 ✅

## P5 前端：仪表盘
- [x] P5.1 `Dashboard.vue`：5 张概览卡片 + 订单数（柱状）/交易额（折线）双图卡 + 7天/30天/全部切换
- [x] P5.2 `useEChart` composable（init/setOption/resize/销毁监听）
- 验证：build ✅；Playwright：卡片 + 双图 canvas + 维度切换 ✅

## P6 前端：商品管理
- [x] P6.1 `ProductManage.vue` 重写：搜索/分类/状态筛选 + 表格（缩略图/名称/分类/价格/库存/销量/状态标签）+ 上下架/删除（确认弹窗）
- [x] P6.2 新增/编辑抽屉（640px）：基本信息表单 + 规格 SKU JSON 折叠高级区（specs/skus 两个 textarea + 设为无规格；前端 JSON.parse 预校验；有规格禁用库存输入框）
- 验证：build ✅；Playwright：新增（必填拦截→保存）→ 列表可见 → 搜索 → 编辑改价 → 删除 ✅

## P7 前端：分类 + 用户 + 订单增强
- [x] P7.1 `CategoryManage.vue`：列表表格 + 新增/编辑弹窗（名称+排序）+ 启停/删除
- [x] P7.2 `UserManage.vue`：列表 + 搜索 + 设管理员/取消/禁用/启用（确认弹窗；自操作隐藏显示「当前账号」）
- [x] P7.3 `OrderManage.vue`：加关键词搜索框（订单号/收货手机）并透传 `keyword` 参数；其余沿用
- 验证：build ✅；Playwright：分类/用户/订单页加载 ✅

## P8 手工验证
- [x] P8.1 自动化冒烟 13 项全过（详见 tasks T21）；curl M1–M19 全过
- [ ] P8.2 浏览器人工手测 N 用例待用户执行回填（docs/ch08/manual-test/admin.md N 段）