# 客户端个人中心模块 — 手动测试记录（UserCenter）

> 模块：客户端个人中心（`/user/**`） · 关联接口：`GET/PUT /api/user/profile`、`PUT /api/user/password`、`GET/POST/DELETE /api/user/favorites`
> 用例基线：docs/ch03/spec.md §4 §7 · 任务：T16/T17/T18

## 1. 环境前置

| 项 | 要求 |
|---|---|
| 数据库 | ✅ `schema.sql` + `data.sql` 已导入；本期需追加 `favorite` 建表 DDL |
| 后端 | ✅ `dyshop-api`（8081），需重启加载个人中心代码 |
| 前端 | ✅ vite dev（5173） |
| 账号 | 使用 ch02 手测账号 `alice_01 / 123456`（或用注册接口新建） |

## 2. 启动步骤

```bash
# 追加收藏表（幂等）
docker exec -i mysql-dev mysql -uroot -proot dyshop < backend/sql/favorite.sql

# 后端（8081）
cd backend && ./mvnw -pl dyshop-api spring-boot:run

# 前端（5173）
cd frontend && npm run dev
```

浏览器打开 http://localhost:5173/user/profile

## 3. 测试用例

| 编号 | 用例 | 步骤 | 预期结果 | 实际结果 | 状态 |
|---|---|---|---|---|---|
| U1 | 资料查看 | 登录后 `GET /api/user/profile` | 返回当前用户资料（含昵称/手机号/邮箱） | ✅ 返回 alice_01 完整资料 | ✅ |
| U2 | 资料更新 | `PUT /api/user/profile`（新昵称+手机号） | `code=0`，返回更新后 UserVO | ✅ 昵称/手机号/邮箱全部生效 | ✅ |
| U3 | 资料校验 | 手机号非法（如 `123`）/邮箱非法 | `code=400`（格式提示） | ✅ phone/email 均返回格式提示 | ✅ |
| U4 | 修改密码成功 | `PUT /api/user/password`（正确原密码） | `code=0`；随后旧密码登录失败、新密码登录成功 | ✅ 旧密码 401，新密码 654321 登录成功 | ✅ |
| U5 | 原密码错误 | 错误原密码改密 | `code=401, message="原密码错误"` | ✅ code=401 "原密码错误" | ✅ |
| U6 | 新密码长度 | newPassword=「123」 | `code=400`（长度提示） | ✅ code=400 "新密码长度需为 6~20 位" | ✅ |
| U7 | 收藏添加 | `POST /api/user/favorites/3` | `code=0`；重复添加仍成功（幂等） | ✅ 首次与重复均 code=0 | ✅ |
| U8 | 收藏商品下架 | `POST /api/user/favorites/9`（下架商品） | `code=404, message="商品不存在"` | ✅ code=404 "商品不存在" | ✅ |
| U9 | 收藏列表 | `GET /api/user/favorites` | 分页返回收藏商品（排序倒序，含商品快照） | ✅ 返回 2 条（耳机/音箱），倒序，含价格/图片 | ✅ |
| U10 | 取消收藏 | `DELETE /api/user/favorites/3` | `code=0`；列表不再包含该商品；重复删除幂等 | ✅ 删除后 total=1，仅剩耳机 | ✅ |
| U11 | 未登录访问 | 无 token `GET /api/user/profile` | `code=401` | ✅ code=401 | ✅ |
| U12 | 个人中心布局 | 打开 `/user/profile` | 侧边栏 + 内容区渲染，侧边栏含资料/密码/收藏/订单/地址 | 页面已实现 + build 通过 | ⚠️ 浏览器复测 |
| U13 | 资料编辑保存 | 页面修改资料提交 | 提示成功，昵称更新（含 Header） | 逻辑已实现 | ⚠️ 浏览器复测 |
| U14 | 修改密码流程 | 页面改密成功 | 自动登出跳登录页，新密码可登录 | 逻辑已实现 | ⚠️ 浏览器复测 |
| U15 | 收藏操作 | 页面移除收藏 | 卡片消失；空态提示出现 | 逻辑已实现（useFavorites） | ⚠️ 浏览器复测 |
| U16 | 导航入口 | 侧边栏订单/地址 | 跳转 `/orders`、`/addresses`（占位页） | 路由已配置 | ⚠️ 浏览器复测 |

> ⚠️：无浏览器工具项，代码实现 + build 通过后由人工复核。

## 4. 验证记录（2026-08-04）

### 执行结果

| 验证项 | 结果 |
|---|---|
| favorite 表 DDL | ✅ 已执行 `backend/sql/favorite.sql`（容器 mysql-dev 内验证） |
| 后端编译 | ✅ `./mvnw -pl dyshop-common install -DskipTests && ./mvnw -pl dyshop-api compile` EXIT=0 |
| 接口 curl 手测（U1-U11） | ✅ 全部通过（见上表实际结果列） |
| 前端构建（T17） | ✅ `npm run build` 722ms 通过 |
| 浏览器手测（U12-U16） | 待复核 |

> 测试账号密码已变更：`alice_01 / 654321`（U4 改密后旧密码 123456 失效）

## 5. 回归说明

- 本期接口均需认证，不影响 ch01 公开接口（商品/分类）
- ch02 登录/注册/me 回归：改密后 `alice_01` 需用新密码登录
- 新增 `favorite` 表不影响既有表结构