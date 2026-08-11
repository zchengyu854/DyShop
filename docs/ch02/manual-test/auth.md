# 客户端登录注册模块 — 手动测试记录（Auth）

> 模块：客户端登录/注册（`/login`、`/register`） · 关联接口：`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me`
> 用例基线：docs/ch02/spec.md §4 §8 · 任务：T18/T19/T20

## 1. 环境前置

| 项 | 要求 |
|---|---|
| MySQL 8+ | ✅ 本机 Docker 容器 `mysql-dev`（127.0.0.1:3306） |
| 数据库 | ✅ `backend/sql/schema.sql` + `data.sql` 已导入（utf8mb4） |
| 后端 | ✅ `dyshop-api`（8081），需重启加载认证代码 |
| 前端 | ✅ vite dev（5173，代理 `/api` → 8081） |

## 2. 启动步骤

```bash
# 后端（api 服务，8081）
cd backend && ./mvnw -pl dyshop-api spring-boot:run

# 前端（dev server，5173）
cd frontend && npm install && npm run dev
```

浏览器打开 http://localhost:5173/login

## 3. 测试用例

| 编号 | 用例 | 步骤 | 预期结果 | 实际结果 | 状态 |
|---|---|---|---|---|---|
| U1 | 注册成功 | `POST /api/auth/register`（新用户名） | `code=0`，返回 token + user（role=0） | ✅ 返回 token + 用户（id=1，昵称"爱丽丝"，role=0） | ✅ |
| U2 | 重复用户名 | 用 U1 用户名再注册 | `code=400, message="用户名已存在"` | ✅ code=400 "用户名已存在" | ✅ |
| U3 | 注册参数校验 | username=「a」/ password=「123」 | `code=400`（格式/长度提示） | ✅ code=400 "password: 密码长度需为 6~20 位" | ✅ |
| U4 | 登录成功 | `POST /api/auth/login`（U1 账号） | `code=0`，返回 token + user | ✅ 返回 token + user | ✅ |
| U5 | 密码错误 | 错误密码登录 | `code=401, message="用户名或密码错误"` | ✅ code=401 "用户名或密码错误" | ✅ |
| U6 | 用户不存在 | 不存在用户名登录 | `code=401`（统一提示，不区分） | ✅ code=401 "用户名或密码错误" | ✅ |
| U7 | me 带 token | `GET /api/auth/me` + U1 token | `code=0`，返回 user 信息（无密码字段） | ✅ 返回 id/username/nickname/role，无密码 | ✅ |
| U8 | me 无 token | `GET /api/auth/me`（无 Authorization） | `code=401, message="未认证或登录已过期"` | ✅ code=401 | ✅ |
| U9 | me 无效 token | 篡改 token | `code=401`（过滤器解析失败） | ✅ code=401 | ✅ |
| U10 | 密码落库加密 | 查库 `SELECT password FROM user` | BCrypt（`$2a$...`）密文，非明文 | ✅ `$2a$10$` 前缀，长度 60，非明文 | ✅ |
| U11 | 登录页访问 | 打开 `/login` | 居中卡片表单渲染正常（Apple 风格） | 页面已实现 + build 通过 | ⚠️ 浏览器复测 |
| U12 | 登录失败提示 | 页面输入错误密码提交 | 卡片内显示"用户名或密码错误"，不跳转 | 逻辑已实现（401 拦截跳过登录请求）；待浏览器确认 | ⚠️ 浏览器复测 |
| U13 | 登录成功跳转 | 正确账号登录 | 跳转 `/`，Header 显示昵称 + 退出 | 逻辑已实现 | ⚠️ 浏览器复测 |
| U14 | 未登录拦截 | 直接访问 `/cart` | 重定向 `/login?redirect=/cart` | 守卫已实现（getToken 校验） | ⚠️ 浏览器复测 |
| U15 | 回跳 | 登录页完成登录 | 跳回原目标页 `/cart` | 逻辑已实现（redirect query） | ⚠️ 浏览器复测 |
| U16 | 注册页校验 | 两次密码不一致提交 | 前端提示，不发请求 | 逻辑已实现（validate() 拦截） | ⚠️ 浏览器复测 |
| U17 | 注册成功自动登录 | 注册新账号 | 直接进入已登录态（Header 显示昵称） | 注册接口返回 token，store 已写入 | ⚠️ 浏览器复测 |
| U18 | 退出登录 | 已登录点「退出」 | token 清除，Header 回到「登录/注册」 | 逻辑已实现（logout action） | ⚠️ 浏览器复测 |
| U19 | 刷新保持登录 | 已登录状态刷新首页 | 仍为登录态，昵称异步加载 | 逻辑已实现（localStorage + fetchMe） | ⚠️ 浏览器复测 |

## 4. 验证记录（2026-08-03）

### 环境
- 后端：dyshop-api（8081），`Started in 6.6s`
- 前端：vite dev（5173）

### 执行结果

| 验证项 | 结果 |
|---|---|
| 三模块编译 `./mvnw -pl dyshop-api -am compile` | ✅ EXIT=0 |
| 接口 curl 手测（U1-U10） | ✅ 全部通过（注册/登录/me/错误分支/BCrypt 落库） |
| 回归：ch01 公开接口 | ✅ `/api/products`、`/api/categories` 无 token 均 200 |
| 前端构建（T19） | ✅ `npm run build` 通过（626ms） |
| 浏览器手测（U11-U19） | ⚠️ 无浏览器工具，待人工复核（逻辑已实现 + 构建通过） |

### 环境问题与解决
- Maven 下载依赖时 JVM TLS 握手失败（`Remote host terminated the handshake`）→ 用 `curl` 手动下载缺失 jar（maven-archiver 3.6.2、spring-boot-buildpack-platform、spring-boot-loader-tools、maven-common-artifact-filters）放入本地仓库解决
- 本地仓库缺少 `com.dyshop` 父 pom/common 构件 → `./mvnw -N install` + `-pl dyshop-common install` 补齐后启动成功

## 5. 回归说明

- 认证接口上线后，`/api/products/**`、`/api/categories` 仍保持公开免认证（回归 ch01 U10）
- 若重建数据库：`schema.sql` → `data.sql` 两步导入**必须加 `--default-character-set=utf8mb4`**，否则中文乱码
