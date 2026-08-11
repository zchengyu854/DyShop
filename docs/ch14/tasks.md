# 浏览历史模块 — 任务拆解（Tasks）

> 前置：docs/ch14/spec.md。勾选状态随开发进度更新（☐ 未开始 / ☑ 完成 / ⚠️ 部分完成-待环境验证）。

## T 数据层

- [x] **T1** `useRecentViewed.js`：容量常量 12 → 30
- [x] **T2** `useRecentViewed.js`：新增 `remove(id)`（过滤 + 写穿 localStorage）、`clear()`（置空 + 写穿）
- [x] **T3** 兼容性确认：`ProductDetail.vue`（record 签名不变）与 `RecentActivity.vue`（slice 0-4）零改动可运行

## T 入口与路由

- [x] **T4** `config/userMenu.js`：浏览历史 `'todo'` → `{ type: 'link', to: '/user/history' }`（偏好设置组）
- [x] **T5** `router/index.js`：`/user` children 新增 `{ path: 'history', name: 'user-history', component: RecentViewsPanel }`（懒加载）

## T 页面

- [x] **T6** `components/user/RecentViewsPanel.vue`：挂载读取列表 + 标题行（共 N 条 + 清空按钮 + 二次确认）
- [x] **T7** 卡片网格：图/名/价（`Number(price).toFixed(2)`）+ hover 移除按钮 + 点击跳 `/products/{id}`
- [x] **T8** 空态：「暂无浏览记录，去逛逛吧」点击跳 `/products`；清空后即时进入空态

## T 验证与文档

- ⚠️ **T10** 手测记录 `docs/ch14/manual-test/history.md` 回填（待用户浏览器手测）