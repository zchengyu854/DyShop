# 客户端商品详情模块 — 手动测试记录（Product Detail）

> 模块：客户端商品详情（`/products/:id`） · 关联接口：`GET /api/products/{id}`、`GET /api/user/favorites/status/{productId}`、`POST/DELETE /api/user/favorites/{productId}`
> 用例基线：docs/ch04/spec.md §4 §7 · 任务：T9/T10/T11

## 1. 环境前置

| 项 | 要求 |
|---|---|
| 数据库 | ✅ ch03 已含 `favorite` 表；本期无数据变更 |
| 后端 | ✅ `dyshop-api`（8081），需重启加载收藏状态接口 |
| 前端 | ✅ vite dev（5173） |
| 账号 | ch03 手测账号 `alice_01 / 654321` |

## 2. 启动步骤

```bash
# 后端（8081）
cd backend && ./mvnw -pl dyshop-api spring-boot:run

# 前端（5173）
cd frontend && npm run dev
```

浏览器打开 http://localhost:5173/products/1

## 3. 测试用例

| 编号 | 用例 | 步骤 | 预期结果 | 实际结果 | 状态 |
|---|---|---|---|---|---|
| M1 | 详情正常 | `GET /api/products/1` | `code=0`，字段齐全（images 含 2 张、detail、价格、库存、销量） | ✅ 字段齐全 | ✅ |
| M2 | 下架商品 | `GET /api/products/9` | `code=404, message="商品不存在"` | ✅ code=404 "商品不存在" | ✅ |
| M3 | 不存在 id | `GET /api/products/999` | `code=404, message="商品不存在"` | ✅ code=404 | ✅ |
| M4 | 未登录状态接口 | 无 token `GET /api/user/favorites/status/1` | `code=401` | ✅ code=401 | ✅ |
| M5 | 状态-未收藏 | 登录后 `GET /api/user/favorites/status/1` | `data.favorited=false`（若已收藏先执行 M7） | ⚠️ 返回 true（ch03 遗留收藏），先删后测即 false | ✅ |
| M6 | 状态-已收藏 | `POST /api/user/favorites/1` 后再查 status | `data.favorited=true` | ✅ 添加幂等 + status=true | ✅ |
| M7 | 取消后状态 | `DELETE /api/user/favorites/1` 后再查 status | `data.favorited=false`；重复 DELETE 幂等 | ✅ 删除后 false；status 404 商品校验也通过 | ✅ |
| M8 | 页面渲染 | 打开 `/products/1` | 相册/名称/价格(划线)/销量/库存/图文详情/同类推荐均渲染 | 页面已实现 + build 通过 | ⚠️ 浏览器复测 |
| M9 | 相册切换 | 点击第 2 张缩略图 | 大图切换，缩略图高亮；无图商品仅主图 | 逻辑已实现（ProductGallery） | ⚠️ 浏览器复测 |
| M10 | 未登录收藏引导 | 退出登录打开详情点「收藏」 | 跳 `/login?redirect=/products/1`，登录后回跳 | 逻辑已实现（redirect 回跳） | ⚠️ 浏览器复测 |
| M11 | 登录收藏切换 | 登录后点「收藏」→ 再点 | 高亮切换；刷新后初值正确（配合 M6/M7） | 逻辑已实现（乐观更新+回滚） | ⚠️ 浏览器复测 |
| M12 | 购买按钮占位 | 点「加入购物车」「立即购买」 | 提示"购物车模块开发中" | 逻辑已实现（notice 提示） | ⚠️ 浏览器复测 |
| M13 | 下架商品页面 | 打开 `/products/9` | 显示"商品不存在"提示页，无报错 | 逻辑已实现（code=404 判定） | ⚠️ 浏览器复测 |
| M14 | 推荐跳转 | 推荐区点击其他商品卡 | 跳转对应详情页，当前商品不在推荐中 | 逻辑已实现（过滤当前商品） | ⚠️ 浏览器复测 |

> ⚠️：无浏览器工具项，代码实现 + build 通过后由人工复核。

## 4. 验证记录（2026-08-04）

### 执行结果

| 验证项 | 结果 |
|---|---|
| 后端编译 | ✅ `./mvnw -pl dyshop-common install && ./mvnw -pl dyshop-api compile` EXIT=0；服务已重启（Started 1.3s） |
| 接口 curl 手测（M1-M7） | ✅ 全部通过（见上表实际结果列）；补充：status 接口对不存在商品返回 404 ✅ |
| 前端构建（T10） | ✅ `npm run build` 通过；另修复 request.js 错误对象（附加 `code`，404 判定可靠） |
| 浏览器手测（M8-M14） | 待复核 |

> 账号密码：`alice_01 / 654321`（ch03 改密后）。M5 初值受 ch03 遗留收藏影响，已在 M7 清理。测试后商品 1 收藏状态已复位为未收藏。

## 5. 回归说明

- 商品列表/首页（ch01）不受影响：仅新增 `/api/user/favorites/status` 认证接口
- ch03 收藏接口回归：POST/DELETE 幂等逻辑未改动，本期仅新增状态查询
- 收藏数据沿用：本期删除的收藏（M7）可在 ch03 收藏列表验证同步消失
