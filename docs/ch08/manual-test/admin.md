# 后台管理模块 — 手工测试（Manual Test）

> 项目：dyshop 购物程序 · 模块：后台管理（ch08）
> 前置：`sql/data.sql` 已执行（含 admin 种子，密码 admin123）；`dyshop-api`(:8081) 与前端(:5173) 已启动
> 状态：M 段 curl 已全过（2026-08-05）；N 段浏览器用例已用 Playwright 自动冒烟覆盖主链路，剩余人工手测待回填
> 工具：终端 curl + 浏览器（参考 docs/ch07/manual-test 的既有风格）

## M 段：后端接口 curl 联调

发起前先登录拿 token（`TOKEN=...`；管理员登录凭证 admin/admin123）。

| 编号 | 操作 | 期望 |
|---|---|---|
| M1 | `POST /api/auth/login {username:admin,password:admin123}` | 成功，`data.user.role=1` |
| M2 | 普通注册用户带自己 token 访问 `GET /api/admin/stats/overview` | 403 FORBIDDEN |
| M3 | admin token `GET /api/admin/stats/overview` | 5 个计数器字段齐全 |
| M4 | `GET /api/admin/stats/trend?days=7` / `days=30` / `days=all` | 三数组同长度；`all` 覆盖最早订单日起 |
| M5 | 付一单后趋势交易额对应当天 +pay_amount（compare pay_time 当天） | 口径正确（status1/2/3） |
| M6 | `GET /api/admin/products?keyword=耳机&categoryId=1&status=1&page=1&size=10` | 过滤正确、含 categoryName |
| M7 | `POST /api/admin/products` 无规格商品 `{...stock=10,specs:null}` | 成功；GET 详情回读正确 |
| M8 | `POST /api/admin/products` 有规格 `{specs:[...],skus:[{stock:3},{stock:4}]}` | 成功；`product.stock=7`（自动汇总） |
| M9 | `POST /api/admin/products` 非法规格 JSON | 400「规格数据格式错误」 |
| M10 | `PUT /api/admin/products/{id}/status?status=0` | C 端 `GET /api/products` 不再返回该商品 |
| M11 | `DELETE /api/admin/products/{id}`（该商品有订单/购物车/收藏引用） | 400「存在交易/收藏引用，请改为下架」 |
| M12 | 新建无引用商品后 `DELETE` | 成功；C 端/后台列表消失（逻辑删除） |
| M13 | `POST /api/admin/categories（parentId=0 顶级）` → `PUT` 改名+排序 → `PUT status=0` | C 端 `GET /api/categories` 不再返回；恢复 status=1 回显 |
| M14 | 分类下有商品时 `DELETE` | 400「分类下存在商品」 |
| M15 | `GET /api/admin/users?keyword=admin` | 命中 admin 账号 |
| M16 | `PUT /api/admin/users/{buyerId}/role?role=1` → 该买家登录可访问后台；再 `role=0` 恢复 | 授权生效/回收 |
| M17 | `PUT /api/admin/users/{buyerId}/status?status=1` → 旧 token 调任意需认证接口 | 401「未认证或登录已过期」（JwtAuthFilter 直接拒绝，账号立即失效） |
| M18 | 对自己 `PUT /api/admin/users/{me}/status=1` 或 `role=0` | 400「不能操作当前登录账号」 |
| M19 | `GET /api/admin/orders?keyword=订单号片段` 与 `keyword=收货手机号` | 分别命中对应订单 |

> M 段结果（2026-08-05 实测）：M1–M19 全部通过 ✅

## N 段：浏览器手测

管理员 `admin/admin123`、普通买家账号各一（双浏览器/隐身窗口并行）。

| 编号 | 场景 | 操作 | 期望 |
|---|---|---|---|
| N1 | 买家登录 | 买家账号登录后回原页面 | 不出现后台入口；直接访问 `/admin/dashboard` 被重定向回 C 端 |
| N2 | 管理员登录分流 | admin 登录 | 跳 `/admin/dashboard`；顶栏显示 admin + 返回商城 + 退出 |
| N3 | 仪表盘 | 进入 dashboard | 6 张指标卡（2×3）数字合理、卡片等高对齐；右上「实时业务概览 · 更新于 HH:MM」；刷新按钮更新时间戳；导出数据生成 CSV（含概览+趋势明细，Excel 中文正常）；时间切换器仅一组，切换后双图同时刷新；柱状/折线样式正常；缩窗至 <1200px 卡片变 2 列、图表堆叠，<768px 卡片单列 |
| N4 | 无数据态 | 空库（或全取消）看 all 维度 | 图表显示空态文案不白屏 |
| N5 | 商品筛选 | 关键词/分类/状态组合筛选、翻页 | 列表正确；空结果有提示 |
| N6 | 新增商品 | 新建无规格商品（stock=10）保存 | 列表出现；C 端商品详情可购 |
| N7 | 规格编辑 | 编辑有规格商品 specs/skus JSON 保存 | 保存成功且总库存=Σsku.stock；非法 JSON 前端先提示 |
| N8 | 上下架 | 点上下架确认 | 状态标签即时切换；C 端对应隐藏/出现 |
| N9 | 删除保护 | 删除有订单引用的商品 | toast「存在交易引用，请改为下架」 |
| N10 | 分类管理 | 新增/改名/启停/删除 | C 端分类同步；有商品分类删除被拦 |
| N11 | 用户管理 | 搜索、设某买家为管理员、禁用某买家 | 授权后该买家可进后台；禁用后该买家已登录会话受访 403 跳登录 |
| N12 | 自操作保护 | 对 admin 自己进行禁用/降级 | 操作按钮不显示（或操作被拦） |
| N13 | 订单搜索 | 订单页输入订单号/手机搜索 | 命中；发货流程仍可走通 |
| N14 | 退出登录 | 顶栏退出 | 回 C 端首页；再访问 `/admin/dashboard` 跳登录 |
| N15 | 回归 | 首页/商品/购物车/结算/订单全链路 | 无回归；take build 通过 |
| N16 | 独立后台登录页 | 未登录访问 `/admin/dashboard` | 跳 `/admin/login?redirect=/admin/dashboard`（独立登录窗口） |
| N17 | 后台整体规范 | 逐页浏览 orders/products/categories/users | 各页统一：内容宽 1360px 居中、页头 h1 + 右操作、表格卡片化（8px 圆角+阴影、末行无分隔线）、抽屉贴右侧、弹窗居中，视觉与仪表盘同一体系 |
| N18 | 样式隔离 | 进入 C 端首页/登录页/订单页 | 与后台视觉无互扰（后台公共样式 `.admin-page` 隔离，不泄漏 C 端） |
| N19 | 非管理员登录被拒 | 后台登录页用买家账号提交 | 页内提示「该账号无后台权限」，留在登录页；C 端会话不受影响 |
| N20 | 乱码回归 | 登录后台看顶栏昵称 / 各列表中文 | 全部正常无乱码（修复 admin 昵称双重编码） |
| N21 | 会话隔离 | 后台登录后回 C 端首页；C 端买家登录后进后台；后台退出 | C 端首页显示游客态（不显示管理员账号）；双会话共存互不影响；后台退出不清 C 端买家会话 |

> 回填规则：完成后将上文 `[ ]` 打勾，失败用 ⚠️ 标注并附现象/日志。