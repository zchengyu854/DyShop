# 客户端收货地址模块 — 规格说明（Spec）

> 项目：dyshop 购物程序 · 模块：客户端收货地址管理（C 端 `/addresses`，需登录）
> 状态：v1.0 定稿
> 关联：`backend/dyshop-api`（address 表已建，接口本期新增）、`frontend/src/views/user/AddressList.vue`（占位改实现）

## 1. 目标

实现**收货地址管理**：列表、新增、编辑、删除、设默认；页面风格参考**苹果官网**——白底卡片列表 + 弹层表单。本期仅地址管理 CRUD，结算页地址选择留待订单模块（ch05 已约定结算占位）。

## 2. 范围

### 2.1 本期（In Scope）

| 编号 | 内容 | 归属 |
|---|---|---|
| S1 | 后端地址接口：列表 / 新增 / 编辑 / 删除（逻辑删除）/ 设默认 | 后端 |
| S2 | 默认地址规则：首条自动默认、设默认互斥、删除默认自动顺延 | 后端 |
| S3 | 前端数据层：`api/address.js`（+ 内置地区数据） | 前端 |
| S4 | `AddressList.vue`：卡片列表（默认标签 / 编辑 / 删除）+ 空态 | 前端 |
| S5 | 地址表单弹层：省市区三级联动选择器 + 校验 | 前端 |

### 2.2 本期外（Out of Scope）

- 结算页选择地址（订单模块 ch07 接入）
- 地址分组 / 标签（家、公司等）
- 导入第三方收货人（微信、淘宝地址同步）

## 3. 接口设计

全部接口**需认证**（`/api/addresses/**` 非白名单，SecurityConfig 自动拦截），principal=userId。DTO/VO 放 `dyshop-api/dto`、`dyshop-api/vo`（与 cart 一致）。

| 方法 | 路径 | 请求体 | 返回 |
|---|---|---|---|
| GET | `/api/addresses` | - | `List<AddressVO>` |
| POST | `/api/addresses` | `AddressDTO` | `void` |
| PUT | `/api/addresses/{id}` | `AddressDTO` | `void` |
| DELETE | `/api/addresses/{id}` | - | `void`（逻辑删除） |
| PUT | `/api/addresses/{id}/default` | - | `void`（设默认） |

`AddressVO`：`id, receiverName, receiverPhone, province, city, district, detail, isDefault, fullAddress`（fullAddress = 省+市+区+详细地址，服务端拼接，前端免拼）

`AddressDTO`（新增/编辑共用）：

| 字段 | 校验 | 规则 |
|---|---|---|
| receiverName | 必填 | 2~50 字符 |
| receiverPhone | 必填 | 中国大陆手机号 `1[3-9]\d{9}` |
| province / city | 必填 | ≤50 字符 |
| district | 选填 | ≤50 字符（个别地区无区级，表字段可空） |
| detail | 必填 | 5~200 字符 |
| isDefault | 选填 | 0/1，默认 0 |

### 业务规则

| 场景 | 行为 |
|---|---|
| 地址数量上限 | 每用户最多 **20 条**，超出 400「地址数量已达上限（20 条）」 |
| 新增首条地址 | 无论是否勾选，自动设为默认（保证始终有默认地址） |
| 设默认 | 事务内先清零该用户全部 `is_default`，再置当前 1 |
| 删除默认地址 | 自动把剩余地址中最近创建（create_time 最新）的一条设为默认；无剩余则跳过 |
| 编辑默认地址 | 勾选框禁用：默认地址不可直接取消，须先设其他地址为默认 |
| 越权访问 | 非本人地址一律 404「地址不存在」（不暴露存在性） |
| 删除 | 逻辑删除（deleted=1），列表过滤 deleted=0 |

## 4. UI 规范（苹果官网风格）

| 元素 | 规范 |
|---|---|
| 页面容器 | `max-width 1200px` 居中 + **Sticky Footer**（`.page-shell` min-height 100dvh flex column，`.addr-page` flex:1）；标题行：「收货地址」22px 600 + 计数 `N/20`（上限时橙色 600） |
| 网格 | `repeat(auto-fill, minmax(352px, 1fr))`，gap 16px（移动端 1 列 gap 12px）；卡片 min-height 176px |
| 卡片内容 | 三层级：L1 行（姓名 17px 600 + 手机号 14px 灰 tabular-nums **脱敏 `138****8000`** + 「默认」胶囊标签右对齐 `rgba(0,113,227,.12)`/`#0063c1`，仅默认卡显示）；L2 地址 14px lh1.6 **2 行 clamp**（桌面 hover 展开，悬停 title 兜底）；L3 操作区底部右对齐 `border-top:1px var(--border-line)` 分隔 |
| 操作区 | 按钮 36px 高 radius 8px，hover `--bg-gray`；删除 hover `rgba(255,59,48,.08)` + `#ff3b30`；非默认卡含「设为默认」 |
| 删除确认 | 自绘弹窗（非原生 confirm）：红底删除按钮；删除默认地址时追加说明「删除后将自动把最近添加的地址设为默认」 |
| 设为默认 | 乐观更新（本地置首 + isDefault 互斥）→ `TransitionGroup` FLIP 位移动画 → `toast.success('已设为默认地址')` 后以后端为准刷新 |
| 新增入口 | 虚线占位卡为**唯一**新增入口（右上角独立按钮已移除）：网格内「＋ 新增地址」虚线卡，hover 边框变蓝 + 背景微亮 + `＋` 放大（scale 1.25）+ focus-visible 描边 |
| 空态 | 邮筒插画（inline SVG）+「还没有收货地址」+ 副文案「保存常用地址，下单时免重复填写」+ 蓝色按钮 |
| 上限 | 计数显示 `20/20`（橙色 600）；达上限隐藏虚线占位卡（唯一入口自然消失，避免无效点击），删除 1 条后恢复 |
| 表单弹层 | 居中 Modal：遮罩 `rgba(0,0,0,.4)` + 白卡 20px 圆角，标题「新增收货地址 / 编辑收货地址」17px 600，字段纵向堆叠，底部「保存」蓝色胶囊全宽 44px |
| 三级联动 | 三个 select 并排（省 / 市 / 区），选省重置市和区，选市重置区；无区数据的直辖市直接选「区」跳过 |
| 表单校验 | 失焦即时校验，错误红字 12px 显示在字段下方；保存时全量校验 |

## 5. 交互细节

- 删除：点击「删除」弹自绘确认弹窗（删除默认地址时追加顺延说明），确认后调接口，成功 toast.success，列表移除
- 设默认：乐观更新本地置顶 + isDefault 互斥 + FLIP 动画，成功后 toast.success「已设为默认地址」并以后端为准刷新；失败回滚
- 新增/编辑保存成功：关弹层 + toast.success + 刷新列表
- 页面加载：进入 `/addresses` 拉列表；401 由 request.js 统一跳登录
- 地区数据：静态 JSON 放 `frontend/src/assets/data/pca.json`（民政部行政区划数据，省名 -> {市名 -> [区名]}，31 省/市/区全量，gzip 约 21KB），`src/utils/region.js` 提供 getProvinces/getCities/getDistricts 读取，Vite 原生 JSON 导入，运行时无请求开销

## 6. 验收标准

- 后端：编译通过；curl 覆盖 列表/新增/首条默认/上限 400/手机号 400/越权 404/编辑/设默认互斥/删除默认顺延/未登录 401
- 前端：`npm run build` 通过；浏览器手测全流程通过（详见 docs/ch06/manual-test/address.md）
