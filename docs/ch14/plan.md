# 浏览历史模块 — 实施计划（Plan）

> 模块：ch14 浏览历史 · 基于 `docs/ch14/spec.md` v1.0
> 本计划仅文档排期与依赖，不预估工时；完成后需过手工验收（`docs/ch14/manual-test/history.md`）。

## 0. 依赖与前置

- 数据层 `useRecentViewed` 已存在（ch04 接入商品详情页记录 + 个人中心预览），本期在其上增量开发。
- 无后端改动：浏览历史为设备本地数据，不建模、不开接口。
- 存量占位：`frontend/src/config/userMenu.js` 中「浏览历史」为 `type: 'todo'`（点击 toast 占位），本期替换为真实入口。

## 1. 阶段划分

### P0 数据层扩充（P0-ready）

- 产出：`useRecentViewed` 容量 12 → 30；新增 `remove(id)`、`clear()`（均写穿 localStorage）。
- 关键：不破坏 `RecentActivity.vue` 预览（`slice(0, 4)`）与 `ProductDetail.vue` 记录（`record` 签名不变）。
- 验收：浏览器 console 调用/页面操作后 localStorage 数组与界面一致。

### P1 入口与路由（P1-ready）

- 产出：`userMenu.js` 浏览历史 `'todo'` → `{ type: 'link', to: '/user/history' }`；`router` 新增 `/user/history` 子路由。
- 关键：路由懒加载、组件指向 `RecentViewsPanel.vue`；`requiresAuth` 由 `/user` 父级继承，无需重复声明。
- 验收：个人中心侧边栏点击「浏览历史」进入新页面且高亮。

### P2 页面（P2-ready）

- 产出：`RecentViewsPanel.vue` — 标题行（共 N 条 + 清空）、卡片网格（图/名/价 + hover 移除）、空态。
- 关键交互：卡片点击跳详情；移除即时生效；清空二次确认；空态跳 `/products`。
- 验收：详细手测见 `docs/ch14/manual-test/history.md`，前端 `npm run build` 通过。

## 2. 验收汇总

| 阶段 | 验收方式 |
|---|---|
| P0 | 数据层单测（浏览器操作 + localStorage 校验） |
| P1 | 路由跳转与菜单高亮 |
| P2 | 手测清单全过 + 构建通过 |