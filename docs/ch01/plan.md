# 客户端首页模块 — 开发计划（Plan）

> 前置：docs/spec.md、docs/tasks.md。计划按阶段推进，每阶段有明确产出与验证。

## P1 文档与准备（本期已完成）

- 产出：docs/spec.md、docs/tasks.md、docs/plan.md
- 验证：文档评审

## P2 后端基础与接口（T1–T8）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P2.1 | common：Result / ResultCode / BizException / GlobalExceptionHandler | 编译通过 |
| P2.2 | entity：Product / Category（MyBatis-Plus 注解） | 编译通过 |
| P2.3 | mapper + service：分页列表（分类/关键词/仅上架）、详情、分类列表 | 编译通过 |
| P2.4 | controller：3 个公开接口 | curl 手测 |
| P2.5 | SecurityConfig 放行公开接口 | curl 未带 token 访问 200 |

- 依赖风险：common 模块需先于 api 编译（`./mvnw install`）

## P3 数据（T9）

- 产出：`backend/sql/` 种子数据脚本
- 验证：MySQL 执行后列表接口返回种子商品

## P4 前端首页（T10–T12）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P4.1 | api/product.js 封装 | 代码评审 |
| P4.2 | Home.vue：布局 + 分类导航 + 轮播位 + 商品列表（分页/空态/错误态） | `npm run build` |
| P4.3 | 卡片跳转详情路由 | 浏览器手测 |

## P5 手测与文档（T13–T15）

- 产出：docs/manual-test/home.md（用例、步骤、实际结果、通过/失败标注）
- 验证：所有用例执行完毕，失败项记录原因

### 进度记录（2026-08-03）

| 阶段 | 状态 | 备注 |
|---|---|---|
| P1 文档 | ✅ 完成 | spec / tasks / plan |
| P2 后端基础与接口 | ✅ 完成 | 编译通过；启动验证通过；放行/拦截/统一异常链路已验证 |
| P3 数据 | ✅ 完成 | schema.sql + data.sql 已在 Docker MySQL 执行（utf8mb4 导入），并修复乱码 |
| P4 前端首页 | ✅ 完成 | `npm run build` 通过（611ms）；vite dev 代理联调通过 |
| P5 手测与文档 | ✅ 完成 | U2-U6/U8/U10-U14 全部通过；U1/U7/U9 待浏览器人工复核（已标注） |

## 风险与对策

| 风险 | 对策 |
|---|---|
| 本机无 MySQL 8，接口无数据 | 见数据库环境决策：H2 dev 兜底 / 用户自备 MySQL / brew 安装 |
| Spring Security 默认拦截所有请求 | P2.5 显式放行公开路径 |
| 图片资源缺失 | 种子数据使用占位图 URL（如 placehold.co），不依赖本地存储 |
