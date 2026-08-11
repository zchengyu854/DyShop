# 客户端首页模块 — 任务拆解（Tasks）

> 前置：docs/spec.md。勾选状态随开发进度更新（☐ 未开始 / ☑ 完成 / ⚠️ 部分完成-待环境验证）。

## T 后端 — common 基础

- [x] **T1** 统一返回体 `Result<T>` + 错误码 `ResultCode`（`com.dyshop.common.result`）
- [x] **T2** 业务异常 `BizException` + 全局异常处理器 `GlobalExceptionHandler`（`com.dyshop.common.exception`）

## T 后端 — 实体与数据访问

- [x] **T3** 实体 `Product`、`Category`（`com.dyshop.common.entity`，MyBatis-Plus 注解，逻辑删除）
- [x] **T4** `ProductMapper`、`CategoryMapper`（`com.dyshop.api.mapper`）

## T 后端 — 接口

- [x] **T5** `ProductService` / `ProductServiceImpl`：分页查询（分类/关键词/仅上架）、详情查询
- [x] **T6** `CategoryService` / `CategoryServiceImpl`：启用分类列表
- [x] **T7** `ProductController`：`GET /api/products`、`GET /api/products/{id}`；`CategoryController`：`GET /api/categories`
- [x] **T8** `SecurityConfig`：放行 `/api/products/**`、`/api/categories`，其余保持认证

## T 数据

- [x] **T9** 种子数据脚本（`backend/sql/data.sql`：3 分类、10 商品含 2 下架），README 已注明执行顺序

## T 前端

- [x] **T10** `api/product.js`：`fetchProducts`、`fetchCategories` 封装
- [x] **T11** `Home.vue`：Header / 分类导航 / 轮播位 / 商品列表（分页加载/空态/错误态）/ Footer
- [x] **T12** 商品卡片点击跳转 `/products/:id`（详情页保持占位）

## T 验证与文档

- [x] **T13** 后端编译 + 启动 + 接口手测（✅ 真实 MySQL 数据下 U2-U14 全部通过，含中文验证与乱码修复）
- [x] **T14** 前端 `npm run build` 构建验证（✅ built in 611ms）
- [x] **T15** 手测记录 `docs/manual-test/home.md`（用例 + 步骤 + 实际结果 + 待执行项）
