# 客户端登录注册模块 — 开发计划（Plan）

> 前置：docs/ch02/spec.md、docs/ch02/tasks.md。计划按阶段推进，每阶段有明确产出与验证。

## P1 文档与准备（本期已完成）

- 产出：docs/ch02/spec.md、docs/ch02/tasks.md、docs/ch02/plan.md
- 验证：文档评审

## P2 后端认证链路（T1–T10）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P2.1 | User 实体 + UserMapper + jjwt 依赖 | 编译通过 |
| P2.2 | JwtService（签发/解析） | 编译通过 |
| P2.3 | JwtAuthFilter + SecurityConfig（放行/拦截/密码编码器） | 编译通过 |
| P2.4 | DTO/VO + AuthService + AuthController | 编译通过 |
| P2.5 | 重启 api 服务（8081） | curl 手测 |

- 依赖风险：common 模块新增实体需先于 api 编译（`./mvnw clean install`）
- 配置：`application-dev.yml` 已有 `dyshop.jwt.*`（secret ≥32 字节、有效期 24h），无需改动

## P3 前端认证（T11–T17）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P3.1 | api/auth.js + user store actions + request 401 处理 | 代码评审 |
| P3.2 | Login.vue / Register.vue（Apple 风格表单页） | `npm run build` |
| P3.3 | 路由守卫（requiresAuth） | 浏览器手测 |
| P3.4 | HomeHeader 登录态三态（未登录/已登录/退出） | 浏览器手测 |

## P4 手测与文档（T18–T20）

- 产出：docs/ch02/manual-test/auth.md（用例、步骤、实际结果、通过/失败标注）
- 验证：所有用例执行完毕，失败项记录原因

### 进度记录（2026-08-03）

| 阶段 | 状态 | 备注 |
|---|---|---|
| P1 文档 | ✅ 完成 | spec / tasks / plan |
| P2 后端认证链路 | ✅ 完成 | 编译通过；curl 手测 U1-U10 全部通过（见 manual-test） |
| P3 前端认证 | ✅ 完成 | `npm run build` 通过；浏览器交互项标注 ⚠️ 待人工复核 |
| P4 手测与文档 | ✅ 完成 | 接口层全通过；U11-U19 浏览器复测项已标注 |

## 风险与对策

| 风险 | 对策 |
|---|---|
| JWT 密钥硬编码在 yml | 仅限 dev；生产需环境变量注入（yml 已注释提示） |
| 401 统一跳转误伤登录接口 | 前端拦截器跳过 `/auth/login` 请求路径 |
| 刷新后登录态丢失 | token 持久化 localStorage，store 初始化时读取，异步补拉 /me |
| 注册接口重复提交 | 用户名唯一索引兜底 + 接口预校验；前端按钮提交中禁用 |
