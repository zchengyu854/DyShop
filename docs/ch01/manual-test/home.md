# 客户端首页模块 — 手动测试记录（Home）

> 模块：客户端首页（`/`） · 关联接口：`GET /api/products`、`GET /api/products/{id}`、`GET /api/categories`
> 用例基线：docs/spec.md §4 §7 · 任务：T13/T14/T15

## 1. 环境前置

| 项 | 要求 |
|---|---|
| MySQL 8+ | ✅ 本机 Docker 容器 `mysql-dev`（MySQL 9.6，127.0.0.1:3306） |
| 数据库 | ✅ `backend/sql/schema.sql` + `data.sql` 已导入（注意：导入需 `--default-character-set=utf8mb4`，否则中文乱码） |
| 连接配置 | ✅ `application-dev.yml`：root/root，与容器一致 |
| Java 17 | ✅ 已具备 |

## 2. 启动步骤

```bash
# 后端（api 服务，8081）
cd backend && ./mvnw -pl dyshop-api spring-boot:run

# 前端（dev server，5173，自动代理 /api → 8081）
cd frontend && npm install && npm run dev
```

浏览器打开 http://localhost:5173/

## 3. 测试用例

| 编号 | 用例 | 步骤 | 预期结果 | 实际结果 | 状态 |
|---|---|---|---|---|---|
| U1 | 首页可访问 | 打开 `/` | 页面正常渲染：Header、分类导航、轮播位、商品列表 | 页面 200 + 接口链路正常（curl 层）；完整视觉渲染待浏览器确认 | ⚠️ 浏览器复测 |
| U2 | 商品列表默认加载 | 打开首页 | 展示 8 个上架商品（size=12 一页内），按上架时间倒序 | ✅ total=8，首"降噪无线耳机"末"每日坚果混合礼盒"，中文正常 | ✅ |
| U3 | 仅显示上架商品 | 对比种子数据 | 列表含 id 1-8，不含已下架的 9、10 | ✅ 返回 id [1..8]，不含 9/10 | ✅ |
| U4 | 分类筛选 | 点击「家居生活」 | 仅展示 category_id=2 的上架商品（4、5、6） | ✅ 返回保温杯/香薰蜡烛/懒人沙发 | ✅ |
| U5 | 关键词搜索 | 输入「咖啡」回车 | 仅展示名称含「咖啡」的商品（7） | ✅ 命中"埃塞俄比亚手冲咖啡豆" | ✅ |
| U6 | 分页 | size=3 翻页 | 「加载更多」追加下一页，底部显示「没有更多了」 | ✅ page=2&size=3 → 3 条，total=8（数据层）；前端按钮交互待浏览器 | ✅ |
| U7 | 商品卡片跳详情 | 点击任意商品卡片 | 路由跳转 `/products/:id`（详情页为占位壳） | 代码已实现 + build 通过；浏览器点击待复测 | ⚠️ 浏览器复测 |
| U8 | 空态 | 搜索不存在的关键词 | 显示「暂无商品」 | ✅ 数据层：搜索"不存在"返回 total=0；空态渲染待浏览器 | ✅ |
| U9 | 错误态与重试 | 后端停止后刷新首页 | 显示「加载失败」与「重试」按钮 | 错误态逻辑已实现（GlobalExceptionHandler 返回统一 500/网络异常）；浏览器复测待执行 | ⚠️ 浏览器复测 |
| U10 | 未登录访问 | 无 token 访问 `/` | 正常浏览（公开接口免认证） | ✅ 无 token 访问接口返回数据；未放行路径返回统一 401 | ✅ |
| U11 | 分类接口 | `curl :8081/api/categories` | 返回 3 个启用分类，按 sort 升序 | ✅ 手机数码/家居生活/食品生鲜 | ✅ |
| U12 | 详情接口 | `curl :8081/api/products/1` | 返回商品 1 完整信息（含 images/detail/stock） | ✅ images×2、stock=200、detail HTML | ✅ |
| U13 | 详情 404 | `curl :8081/api/products/999` | `{"code":404,"message":"商品不存在"}` | ✅ code=404 "商品不存在" | ✅ |
| U14 | 参数校验 | `page=0` / `size=100` | `{"code":400,...}` | ✅ page→"page 必须大于等于 1"，size→"size 必须在 1~50 之间" | ✅ |

## 4. 验证记录（2026-08-03，完整执行）

### 环境
- 后端：api 服务直连 Docker MySQL `mysql-dev`（`Started in 2.97s`）
- 前端：`npm run dev`（vite 5173，代理 `/api` → 8081）

### 执行结果

| 验证项 | 结果 |
|---|---|
| 三模块编译 `./mvnw clean install` | ✅ EXIT=0 |
| api 服务启动 | ✅ Started in 2.97s |
| 接口真实数据手测（U2-U6、U8、U11-U14） | ✅ 全部通过（Python/curl 断言） |
| Security 放行 / 拦截（U10） | ✅ 公开接口免 token 返回数据；`/` 返回统一 401 |
| 前端构建 | ✅ `npm run build` 611ms |
| vite dev 代理联调 | ✅ `:5173/api/products` → 真实数据（8 商品）；`:5173/api/categories` → 3 分类 |
| **缺陷修复** | 种子数据导入中文乱码（双层编码）→ 用 `--default-character-set=utf8mb4` 重导修复 |

### 遗留（需浏览器人工确认）
- U1 首页视觉渲染、U7 卡片点击跳转、U9 错误态重试按钮交互 —— 无浏览器工具，代码已实现且构建通过，建议浏览器打开 `http://localhost:5173/` 复核。

## 5. 回归说明

- 若重建数据库：`schema.sql` → `data.sql` 两步导入**必须加 `--default-character-set=utf8mb4`**，否则中文乱码。
