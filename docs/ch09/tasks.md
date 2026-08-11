# 会员模块 — 任务清单（Tasks）

> 项目：dyshop 购物程序 · 模块：会员（ch09）
> 约定：编译 `./mvnw install -pl dyshop-common && ./mvnw compile -pl dyshop-api`（改动 common 实体时）；前端 `npm run build`
> 状态：T1–T9 完成（后端 curl 全过 + 前端 build 通过 + Playwright 冒烟 3/3），N 段手工待用户回填

## T1 数据库与种子
- [x] T1.1 `sql/schema.sql`：新增 `member_level` 表 + `point_log` 表；`user` 加 `points` 列；`product` 加 `vip_price` 列
- [x] T1.2 `sql/seed-member.sql`：`member_level` 种子 4 行 + 商品 3/4 演示专享价；开发库执行并验证（注意 utf8mb4 字符集灌中文）

## T2 后端：等级计算 + 会员全景接口
- [x] T2.1 `MemberLevelVO`：code/name/threshold/discountRate/pointRate
- [x] T2.2 `MemberOverviewVO`：level/totalConsumption/annualConsumption/nextLevel/progressPct/points
- [x] T2.3 `MemberLevelService` 接口 + 实现：`getCurrentLevel`（近12月已成交订单 pay_amount 求和 → 等级表最高匹配门槛，可降级）、`overview`、`resolvePrice`、`grantPoints`
- [x] T2.4 C 端 `GET /api/user/member/overview`（UserController）
- [x] T2.5 C 端 `GET /api/user/member/points` 分页
- 验证：compile ✅；curl 初级/升级/权限三态 ✅

## T3 下单价格接入
- [x] T3.1 `OrderServiceImpl` 下单时 `unitPrice` 走 `resolvePrice`：普通=原价；银卡+=无规格且有 vip_price 用专享价，否则 price×折扣；SKU 按 SKU 价×折扣
- 验证：curl：普通624（129/79 原价）、银卡保温杯 109 专享优先、咖啡豆 77.42（79×0.98）✅

## T4 积分结算
- [x] T4.1 `OrderServiceImpl.confirm`（status 2→3）后 `grantPoints`：`points = floor(payAmount × pointRate)`；`user.points` 累加；写 `point_log`（balance）
- [x] T4.2 幂等：`point_log.order_id` 唯一索引 + 前置状态判断兜底
- 验证：curl 完成后 points/流水一致（624、1296），改等级后倍率取完成时刻 ✅

## T5 前端：个人中心会员卡片
- [x] T5.1 `api/user.js` 新增 `fetchMemberOverview()` / `fetchMemberPoints()`
- [x] T5.2 重写 `AccountOverview.vue` 会员卡：等级/积分/距下一级差额/进度条实时渲染；「优惠券」占位替换为「积分」
- [x] T5.3 `UserCenter.vue` 侧边会员卡随接口渲染
- 验证：build ✅；Playwright：银卡/41%/再消费 ¥2,906 升级金卡/积分 1,296 ✅

## T6 前端：商品详情会员价展示（按需接口版）
- [x] T6.1 `ProductInfoPanel.vue`：`onMounted` 拉 `member/overview` 取 `.level`（需解包，直接赋整体对象会丢 discountRate）；`memberRate`（折扣<1 才启用）→ `memberPrice`（非规格优先 vip_price else 价格×折扣率）；模板「会员价 ¥X · 9.8 折」+ 原价划线
- [x] T6.2 匿名/普通等级不显示会员价（memberRate 为空即整体不展示，含 vip_price 商品）
- 验证：build ✅；Playwright：银卡看商品4「¥109」+商品7「¥77.42」，匿名不显示 ✅

## T7 后台：会员管理
- [x] T7.1 `api/admin/member.js`：levels（list/update）+ users（分页列表）
- [x] T7.2 `AdminMember.vue`：Tab「等级配置」（门槛/折扣/倍率行内可编辑保存）+「会员列表」（搜索/表格：等级/近12月消费/累计消费/积分）+ 分页
- [x] T7.3 后台路由 /admin/members + AdminLayout 侧边栏「会员管理」
- 验证：build ✅；Playwright：等级配置 4 行、会员列表含 buyer_m 银卡/¥2,094.00/1296 ✅；curl 改折扣 0.97→生效→还原 ✅
- 合并（2026-08-06）：会员管理并入「用户管理」为 Tab（用户列表 + 会员等级），`/admin/members` 重定向至 `/admin/users`，`AdminMember.vue` 移除

## T8 前端：结算页会员价预览（下单页体现折扣）
- [x] T8.1 后端 `MemberPricePreviewVO` + `MemberLevelService.previewPrices`（复用 resolvePrice 规则）+ `POST /api/user/member/price-preview`（未登录 401）
- [x] T8.2 `api/user.js` 新增 `fetchMemberPricePreview(rows)`
- [x] T8.3 `Checkout.vue`：商品行「会员价 ¥X / 划线原价」；底部「会员优惠 -¥X」+「应付合计」行；支付弹层金额一致
- 验证：curl 银卡 [4→109, 7→77.42, SKU101→391.02]，普通/未登录态 ✅；Playwright 3/3：结算页 295.42（129×2+79 折后）、详情 109/77.42、匿名无徽标 ✅

## T9 冒烟与回填
- [x] T9.1 自动化冒烟会员主链路 3/3（结算会员价 / 详情会员价 / 匿名不显示）
- [ ] T9.2 手工测试 N 段由用户回填（docs/ch09/manual-test/member.md）