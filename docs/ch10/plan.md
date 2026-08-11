# 订单操作交互重构 — 实施计划（Plan）

> 项目：dyshop 购物程序 · 模块：订单操作交互（ch10）
> 状态：已按序执行完毕（T1–T9 ✅），详见 tasks.md
> 目标：毫秒级视觉响应 + 可靠状态同步（点击即响应 / 状态锁 / 失败回滚 / 批量 / 菜单精简 / Tab 联动）

## P1 后端幂等（先固化契约，前端才敢乐观回滚+重试）

- P1.1 `OrderServiceImpl.cancel/pay/confirm/remove`：已处于目标状态 → no-op 成功
- 理由：弱网超时前端回滚后用户重试，不可再因「状态不允许」误报错；杜绝库存/销量/积分二次副作用
- 验证：curl 幂等 12/12 ✅

## P2 前端基础件

| 编号 | 件 | 职责 |
|---|---|---|
| P2.1 | `api/order.js` | 动作类接口 8s 超时（`ACTION_TIMEOUT`） |
| P2.2 | `composables/useOrderAction` | 命令注册表 + pending 锁 + 乐观/回滚 + 批量 runBatch |
| P2.3 | `components/shop/OrderActionButton` | 状态机按钮（spinner/loading/禁用，原生键盘） |
| P2.4 | `components/shop/OrderConfirmDialog` | 高危二次确认（聚焦/焦点圈定/Esc/busy 锁定） |

## P3 列表集成（UserOrders.vue）

- 替换原生 ops 按钮 → `OrderActionButton`（:busy=isPending, :disabled=anyPending/超时）
- 替换内联确认弹窗 → `OrderConfirmDialog`（confirmMeta 派生文案）；`patchOrder` 不可变
- 删除 `doAction/applyLocalStatus/rollbackStatus` 旧逻辑
- 验证：build + 取消流程 Playwright ✅

## P4 冒烟与回填

- Playwright 边界 3/3：500 回滚 / 12s 延迟→8s 超时回滚 / 连点 loading 锁定 ✅
- docs/ch10 spec/tasks/manual-test 回填 ✅

## P5 个人中心侧栏精简

- P5.1 菜单配置化：`config/userMenu.js`（六状态入口删除，注释留存）+ `components/user/SidebarMenu.vue` 纯展示
- P5.2 `UserCenter.vue` 收敛渲染：菜单由配置驱动，`menu` computed 合并管理员组；组件零业务逻辑
- P5.3 布局自适应：内容区 `1fr` 轨道自动扩展（无空白列）；`?status=` 外部直达保留
- 理由：侧栏 6 个状态入口与列表页 Tab 双份重复 → 路径混淆；收敛后单入口 + Tab 单份
- 验证：build ✅；Playwright 17/17 ✅；订单动作回归 4/4 ✅

## P6 Tab 筛选联动修复

- P6.1 根因定位：switchTab 先置 tab + watch 短路 → load 永不执行；store 兜底破坏后退态
- P6.2 收敛拉取到 `useOrderList(tab)`（watch 触发），URL 仅写入(push)与外部驱动回写
- P6.3 `OrderTabs` 受控控件 + 30s Tab 缓存 + 骨架/失败保留/竞态丢弃
- 理由：任意改 Tab 的入口（点击/手势/后退/直达）唯一收敛到一次 refetch，杜绝双份来源
- 验证：build ✅；Playwright 9/9 ✅（含后退三同步、快速切换、缓存无骨架、500 重试恢复）

## 风险与对策

| 风险 | 对策 |
|---|---|
| 乐观状态与服务端不一致 | 成功后 `load({silent})` 服务端校正；失败回滚快照 |
| 并发取消/确认 | 单订单 pending 锁 + 全局 anyPending + 后端幂等 400 |
| 多 tab 双支付 | 后端 pay 幂等（已支付直接成功，不重复插支付单） |
| keep-alive 残留 loading | pending 生命周期在组件内（finally 释放）；切走再回按需重建 |