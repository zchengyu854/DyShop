# 订单操作交互 — 边界场景测试清单（Manual Test）

> 项目：dyshop 购物程序 · 模块：订单操作交互（ch10）
> 前置：`dyshop-api`(:8081) 与前端(:5173) 已启动；`buyer_m/123456` 有若干待支付订单
> 方法：浏览器手测为主，curl 辅助；Playwright 已覆盖核心用例（标注 ✅）

## 状态机矩阵（每个动作 × 三态）

| 动作 | 点击瞬间（50ms） | 成功回调 | 失败回调 |
|---|---|---|---|
| 去支付 | 打开支付弹窗（即时） | 支付成功 → 状态「待发货」 | toast「支付失败：msg」 |
| 取消订单 | 弹确认框 → 确定后按钮 loading | 状态「已取消」+ 按钮消失 | 回滚 + toast「取消失败：msg」 |
| 确认收货 | 弹确认框 → 确定后按钮 loading | 状态「已完成」 | 回滚 + toast「确认失败：msg」 |
| 删除订单 | 弹确认框 → loading | 行移除 | 回滚 + 按钮恢复 + toast「删除失败：msg」 |
| 重新下单 | loading | 加购后跳结算 | toast「商品均已下架…」 |

## M1 失败回滚（HTTP 500）

1. 打开订单列表，点「取消订单」→ 确认框 → 确定
2. 用浏览器 DevTools Network → 拦截 cancel 请求 → Response 500（或 mock）
3. 期望：确定后按钮立即 loading；**完成后** toast 报「取消失败」，状态仍为「待支付」，按钮恢复可点
   - Playwright 已验证 ✅（mock 500 → 状态回滚待支付 + toast）

## M2 弱网超时（>8s 无响应）

1. DevTools Network → Slow 3G / 或用延迟代理 12s
2. 点「取消订单」→ 确定
3. 期望：8s 时触发超时 → toast「网络较慢，请稍后重试」；状态回滚待支付；按钮恢复
   - Playwright 已验证 ✅（12s 延迟 → 8s 回滚）

## M3 重复点击 / 并发

1. 快速双击「取消订单」→ 确定按钮连点两次 / Enter + 双击
2. 期望：仅执行一次（确认后按钮 disabled 锁定）
3. 双开两个标签页同一订单各点「确认收货」+「取消」
4. 期望：后到请求被后端幂等 400 拒绝（一次动作只生效一次），前端回滚不闪断
   - Playwright 已验证确认按钮 loading 锁定 ✅

## M4 键盘可访问

1. Tab 聚焦「取消订单」→ Enter 打开确认框（自动聚焦「确定」）
2. Enter 确认 / Esc 关闭（busy 期间 Esc 无效）
3. Shift+Tab 在确认框内循环
   - Playwright 确认聚焦已验证 ✅；Esc/Tab 手测

## M5 倒计时超时（订单超时后支付禁用）

1. 创建待支付订单 → 等 15 分钟（或改 DB create_time）
2. 列表出现倒计时归零 → 支付按钮变「已超时」disabled；取消/重新下单可用

## M6 幂等验证（curl）

```bash
TOKEN=$(curl -s -X POST :8081/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"buyer_m","password":"123456"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
# 建单 → 取消两次 → 第二次应为 200（幂等）
curl -X POST :8081/api/orders -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"buyNow","productId":4,"quantity":1,"addressId":1}'
# 记下返回 id，以下重复调用应两次都 200：
#  POST /orders/{ID}/cancel ×2 ；POST /orders/{ID}/pay ×2 ；POST /orders/{ID}/confirm ×2 ；DELETE /orders/{ID} ×2
```

期望：
- 重复 `cancel`/`pay`/`confirm`/`delete` 均返回 200（已是目标状态）
- 无副作用重复：库存只回补一次、销量只加一次、积分流水只一条（point_log.order_id 唯一）
  - curl 已通过 12/12 ✅

## M7 切换 Tab / keep-alive

1. 「全部」下单 → 切到「个人资料」再切回订单列表
2. 期望：操作 loading 状态消失，列表经 onActivated 静默刷新保持一致（无残 loading）

## M8 Tab 筛选联动（ch10 P0 修复）

1. **点击刷新**：任一 Tab → URL 变 `?status=X`、列表 100ms 内出骨架/缓存、接口带 status 参数
2. **前进/后退**：待发货 → 待收货 → 后退：URL 回 `status=1`、Tab 回待发货、列表再刷新
3. **快速连续切换**：快速点 3 个 Tab → 终态 Tab 决定列表，无旧数据残留
4. **直达/书签**：地址栏输入 `/user/orders?status=2` 直接回车 → 激活待收货并加载
5. **缓存**：20s 内切回已访问 Tab → 立即呈现，无骨架闪烁；点「刷新」强制清缓存重拉
6. **网络失败重试**：断网点「刷新」→ 有数据保留 + 顶部细 banner + toast「加载失败，点击重试」；
   点重试恢复；无数据则整区错误态 + 重试
7. **空态**：切到无订单状态 → 插画 + 文案 + 去逛逛

> 已自动化：Playwright 9/9 ✅（tab-linkage-test.js）

## 已知限制（Out of Scope）

- 批量多选 UI 未做（runBatch 能力就绪）；物流展示无数据模型。
- 400 被幂等吞掉的场景：用户恰在「支付成功瞬间」刷新列表，状态短暂显示「待支付」→ 下次加载校正为「待发货」。