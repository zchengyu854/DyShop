# 客户端个人中心模块 — 规格说明（Spec）

> 项目：dyshop 购物程序 · 模块：客户端个人中心（C 端 `/user/**`，需登录）
> 状态：v1.0 定稿
> 关联：`backend/dyshop-api`（用户资料/密码/收藏接口）、`frontend/src/views/user/UserCenter.vue`

## 1. 目标

实现登录用户**个人中心**：个人资料查看与编辑、修改密码、我的收藏管理，并提供订单/地址入口；页面采用**侧边栏导航 + 内容区**结构（参考苹果官网账号页风格）。

## 2. 范围

### 2.1 本期（In Scope）

| 编号 | 内容 | 归属 |
|---|---|---|
| S1 | 资料接口：`GET /api/user/profile`（查看）、`PUT /api/user/profile`（编辑昵称/手机号/邮箱） | 后端 |
| S2 | 修改密码接口：`PUT /api/user/password`（校验原密码，BCrypt 更新） | 后端 |
| S3 | 收藏功能：新增 `favorite` 表 + 列表（分页）/添加/取消接口 | 后端 |
| S4 | 个人中心布局：侧边栏（资料/密码/收藏/订单/地址）+ 内容区 | 前端 |
| S5 | 三个内容面板：个人资料（含默认占位头像）、修改密码、我的收藏 | 前端 |
| S6 | 首页 Header 昵称入口改为 `/user/profile` | 前端 |
| S7 | 手测用例与记录（docs/ch03/manual-test/user-center.md） | 文档 |

### 2.2 本期不做（Out of Scope）

- 头像上传（本期头像展示默认占位图：无自定义头像时显示昵称首字圆形占位）
- 收藏**添加**入口 UI（商品详情页后续模块实现，本期仅提供 `POST` 接口供调用）
- 订单模块、地址模块本体（个人中心仅提供入口跳转 `/orders`、`/addresses`）
- 手机/邮箱验证码校验（本期仅格式校验）

## 3. 页面结构（前端）

```
┌──────────────────────────────────────────────────────────┐
│ Header（复用 HomeHeader，昵称 → /user/profile）             │
├──────────────┬───────────────────────────────────────────┤
│ 侧边栏        │ 内容区（router-view）                       │
│              │                                           │
│  ┌────────┐  │  ┌─────────────────────────────────────┐  │
│  │ 头像   │  │  │ 个人资料                             │  │
│  │ (占位) │  │  │  ┌───────────────────────────────┐  │  │
│  │ 昵称   │  │  │  │ 昵称  [输入框]                │  │  │
│  └────────┘  │  │  │ 手机号 [输入框]                │  │  │
│ ──────────── │  │  │ 邮箱   [输入框]                │  │  │
│ 个人资料      │  │  │ [保存更改]（蓝色胶囊按钮）       │  │  │
│ 修改密码      │  │  └───────────────────────────────┘  │  │
│ 我的收藏      │  │                                     │  │
│ 我的订单 →    │  │  或 修改密码 / 我的收藏（白色圆角卡片）  │  │
│ 收货地址 →    │  └─────────────────────────────────────┘  │
└──────────────┴───────────────────────────────────────────┘
```

- 布局：内容区灰底 `#f5f5f7`，面板为白色圆角卡片（`--radius-lg`），侧边栏细字导航、激活项高亮
- 个人资料面板：头像（无头像时昵称首字圆形占位，不可编辑）+ 昵称/手机号/邮箱表单
- 修改密码面板：原密码 / 新密码 / 确认新密码；成功后自动登出并跳转登录页重新登录
- 我的收藏面板：收藏商品卡片网格（复用首页商品卡风格）+「移除」按钮 + 分页加载；空态提示去逛逛
- 侧边栏「我的订单」「收货地址」为外链入口（`/orders`、`/addresses`）

## 4. 接口定义（后端 dyshop-api）

> 统一前缀 `/api`；统一返回 `Result<T>`。以下接口**均需认证**（`Authorization: Bearer <token>`，principal=userId），未登录返回 `code=401`。

### 4.1 GET `/api/user/profile` — 查看个人资料

响应 `data`：`{ id, username, nickname, avatar, phone, email, role }`（同 `/api/auth/me`，供个人中心直接使用）。

### 4.2 PUT `/api/user/profile` — 编辑个人资料

请求体：

```json
{ "nickname": "爱丽丝", "phone": "13800138000", "email": "alice@example.com" }
```

| 参数 | 类型 | 必填 | 校验 |
|---|---|---|---|
| nickname | string | 否 | ≤20 位，缺省不修改 |
| phone | string | 否 | 空 或 `^1\d{10}$` |
| email | string | 否 | 空 或 基本邮箱格式 |

响应 `data`：更新后的 UserVO。

### 4.3 PUT `/api/user/password` — 修改密码

请求体：`{ "oldPassword": "123456", "newPassword": "654321" }`

| 参数 | 类型 | 必填 | 校验 |
|---|---|---|---|
| oldPassword | string | 是 | 必须与当前密码匹配 |
| newPassword | string | 是 | 6~20 位 |

- 原密码错误 → `code=401, message="原密码错误"`
- 成功：BCrypt 更新，返回 `code=0`。前端随后自动登出并跳登录页（JWT 无状态，旧 token 到期前仍有效，故强制重新登录）

### 4.4 GET `/api/user/favorites` — 我的收藏（分页）

Query：`page`（默认 1）、`size`（默认 12，上限 50）

响应 `data`（分页）：

```json
{
  "records": [
    {
      "favoriteId": 1,
      "productId": 3,
      "name": "示例商品",
      "subtitle": "卖点",
      "mainImage": "https://...",
      "price": 99.00,
      "originalPrice": 129.00,
      "sales": 120,
      "createTime": "2026-08-03 21:55:40"
    }
  ],
  "total": 1,
  "page": 1,
  "size": 12
}
```

排序：收藏时间倒序。

### 4.5 POST `/api/user/favorites/{productId}` — 添加收藏

- 商品不存在或已下架 → `code=404, message="商品不存在"`
- 已收藏：幂等返回成功（`code=0`）

### 4.6 DELETE `/api/user/favorites/{productId}` — 取消收藏

- 未收藏：幂等返回成功（`code=0`）

## 5. 数据模型

新增表（追加到 `backend/sql/schema.sql`，与现有表兼容，建库时一并创建）：

```sql
CREATE TABLE IF NOT EXISTS `favorite` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `product_id`  BIGINT   NOT NULL COMMENT '商品ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE = InnoDB COMMENT = '收藏表';
```

> 已有库需单独执行该 `CREATE TABLE` 追加（DDL 幂等，可重复执行）。

## 6. 非功能要求

- N1：本期全部接口需认证（SecurityConfig 默认拦截，无需额外配置）
- N2：收藏唯一键（user_id, product_id）防重复；删除/添加均幂等
- N3：列表分页上限 50，防全量拉取
- N4：密码仅 BCrypt 落库；原密码校验不区分"用户不存在/密码错误"类提示（本期用户必为已认证状态）
- N5：UI 与全站一致（Apple 风格：灰底 `#f5f5f7`、白卡片、蓝色胶囊按钮、SF 字体栈）

## 7. 验收标准

1. 登录后访问 `/user/profile` 显示当前资料与占位头像
2. 编辑昵称/手机号/邮箱保存后，重新进入资料仍生效
3. 修改密码：原密码错误提示；成功后旧密码登录失败、新密码可登录
4. 收藏列表：接口添加后列表可见；「移除」后消失；空态正常
5. 未登录访问 `/user/**` → 重定向登录页，登录后回跳
6. 首页 Header 昵称点击进入个人中心
7. 后端接口 curl 手测通过（见 docs/ch03/manual-test/user-center.md）
8. 前端 `npm run build` 通过
