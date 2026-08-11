# 订单操作交互重构 — 任务清单（Tasks）

> 项目：dyshop 购物程序 · 模块：订单操作交互（ch10）
> 约定：前端 `npm run build`；后端 `./mvnw compile -pl dyshop-api`（改 common 实体时 install）
> 状态：T1–T9 完成（前端 build + 后端 compile + Playwright 边界 3/3 + curl 幂等 12/12 + 侧栏精简 17/17 + Tab 联动 9/9）

## T1 后端幂等
- [x] T1.1 `OrderServiceImpl`：`cancel/pay/confirm/remove` 对「已处于目标状态」幂等返回成功（no-op）：
  cancel 已取消、pay 已支付、confirm 已完成、remove 已删 → 200 不重复副作用（库存/销量/积分唯一）
- 验证：curl 幂等 12/12（首次+重复 cancel、重复 pay 不重复销量、confirm 已确认不重复积分、delete 幂等、违例拒绝）✅

## T2 前端 API 层
- [x] T2.1 `api/order.js`：动作类请求统一 8s 超时（`ACTION_TIMEOUT=8000`），与列表/详情读 10s 区分
- [x] T2.2 保持 `isOrderActionUrl` 供 request 层 no-store 判定（不变）

## T3 `useOrderAction` 钩子
- [x] T3.1 命令注册表 `ORDER_ACTIONS`（pay/cancel/confirm/delete：api/乐观目标/文案/needsConfirm）
- [x] T3.2 pending 状态锁（Map<orderId,action>）+ `isPending/anyPending`
- [x] T3.3 乐观更新 + 快照回滚（不可变 patchOrder，不 mutate 订单对象）
- [x] T3.4 超时错误归一（ECONNABORTED/timeout → 网络较慢提示）
- [x] T3.5 批量 `runBatch`：并发执行 + 逐单回滚 + `{succeeded,failed}` 汇总
- 验证：build ✅

## T4 `OrderActionButton` 状态机按钮
- [x] T4.1 props：variant/busy/disabled/label/loadingText/compact；emits click
- [x] T4.2 busy → spinner + loadingText + aria-busy + 禁用；原生 button 键盘可达
- 验证：build ✅

## T5 `OrderConfirmDialog` 二次确认
- [x] T5.1 打开自动聚焦「确定」；Tab 焦点圈定；Enter/Space 触发；Esc 关闭（busy 无效）
- [x] T5.2 busy：确认按钮 spinner + 禁用，防重复提交
- 验证：build ✅；Playwright：打开聚焦确定、确认后 loading ✅

## T6 列表页集成（UserOrders.vue）
- [x] T6.1 替换 ops 原生按钮为 OrderActionButton（取消/确认/删除 + 去支付触发 PayModal）
- [x] T6.2 替换内联确认弹窗为 OrderConfirmDialog（confirmMeta 派生 title/message/danger）
- [x] T6.3 移除旧 `doAction/applyLocalStatus/rollbackStatus`；`patchOrder/removeOrder` 不可变实现
- [x] T6.4 全局 `anyPending` 互斥禁用 + 对单卡 `isPending` 精确 loading
- 验证：build ✅；取消流程 Playwright ✅

## T7 边界验证与文档
- [x] T7.1 Playwright 边界用例 3/3：失败回滚（500）+ 弱网超时（12s 延迟→8s 回滚）+ 防连点 loading
- [x] T7.2 curl 幂等 12/12（T1.1 验证）
- [x] T7.3 docs/ch10：spec.md（时序图）/ tasks.md / manual-test（边界清单）

## T8 个人中心侧栏精简（订单状态分类收敛）
- [x] T8.1 `config/userMenu.js` 新增：`USER_MENU` / `ADMIN_MENU` 配置数组（type: link/orders/todo），
  删除的六个状态入口以注释留存
- [x] T8.2 `components/user/SidebarMenu.vue` 新增：配置驱动纯展示组件，orders/todo 上抛事件，
  link 走 router-link（active-class 高亮）
- [x] T8.3 `UserCenter.vue`：内联菜单收敛为组件；`menu` computed 按 isAdmin 合并 ADMIN_MENU；
  删除失效的 `.menu/.menu-item` 样式；goOrders 不变（store 记忆筛选）
- [x] T8.4 布局验证：内容区 `1fr` 轨道自适应（cw=792px@1100 容器 = 总宽−内边距−侧栏−间距）；移动端单列不变
- [x] T8.5 URL query 直达保留：`?status=0..4` 外部链接仍激活对应 Tab（watch route.query.status 不变）
- 验证：build ✅；Playwright 17/17（六项删除/保留项/单入口不重置筛选/query 直达/1fr 宽度/项数）✅；
  订单动作回归 4/4 ✅

## T9 Tab 筛选联动修复（P0：高亮切换但列表不刷新）
- [x] T9.1 `composables/useOrderList.js` 新增：`useOrderList(statusKey)` + 30s Tab 缓存
  （命中直接渲染+后台静默刷新）/ 骨架 / 失败保留内容 / 竞态 seq；`clearOrderCache` 导出
- [x] T9.2 `components/shop/OrderTabs.vue` 新增：受控分段控件（modelValue 驱动，内部零状态）
- [x] T9.3 `UserOrders.vue` 修复根因：拉取仅由 hook watch(tab) 触发；switchTab 更新 tab 后
  `router.push` 同步 URL；watch(route.query.status) 只回写 tab（去掉 store 兜底覆盖后退态）
- [x] T9.4 tab 以 URL 为初始种子（`ref(tabFromQuery())`），消除挂载双请求
- [x] T9.5 错误语义：失败保留列表 + 顶部 banner/空态重试 + toast「加载失败，点击重试」
- [x] T9.6 列表容器 `:key="tab"` 强制重建 DOM；空态插画 + 文案
- 验证：build ✅；Playwright 9/9（点击刷新/URL 同步/后退三同步/快速切换/缓存无骨架/骨架先显/500 重试恢复/空态）✅