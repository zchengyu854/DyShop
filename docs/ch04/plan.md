# 客户端商品详情模块 — 开发计划（Plan）

> 前置：docs/ch04/spec.md、docs/ch04/tasks.md。计划按阶段推进，每阶段有明确产出与验证。

## P1 文档与准备（本期已完成）

- 产出：docs/ch04/spec.md、docs/ch04/tasks.md、docs/ch04/plan.md
- 验证：文档评审

## P2 后端收藏状态接口（T1–T2）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P2.1 | `FavoriteService.hasFavorited`（存在性校验 + 唯一键查询） | 编译通过 |
| P2.2 | `UserController.GET /api/user/favorites/status/{productId}` | curl 手测 |

- 配置：无新增；走既有 JWT 认证链路；`/api/user/**` 默认需认证
- 依赖：ch03 收藏服务与表已就绪，无数据变更

## P3 前端商品详情页（T3–T8）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P3.1 | `api/product.js` / `api/user.js` 补充 API | 代码评审 |
| P3.2 | `ProductGallery` 相册（缩略图切换） | `npm run build` |
| P3.3 | `ProductInfoPanel`（价格/收藏/购买按钮，收藏乐观更新） | `npm run build` |
| P3.4 | `ProductDetail.vue` 组装（404 / 详情 / 推荐区） | 浏览器手测 |

## P4 手测与文档（T9–T11）

- 产出：docs/ch04/manual-test/product-detail.md（用例、步骤、实际结果、通过/失败标注）
- 验证：所有用例执行完毕，失败项记录原因

### 进度记录（2026-08-04）

| 阶段 | 状态 | 备注 |
|---|---|---|
| P1 文档 | ✅ 完成 | spec / tasks / plan |
| P2 后端收藏状态接口 | ✅ 完成 | status 接口 + 404/401 校验 curl 通过 |
| P3 前端商品详情页 | ✅ 完成 | 全部组件 + 页面 build 通过；request.js 统一错误 code |
| P4 手测与文档 | 进行中 | 后端用例已回填；浏览器复测待人工 |

## 风险与对策

| 风险 | 对策 |
|---|---|
| 收藏状态接口需认证，而详情页公开 | 未登录不调用状态接口，收藏按钮点击引导登录（redirect 回跳） |
| 推荐接口分页可能包含当前商品 | 前端 `size=8` 拉取后过滤当前 id，空则隐藏区块 |
| detail 为富文本，存在注入面 | 仅渲染后端可信数据（本系统自产数据）；不引入外部富文本编辑 |
| 收藏切换请求并发（快速连点） | 乐观更新 + 请求中禁用按钮；失败回滚并提示 |
| picsum 外链图片加载慢 | 图片 lazy-load；缩略图切换不重复请求 |
