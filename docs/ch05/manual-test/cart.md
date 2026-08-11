# 购物车模块 — 手动测试记录（ch05）

> 用例编号：M1-M13。执行时记录实际结果与状态（✅ 通过 / ❌ 失败+原因 / ⏳ 待执行）。

## 环境

- 后端：8081（dyshop-api），测试账号 alice_01 / 654321
- 前端：vite dev 5173（/api 代理到 8081）
- 测试商品：默认商品集（picsum 图片）

## M 后端接口（curl）

| 编号 | 用例 | 步骤 | 预期 | 实际结果 | 状态 |
|---|---|---|---|---|---|
| M1 | 未登录访问购物车 | `curl http://localhost:8081/api/cart`（无 token） | code=401 | code=401 未认证 | ✅ |
| M2 | 登录获取 token | `POST /api/auth/login`（alice_01/654321） | 返回 token | 返回 token | ✅ |
| M3 | 空购物车列表 | `GET /api/cart`（带 token） | code=0，空数组 | `data:[]` | ✅ |
| M4 | 加购商品 | `POST /api/cart/items`（{productId:1, quantity:1}） | code=0 | code=0 | ✅ |
| M5 | 重复加购累加 | 再次 POST 同一商品 | code=0，列表 quantity=2 | quantity=2 | ✅ |
| M6 | 加购不存在商品 | `POST /api/cart/items`（productId=99999） | code=404 商品不存在 | code=404 商品不存在 | ✅ |
| M7 | 改量 | `PUT /api/cart/items/1`（{quantity:3}） | code=0，列表 quantity=3 | quantity=3 | ✅ |
| M8 | 改量超库存 | 加购库存 60 的商品后 `PUT quantity=61` | code=400 库存不足 | code=400 库存不足（剩余 60 件）；quantity=60 通过 | ✅ |
| M9 | 移除商品 | `DELETE /api/cart/items/1` | code=0，列表为空 | code=0，列表空 | ✅ |
| M10 | 移除不存在条目 | `DELETE /api/cart/items/99999` | code=0（幂等） | code=0 | ✅ |
| M11 | 清空购物车 | 加购两个商品后 `DELETE /api/cart` | code=0，列表为空 | code=0，列表空 | ✅ |
| M12 | 加购参数非法 | `POST /api/cart/items`（{productId:1, quantity:0}） | code=400 参数错误 | code=400 数量至少为 1；99999 被 @Max 拦 | ✅ |
| M13 | 列表商品信息完整 | 加购后 `GET /api/cart` | 含 name/price/mainImage/stock/quantity | 10 字段齐全 | ✅ |

## N 前端页面（浏览器）

| 编号 | 用例 | 步骤 | 预期 | 实际结果 | 状态 |
|---|---|---|---|---|---|
| N1 | 未登录加购引导登录 | 详情页点「加入购物车」 | 跳 /login?redirect=详情页；登录后回详情页 | | ⏳ |
| N2 | 已登录加购成功 | 登录后详情页点「加入购物车」 | 提示「已加入购物车」，Header 角标=1 | | ⏳ |
| N3 | 购物车列表渲染 | 访问 /cart | 左列表（图/名称/单价/数量）右摘要卡 | | ⏳ |
| N4 | 数量加/减 | 点步进器 + | 数量与行小计/合计同步；- 到 1 置灰 | | ⏳ |
| N5 | 步进器上限 | 连点 + 至库存 | 到达库存上限置灰，不再增长 | | ⏳ |
| N6 | 移除商品 | 点「移除」 | 行消失，合计/角标更新 | | ⏳ |
| N7 | 空购物车 | 移除全部商品后刷新 | 空态「购物车是空的」+「去逛逛」回首页 | | ⏳ |
| N8 | 结算按钮占位 | 点「结算」 | 提示「结算模块开发中」 | | ⏳ |
| N9 | 角标联动 | 多商品数量变化 | Header 角标始终等于总数 | | ⏳ |
| N10 | 未登录访问 /cart | 退出登录后直接访问 | 跳登录页，登录后回 /cart | | ⏳ |

## 结论

- 通过/失败汇总：
- 遗留问题：
