# 会员模块 — 开发计划（Plan）

> 项目：dyshop 购物程序 · 模块：会员（ch09）
> 状态：P0–P6 完成（后端 curl 联调全过 + 前端 build 通过 + Playwright 冒烟 3/3），P7 手工验证待用户执行
> 顺序：数据库与种子 → 后端规则计算与客服接口（可 curl 联调）→ 下单价格接入 → 积分发放 → 前端个人中心/商品/后台 → 手工验证
> 约定：后端全部走 dyshop-api:8081 `/api/admin/**`（D1）+ `/api/user/**`；编译用 `./mvnw install -pl dyshop-common`（如涉 common）+ `./mvnw compile -pl dyshop-api`；前端 `npm run build`

## P0 数据库与基础
- [ ] P0.1 `sql/schema.sql` 新增 `member_level` 表（等级配置，可编辑）+ `user.points` 积分余额列 + `product.vip_price` 会员专享价列 + point_log 积分流水表（预留）
- [ ] P0.2 `sql/seed-member.sql` 种子默认 4 级（普通/银卡/金卡/钻石，门槛 0/2000/5000/10000，折扣 1.0/0.98/0.95/0.90）；开发库执行
- 验证：后端启动 SQL 无报错；`SELECT * FROM member_level` 4 行

## P1 后端：等级规则计算
- [ ] P1.1 `MemberLevelVO`（code/name/threshold/discountRate/pointRate）+ `MemberLevelService.calc`：按用户**近12个月**已成交订单 `pay_amount` 之和匹配最齐全门槛取最高等级（可降级）
- [ ] P1.2 `MemberLevelServiceImpl`：`overview(userId)` 返回当前等级、累计消费、近12月消费、距离下一级进度/差额、积分余额；等级=后端实时计算（不落库，避免同步过期）
- [ ] P1.3 `UserController` 新增 `GET /api/user/member/overview`（会员全景，需登录）
- 验证：compile ✅；curl：新用户普通档、下单完成后升级到银卡门槛再查等级变化、无效 token 401

## P2 后端：下单价格接入
- [ ] P2.1 `OrderServiceImpl` 结算时按 `getCurrent(userId)` 取会员价：有 `vip_price` 优先 else `price × discountRate`；`order_item.price/subtotal` 写入该价；总价用折扣后小计
- [ ] P2.2 普通会员（discountRate=1.0）不改变现价路径，历史订单/线宽不受影响
- 验证：compile ✅；curl 下单：普通用户单价=原价，银卡=floor 折扣价；订单金额=折扣后合计

## P3 后端：积分结算
- [ ] P3.1 订单「已完成」（status=3）时按 `pay_amount × 等级积分倍率`（后端当前等级实时倍率）记积分；`point_log` 记录流水 + `user.points` 累加
- [ ] P3.2 幂等：已完成订单去重（order_id 唯一 / 前置状态角
- [ ] P3.3 `GET /api/user/member/points` 分页查询积分流水
- 验证：curl 完成一笔结束后 `GET /api/user/member/overview` points 增加且与流水一致；重复 confirm 不重复加

## P4 前端：个人中心会员卡片
- [ ] P4.1 `api/user.js` 新增 `fetchMemberOverview()` → `/user/member/overview`
- [ ] P4.2 重写 `AccountOverview.vue` 右侧会员卡：当前等级/积分/近12月消费/距下一级差额/进度条（实时计算），移除写死「银卡会员·再消费¥800」
- [ ] P4.3 `UserCenter.vue` 顶部会员名/提示随接口返回的数据渲染
- 验证：build ✅；Playwright：会员卡显示真实等级与积分，买入/确认收货后刷新进度更新
## P5 前端：商品会员价展示

- [x] P5.1 商品详情 `ProductInfoPanel.vue` 会员价展示：登录并取到 `member/overview.level` 后，`memberRate`（折扣<1 才启用）→ `memberPrice`（非规格优先 vip_price else 价格×折扣率），模板显示「会员价 ¥x · 9.8 折」+ 原价划线
- [x] P5.2 结算页 `Checkout.vue` 会员价预览：调 `POST price-preview`，商品行显示会员价/划线价，底部「会员优惠 -¥x」+「应付合计」与支付弹层一致
- [x] P5.3 商品列表可选（本期不做列表徽标）
- 验证：build ✅；Playwright：有 vip_price 商品详情显示会员价

## P6 前端：后台会员管理（本期范围）
- [ ] P6.1 `api/admin/member.js`：等级列表/更新（改门槛/折扣/积分倍率）+ 会员列表分页
- [ ] P6.2 `AdminMemberLevel.vue`：等级配置表格（code/名称/门槛/折扣率/积分倍率 可编辑保存）
- [ ] P6.3 `AdminMemberList.vue`：会员分页列表（用户名/等级/当前消费/累计消费/积分/加入时间，关键词搜索）
- [ ] P6.4 后台侧边栏 + 路由注册（/admin/members, /admin/member-levels）
- 验证：build ✅；Playwright：改折扣保存→C端等级 VIP 对应折扣生效

## P7 手工验证与回填
- [ ] P7.1 自动化冒烟覆盖会员主链路
- [ ] P7.2 浏览器人工手测 N 用例待用户执行回填（docs/ch09/manual-test/member.md N 段）