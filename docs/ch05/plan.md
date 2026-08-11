# 客户端购物车模块 — 开发计划（Plan）

> 前置：docs/ch05/spec.md、docs/ch05/tasks.md。计划按阶段推进，每阶段有明确产出与验证。

## P1 文档与准备（本期已完成）

- 产出：docs/ch05/spec.md、docs/ch05/tasks.md、docs/ch05/plan.md
- 验证：文档评审（编号确认：ch04=商品详情，ch05=购物车；策略：需登录、左列表右摘要）

## P2 后端购物车接口（T1–T5）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P2.1 | `CartItem` 实体 + `CartItemMapper` | 编译通过 |
| P2.2 | `CartItemVO` + `CartAddDTO` / `CartQuantityDTO` | 代码评审 |
| P2.3 | `CartService` / `CartServiceImpl`（累加、库存上限、404/400 校验） | 编译通过 |
| P2.4 | `CartController` 五个接口 | 编译通过 |
| P2.5 | 重启服务 + curl 手测 | curl 用例通过 |

- 配置：无新增；`/api/cart/**` 走既有 JWT 认证（默认拦截非白名单路径）
- 依赖：`cart_item` 表已存在（UNIQUE(user_id, product_id)），无数据变更

## P3 前端购物车（T6–T10）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P3.1 | `api/cart.js` + `stores/cart.js`（角标数量/合计 getters） | 代码评审 |
| P3.2 | `ProductInfoPanel` 加购接入（登录引导 + 成功/失败提示） | `npm run build` |
| P3.3 | `Cart.vue` 左列表右摘要（步进器/移除/空态/结算占位） | `npm run build` |
| P3.4 | `HomeHeader` 角标 + 全链路浏览器手测 | 浏览器手测 |

## P4 手测与文档（T11–T12）

- 产出：docs/ch05/manual-test/cart.md（用例、步骤、实际结果、通过/失败标注）
- 验证：所有用例执行完毕，失败项记录原因

### 进度记录（2026-08-04）

| 阶段 | 状态 | 备注 |
|---|---|---|
| P1 文档 | ✅ 完成 | spec / tasks / plan |
| P2 后端接口 | ✅ 完成 | 5 接口 + 编译 + 重启 + curl M1-M13 全部通过 |
| P3 前端购物车 | ✅ 完成 | api/store + 详情页加购 + Cart.vue + Header 角标，build 通过 |
| P4 手测与文档 | 进行中 | M 接口用例已回填；N 浏览器用例待人工 |

## 风险与对策

| 风险 | 对策 |
|---|---|
| 加购/改量超库存造成超卖 | 后端以商品当前库存为上限校验（1 ~ min(99, stock)），前端步进器同步置灰 |
| 快速连点加购重复请求 | 请求中禁用按钮；后端按 (user_id, product_id) 累加保证幂等 |
| 商品下架/删除后购物车残留 | 列表接口过滤失效商品并隐藏；不阻塞购物车页 |
| 401 并发触发多次跳登录 | request.js 统一处理（已在 ch04 加固），页面不做重复跳转 |
| 步进器请求失败 | 本地先行更新 + 失败回滚并提示 |
