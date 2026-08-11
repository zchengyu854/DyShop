# 客户端登录注册模块 — 任务拆解（Tasks）

> 前置：docs/ch02/spec.md。勾选状态随开发进度更新（☐ 未开始 / ☑ 完成 / ⚠️ 部分完成-待环境验证）。

## T 后端 — 实体与依赖

- [x] **T1** 实体 `User`（`com.dyshop.common.entity`，MyBatis-Plus 注解 + 逻辑删除，含密码/角色/状态字段）
- [x] **T2** `UserMapper`（`com.dyshop.api.mapper`，继承 BaseMapper）
- [x] **T3** `dyshop-api/pom.xml` 引入 jjwt-api / jjwt-impl / jjwt-jackson（版本由父 pom 统一管理）

## T 后端 — 安全链路

- [x] **T4** `JwtService`（`com.dyshop.api.security`）：HS256 签发（subject=userId + role claim）/ 解析
- [x] **T5** `JwtAuthFilter`（OncePerRequestFilter）：解析 Bearer token → 查库校验（含禁用）→ 写入 SecurityContext
- [x] **T6** `SecurityConfig`：放行注册/登录/商品/分类，注册 `BCryptPasswordEncoder`，注入 JWT 过滤器

## T 后端 — 接口

- [x] **T7** DTO：`RegisterDTO`（用户名/密码/昵称 + 参数校验）、`LoginDTO`
- [x] **T8** VO：`UserVO`（脱敏用户信息）、`LoginVO`（token + user）
- [x] **T9** `AuthService` / `AuthServiceImpl`：注册（唯一性校验 + BCrypt + 自动登录）、登录（BCrypt 校验 + 禁用校验 + 签发）、me
- [x] **T10** `AuthController`：`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me`

## T 前端 — 数据层

- [x] **T11** `api/auth.js`：`register` / `login` / `fetchMe` 封装
- [x] **T12** `stores/user.js`：补全 login / register / logout / fetchUserInfo actions，token 初始化自 localStorage
- [x] **T13** `utils/request.js`：`code=401` 清除本地 token，非登录请求跳转 `/login`

## T 前端 — 页面与守卫

- [x] **T14** `Login.vue`：居中卡片表单、错误提示、成功后回跳 redirect 或 `/`
- [x] **T15** `Register.vue`：前端校验（用户名格式/密码长度/两次一致）、成功后跳 `/`
- [x] **T16** 路由守卫：`requiresAuth` 未登录 → `/login?redirect=...`
- [x] **T17** `HomeHeader.vue` 登录态：未登录「登录/注册」，已登录「昵称→/orders + 退出」，刷新后拉取 `/me`

## T 验证与文档

- [x] **T18** 后端编译 + 重启服务 + 接口手测（注册/登录/me/错误分支）
- [x] **T19** 前端 `npm run build` 构建验证（✅ 626ms）
- [x] **T20** 手测记录 `docs/ch02/manual-test/auth.md`（用例 + 步骤 + 实际结果；U11-U19 浏览器项标注 ⚠️）
