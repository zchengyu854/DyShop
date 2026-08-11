# 客户端商品详情模块 — 任务拆解（Tasks）

> 前置：docs/ch04/spec.md。勾选状态随开发进度更新（☐ 未开始 / ☑ 完成 / ⚠️ 部分完成-待环境验证）。

## T 后端 — 接口

- [x] **T1** `FavoriteService.hasFavorited(userId, productId)`：收藏状态查询（含商品存在性校验，不存在抛 404）
- [x] **T2** `UserController` 新增 `GET /api/user/favorites/status/{productId}`，返回 `{ favorited }`

## T 前端 — 数据层

- [x] **T3** `api/product.js` 新增 `fetchProductDetail(id)`（`GET /products/{id}`）
- [x] **T4** `api/user.js` 新增 `fetchFavoriteStatus(productId)`（`GET /user/favorites/status/{productId}`）

## T 前端 — 页面

- [x] **T5** `components/shop/ProductGallery.vue`：大图 + 缩略图切换（当前缩略图高亮；无 images 兜底 mainImage；懒加载）
- [x] **T6** `components/shop/ProductInfoPanel.vue`：
  - 名称/副标题/价格（划线原价）/销量/库存
  - 收藏按钮：未登录空心+跳登录（redirect 回跳）；已登录状态接口初始化 + 乐观切换（失败回滚）
  - 加入购物车 / 立即购买：占位提示"购物车模块开发中"
- [x] **T7** `views/shop/ProductDetail.vue`：组装（Header + 主区 + 图文详情 v-html + 推荐区 + Footer）
  - 加载/404 状态：加载中骨架、商品不存在提示页
  - 推荐区：`fetchProducts({ categoryId, size: 8 })` 过滤当前商品，复用 ProductGrid/ProductCard；空则不渲染
- [x] **T8** 细节：标题（`document.title` 商品名）、面包屑「首页 > 商品名」

## T 验证与文档

- [x] **T9** 后端编译 + 重启服务 + curl 手测（详情/404/状态 401/状态切换 true-false）
- [x] **T10** 前端 `npm run build` 构建验证
- [x] **T11** 手测记录 `docs/ch04/manual-test/product-detail.md`
