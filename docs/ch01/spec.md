# 客户端首页模块 — 规格说明（Spec）

> 项目：dyshop 购物程序 · 模块：客户端首页（C 端 `/` 首页）
> 状态：v1.0 定稿
> 关联：`backend/dyshop-api`（商品/分类接口）、`frontend/src/views/shop/Home.vue`

## 1. 目标

在前后端分离架构下，实现购物程序**客户端首页**：未登录用户即可浏览商品、按分类筛选、关键词搜索、分页加载，点击商品卡片进入详情页（详情页本期仅占位）。

## 2. 范围

### 2.1 本期（In Scope）

| 编号 | 内容 | 归属 |
|---|---|---|
| S1 | 首页页面结构与样式（Header / 分类导航 / 商品列表 / Footer） | 前端 |
| S2 | 商品列表接口（分页 + 分类筛选 + 关键词搜索，仅上架商品） | 后端 |
| S3 | 商品详情接口（供详情页后续使用，本期仅提供接口） | 后端 |
| S4 | 分类列表接口 | 后端 |
| S5 | 统一返回体 Result / 错误码 / 全局异常 | 后端 common |
| S6 | 公开接口免认证（Spring Security 放行） | 后端 |
| S7 | 商品/分类种子数据 + 数据库初始化脚本 | 数据 |
| S8 | 手测用例与记录（docs/manual-test/） | 文档 |

### 2.2 本期不做（Out of Scope）

- 用户登录/注册、购物车、下单（后续模块）
- 商品详情页开发（路由已预留，本期保持占位壳）
- 轮播 Banner 后台管理（本期使用静态配置位）
- 后台管理模块（dyshop-admin）

## 3. 页面结构（前端）

```
┌──────────────────────────────────────────┐
│ Header: Logo · 搜索框 · [登录/注册|用户] · 购物车入口 │
├──────────────────────────────────────────┤
│ 分类导航：全部分类 | 分类A | 分类B | ...        │
├──────────────────────────────────────────┤
│ 轮播位（静态占位图，后续可换接口）              │
├──────────────────────────────────────────┤
│ 商品列表（卡片网格，分页加载）                 │
│   [图][名] [价格] × N                      │
├──────────────────────────────────────────┤
│ Footer: © dyshop                         │
└──────────────────────────────────────────┘
```

- 商品卡片：主图、名称、售价（含划线原价）、点击跳转 `/products/:id`
- 空态：无商品时展示"暂无商品"
- 加载态：请求中显示 loading，失败显示错误 + 重试

## 4. 接口定义（后端 dyshop-api）

> 统一前缀 `/api`；统一返回 `Result<T>`：`{ code, message, data }`，`code = 0` 成功。
> 分页结构：`data = { records: [...], total, page, size }`。

### 4.1 GET `/api/products` — 商品分页列表（公开）

Query 参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| page | int | 否 | 1 | 页码（≥1） |
| size | int | 否 | 12 | 每页条数（1~50） |
| categoryId | long | 否 | - | 分类筛选 |
| keyword | string | 否 | - | 名称模糊搜索 |

响应 `data`（分页）：

```json
{
  "records": [
    {
      "id": 1,
      "name": "示例商品",
      "subtitle": "卖点",
      "mainImage": "https://...",
      "price": 99.00,
      "originalPrice": 129.00,
      "sales": 120
    }
  ],
  "total": 1,
  "page": 1,
  "size": 12
}
```

约束：仅返回 `status=1`（上架）且未逻辑删除的商品；排序：上架时间倒序。

### 4.2 GET `/api/products/{id}` — 商品详情（公开，本期仅提供接口）

响应 `data`：商品全部展示字段（含 `detail`、`images`、`stock` 等）。
不存在或已下架 → `code=404, message="商品不存在"`。

### 4.3 GET `/api/categories` — 分类列表（公开）

响应 `data`：`[{ id, name, sort }]`，仅启用（`status=1`）且未删除，按 `sort` 升序。

## 5. 数据模型（已有，见 backend/sql/schema.sql）

- `product`：id / category_id / name / subtitle / main_image / images / detail / price / original_price / stock / sales / status / deleted / create_time / update_time
- `category`：id / parent_id / name / sort / status / deleted

本期种子数据：≥2 个分类、≥8 个商品（含上架/下架混合，验证筛选与"仅上架"逻辑）。

## 6. 非功能要求

- N1：首页接口无需登录（Security 放行 `/api/products/**`、`/api/categories`）
- N2：列表接口强制分页上限（size ≤ 50），防止全量拉取
- N3：统一错误码（见 `ResultCode`）：成功 0 / 参数错误 400 / 未认证 401 / 无权限 403 / 资源不存在 404 / 服务器错误 500
- N4：日志：mapper 层 debug 输出 SQL（dev 环境）

## 7. 验收标准

1. 未登录状态直接访问首页，可看到轮播位与商品列表
2. 切换分类 / 输入关键词搜索 / 翻页，列表正确变化
3. 商品卡片跳转 `/products/:id` 路由可达（占位页）
4. 后端接口 curl 手测通过（见 docs/manual-test/home.md）
5. 前端 `npm run build` 通过
