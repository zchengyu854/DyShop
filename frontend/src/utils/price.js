// 价格计算纯函数引擎（整数分运算，禁止浮点直接计算）
// 规则与后端 OrderServiceImpl.preview/createOrder 完全一致（docs/ch11/spec.md §6.4）：
//   1) 门槛/抵扣按「适用商品原价小计」判定；discount = min(券额, 适用小计)，不超适用小计
//   2) 二选一自动取优：券方案（原价 − 券额）与会员方案（会员价合计）取更省；券不生效则回退会员方案
//   3) 实付下限 0，不返现（极端：券额 ≥ 应付 → 实付 0）
//   4) 多券叠加：按优先级数组顺序依次扣减，每步校验剩余金额满足下一张门槛

/**
 * 元 → 整数分。非法输入按 0 处理。
 * @param {number|string} yuan
 * @returns {number} cents
 */
export function toCents(yuan) {
  const n = Number(yuan)
  if (!Number.isFinite(n)) return 0
  return Math.round((n + Number.EPSILON) * 100)
}

/**
 * 整数分 → 元（以分为单位的精确除法，避免 0.1+0.2 类浮点误差）
 * @param {number} cents
 * @returns {number} 元
 */
export function centsToYuan(cents) {
  return (cents || 0) / 100
}

/**
 * 金额格式化：¥X.XX 千分位；负数显示 -¥X.XX。
 * @param {number} cents
 * @returns {string}
 */
export function formatMoney(cents) {
  const abs = Math.abs(Math.trunc(cents || 0))
  const yuan = Math.floor(abs / 100)
  const fen = String(abs % 100).padStart(2, '0')
  const thousands = String(yuan).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return `${(cents || 0) < 0 ? '-' : ''}¥${thousands}.${fen}`
}

/**
 * 结算行输入（price.js 内部统一转为分）：
 * @typedef {Object} PriceItem
 * @property {number} basePriceCents  原价单价（分）
 * @property {number} quantity        数量
 * @property {number} [memberPriceCents] 会员价单价（分，缺省=原价，即无会员权益）
 * @property {number} [productId]     商品 id（LIMITED 范围匹配用）
 * @property {number} [categoryId]    分类 id（LIMITED 范围匹配用）
 */

/**
 * 券输入：
 * @typedef {Object} PriceCoupon
 * @property {string} [id]            券实例标识（userCouponId）
 * @property {'REDUCE'} type          券类型（本期仅立减）
 * @property {number} minAmountCents  门槛（0=无门槛）
 * @property {number} discountAmountCents 券面额
 * @property {'ALL'|'LIMITED'} [scope] 适用范围，默认 ALL
 * @property {number[]} [categoryIds] LIMITED 时：命中分类（与 productIds 并集）
 * @property {number[]} [productIds]  LIMITED 时：命中商品
 * @property {number} [priority]      多券叠加优先级（小者先扣）
 */

/**
 * @typedef {Object} PriceFreight
 * @property {number} amountCents 运费（本项目恒 0，预留）
 */

// 字段均为整数分（cents），此处只做安全清洗，不再换算（换算入口在外部：元→分用 toCents）
function safeCents(v) {
  const n = Number(v)
  return Number.isFinite(n) && n > 0 ? Math.trunc(n) : 0
}

/**
 * 价格计算纯函数。
 * @param {PriceItem[]} items 商品行
 * @param {PriceCoupon|PriceCoupon[]|null} coupon 单券对象 / 多券数组（按 priority 依次叠加）/ null
 * @param {PriceFreight} [freight] 运费
 * @returns {{
 *   totalCents: number,          // 商品总额（原价合计）
 *   memberTotalCents: number,    // 会员价合计
 *   memberBenefitCents: number,  // 会员优惠额（= 会员方案优惠）
 *   couponDiscountCents: number, // 实际抵扣总额（券方案生效时为券额合计，否则 0）
 *   discountCents: number,       // 总优惠（自动取优后：会员方案=会员优惠额，券方案=券额合计）
 *   payCents: number,            // 实付（≥0，不返现）
 *   freightCents: number,
 *   couponApplied: boolean,      // 券方案是否生效
 *   usedCoupons: Array<{ id: string, name?: string, discountCents: number }>,
 *   invalid: Array<{ id: string, name?: string, reason: string }>, // 不可用券及原因
 * }}
 */
export function calculateOrderTotal(items, coupon, freight = {}) {
  const list = (coupon == null ? [] : Array.isArray(coupon) ? coupon : [coupon])
    .filter(Boolean)

  const rows = (items || []).map((it) => ({
    basePriceCents: safeCents(it.basePriceCents),
    quantity: Math.max(1, Math.trunc(it.quantity) || 1),
    memberPriceCents: it.memberPriceCents == null ? null : safeCents(it.memberPriceCents),
    productId: it.productId,
    categoryId: it.categoryId,
  }))

  const baseTotal = rows.reduce((sum, r) => sum + r.basePriceCents * r.quantity, 0)
  const memberTotal = rows.reduce(
    (sum, r) => sum + (r.memberPriceCents ?? r.basePriceCents) * r.quantity,
    0
  )
  const memberBenefit = baseTotal - memberTotal

  // ---- 多券叠加：按 priority 升序依次扣减，每步校验剩余应付金额是否满足下一张门槛 ----
  const sorted = [...list].sort(
    (a, b) => (a.priority ?? 0) - (b.priority ?? 0)
  )
  const usedCoupons = []
  const invalid = []
  let accumulatedDiscount = 0
  let anyApplied = false

  for (const c of sorted) {
    const remaining = baseTotal - accumulatedDiscount
    const evaluated = evaluateCoupon(c, rows, remaining)
    if (!evaluated.ok) {
      invalid.push({ id: c.id, name: c.name, reason: evaluated.reason })
      continue
    }
    accumulatedDiscount += evaluated.discountCents
    usedCoupons.push({ id: c.id, name: c.name, discountCents: evaluated.discountCents })
    anyApplied = true
  }

  // ---- 二选一自动取优（券方案 vs 会员方案） ----
  const couponScheme = baseTotal - accumulatedDiscount
  const memberScheme = memberTotal
  const couponApplied = anyApplied && couponScheme <= memberScheme

  const payCents = couponApplied
    ? Math.max(0, couponScheme)
    : Math.max(0, memberScheme)

  return {
    totalCents: baseTotal,
    memberTotalCents: memberTotal,
    memberBenefitCents: couponApplied ? 0 : Math.max(0, memberBenefit),
    couponDiscountCents: couponApplied ? accumulatedDiscount : 0,
    discountCents: Math.max(0, baseTotal - payCents),
    payCents,
    freightCents: Math.max(0, safeCents(freight.amountCents)),
    couponApplied,
    usedCoupons: couponApplied ? usedCoupons : [], // 取优未生效 → 视为未使用
    invalid,
  }
}

/**
 * 单券适用性判定 + 抵扣额计算（纯函数内部）。
 * 门槛只认适用商品（scope=ALL 全单；LIMITED 为 分类∪商品 并集）的原价小计；
 * 抵扣额额外受「当前剩余应付」（多券叠加时每步扣减后）上限约束。
 */
function evaluateCoupon(coupon, rows, remainingCents) {
  const id = coupon.id
  const name = coupon.name
  const scope = coupon.scope || 'ALL'
  const catIds = new Set((coupon.categoryIds || []).map(Number))
  const prodIds = new Set((coupon.productIds || []).map(Number))

  const subtotal = rows.reduce(
    (sum, r) =>
      sum +
      (scope === 'ALL' || catIds.has(r.categoryId) || prodIds.has(r.productId)
        ? r.basePriceCents * r.quantity
        : 0),
    0
  )

  if (subtotal <= 0) {
    return { ok: false, reason: '订单中没有该券适用的商品' }
  }
  const min = safeCents(coupon.minAmountCents)
  // 门槛校验：适用小计 ≥ min 且 当前剩余应付 ≥ min（多券叠加的「每步剩余校验」）
  const cap = Number.isFinite(remainingCents) ? Math.max(0, remainingCents) : subtotal
  const thresholdBase = Math.min(subtotal, cap)
  if (thresholdBase < min) {
    return {
      ok: false,
      reason: `还差 ¥${((min - thresholdBase) / 100).toFixed(2)} 可用`,
    }
  }
  const discountCents = Math.min(safeCents(coupon.discountAmountCents), subtotal, cap)
  if (discountCents <= 0) {
    return { ok: false, reason: '应付金额已为 0，券无法再抵扣' }
  }
  return { ok: true, discountCents }
}
