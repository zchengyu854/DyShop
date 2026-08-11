# 客户端个人中心模块 — 任务拆解（Tasks）

> 前置：docs/ch03/spec.md。勾选状态随开发进度更新（☐ 未开始 / ☑ 完成 / ⚠️ 部分完成-待环境验证）。

## T 后端 — 数据与实体

- [ ] **T1** `schema.sql` 追加 `favorite` 表（user_id + product_id 唯一键），已建库环境单独执行 DDL
- [ ] **T2** 实体 `Favorite`（`com.dyshop.common.entity`，MyBatis-Plus 注解）

## T 后端 — 接口

- [ ] **T3** DTO：`UpdateProfileDTO`（昵称/手机号/邮箱 + 格式校验）、`UpdatePasswordDTO`（原密码/新密码）
- [ ] **T4** VO：`FavoriteVO`（收藏 ID + 商品快照信息 + 收藏时间）
- [ ] **T5** `UserCenterService` / `UserCenterServiceImpl`：资料查看/更新、修改密码（原密码校验 + BCrypt）
- [ ] **T6** `FavoriteService` / `FavoriteServiceImpl`：分页列表（收藏时间倒序）、添加（幂等 + 商品校验）、取消（幂等）
- [ ] **T7** `UserController`（`com.dyshop.api.controller.user`）：
  - `GET /api/user/profile`、`PUT /api/user/profile`
  - `PUT /api/user/password`
  - `GET /api/user/favorites`、`POST /api/user/favorites/{productId}`、`DELETE /api/user/favorites/{productId}`

## T 前端 — 数据层

- [ ] **T8** `api/user.js`：`fetchProfile` / `updateProfile` / `updatePassword` / `fetchFavorites` / `addFavorite` / `removeFavorite`
- [ ] **T9** `stores/user.js`：补 `updateProfile` action（同步刷新 userInfo）

## T 前端 — 页面

- [ ] **T10** 路由：`/user`（UserCenter，requiresAuth）+ 子路由 `profile`（默认）/ `password` / `favorites`
- [ ] **T11** `UserCenter.vue`：侧边栏导航（用户卡片 + 资料/密码/收藏 + 订单/地址外链）+ 内容区 router-view
- [ ] **T12** `components/user/ProfilePanel.vue`：占位头像 + 资料表单 + 保存
- [ ] **T13** `components/user/PasswordPanel.vue`：改密表单 + 成功自动登出跳登录
- [ ] **T14** `components/user/FavoritesPanel.vue`：收藏卡片网格 + 移除 + 分页 + 空态
- [ ] **T15** `HomeHeader.vue`：昵称入口改 `/user/profile`

## T 验证与文档

- [ ] **T16** 后端编译 + 重启服务 + 接口手测（资料/密码/收藏/401/404）
- [ ] **T17** 前端 `npm run build` 构建验证
- [ ] **T18** 手测记录 `docs/ch03/manual-test/user-center.md`
