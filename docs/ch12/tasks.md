# 售后/退款模块 — 任务拆解（Tasks）

> 模块：ch12 售后 · 依据 `docs/ch12/spec.md` v1.0
> 勾选规则：实现 → 代码评审 → 手工用例过完，方可打勾。

## P0 文档

- [x] spec / tasks / plan 三件套（docs/ch12/）

## P1 数据层

- [x] 迁移脚本 `backend/sql/after-sale.sql`（after_sale 表 + order_item_id 唯一索引），schema.sql 同步
- [x] 实体 `AfterSale` + `AfterSaleMapper`（沿用 MyBatis-Plus 风格）
- [x] `OrderItemVO` 补 `id`（order_item 主键，售后申请入参），`OrderServiceImpl.fetchItems` 填充

## P2 后端接口

- [x] `AfterSaleService` + C 端接口：申请（R1-R3 校验 + 退款金额自动算 + 单号生成）、我的列表/详情、取消（R4）
- [x] 后台接口：列表（状态/keyword 筛选）、同意（模拟退款 R5）、拒绝（必填理由）
- [x] ResultCode 追加 409「该商品已申请售后」

## P3 前端

- [x] C 端：`api/after-sale.js` 封装；「我的售后」列表页（/user/aftersales）+ 申请弹窗
- [x] 订单列表/详情：已完成订单显示「申请售后」入口（按 order_item）
- [x] userMenu / 快捷操作区「售后/退款」入口接入
- [x] 后台：`api/admin/after-sale.js` + 售后管理页（列表/审核弹窗）+ 后台菜单

## P4 验证与文档

- [x] 后端编译 + 启动 + 接口联调（申请/重复409/取消/同意/拒绝/列表筛选）
- [x] 前端 `npm run build`
- [x] 手测记录 `docs/ch12/manual-test/after-sale.md` 回填
