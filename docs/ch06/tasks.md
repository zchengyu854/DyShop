# 客户端收货地址模块 — 任务拆解（Tasks）

> 前置：docs/ch06/spec.md。勾选状态随开发进度更新（☐ 未开始 / ☑ 完成 / ⚠️ 部分完成-待环境验证）。

## T 后端 — 接口

- [x] **T1** `Address` 实体（`dyshop-common/entity`，@TableName("address")，逻辑删除注解）+ `AddressMapper`（继承 BaseMapper）
- [x] **T2** `AddressDTO`（jakarta 校验：姓名/手机号正则/省市区/详情长度）+ `AddressVO`（含 fullAddress 拼接）
- [x] **T3** `AddressService` / `AddressServiceImpl`：`list / add / update / delete / setDefault`（数量上限 20、首条自动默认、设默认互斥事务、删除默认自动顺延、越权 404）
- [x] **T4** `AddressController`：`/api/addresses` 五个接口（principal=userId）
- [x] **T5** 后端编译 + 重启 + curl 手测（列表/新增/首条默认/上限 400/手机号 400/越权 404/编辑/设默认互斥/删除默认顺延/未登录 401）

## T 前端 — 数据层与地区数据

- [x] **T6** 引入地区数据：`frontend/src/assets/data/pca.json`（省->市->区 单文件，民政部行政区划）+ `src/utils/region.js` 读取封装
- [x] **T7** `api/address.js`：`fetchAddresses / createAddress / updateAddress / deleteAddress / setDefaultAddress`

## T 前端 — 页面

- [x] **T8** `AddressList.vue` 重写：标题行 + 新增按钮 + 卡片列表（默认标签/编辑/删除）+ 空态
- [x] **T9** `AddressFormModal.vue`：新增/编辑共用弹层表单（三级联动选择器、字段校验、默认开关与默认地址禁用规则）
- [x] **T10** 交互接入：删除确认、设默认、保存刷新列表，toast 提示

## T 验证与文档

- [x] **T11** 前端 `npm run build` 构建验证
- [x] **T12** 浏览器手测 + 记录 `docs/ch06/manual-test/address.md` + tasks/plan 回填
