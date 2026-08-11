# 会员模块 — 手工测试（Manual Test）

> 项目：dyshop 购物程序 · 模块：会员（ch09）
> 前置：`backend/sql/seed-member.sql` 已执行（注意用 `--default-character-set=utf8mb4` 灌中文）；`dyshop-api`(:8081) 与前端(:5173) 已启动
> 状态：M 段 curl 已全过（2026-08-06）；N 段浏览器用例已用 Playwright 自动冒烟覆盖主链路，剩余人工手测待回填
> 工具：终端 curl + 浏览器（参考 docs/ch07・docs/ch08/manual-test 的既有风格）

## M 段：后端接口 curl 联调

发起前先登录拿 token（买家 `buyer_m/123456`；管理员 `admin/admin123`）。

| 编号 | 操作 | 期望 |
|---|---|---|
| M1 | `GET /api/user/member/overview`（普通新用户） | 200：`level.code=NORMAL`，`annualConsumption=0`，`nextLevel.threshold=2000`，`progressPct=0` |
| M2 | 该用户下金额 ≥2000 的订单并确认收货后再查 | 等级升为 SILVER；`nextLevel.threshold=5000` |
| M3 | 未登录访问 `GET /api/user/member/overview` | 401 |
| M4 | 买家 token 访问 `GET /api/admin/member/levels` | 403 |
| M5 | admin token `GET /api/admin/member/levels` | 返回 4 个等级（NORMAL/SILVER/GOLD/DIAMOND） |
| M6 | `PUT /api/admin/member/levels/{SILVER id}` 改 `discountRate=0.97` → 银卡用户下单 | `order_item.price` 按 97 折，非 98 折 |
| M7 | 有 `vip_price` 的商品下单选银卡用户 | 价格 = vip_price（优先于折扣） |
| M8 | 完成订单后 `GET /api/user/member/overview` | `points` = floor(pay_amount×倍率)，`balance`≥0 |
| M9 | 重复确认收货该订单 | 积分不重复加（point_log 唯一） |
| M10 | `GET /api/user/member/points` | 分页返回流水，第一条 balance 与 overview points 一致 |
| M11 | `GET /api/admin/member/users?keyword=买家名` | 命中该会员，含等级/消费/积分字段 |
| M12 | 银卡 token `POST /api/user/member/price-preview` body `{rows:[{productId:7}]}` | 返回 `basePrice=79.00, memberPrice=77.42`，`userLevel.discountRate=0.98` |
| M13 | 银卡 token 同接口 productId 4 | `memberPrice=109.00`（vip_price 优先） |
| M14 | 未登录 POST price-preview | 401 |

> M 段结果（2026-08-06 实测）：M1–M14 全部通过 ✅（实盘口径：buyer_m 普通→银卡，annual=2094，专享价 109 优先于 98 折，咖啡豆 79×0.98=77.42，积分 624/1296，后台改折扣 0.97→还原 0.98；preview：7→77.42、4→109、SKU101→391.02、未登录 401）

## N 段：浏览器手测

账号：普通买家、管理员（admin/admin123）各一。

| 编号 | 场景 | 操作 | 期望 |
|---|---|---|---|
| N1 | 个人中心会员卡 | 登录普通买家进「我的-个人资料」 | 会员卡显示「普通会员 / 积分 / 距升级还差 ¥2000 / 进度条」非写死 |
| N2 | 下单享受折扣 | 银卡买家下单 | 结算页每行显示「会员价 ¥X（划线原价）」，底部「会员优惠 -¥X」+「应付合计」，支付弹层金额一致，为 98 折（无专享价商品） |
| N3 | 会员专享价商品详情 | 登录银卡打开商品详情 | 显示「会员价 ¥109 · 9.8 折」≠ 划线原价 |
| N4 | 折扣商品详情 | 登录银卡打开无 vip_price 的商品（如咖啡豆） | 显示「会员价 ¥77.42」（99→79×0.98） |
| N5 | 匿名详情不显示会员价 | 登出后打开商品详情 | 不出现「会员价」徽标 |
| N6 | 积分到账 | 确认收货一个订单 → 查看会员卡/积分流水 | 积分按倍率到账，流水有记录 |
| N7 | 后台等级配置 | 后台改金卡门槛/折扣保存 | 配置立即生效（C 端进度/下单价变化） |
| N8 | 后台会员列表 | 搜索买家/查看等级列 | 列表正确、可搜索 |

> N 段自动化冒烟（2026-08-06 Playwright 3/3 过）：银卡结算 295.42（129×2+79 折后）、详情 109/77.42、匿名无徽标。其余待用户执行后回填。