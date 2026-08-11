# 客户端收货地址模块 — 开发计划（Plan）

> 前置：docs/ch06/spec.md、docs/ch06/tasks.md。计划按阶段推进，每阶段有明确产出与验证。

## P1 文档与准备（本期已完成）

- 产出：docs/ch06/spec.md、docs/ch06/tasks.md、docs/ch06/plan.md
- 验证：文档评审（编号确认：ch05=购物车，ch06=收货地址；方案确认：三级联动内置地区数据、仅地址管理 CRUD、弹层表单）

## P2 后端地址接口（T1–T5）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P2.1 | `Address` 实体 + `AddressMapper`（逻辑删除） | 编译通过 |
| P2.2 | `AddressDTO` + `AddressVO` | 代码评审 |
| P2.3 | `AddressService` / `AddressServiceImpl`（上限 20、默认互斥、删除顺延） | 编译通过 |
| P2.4 | `AddressController` 五个接口 | 编译通过 |
| P2.5 | 重启服务 + curl 手测 | curl 用例通过 |

- 配置：无新增；`/api/addresses/**` 走既有 JWT 认证（默认拦截非白名单路径）
- 依赖：`address` 表已存在（schema.sql 已建，含 deleted/is_default 字段），**无数据变更**
- 注意：`dyshop-common` 新增实体后先 `./mvnw install -pl dyshop-common` 再启动 api

## P3 前端数据层与地区数据（T6–T7）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P3.1 | 下载省市区静态 JSON 至 `src/assets/data/pca.json`（省->市->区 单文件，gzip ~21KB）+ `src/utils/region.js` 读取封装 | 文件可 JSON.parse |
| P3.2 | `api/address.js` 五个方法 | 代码评审 |

## P4 前端页面（T8–T10）

| 步骤 | 内容 | 验证 |
|---|---|---|
| P4.1 | `AddressList.vue` 卡片列表 + 空态 + 默认标签 | `npm run build` |
| P4.2 | `AddressFormModal.vue` 三级联动表单 + 校验 | `npm run build` |
| P4.3 | 删除确认 / 设默认 / 保存刷新 + 浏览器手测 | 浏览器手测 |

## P5 手测与文档（T11–T12）

- 产出：docs/ch06/manual-test/address.md（用例、步骤、实际结果、通过/失败标注）
- 验证：所有用例执行完毕，失败项记录原因

### 进度记录

| 阶段 | 状态 | 备注 |
|---|---|---|
| P1 文档 | ✅ 完成 | spec / tasks / plan |
| P2 后端接口 | ✅ 完成 | 实体/Mapper/DTO/VO/Service/Controller + 编译 + 重启 + curl M1-M12 全部通过 |
| P3 前端数据层 | ✅ 完成 | pca.json + region.js + api/address.js |
| P4 前端页面 | ✅ 完成 | AddressList + AddressFormModal（三级联动），build 通过 |
| P5 手测与文档 | ✅ 完成 | 自动化 35 断言全 PASS；`manual-test/address.md` 已落盘；tasks T12 回填 |

## 风险与对策

| 风险 | 对策 |
|---|---|
| 省市区数据源体积大 / 结构不一致 | 选用民政部标准三表（province/city/area），打包前确认 gzip 体积；三级关联用 parent code 匹配 |
| 直辖市/省直辖县无区级数据 | district 允许为空，联动选择器可跳过区一级 |
| 并发设默认导致多个默认 | setDefault 在事务内先全清零再置位（行锁 + user_id 范围更新），Redis 锁本期不引入 |
| 删除默认地址后无默认地址 | 删除时事务内自动顺延最新创建地址为默认，保证恒有一个默认（非空时） |
| 用户信息手机号与地址手机号混淆 | 地址手机号为独立字段（收货人电话），与账号手机号无关 |
