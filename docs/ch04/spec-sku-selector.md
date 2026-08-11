# 商品规格/SKU 多维选择器 — 方案设计（Spec + Plan + Tasks）

> 项目：dyshop 购物程序
> 模块：商品详情页规格选择（P0 体验缺陷修复）
> 关联：`docs/ch04/spec.md`（商品详情基础版）、`docs/ch05/`（购物车）、`docs/ch07/`（订单）
> 状态：设计定稿 · 开发按文末任务清单推进
> 触发背景：详情页右侧信息区（ProductInfoPanel）无规格选择模块，无法完成多规格商品的加购/下单。

---

## 1. 目标与范围

### 1.1 目标

在商品详情页右侧信息区实现**多维规格选择器**：

- 支持任意数量规格维度（型号/显存/内存/颜色/尺寸…）的动态组合选择
- 组合智能联动：无效组合自动置灰，缺货组合标注「缺货」并禁用
- 价格、库存、主图随 SKU 选择**实时联动**
- 未选完规格时**禁止**加购/下单（按钮与规格完整性绑定）
- 键盘可操作（WCAG 2.1）：radiogroup 语义 + 方向键切换
- 无规格商品（现有 10 个种子商品的 8 个）行为**完全不变**（向后兼容）

### 1.2 范围（In Scope）

| 编号 | 内容 | 归属 |
|---|---|---|
| S1 | 数据结构：`product` 表新增 `specs` / `skus` JSON 列；`ProductDetailVO` 暴露 | 后端 |
| S2 | 规格选择器组件 `SpecSelector.vue` + 联动算法 `useSpecSelector` | 前端 |
| S3 | 信息区集成：价格区间/实时价格、库存、主图联动、按钮完整性绑定 | 前端 |
| S4 | 加购/下单 SKU 穿透：`cart_item.sku_id`、`order_item.spec_text` 快照、SKU 库存校验/扣减/回补 | 后端 |
| S5 | 购物车/结算/订单展示规格文本；buyNow 携带 skuId | 前端 |
| S6 | 种子数据：3 个规格商品（耳机 2 维 / 手表 2 维 / 笔记本 4 维，含售罄与无效组合） | 数据 |
| S7 | 无障碍（radiogroup + 方向键 + aria-live）与体验细节（长文案换行/tooltip、置灰提示） | 前端 |
| S8 | 手测 + Playwright 自动化验证记录 | 文档 |

### 1.3 范围外（Out of Scope）

- 规格**依赖**（如选 Pro 才出现 32G 选项）：本期不做，值集合为静态笛卡尔
- 规格图片**矩阵**：本期仅 SKU 级 `image` 覆盖主图（颜色维度典型场景），不做每规格组图片
- 组合数 > 1000 时的懒计算/防抖：本期全部商品组合数 < 100（见 3.4 阈值说明，算法结构已预留替换点）
- 后台商品编辑界面的规格录入（无后台商品 CRUD，种子数据直接写库）

---

## 2. 设计决策（D1–D6）

### D1 数据结构：JSON 列 vs 规范化表

采用 **`product` 表 JSON 列**（`specs` + `skus`），不建 `sku`/`spec_value` 表。

理由：
- 本项目为演示工程，无后台商品 CRUD 入口，无按规格聚合查询需求
- MyBatis-Plus 下 JSON 列零 Mapper 成本，实体仅新增两个 String 字段，服务层用 Jackson 解析
- 数据规模小（单商品组合 ≤ 数十），JSON 读写性能无压力

```sql
ALTER TABLE `product`
    ADD COLUMN `specs` TEXT NULL COMMENT '规格维度定义(JSON数组)',
    ADD COLUMN `skus`  TEXT NULL COMMENT 'SKU列表(JSON数组)';
```

`specs` JSON 结构（维度定义，顺序即展示顺序）：

```json
[
  { "name": "型号", "values": ["MacBook Air", "MacBook Pro"] },
  { "name": "显存", "values": ["16GB", "32GB"] },
  { "name": "内存", "values": ["512GB", "1TB"] },
  { "name": "颜色", "values": ["深空灰", "银色"] }
]
```

`skus` JSON 结构（SKU 矩阵，`specs` 键为规格名 → 值）：

```json
[
  {
    "id": 1101,
    "specs": { "型号": "MacBook Air", "显存": "16GB", "内存": "512GB", "颜色": "深空灰" },
    "price": 7999.00,
    "originalPrice": 8999.00,
    "stock": 12,
    "image": "https://picsum.photos/seed/mba-gray/600/600"
  }
]
```

规则：
- `specs` 为 NULL → 无规格商品，前端不渲染选择器，全部走原有逻辑
- `skus` 中 `id` 全商品唯一（种子数据内约定 1xxx 区间），购物车/下单以此引用
- 无 `image` 的 SKU 不覆盖主图
- SKU 缺组合（如 Air+32G 不存在）→ 前端置灰（见 D3）

### D2 初始状态策略：**引导式（不自动全选）**

- 进入页面**不自动选中**第一个有效 SKU
- 仅当某维度只有 1 个值时自动预选（减少点击，无歧义）
- 未选完 → 价格显示**区间**（`¥min - ¥max`，min=max 时显示单值）、库存显示合计、按钮禁用并提示「请选择：型号, 显存」

理由：示例商品（笔记本等）为高客单价，误购成本高；与「未选完禁止提交」的需求一致。文档记录该策略取舍，若后续追求转化率可切「自动选中首个库存>0 的 SKU」模式（仅改一处初始化逻辑）。

### D3 智能联动算法（组合有效性）

核心不变量：**某规格值可选 ⟺ 存在「已选规格 ∪ 该值」约束下的有效 SKU（库存 > 0）**。

实现（纯函数，放 `useSpecSelector`，便于单测与替换）：

```
selectable(skus, selected, dim, value):
    matches = skus.filter(sku =>
        for each (d, v) in selected ∪ {(dim, value)}:
            sku.specs[d] == v
    )
    return matches.length > 0 && matches.some(sku => sku.stock > 0)

soldOut(skus, selected, dim, value):
    matches = skus.filter(sku => 已选 ∪ {value} 约束匹配)
    return matches.length > 0 && matches.every(sku => sku.stock == 0)
```

- 置灰（disabled）两种情形：**无效组合**（缺组合，无任何 SKU 匹配）与**缺货**（匹配但全部库存 0，追加「缺货」标注）
- 已选全维度且精确命中 SKU → `currentSku` 确定：价格/原价/库存/主图切换到该 SKU
- 全选后命中缺货 SKU → 价格照常显示 + 按钮禁用 + 「该规格已售罄」提示
- 部分选择但约束已收敛到唯一有货 SKU（仅剩一种组合时）→ 直接显示该 SKU 价格，仍提示「请选择：剩余维度」
- 已选维度被新选择**推翻**：只做派生约束（不重置已选值），冲突值自动置灰，用户可改回 —— 状态源唯一（`selected` Map），全部 UI 由 computed 派生（遵循 reactivity 最小状态原则）

### D4 库存语义与扣减

- **无规格商品**：`product.stock` 为唯一库存（原逻辑不动）
- **规格商品**：
  - `sku.stock` = SKU 级显示库存（种子数据维护）
  - `product.stock` = 全 SKU 库存合计，作为**并发安全扣减的真源**（沿用现有条件更新 `WHERE stock >= qty` 防超卖）
  - 下单同时**尽力更新** SKU JSON 内该 SKU 的 stock（读改写，事务内），保持选择器「缺货置灰」与订单口径一致；取消订单回补同理（read-modify-write 递增）
  - 权衡（记录）：JSON 列读改写非原子，极端并发下 SKU 显示库存可能轻微漂移，但真实防超卖由 product.stock 条件更新兜底，演示场景可接受
- 加购库存上限校验：规格商品按 `min(99, sku.stock)` 校验（`CartServiceImpl.checkStockLimit` 扩展）

### D5 性能阈值

- 组合数（笛卡尔积）≤ 1000：**预计算**（每次 selected 变化全量重算 selectable，O(维度 × 值 × SKU数)，本数据量微秒级）
- 组合数 > 1000（文档约定，本期不实现）：切换为懒计算 + 防抖查询，替换 `computeSelectable()` 单点即可
- 本期最大商品：笔记本 2×2×2×2 = 16 SKU，耳机/手表 6 SKU —— 远低于阈值

### D6 无障碍与体验细节（WCAG 2.1 AA）

| 细节 | 实现 |
|---|---|
| 语义 | 每维度一个 `role="radiogroup"`（`aria-label`=规格名）；规格项 `role="radio"` + `aria-checked` |
| 键盘 | 方向键在同组内移动（选中项 `tabindex="0"`，其余 `-1`）；Space/Enter 选中 |
| 禁用 | 置灰项 `aria-disabled="true"` + `tabindex="-1"`（跳读） |
| 实时反馈 | 价格/库存区域 `aria-live="polite"`，切换规格即时播报 |
| 触控 | 规格项最小 44px 高、字号 ≥14px（沿用全站触控规范） |
| 长文案 | 值文本超宽：CSS 自动换行；`title` 原生 tooltip 兜底；整组超 8 项时组内滚动（max-height） |
| 错误提示 | 未选完时按钮禁用 + 按钮旁提示「请选择：型号, 显存」；已登录可正常点击但未选规格时给 toast 兜底 |

---

## 3. 接口变更

### 3.1 GET `/api/products/{id}`（ProductDetailVO 扩展）

新增字段（无规格商品为 `[]`，保持兼容）：

```json
{
  "id": 1,
  "name": "降噪无线耳机",
  "price": 399.00,
  "stock": 200,
  "specs": [ { "name": "颜色", "values": ["黑色", "白色", "蓝色"] } ],
  "skus": [
    { "id": 101, "specs": { "颜色": "黑色" }, "price": 399.00, "originalPrice": 499.00, "stock": 60, "image": null }
  ]
}
```

### 3.2 购物车接口（SKU 穿透）

| 变更 | 说明 |
|---|---|
| `POST /api/cart/items` | body 增加 `skuId`（Long，默认 0=无规格）、`specText`（String，展示快照）；规格商品 skuId>0 且须校验存在/库存 |
| `PUT /api/cart/items/{cartItemId}` | **路径参数由 productId 改为 cartItemId**（同商品多 SKU 行需按行定位）；`DELETE`、`checked` 同改 |
| `GET /api/cart` | `CartItemVO` 增加 `skuId`、`specText`；行价格/库存按 SKU 取（规格商品） |

- `cart_item` 唯一键：`(user_id, product_id)` → `(user_id, product_id, sku_id)`（sku_id 默认 0，无规格商品互不干扰）
- 前端 Cart.vue 列表 key 与操作接口同步改用 `cartItemId`

### 3.3 下单接口

| 变更 | 说明 |
|---|---|
| `CreateOrderDTO` | 增加 `skuId`（buyNow 模式，Long，默认 0） |
| `order_item` | 增加 `spec_text`（下单快照，历史订单显示规格） |
| `OrderItemVO` | 增加 `specText` |

- 下单扣减：SKU 商品校验/扣减 SKU 库存（见 D4）；快照 `spec_text` 由服务端按 SKU 重新生成（`"型号:MacBook Air, 颜色:深空灰"`），**不信任前端直传**
- 前端 Checkout buyNow query 携带 `skuId` + `specText`（展示用）；cart 模式从购物车行读取

---

## 4. 前端组件设计

```
ProductDetail.vue（组装层，保持薄）
 ├─ ProductGallery.vue  ─── + prop: skuImage（覆盖大图；重置为商品图）
 ├─ ProductInfoPanel.vue ── 拥有 useSpecSelector 状态（业务层）
 │   ├─ SpecSelector.vue ── 展示层（纯 props/emits，radiogroup 语义）
 │   └─ 价格/库存/按钮联动 + 完整性绑定 + skuId/specText 穿透
 └─ （图文详情/推荐区不变）
```

### 4.1 `composables/useSpecSelector.js`（纯逻辑，可单测）

```js
useSpecSelector(specs, skus)  // props 为 shallowRef 级别的数据源
  state:  selected: reactive Map<dimName, value>（外部可写）
  computed:
    dims               // 规格组（含每值的 selectable/soldOut 标注）
    currentSku         // 全选且命中 → SKU；否则 null
    selectedComplete   // 全维度已选
    priceRange         // {min, max}（未全选时区间；无规格商品=商品价）
    totalStock         // 合计库存
    specText           // "型号:Air, 颜色:灰"（下单/加购展示快照）
    missingDims        // 未选维度名列表（提示文案）
  actions:
    select(dim, value) // 置灰项忽略；已选值再次点击可取消（本商品仅一个值时取消=空）
    reset()            // 商品切换时重置
```

- 无规格（specs 空）时：`currentSku=null`、`priceRange=商品价`、`selectedComplete=true`、`specText=''` —— **面板回归原有行为**
- 商品切换（路由复用）时 `reset()` + 单值维度自动预选

### 4.2 `components/shop/SpecSelector.vue`（展示层）

- props：`specs`、`skus`、`selected`（Map）、`modelValue` 语义用 `v-model:selected` 太重，**直接传 Map + emit('select', {dim, value})**
- 职责：渲染分组 + radiogroup 键盘导航 + 置灰/缺货样式 + 长文案处理；**不持有业务状态**
- 置灰项视觉：降低透明度 + 斜线删除线（区分「缺货」文案标注）；选中项蓝色描边高亮

### 4.3 `ProductInfoPanel.vue` 集成

- 价格区：未全选 → `¥min - ¥max`（区间）+ 划线原价区间；全选 → SKU 精确价；`aria-live`
- 库存区：全选 → `SKU 库存 N 件` / `该规格已售罄`；未全选 → `库存合计 N 件`
- 主图联动：选中 SKU 有 `image` → `emit('sku-image', url)`，ProductDetail 转发给 Gallery 覆盖大图；取消选中/换 SKU → 重置
- 按钮：规格商品未选完 → `disabled` + 「请选择：型号, 显存」提示；加购/立即购买携带 `skuId` + `specText`
- 加购：`cartStore.addToCart(productId, 1, skuId, specText)`；立即购买：`/checkout?buyNow=1&productId=&quantity=1&skuId=&specText=`

### 4.4 购物车/结算/订单规格展示

- Cart.vue：行内商品名下方显示 `specText`（小字灰）；key/接口改 cartItemId
- Checkout.vue：buyNow 从 query 读 skuId/specText 展示；行显示规格；payload 带 skuId
- OrderList/OrderDetail/UserOrders/AdminOrderDetail：订单项显示 `specText`

---

## 5. 种子数据设计（data.sql 追加 / 开发库同步）

| 商品 | 维度 | SKU 数 | 演示点 |
|---|---|---|---|
| id=1 降噪无线耳机 | 颜色×版本 | 6 | 1 个缺货 SKU（「白色/无线充电版」stock=0 置灰）；1 个无效组合（「蓝色/无线充电版」不存在） |
| id=2 智能运动手表 | 表带×尺寸 | 6 | 价格随规格变化（真皮款 +300） |
| id=11 **轻薄笔记本电脑（新增）** | 型号×显存×内存×颜色 | 16 | 4 维完整演示：Air+32G 无效；Pro 深空灰 1TB 缺货；SKU 带图（颜色联动主图） |

- 规格商品 `product.price` = 最低 SKU 价、`original_price` = 最高（列表页/兜底展示口径）
- 开发库同步：`docker exec mysql-dev mysql -uroot -proot dyshop < /path/sql/alter-sku.sql`（schema 变更 + 种子 UPDATE/INSERT，见 7.2）

---

## 6. 兼容性与回归

- 无规格商品：详情 VO specs/skus=[] → 面板行为与现状逐像素一致；购物车唯一键 (user_id, product_id, sku_id=0) 与原键等效
- 既有购物车数据：ALTER 后存量行 sku_id 默认 0，可正常结算（无规格）
- 既有订单：order_item.spec_text 为 NULL，前端渲染时为空串（不显示规格行）
- 旧书签/分享链接不变；`/api/products` 列表接口不变

---

## 7. 任务清单与验收

### 7.1 任务清单

**T 后端 — 数据与接口**
- [x] **T1** `schema.sql`：product 加 `specs`/`skus` TEXT；cart_item 加 `sku_id`/`spec_text` + 唯一键改 (user_id, product_id, sku_id)；order_item 加 `spec_text`；追加 `sql/alter-sku.sql`（开发库增量）
- [x] **T2** `Product` 实体 + `ProductDetailVO` 加字段；`ProductServiceImpl.toDetailVO` Jackson 解析（null → []，解析失败兜底空）
- [x] **T3** `CartItem` 实体 / `CartAddDTO` / `CartItemVO` / `CreateOrderDTO` 加 skuId/specText；`OrderItem` 实体 + `OrderItemVO` 加 specText
- [x] **T4** `CartServiceImpl`：addItem 支持 skuId（规格校验、按 (productId, skuId) upsert、checkStockLimit 按 SKU 库存）；update/remove/checked 改 cartItemId 定位
- [x] **T5** `OrderServiceImpl`：buildLines 解析 SKU（cart 读 cart_item.sku_id / buyNow 读 dto.skuId）、金额按 SKU 价、扣减/回补 SKU 库存（D4）、order_item 写 specText 快照
- [x] **T6** `data.sql` 追加：耳机/手表 specs+skus UPDATE、新增笔记本商品 id=11（含 specs/skus/图片）
- [x] **T7** 后端编译 + 重启 + curl 手测（详情 specs/skus、加购 skuId、库存校验、下单快照）

**T 前端 — 选择器**
- [x] **T8** `composables/useSpecSelector.js`：联动算法 + 派生状态（D2/D3）
- [x] **T9** `components/shop/SpecSelector.vue`：radiogroup 语义 + 方向键 + 置灰/缺货 + 长文案（D6）
- [x] **T10** `ProductInfoPanel.vue`：价格区间/实时价、库存、`emit('sku-image')`、按钮完整性绑定、skuId/specText 穿透
- [x] **T11** `ProductGallery.vue`：`skuImage` 覆盖大图（有值优先、清空回退）
- [x] **T12** `stores/cart.js` + `api/cart.js`：addToCart 带 skuId/specText
- [x] **T13** `Cart.vue`：cartItemId key + specText 展示
- [x] **T14** `Checkout.vue`：buyNow skuId/specText 穿透与展示
- [x] **T15** 订单展示 specText：OrderList/UserOrders/OrderDetail/AdminOrderDetail
- [x] **T16** `npm run build` + Playwright 自动化验证（联动/置灰/缺货/按钮禁用/购物车/下单全链路）

**T 文档**
- [x] **T17** 手测与自动化验证记录落盘（本文档 §8 + manual-test）

### 7.2 开发库同步 SQL（示意）

```bash
docker exec -i mysql-dev mysql -uroot -proot dyshop < backend/sql/alter-sku.sql
```

`alter-sku.sql` 内容：ALTER 三表 + 耳机/手表 UPDATE（specs/skus）+ 笔记本 INSERT。与 `schema.sql`（全新部署）保持一致口径。

### 7.3 验收标准

1. `/products/11`（笔记本）：4 维规格组渲染，未选完按钮禁用并提示缺失维度
2. 选「Air」→「32GB」置灰不可选；选「Pro/深空灰/1TB」→ 该 SKU 缺货置灰 + 标注
3. 选择「深空灰」系列 → 主图切换为 SKU 图；改选回其他 → 恢复商品图
4. 全选有效 SKU → 价格/原价/库存切换为 SKU 精确值；价格区间与合计库存随选择实时变化
5. 键盘：Tab 进入规格组，方向键切换选中，Screen Reader 播报 aria-checked
6. 加购：规格商品未选完点击加购无效；选完加购 → 购物车出现规格文本行；同 SKU 重复加购合并数量；不同 SKU 分行
7. buyNow 携带规格 → 结算页/订单显示规格文本 → 下单成功扣 SKU 库存 → 取消订单回补
8. 无规格商品（id=4 保温杯）全流程与改造前一致
9. 既有购物车/订单数据不破坏；`npm run build` 通过

## 8. 验证记录（2026-08-05）

### 8.1 后端 curl 手测（T7，API 运行于 :8081）

| 用例 | 结果 |
| --- | --- |
| `GET /api/products/1|2|11`：specs/skus 解析、null → []、无效 JSON 兜底 | ✅ |
| 加购带 skuId：同 SKU 累加、不同 SKU 分两行（唯一键 user+product+sku） | ✅ |
| 售罄 SKU 加购 → 400；无效组合 SKU（blue+无线充电）→ 400 | ✅ |
| buyNow/购物车下单：金额按 SKU 价、order_item 写 specText 快照 | ✅ |
| 下单扣 SKU 库存（70→68）→ 取消回补（→70） | ✅ |
| 无规格商品（/products/4）全流程回归 | ✅ |

### 8.2 前端 Playwright 自动化（T16，`frontend/sku-e2e2.cjs`，33 断言全 PASS）

覆盖：4 维渲染 / 初始按钮禁用+缺失提示 / 价格区间 / 合计库存 / Air+32G 无效置灰 / Pro+32GB+1TB 深空灰缺货置灰+标注 / 银色可选后按钮启用 / SKU 图覆盖大图 / 方向键切选中 / 加购行规格文本与 SKU 价 / 同 SKU 合并数量 / buyNow→结算→支付→订单详情规格 / 无规格商品回归（不渲染选择器、按钮可用、显示商品价）。

### 8.3 问题与修复记录

1. **E2E 中文 locator 超时根因（数据层乱码）**：首次自动化在「深空灰」hasText 断言处超时。排查（DOM codepoint 取证 + `xxd` API 字节）定位为**开发库种子数据被双重编码**（UTF-8 字节以 latin1 再编码入库，如 深 → `C3A8 C2BD C2BB`）。API 返回即乱码，前端渲染正确仍无法匹配。修复：用 `--default-character-set=utf8mb4` 连接 + 文件重定向（不经 shell 内联参数）重新写入 specs/skus/名称（`repair-sku-utf8.sql`），DB 字节恢复 `E6B7B1...` 正确编码。教训：**执行含中文的 SQL 一律走文件重定向 + 显式 utf8mb4 连接**。
2. **测试脚本误用已失效的 locator 方式**：初次失败被误判为产品代码问题，调试确认 DOM 正常后修正脚本断言（改用 `filter({ hasText })` + `\uXXXX` 转义规避脚本文件编码干扰）。
3. **E2E 二次进入商品页规格重置**：buyNow 段未重选规格导致「立即购买」禁用（符合预期行为），修正脚本补选。
