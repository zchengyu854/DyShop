import { computed, readonly, ref, watch } from 'vue'
import { calculateOrderTotal, formatMoney, toCents } from '@/utils/price'

/**
 * usePriceCalculator —— 结算页价格计算 hook（选券即重算 + 后端权威对账）。
 *
 * 输入（接受 getter / ref / 普通值，create-adaptable-composable 规范）：
 *   lines        结算行来源：行含 { productId, price(原价单价), quantity }
 *   buildParams  构造 preview 请求参数（含 source/product/couponId…），随券变更重新求值
 *   fetchPreview 异步预览请求：(params) => Promise<OrderPreviewVO>
 *
 * 数据流：
 *   1) 选券 / 取消券 / 结算行变化 → 事件回调内【同步】本地估算（calculateOrderTotal 纯函数，
 *      <16ms、无 loading），金额 1 帧内更新；
 *   2) 同步发起后端 preview（无防抖、带竞态守卫，只采纳最后一次响应）作为权威校验；
 *   3) 后端结果到达后对账覆盖 —— 本地引擎与后端同一规则、同一输入，结果完全一致。
 *
 * 金额统一整数分（cents）运算；暴露 ¥X.XX 千分位格式化文本与最近 5 次计算快照（回溯排查）。
 */

const HISTORY_LIMIT = 5

export function usePriceCalculator(options = {}) {
  const linesGetter = normalize(options.lines, () => [])
  const buildParamsGetter = normalize(options.buildParams, () => ({}))
  const fetchPreviewGetter = normalize(
    options.fetchPreview,
    async () => ({})
  )

  // ---- 状态（内部可变，对外只读） ----
  const selectedCouponId = ref(null)
  const settled = ref(null) // 后端 preview 权威快照
  const loading = ref(false) // preview 请求进行中
  const previewError = ref('')
  const history = ref([]) // 最近 5 次计算快照（回溯排查）

  const totalCents = ref(0)
  const memberBenefitCents = ref(0)
  const couponDiscountCents = ref(0)
  const couponApplied = ref(false)
  const payCents = ref(0)
  const couponOptions = ref([])
  const freightCents = ref(0)

  // ---- 竞态守卫：只采纳最后一次 preview 响应 ----
  let previewSeq = 0

  // ---- 派生 ----
  const selectedCoupon = computed(
    () => couponOptions.value.find((o) => o.userCouponId === selectedCouponId.value) || null
  )
  const hasInitialData = computed(() => settled.value != null)

  // 商品清单展示行：优先后端会员价口径，未到达时用本地估算行
  const displayLines = computed(() => {
    if (settled.value?.lines?.length) return settled.value.lines
    return linesGetter()
  })

  const totalText = computed(() => formatMoney(totalCents.value))
  const memberBenefitText = computed(() => formatMoney(memberBenefitCents.value))
  const couponDiscountText = computed(() => formatMoney(couponDiscountCents.value))
  const payText = computed(() => formatMoney(payCents.value))

  // ---- 本地即时估算（同步、纯函数） ----
  function localCalc() {
    const src = linesGetter()
    if (!src.length) {
      applyEstimate(calculateOrderTotal([], null, { amountCents: 0 }))
      return
    }
    const settleLines = settled.value?.lines || []
    const items = src.map((l, i) => ({
      productId: l.productId,
      basePriceCents: toCents(l.price),
      // 会员单价：后端快照行优先（会员价口径）；未 settle 前按原价估算
      memberPriceCents: settleLines[i]?.price != null ? toCents(settleLines[i].price) : toCents(l.price),
      quantity: l.quantity,
    }))
    applyEstimate(calculateOrderTotal(items, asCouponInputFrom(selectedCouponId.value), { amountCents: 0 }))
  }

  function applyEstimate(r) {
    totalCents.value = r.totalCents
    memberBenefitCents.value = r.memberBenefitCents
    couponDiscountCents.value = r.couponDiscountCents
    couponApplied.value = r.couponApplied
    payCents.value = r.payCents
    freightCents.value = r.freightCents
    pushHistory('local')
  }

  // 选中券 → 本地引擎输入：以「后端已校验」的预计抵扣额为准（范围/门槛已消化），保证与后端一致
  function asCouponInputFrom(ucId) {
    if (ucId == null) return null
    const opt = couponOptions.value.find((o) => o.userCouponId === ucId)
    if (!opt || !opt.applicable) return null // 后端判定不可用 → 不参与（走会员/无券方案）
    return {
      id: opt.userCouponId,
      type: 'REDUCE',
      minAmountCents: toCents(opt.minAmount),
      discountAmountCents: toCents(opt.discount ?? opt.discountAmount),
      scope: 'ALL', // 后端已将范围/门槛消化进 discount
      categoryIds: [],
      productIds: [],
    }
  }

  // ---- 后端权威对账 ----
  async function reconcile() {
    if (!linesGetter().length) return
    const seq = ++previewSeq
    loading.value = true
    previewError.value = ''
    try {
      const params = { ...(buildParamsGetter() || {}) }
      // 券 ID 由 hook 按当前选中态注入（外部 buildParams 无需感知）
      if (selectedCouponId.value == null) delete params.couponId
      else params.couponId = selectedCouponId.value
      const data = await fetchPreviewGetter(params)
      if (seq !== previewSeq) return
      settled.value = data
      couponOptions.value = data.couponOptions || []
      // 后端权威快照直接落位展示值（同一规则、同一输入 ⇒ 本地估算与其一致；此处不二次推导，杜绝偏差）
      totalCents.value = toCents(data.totalAmount)
      memberBenefitCents.value = toCents(data.memberBenefit)
      couponDiscountCents.value = toCents(data.couponDiscount)
      couponApplied.value = !!data.couponApplied
      payCents.value = toCents(data.payAmount)
      pushHistory('server')
    } catch (e) {
      if (seq !== previewSeq) return
      // 预览失败不阻塞：保留上次结果（本地估算仍即时可用，history 可回溯）
      previewError.value = e?.message || '价格校验暂不可用'
    } finally {
      if (seq === previewSeq) loading.value = false
    }
  }

  function pushHistory(source) {
    const snap = {
      ts: Date.now(),
      source,
      couponId: selectedCouponId.value,
      totalCents: totalCents.value,
      memberBenefitCents: memberBenefitCents.value,
      couponDiscountCents: couponDiscountCents.value,
      payCents: payCents.value,
      couponApplied: couponApplied.value,
    }
    history.value = [...history.value.slice(-(HISTORY_LIMIT - 1)), snap]
  }

  // ---- 对外动作（事件回调内同步重算 + 异步对账） ----
  function selectCoupon(ucId) {
    selectedCouponId.value = selectedCouponId.value === ucId ? null : ucId
    localCalc() // 同步：1 帧内更新金额
    reconcile() // 异步：后端校验
  }

  function clearCoupon() {
    selectedCouponId.value = null
    localCalc()
    reconcile()
  }

  function recalculate() {
    localCalc()
    reconcile()
  }

  // ---- 响应式依赖：结算行变化 → 自动重算（选券走 selectCoupon 事件回调，避免双触发） ----
  watch(
    () => linesGetter(),
    () => {
      localCalc()
      reconcile()
    },
    { immediate: true }
  )

  return {
    // 动作
    selectCoupon,
    clearCoupon,
    recalculate,
    // 状态（只读引用，避免外部意外变更）
    selectedCouponId: readonly(selectedCouponId),
    displayLines: readonly(displayLines),
    couponOptions: readonly(couponOptions),
    selectedCoupon,
    // 金额（整数分）
    totalCents: readonly(totalCents),
    memberBenefitCents: readonly(memberBenefitCents),
    couponDiscountCents: readonly(couponDiscountCents),
    payCents: readonly(payCents),
    freightCents: readonly(freightCents),
    couponApplied: readonly(couponApplied),
    // 格式化文本
    totalText,
    memberBenefitText,
    couponDiscountText,
    payText,
    // 加载/错误/历史
    loading: readonly(loading),
    hasInitialData,
    previewError: readonly(previewError),
    history: readonly(history),
  }
}

/** 输入归一化：函数直接作为 getter，ref/普通值包一层（create-adaptable-composable 规范） */
function normalize(input, fallback) {
  if (typeof input === 'function') return input
  if (input && typeof input.value !== 'undefined') return () => input.value
  return fallback
}
