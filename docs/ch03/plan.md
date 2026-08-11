# 客户端个人中心模块 — 开发计划（Plan）

> 前置：docs/ch03/spec.md、docs/ch03/tasks.md。计划按阶段推进，每阶段有明确产出与验证。

## P1 文档与准备（本期已完成）

- 产出：docs/ch03/spec.md、docs/ch03/tasks.md、docs/ch03/plan.md
- 验证：文档评审

## P2 后端个人中心接口（T1–T7）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P2.1 | favorite 表 DDL + 实体 | 建库脚本幂等执行 |
| P2.2 | DTO / VO（资料、密码、收藏） | 编译通过 |
| P2.3 | UserCenterService（资料/密码）+ FavoriteService | 编译通过 |
| P2.4 | UserController 6 个接口 | curl 手测 |
| P2.5 | 重启 api 服务（8081） | curl 手测 |

- 配置：无新增；全部接口走既有 JWT 认证链路（principal=userId）
- 依赖风险：schema.sql 追加建表需在已有库执行；实体新增后 common 需先 install

## P3 前端个人中心（T8–T15）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P3.1 | api/user.js + user store 扩展 | 代码评审 |
| P3.2 | UserCenter 布局（侧边栏 + 内容区，Apple 账号页风格） | `npm run build` |
| P3.3 | ProfilePanel / PasswordPanel / FavoritesPanel | `npm run build` |
| P3.4 | Header 昵称入口调整 | 浏览器手测 |

## P4 手测与文档（T16–T18）

- 产出：docs/ch03/manual-test/user-center.md（用例、步骤、实际结果、通过/失败标注）
- 验证：所有用例执行完毕，失败项记录原因

### 进度记录（2026-08-03）

| 阶段 | 状态 | 备注 |
|---|---|---|
| P1 文档 | ✅ 完成 | spec / tasks / plan |
| P2 后端个人中心接口 | 待执行 | |
| P3 前端个人中心 | 待执行 | |
| P4 手测与文档 | 待执行 | |

## 风险与对策

| 风险 | 对策 |
|---|---|
| JWT 无状态，改密后旧 token 仍有效 | 改密成功后前端强制登出跳登录页重新登录 |
| 收藏表未建导致接口 500 | 启动前先执行追加 DDL；文档注明执行方式 |
| 收藏商品被下架 | 列表仍展示（快照字段），详情页后续处理；添加时校验上架状态 |
| 编辑资料并发覆盖 | 本期单用户场景不做乐观锁；后续引入 update_time 比对 |
