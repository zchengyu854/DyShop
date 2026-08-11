# dyshop-frontend

dyshop 购物程序前端（前后端分离架构，独立项目）。

## 技术栈

Vue 3 · Vite · Vue Router · Pinia · Axios

## 快速开始

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # 产物输出到 dist/
```

## 与后端的对接约定

| 前端路径 | 后端服务 | 说明 |
|---|---|---|
| `/api/*` | dyshop-api（8081） | C 端用户接口 |
| `/api/admin/*` | dyshop-api（8081） | 后台管理接口（角色 ADMIN） |

- 开发模式由 Vite 代理转发（见 `vite.config.js`）；生产部署时由 Nginx 等网关统一转发，前端无需关心后端地址。
- 认证：请求头 `Authorization: Bearer <token>`，由 `src/utils/request.js` 统一注入（`/admin/` 前缀自动挂后台 token）。
- 后端统一返回 `{ code, message, data }`，`src/utils/request.js` 已封装解包与错误处理。

## 目录结构

```
src/
├── api/          # 按模块拆分的接口调用（user/coupon/order/product/admin 等）
├── router/       # 路由（C 端 + 后台管理，meta 声明登录/角色要求）
├── stores/       # Pinia（user / admin / cart / userOrders）
├── views/        # 页面（shop / user / order / admin）
├── components/   # 公共组件
└── utils/        # Axios 封装、JWT 存取
```

> 当前为架构骨架阶段：页面为占位壳，业务逻辑后续填充。
