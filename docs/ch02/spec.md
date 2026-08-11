# 客户端登录注册模块 — 规格说明（Spec）

> 项目：dyshop 购物程序 · 模块：客户端登录/注册（C 端 `/login`、`/register`）
> 状态：v1.0 定稿
> 关联：`backend/dyshop-api`（认证接口）、`frontend/src/views/user`（登录/注册页）、`frontend/src/stores/user.js`

## 1. 目标

在前后端分离架构下，实现购物程序**用户注册与登录**：注册即自动登录、登录签发 JWT、前端持久化 token 并展示登录态；`requiresAuth` 路由未登录时重定向到登录页，登录成功后回跳原目标页。

## 2. 范围

### 2.1 本期（In Scope）

| 编号 | 内容 | 归属 |
|---|---|---|
| S1 | 注册接口（用户名唯一校验、BCrypt 加密、注册即登录） | 后端 |
| S2 | 登录接口（密码校验、签发 JWT） | 后端 |
| S3 | 当前用户信息接口 `GET /api/auth/me`（需认证） | 后端 |
| S4 | JWT 认证链路：签发/解析、过滤器写入 SecurityContext、SecurityConfig 放行与拦截 | 后端 |
| S5 | 前端认证 API 封装 + 用户 Pinia store（登录/注册/登出/拉取用户信息） | 前端 |
| S6 | 登录页 / 注册页（表单校验、错误提示、登录后回跳） | 前端 |
| S7 | 路由守卫：`requiresAuth` 未登录跳转登录页 | 前端 |
| S8 | 首页 Header 登录态：未登录显示"登录/注册"，已登录显示昵称/订单/退出 | 前端 |
| S9 | 统一 401 处理：token 失效清本地 token 并跳登录页 | 前端 |
| S10 | 手测用例与记录（docs/ch02/manual-test/auth.md） | 文档 |

### 2.2 本期不做（Out of Scope）

- 用户资料修改、头像上传、邮箱/手机验证（后续模块）
- 购物车、下单、地址管理（依赖登录态，独立模块推进）
- 后台管理登录（dyshop-admin 独立模块）
- 密码找回 / 短信验证码

## 3. 页面结构（前端）

### 3.1 登录页 `/login`

```
┌──────────────────────────────────────────┐
│  (无 Header，独立居中卡片，灰底 #f5f5f7)      │
│  ┌────────────────────────────────────┐  │
│  │ dyshop  登录                        │  │
│  │ [ 用户名 ____________________ ]    │  │
│  │ [ 密码   ____________________ ]    │  │
│  │ [  登 录  ]  (蓝色胶囊按钮，全宽)      │  │
│  │ 还没有账号？ 立即注册 →              │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

- 提交失败：卡片内红字提示后端 message（如"用户名或密码错误"）
- 成功：写入 token + 用户信息，跳转 `redirect` 参数或 `/`

### 3.2 注册页 `/register`

```
┌──────────────────────────────────────────┐
│  ┌────────────────────────────────────┐  │
│  │ dyshop  注册                        │  │
│  │ [ 用户名（3~20 位字母/数字/下划线）]  │  │
│  │ [ 昵称（可选）____________________ ] │  │
│  │ [ 密码（6~20 位）________________ ]  │  │
│  │ [ 确认密码 ______________________ ]  │  │
│  │ [  注 册  ]                        │  │
│  │ 已有账号？ 去登录 →                 │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

- 前端校验：用户名格式、密码长度、两次密码一致（不一致不请求后端）
- 成功：自动登录并跳转 `/`（注册接口直接返回 token）

### 3.3 首页 Header 登录态

- 未登录：右侧导航显示「购物车 | 登录 | 注册」
- 已登录：右侧导航显示「购物车 | 昵称(→ /orders) | 退出」
- 页面刷新后：token 存在则显示已登录态，并异步拉取 `/api/auth/me` 补充用户信息

## 4. 接口定义（后端 dyshop-api）

> 统一前缀 `/api`；统一返回 `Result<T>`：`{ code, message, data }`，`code = 0` 成功。

### 4.1 POST `/api/auth/register` — 注册（公开）

请求体：

```json
{
  "username": "alice_01",
  "password": "123456",
  "nickname": "爱丽丝"
}
```

| 参数 | 类型 | 必填 | 校验 |
|---|---|---|---|
| username | string | 是 | `^[a-zA-Z0-9_]{3,20}$` |
| password | string | 是 | 6~20 位 |
| nickname | string | 否 | ≤20 位，缺省用用户名 |

响应 `data`（注册即登录）：

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": { "id": 1, "username": "alice_01", "nickname": "爱丽丝",
            "avatar": null, "phone": null, "email": null, "role": 0 }
}
```

约束：用户名已存在 → `code=400, message="用户名已存在"`；密码以 BCrypt 落库。

### 4.2 POST `/api/auth/login` — 登录（公开）

请求体：`{ "username": "alice_01", "password": "123456" }`

响应 `data`：同注册（token + user）。

约束：用户名不存在或密码错误（统一提示，不区分）→ `code=401, message="用户名或密码错误"`；账号被禁用 → `code=403, message="账号已被禁用"`。

### 4.3 GET `/api/auth/me` — 当前用户信息（需认证）

请求头：`Authorization: Bearer <token>`

响应 `data`：`{ id, username, nickname, avatar, phone, email, role }`（不含密码）。

约束：无 token / token 失效 → `code=401, message="未认证或登录已过期"`。

## 5. 数据模型（已有，见 backend/sql/schema.sql）

- `user`：id / username（唯一）/ password（BCrypt）/ nickname / avatar / phone / email / role（0买家 1管理员）/ status（0正常 1禁用）/ deleted（逻辑删除）/ create_time / update_time

本期无需新增表，种子数据中无用户（全部通过注册接口创建）。

## 6. 安全设计

- **密码存储**：Spring Security `BCryptPasswordEncoder`，明文不入库
- **JWT**：jjwt 0.11.5，HS256，密钥来自 `application-dev.yml` 的 `dyshop.jwt.secret`（≥32 字节），有效期 `expire-hours: 24`；载荷 `subject=userId`，`claim=role`
- **认证链路**：`JwtAuthFilter`（OncePerRequestFilter）解析 `Authorization: Bearer` → 查库（含禁用校验）→ 写入 `SecurityContext`（principal=userId，ROLE_USER/ROLE_ADMIN）
- **放行路径**：`/api/products/**`、`/api/categories`、`/api/auth/register`、`/api/auth/login`；其余需认证（401 统一 JSON）
- **前端 401**：拦截器清除本地 token，非登录请求跳转 `/login`
- **路由守卫**：`meta.requiresAuth` 未登录 → `/login?redirect=<原路径>`

## 7. 非功能要求

- N1：注册/登录接口免认证（公开）
- N2：用户名唯一（数据库唯一索引 + 接口预校验）
- N3：密码不明文传输（HTTPS 生产环境约定）、不明文落库
- N4：错误提示统一来自后端 message，前端不拼装错误细节
- N5：登录/注册页与首页视觉统一（Apple 风格：灰底 #f5f5f7、蓝胶囊按钮、SF 字体栈）

## 8. 验收标准

1. 注册新用户成功 → 返回 token，首页 Header 显示登录态（昵称）
2. 重复用户名注册 → 提示"用户名已存在"
3. 登录成功 → 返回 token；错误密码 → 提示"用户名或密码错误"
4. 携带 token 访问 `/api/auth/me` 返回用户信息；无 token 返回 401
5. 未登录访问 `/cart`、`/orders`、`/checkout` → 重定向登录页，登录后回跳
6. 首页 Header 未登录/已登录/退出三个状态切换正确
7. 后端接口 curl 手测通过（见 docs/ch02/manual-test/auth.md）
8. 前端 `npm run build` 通过
