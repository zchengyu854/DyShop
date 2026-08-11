import { computed, reactive, readonly, watch } from 'vue'

/**
 * 规格/SKU 联动选择算法（纯派生，无副作用）。
 *
 * 用法：useSpecSelector(specsRef, skusRef)
 *   - specsRef/skusRef 为 ref/computed（商品详情的 specs/skus 数组）
 *   - 无规格商品：hasSpecs=false，其余派生值自动回退，面板可零分支复用
 *
 * 联动不变量：某规格值可选 ⟺ 存在「已选 ∪ 该值」约束下有库存的 SKU；
 * 缺货组合（匹配但库存 0）→ soldOut=true 置灰并标注。
 * 组合数 ≤ 1000 预计算；超阈值时可整体替换 optionStates 为懒计算（见 docs/ch04/spec-sku-selector.md D5）。
 */
export function useSpecSelector(specsRef, skusRef) {
  const selected = reactive({})

  const hasSpecs = computed(() => (specsRef.value?.length || 0) > 0)

  // 单值维度自动预选；商品切换（specs 引用变化）时重置并保留仍存在的已选项
  watch(
    specsRef,
    () => {
      const next = {}
      for (const spec of specsRef.value || []) {
        if (spec.values?.length === 1) next[spec.name] = spec.values[0]
      }
      for (const name of Object.keys(selected)) {
        const spec = (specsRef.value || []).find((s) => s.name === name)
        if (spec?.values?.includes(selected[name])) next[name] = selected[name]
      }
      for (const k of Object.keys(selected)) delete selected[k]
      Object.assign(selected, next)
    },
    { immediate: true },
  )

  function matches(sku, constraints) {
    if (!sku?.specs) return false
    return Object.entries(constraints).every(([name, value]) => sku.specs[name] === value)
  }

  /** 每个规格值的可选性/缺货状态：{ [dimName]: { [value]: { selectable, soldOut } } } */
  const optionStates = computed(() => {
    const states = {}
    for (const spec of specsRef.value || []) {
      states[spec.name] = {}
      for (const value of spec.values || []) {
        const constraint = { ...selected, [spec.name]: value }
        const matched = (skusRef.value || []).filter((sku) => matches(sku, constraint))
        states[spec.name][value] = {
          selectable: matched.length > 0 && matched.some((sku) => (sku.stock ?? 0) > 0),
          soldOut: matched.length > 0 && !matched.some((sku) => (sku.stock ?? 0) > 0),
        }
      }
    }
    return states
  })

  const dims = computed(() => (specsRef.value || []).map((s) => s.name))

  const missingDims = computed(() =>
    hasSpecs.value ? dims.value.filter((name) => !selected[name]) : [],
  )

  const selectedComplete = computed(() => missingDims.value.length === 0)

  const currentSku = computed(() => {
    if (!hasSpecs.value || !selectedComplete.value) return null
    return (
      (skusRef.value || []).find((sku) => matches(sku, selected)) || null
    )
  })

  /** SKU 售罄：组合存在但无库存（已选完时按钮禁用） */
  const skuSoldOut = computed(() => !!currentSku.value && (currentSku.value.stock ?? 0) <= 0)

  const priceRange = computed(() => {
    if (!hasSpecs.value) return null
    const prices = (skusRef.value || [])
      .map((s) => Number(s.price))
      .filter((p) => !Number.isNaN(p))
    if (!prices.length) return null
    return { min: Math.min(...prices), max: Math.max(...prices) }
  })

  const originalRange = computed(() => {
    if (!hasSpecs.value) return null
    const prices = (skusRef.value || [])
      .map((s) => Number(s.originalPrice))
      .filter((p) => !Number.isNaN(p) && p > 0)
    if (!prices.length) return null
    return { min: Math.min(...prices), max: Math.max(...prices) }
  })

  const totalStock = computed(() =>
    (skusRef.value || []).reduce((sum, s) => sum + (s.stock ?? 0), 0),
  )

  const currentPrice = computed(() => currentSku.value?.price ?? null)
  const currentOriginalPrice = computed(() => currentSku.value?.originalPrice ?? null)
  const currentStock = computed(() => currentSku.value?.stock ?? null)

  const skuImage = computed(() => currentSku.value?.image || null)

  /** 规格快照文本（按维度顺序），如「型号:MacBook Air, 颜色:深空灰」 */
  const specText = computed(() => {
    const sku = currentSku.value
    if (!sku) return ''
    return (specsRef.value || [])
      .map((spec) => `${spec.name}:${sku.specs?.[spec.name] ?? ''}`)
      .join(', ')
  })

  /** 选中规格值；置灰/缺货项忽略；单值维度不可取消 */
  function select(dim, value) {
    const state = optionStates.value[dim]?.[value]
    if (!state?.selectable) return
    const spec = (specsRef.value || []).find((s) => s.name === dim)
    if (selected[dim] === value) {
      if (spec?.values?.length !== 1) delete selected[dim]
      return
    }
    selected[dim] = value
  }

  return {
    // 状态（只读，变更走 select）
    selected: readonly(selected),
    hasSpecs,
    dims,
    missingDims,
    selectedComplete,
    optionStates,
    // 派生值
    currentSku,
    skuSoldOut,
    priceRange,
    originalRange,
    totalStock,
    currentPrice,
    currentOriginalPrice,
    currentStock,
    skuImage,
    specText,
    // 动作
    select,
  }
}
