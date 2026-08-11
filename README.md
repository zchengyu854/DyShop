# dyshop — 购物程序

前后端分离的电商购物 Demo：完整的购物链路（浏览 → 购物车 → 结算 → 下单 → 收货）＋后台管理（商品/订单/用户/优惠券/积分）。

## 技术栈

| 端 | 技术 |
|---|---|
| 前端 `frontend/` | Vue 3 · Vite · Vue Router · Pinia · Axios · ECharts（无 UI 组件库，手写样式） |
| 后端 `backend/` | Java 17 · Spring Boot 3.3 · Spring Security (JWT) · MyBatis-Plus · Maven 多模块 |
| 数据库 | MySQL 8 |

## 架构

```
┌────────────┐   /api 代理    ┌──────────────────────┐        ┌──────┐
│  Vue SPA   │ ─────────────► │ dyshop-api :8081      │ ─────► │ MySQL│
│  :5173     │   /api/admin/** │  C 端 + 后台接口        │        └──────┘
└────────────┘                └──────────────────────┘
```

- 后端多模块：`dyshop-common`（共享库）、`dyshop-api`（应用，端口 8081；后台路径 `/api/admin/**`）
- 认证：C 端 / 后台双 JWT 会话（`frontend/src/utils/auth.js` 双 token，互不影响）
- 浏览历史为设备本地数据（localStorage），不落库

## 项目截图（Screenshots）

> 截图统一放入 `docs/screenshots/`，替换下方文件名即可（建议 1200px 宽、PNG）。

| 首页 | 商品列表 | 商品详情 |
|---|---|---|
| ![首页](![img.png](img.png)) | ![商品列表](![img_1.png](img_1.png)) | ![商品详情](![img_2.png](img_2.png)) |

| 购物车 | 结算 | 个人中心 |
|---|---|---|
| ![购物车](![img_3.png](img_3.png)) | ![结算](![img_4.png](img_4.png)) | ![个人中心](![img_5.png](img_5.png)) |

| 后台管理 | 积分商城 | 优惠券 |
|---|---|---|
| ![后台管理](![img_6.png](img_6.png)) | ![积分商城](![img_7.png](img_7.png)) | ![优惠券](![img_8.png](img_8.png)) |

## 功能地图

| 模块 | 章节文档 | 说明 |
|---|---|---|
| 首页 / 登录注册 | ch01–ch02 | 商品列表、分类筛选、JWT 登录注册 |
| 个人中心 | ch03 | 资料/改密/收藏/地址/浏览历史（ch14） |
| 商品详情 | ch04 | 图集、SKU 选择、收藏、推荐 |
| 购物车 / 结算 | ch05–ch07 | 购物车、地址、下单（价格规则以前端计算为预估、后端为准） |
| 后台管理 | ch08 | 统计、商品、订单、分类、用户、优惠券、售后、积分 |
| 会员 | ch09 | 等级/成长值/积分发放 |
| 订单操作重构 | ch10 | 订单 Tab 筛选、操作按钮收敛 |
| 优惠券 | ch11 | 领券/核销/结算抵扣 |
| 售后 / 退款 | ch12 | 申请/审核/退款 |
| 积分商城 | ch13 | 积分兑换优惠券/兑换码 |

## 快速开始

依赖 MySQL 8，需先建库：

```bash
# 1) 建库建表 + 种子数据
mysql -uroot -p < backend/sql/schema.sql
mysql -uroot -p < backend/sql/data.sql
# 2) 后端（默认 8081）
cd backend && ./mvnw clean install -DskipTests
./mvnw -pl dyshop-api spring-boot:run
# 3) 前端（默认 5173，/api 代理到 8081）
cd frontend && npm install && npm run dev
```

- 数据库连接按 `backend/dyshop-api/src/main/resources/application-dev.yml` 修改
- 种子账号：普通用户（见 `backend/sql/data.sql`）、管理员 `admin` / `admin123`（role=1）

## 文档

- 各模块规格/计划/任务/手测：`docs/ch01–ch14/`（spec.md / plan.md / tasks.md / manual-test/）
- 后端/前端详细说明：`backend/README.md`、`frontend/README.md`

## 验证

- 前端：`cd frontend && npm run build && npx vitest run`
- 后端：`./mvnw -pl dyshop-api test`（各章节手测清单见 `docs/chNN/manual-test/`）
