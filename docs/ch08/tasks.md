# 后台管理模块 — 任务清单（Tasks）

> 状态：开发完成（后端 curl 联调 M1–M19 全过 + 前端 build 通过 + Playwright 冒烟通过），浏览器手测 N 用例待用户回填
> 图例：[x] 完成　[ ] 待做

## 数据与基建
- [x] **T1** `sql/data.sql` 预置管理员 `admin`（BCrypt `admin123`，role=1）；开发库已执行（user id=7）
- [x] **T2** 前端引入 echarts（按需注册 Bar/Line/Grid/Tooltip/Legend + CanvasRenderer）+ `useEChart` composable

## 后端
- [x] **T3** `OverviewVO` / `TrendVO` + `CacheControlFilter` no-store 加 `/api/admin/stats/**`
- [x] **T4** `AdminStatsService/Impl`：overview（今日订单数/今日交易额/待发货/商品数/用户数）+ trend（7/30/all 按日聚合，空天补 0）
- [x] **T5** `AdminStatsController` GET `/api/admin/stats/overview`、GET `/api/admin/stats/trend?days=`
- [x] **T6** `ProductDTO` + `AdminProductVO`（含 categoryName）；`AdminProductService/Impl`：分页列表（keyword/categoryId/status）→ 详情 → create/update（规格 JSON 校验 + stock=Σsku.stock）→ 上下架 → 删除（cart_item/favorite/order_item 三表引用检测）
- [x] **T7** `AdminProductController`：GET(分页) / GET{id} / POST / PUT{id} / PUT{id}/status / DELETE{id}
- [x] **T8** `CategoryDTO` + `AdminCategoryService/Impl`：列表/新增/改名+排序/启停/删除（含子分类商品引用 400）+ `AdminCategoryController`
- [x] **T9** C 端 `ProductServiceImpl` 列表/详情按 `category.status=1` 过滤（分类停用隐藏；`CategoryServiceImpl.listEnabled` 原有 status=1 过滤保持不变）
- [x] **T10** `AdminUserService/Impl`：列表（keyword 用户名/昵称/手机号）/禁用启用/授权取消（自操作 400）+ `AdminUserController`
- [x] **T11** `OrderService.adminList` 支持 `keyword`（订单号模糊 / 收货手机号精确）+ 状态组合；`AdminOrderController` 透传
- [x] **T12** 后端 compile 通过；curl 联调 M1–M19 全部通过（详见 manual-test/admin.md）

## 前端
- [x] **T13** `api/admin/`：stats.js / product.js / category.js / user.js（复用 `request`）
- [x] **T14** `AdminLayout.vue` 重写（侧边栏 5 菜单 + 顶栏 + 退出登录）
- [x] **T15** 路由：`/admin` 重定向 `/admin/dashboard`；新增 dashboard/categories/users 子路由；`beforeEach` 角色校验（fetchUserInfo 兜底）；`Login.vue` 按 role 分流（无 redirect 时管理员进后台）；个人中心侧边栏管理员「后台管理」入口
- [x] **T16** `Dashboard.vue`：5 张概览卡片 + 订单数柱状 / 交易额折线 + 7天/30天/全部切换
- [x] **T17** `ProductManage.vue` 重写：筛选/表格/上下架/删除 + 新增编辑抽屉（含规格 SKU JSON 折叠高级区 + 前端 JSON 预校验 + 有规格禁用库存输入）
- [x] **T18** `CategoryManage.vue`：表格 + 新增/编辑弹窗 + 启停/删除
- [x] **T19** `UserManage.vue`：列表 + 搜索 + 授权/禁用（自操作显示「当前账号」）
- [x] **T20** `OrderManage.vue`：关键词搜索框接入（订单号/收货手机号）
- [x] **T21** `npm run build` 通过；Playwright 冒烟 13 项全过（买家拦截/管理员分流/仪表盘卡片与双图/趋势切换/商品筛选/新增/编辑/删除/分类/用户/订单/退出）；N 手测用例待用户回填

## 文档
- [x] **T22** spec / plan / tasks / manual-test 按开发结果回填；`docs/ch07/notes-admin-module.md` P3 已解决（后台用户管理授权 + SQL 预置管理员）

## 2026-08-05 补充（独立后台登录页 + 数据乱码修复）
- [x] **T23** `AdminLogin.vue` 独立后台登录页 `/admin/login`：管理员独立登录窗口（共用登录接口，`role===1` 放行；非管理员清会话留页提示；`?denied=1` 提示；已登录管理员访问自动进后台）
- [x] **T24** 路由守卫重构：`meta.role==='admin'` 未登录/无用户信息/非管理员 → 一律引导 `/admin/login`（带 redirect 回跳与 denied 标记）；C 端 `/login` 页尾加「前往后台登录」入口
- [x] **T25** 修复 admin 种子数据入库乱码（开发库 nickname 双重编码 `ç®¡ç†å˜` → 正确「管理员」；根因：docker exec 导入 UTF-8 文件时客户端未指定 utf8mb4，`data.sql` 源文件本身无误，重导时须 `--default-character-set=utf8mb4`）
- [x] **T26** Playwright 验证 5 项全过：未登录访问后台→跳独立登录页 / 买家账号被拒（留在页内提示）/ 管理员登录回跳 dashboard / 顶栏昵称无乱码 / 买家已登录访问后台→denied 提示；`npm run build` ✅

## 2026-08-05 ch08.2（仪表盘企业级布局重构）
- [x] **T27** 后端 `OverviewVO` / `AdminStatsServiceImpl` 新增 `waitPayCount`（status=0 订单数），凑齐 6 指标 → curl 验证 6 字段齐全
- [x] **T28** 新建 `DashboardHeader.vue` / `MetricCard.vue` / `ChartSection.vue`：统一卡片规范（104px 高 / 8px 圆角 / `0 2px 8px rgba(0,0,0,.04)` 阴影 / 1.25rem 内边距），MetricCard 六色 tone 点 + 固定三行布局
- [x] **T29** 新建 `useDashboardStats.js`（组合式：overview/trend/range/loading/updatedAt 只读 + loadOverview/loadTrend/refresh/switchRange/exportCsv）与 `utils/formatters.js`（fmtAmount/fmtCount/fmtDateTime）
- [x] **T30** `Dashboard.vue` 重构：gird 3×2 指标卡 + 共享时间筛选器（唯一一组 range-tabs 居中驱动双图）+ DashboardHeader（刷新/导出 CSV）+ ChartSection 双图并排；响应式断点 1199px / 767px；间距改 rem 体系
- [x] **T31** `main.css` 新增 `--sp-sm/--sp-md/--sp-lg/--card-radius/--card-shadow` tokens（含 dark mode），`AdminLayout.vue` 侧边栏 220→208px、顶栏 padding 收紧
- [x] **T32** `npm run build` ✅；Playwright 冒烟 11 项全过（新增 2×3 卡片/唯一筛选器/992px 两列+堆叠图表/640px 单列断言）；导出 CSV 下载 + 刷新更新时间戳另测通过；spec 5.1/5.2、manual-test N3 回填
- [x] **T33** 新建共享后台样式 `assets/admin.css`（`.admin-page` 包装类隔离）：页头/表格卡片化（8px 圆角+`--card-shadow`+边框）/筛选/操作钮/分页/抽屉（贴右）/弹窗（居中）/确认框/表单字段/状态胶囊五色（on/off/warn/admin/buyer）；统一 rem 间距与 1360px 内容宽，全页面 `padding 1.5rem`、`max-width 85rem` 居中
- [x] **T34** 四个管理页接入：OrderManage（toolbar 改共享 input/op、抽屉加 detail 类 460px、删 ~300 行重复样式）、ProductManage（规格 textarea 改 `.textarea-mono`、保留 specs/switch/row3 独有）、CategoryManage（scoped 全删零独有）、UserManage（禁用改 warn 橙胶囊、保留 self）；`npm run build` ✅
- [x] **T35** Playwright 全页面规范冒烟 8/8：四页 admin-page + 1360px 宽度断言、表格卡片化（圆角/阴影）、订单详情抽屉贴右、商品抽屉 640px 贴右、分类弹窗居中、用户角色标签；仪表盘回归 11/11；C 端首页/登录页样式零泄漏（input 高度仍 40px 自有样式）

## 2026-08-05 ch08.3（后台独立系统 · 双 token 会话隔离）
- [x] **T36** 后端新增 `GET /api/admin/users/me`（AdminUserService.me + 控制器）：后台侧拉当前管理员信息；curl 验证 admin 200 / buyer 403
- [x] **T37** `utils/auth.js` 双轨 token：`dyshop_token`（C 端）与 `dyshop_admin_token`（后台）互不覆盖 + `getTokenForPath` 按 `/admin/**` 路径选 token；`request.js` 401 按域清 token 并跳对应登录页
- [x] **T38** 新建 `stores/admin.js`（login 仅管理员放行不写 C 端 token / fetchAdminInfo / adopt / logout）；路由守卫改基于后台 token + adminStore；AdminLogin 改用 adminStore（非管理员不写 token 留在页内提示、有后台 token 自动进后台）
- [x] **T39** AdminLayout 顶栏昵称/退出、UserManage「当前账号」识别改后台会话；Login.vue 管理员账号登录 → `adminStore.adopt` 仅建后台会话直接进后台
- [x] **T40** `npm run build` ✅；隔离专项 8/8：后台登录回 C 端显示游客（★核心）/无 C 端 token 时 /user 跳登录/双会话共存/买家不显示于后台顶栏/后台退出不影响 C 端买家/退出后访问后台跳登录/刷新保持登录；C 端登录页管理员分流另测 ✅；仪表盘 11/11、页面规范 8/8 回归；spec 5.6/验收、manual-test N21 回填

## 2026-08-06 ch08.4（个人中心账户概览动态化）
- [x] **T41** 后端：`UserOrderOverviewVO`（totalConsumption / totalOrders / waitShip / waitReceive）+ `OrderService.overview`（一次查询当前用户订单聚合：累计消费=已完成订单应付合计，待发货/待收货计数）+ `UserController GET /api/user/overview`；curl 全链路验证：下单待发货 waitShip=1 → 后台发货+确认收货后 totalConsumption=798.0、waitShip=0；未认证 401
- [x] **T42** 前端：`api/user.js` 加 `fetchUserOverview`；`AccountOverview.vue` 静态占位改动态（fetch + 千分位格式 + 待收货/待发货 sub + 加载态兜底；优惠券/会员等级维持本地展示）；`npm run build` ✅；Playwright 验证个人中心显示「累计消费 ¥798.00 / 订单总数 1 / 待收货 0 · 待发货 0」
