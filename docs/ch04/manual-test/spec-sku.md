# 规格/SKU 选择器 — 手测与自动化记录（2026-08-05）

> 任务：T16/T17 · 用例基线：docs/ch04/spec-sku-selector.md §7.3

## 1. 环境前置

- 后端：`mvnw -pl dyshop-api spring-boot:run`（:8081，日志 /tmp/dyshop-api.log）
- 前端：`npm run dev`（:5173）
- 数据库：docker mysql-dev，`--default-character-set=utf8mb4` 连接，UTF-8 数据已修复
- 账号：buyer01 / test123456

## 2. 自动化验证（33 断言全 PASS）

```bash
cd frontend && node sku-e2e2.cjs   # 脚本含中文以 \uXXXX 转义，规避编码问题
```

关键结果节选：

| 断言 | 结果 |
| --- | --- |
| /products/11 渲染 4 个规格组（radiogroup） | ✅ |
| 未选完：加购/立即购买禁用 + 提示缺失维度 + 价格区间 + 合计库存 | ✅ |
| Air → 32GB 无效组合置灰（aria-disabled=true） | ✅ |
| Pro/32GB/1TB → 深空灰缺货置灰 +「缺货」标注，银色可选 | ✅ |
| 选银色 → 价格 14999 / 主图切换 SKU 图 / 按钮启用 | ✅ |
| 方向键 ArrowRight 从置灰项跳到银色并选中 | ✅ |
| 加购：1 行、规格文本、SKU 价；同 SKU 数量合并（+1→2） | ✅ |
| 耳机白色+无线充电版缺货置灰、蓝色+无线充电版无效置灰（无缺货标注） | ✅ |
| buyNow → 结算页规格 → 支付 → 订单详情规格快照 | ✅ |
| /products/4 无规格商品回归：无选择器、按钮可用、价格 129 | ✅ |

## 3. 遗留说明

- E2E 产生的测试订单/购物车已清理（orders 16、payment 6、order_item 16），SKU 库存已回补（101 → 70）。
- 乱码根因与修复详见 spec-sku-selector.md §8.3：种子数据经 latin1 连接二次编码入库；已用 utf8mb4 + 文件重定向重写 specs/skus/名称。
- 已知注意点：含中文的 SQL 执行必须 `--default-character-set=utf8mb4` 且数据经文件重定向，禁止经 shell 内联参数传递。
