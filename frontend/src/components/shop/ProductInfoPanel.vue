<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addFavorite, fetchFavoriteStatus, fetchMemberOverview, removeFavorite } from '@/api/user'
import SpecSelector from '@/components/shop/SpecSelector.vue'
import { useSpecSelector } from '@/composables/useSpecSelector'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/toast'

const props = defineProps({
  product: { type: Object, required: true },
})
const emit = defineEmits(['sku-image'])

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

const favorited = ref(false)
const favoriteBusy = ref(false)
const cartBusy = ref(false)

const isLogin = userStore.isLogin

// ---- 规格/SKU 联动（无规格商品自动回退：hasSpecs=false，价格/库存走商品级）----
const specs = computed(() => props.product.specs || [])
const skus = computed(() => props.product.skus || [])
const {
  hasSpecs,
  selected,
  selectedComplete,
  missingDims,
  optionStates,
  skuSoldOut,
  priceRange,
  originalRange,
  totalStock,
  currentPrice,
  currentOriginalPrice,
  currentStock,
  skuImage,
  specText,
  select,
} = useSpecSelector(specs, skus)

// SKU 主图联动：有值覆盖大图，取消/切换规格自动重置（ProductDetail 转发给 Gallery）
watch(skuImage, (img) => emit('sku-image', img))

const displayPrice = computed(() => {
  if (!hasSpecs.value) return { price: props.product.price, original: props.product.originalPrice }
  if (currentPrice.value != null) {
    return { price: currentPrice.value, original: currentOriginalPrice.value }
  }
  // 未选完：价格区间（min=max 显示单值）
  if (!priceRange.value) return { price: props.product.price, original: props.product.originalPrice }
  return {
    price:
      priceRange.value.min === priceRange.value.max
        ? priceRange.value.min
        : `${priceRange.value.min} - ${priceRange.value.max}`,
    original: originalRange.value
      ? originalRange.value.min === originalRange.value.max
        ? originalRange.value.min
        : `${originalRange.value.min} - ${originalRange.value.max}`
      : null,
  }
})

const displayStock = computed(() => {
  if (!hasSpecs.value) return props.product.stock
  if (currentStock.value != null) return currentStock.value
  return totalStock.value
})

// ---- 会员价格展示（ch09）：登录后按当前等级展示会员成交价/折扣 ----
// 规则与后端 resolvePrice 一致：有 vip_price 用专享价，否则价格 × 折扣率；普通等级无优惠。
const memberLevel = ref(null) // { name, discountRate }

// 当前展示单位的原始金额（数字），未选完 SKU/区间时为空
const displayNumber = computed(() => {
  const d = displayPrice.value
  const n = Number(d.price)
  return Number.isFinite(n) ? n : null
})

const memberRate = computed(() => {
  const lv = memberLevel.value
  return lv && Number(lv.discountRate) < 1 ? Number(lv.discountRate) : null
})

// 会员成交价：仅会员（有折扣率）展示；无规格商品优先专享价，否则价格×折扣率
const memberPrice = computed(() => {
  const rate = memberRate.value
  const base = displayNumber.value
  if (rate == null || base == null) return null
  if (!hasSpecs.value && props.product.vipPrice) return Number(props.product.vipPrice)
  return Number((base * rate).toFixed(2))
})

const memberRateText = computed(() => {
  const rate = memberRate.value
  if (rate == null) return ''
  return `${(rate * 10).toFixed(1).replace(/\.0$/, '')} 折`
})

onMounted(async () => {
  if (!userStore.isLogin) return
  try {
    const ov = await fetchMemberOverview()
    // ov：{ level: { name, discountRate }, ... }，仅取等级给价格展示
    memberLevel.value = ov?.level || null
  } catch {
    // 会员信息不可用时静默降级为不显示
  }
})

const specHint = computed(() => {
  if (!hasSpecs.value) return ''
  if (!selectedComplete.value) return `请选择：${missingDims.value.join('、')}`
  if (skuSoldOut.value) return '该规格已售罄，请选择其他规格'
  return ''
})

// 商品整体售罄：无规格看 product.stock，有规格看 SKU 合计库存
const productSoldOut = computed(() => {
  if (!hasSpecs.value) return (props.product.stock ?? 0) <= 0
  return totalStock.value <= 0
})

const canCommit = computed(
  () =>
    !productSoldOut.value &&
    (!hasSpecs.value || (selectedComplete.value && !skuSoldOut.value)),
)

function requireSpec(fn) {
  if (productSoldOut.value) {
    toast.info('该商品已售罄')
    return
  }
  if (!hasSpecs.value) return fn()
  if (!selectedComplete.value) {
    toast.info(`请先选择：${missingDims.value.join('、')}`)
    return
  }
  if (skuSoldOut.value) {
    toast.info('该规格已售罄，请选择其他规格')
    return
  }
  fn()
}

onMounted(() => {
  if (isLogin) {
    fetchFavoriteStatus(props.product.id)
      .then((data) => {
        favorited.value = data.favorited
      })
      .catch(() => {
        // 状态查询失败保持默认未收藏，不阻塞页面
      })
  }
})

function showNotice(text) {
  toast.info(text)
}

async function handleFavorite() {
  if (favoriteBusy.value) return
  if (!isLogin) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  favoriteBusy.value = true
  const next = !favorited.value
  const prev = favorited.value
  favorited.value = next
  try {
    if (next) {
      await addFavorite(props.product.id)
    } else {
      await removeFavorite(props.product.id)
    }
  } catch (e) {
    favorited.value = prev
    toast.error(e.message || '操作失败，请重试')
  } finally {
    favoriteBusy.value = false
  }
}

async function handleAddToCart() {
  if (cartBusy.value) return
  if (!isLogin) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  requireSpec(async () => {
    cartBusy.value = true
    try {
      await cartStore.addToCart(props.product.id, 1, selectedSkuId(), specText.value)
      toast.success('已加入购物车')
    } catch (e) {
      toast.error(e.message || '加入失败，请重试')
    } finally {
      cartBusy.value = false
    }
  })
}

function selectedSkuId() {
  return selectedComplete.value && !skuSoldOut.value
    ? currentSkuId()
    : 0
}

function currentSkuId() {
  // 从 product.skus 反查当前选中 SKU（避免把 composable 内部对象暴露给接口）
  const selectedMap = selected
  return props.product.skus?.find(
    (sku) =>
      Object.entries(selectedMap).every(([k, v]) => sku.specs?.[k] === v) &&
      Object.keys(sku.specs || {}).length === Object.keys(selectedMap).length,
  )?.id || 0
}

function handleBuy() {
  if (!isLogin) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  requireSpec(() => {
    router.push({
      path: '/checkout',
      query: {
        buyNow: 1,
        productId: props.product.id,
        quantity: 1,
        skuId: selectedSkuId(),
        specText: specText.value,
      },
    })
  })
}
</script>

<template>
  <div class="info">
    <h1 class="name">{{ product.name }}</h1>
    <p v-if="product.subtitle" class="subtitle">{{ product.subtitle }}</p>

    <div class="price-row" aria-live="polite">
      <span class="price">¥{{ displayPrice.price }}</span>
      <span v-if="displayPrice.original" class="original">¥{{ displayPrice.original }}</span>
      <span v-if="memberPrice != null && memberPrice < displayNumber" class="vip-price">
        会员价 ¥{{ memberPrice }}<template v-if="memberRateText"> · {{ memberRateText }}</template>
      </span>
      <span v-else-if="memberRateText && displayNumber == null" class="vip-price">
        会员 {{ memberRateText }}
      </span>
    </div>

    <p class="meta" aria-live="polite">
      <span>销量 {{ product.sales }}</span>
      <span v-if="productSoldOut" class="out-of-stock">已售罄</span>
      <span v-else-if="hasSpecs && !selectedComplete">库存合计 {{ displayStock }}</span>
      <span v-else-if="displayStock > 0">库存 {{ displayStock }}</span>
      <span v-else class="out-of-stock">已售罄</span>
    </p>

    <SpecSelector
      v-if="hasSpecs"
      :specs="specs"
      :states="optionStates"
      :selected="selected"
      @select="({ dim, value }) => select(dim, value)"
    />

    <p v-if="specHint" class="spec-hint" role="status">{{ specHint }}</p>

    <button
      class="fav-btn"
      :class="{ favorited }"
      :disabled="favoriteBusy"
      @click="handleFavorite"
    >
      <svg v-if="favorited" class="heart" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
      </svg>
      <svg v-else class="heart" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
      </svg>
      {{ favorited ? '已收藏' : '收藏' }}
    </button>

    <div class="actions">
      <button class="btn primary" :disabled="cartBusy || !canCommit" @click="handleAddToCart">加入购物车</button>
      <button class="btn ghost" :disabled="!canCommit" @click="handleBuy">立即购买</button>
    </div>
  </div>
</template>

<style scoped>
.info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.name {
  margin: 0;
  font-size: 32px;
  font-weight: 600;
  letter-spacing: -0.015em;
  line-height: 1.2;
}
.subtitle {
  margin: 8px 0 0;
  font-size: 15px;
  color: var(--ink-secondary);
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-top: 20px;
}
.price {
  font-size: 30px;
  font-weight: 700;
  color: var(--ink);
}
.original {
  font-size: 16px;
  color: var(--ink-faint);
  text-decoration: line-through;
}
.vip {
  font-size: 13px;
  color: var(--blue);
  background: rgba(0, 113, 227, 0.08);
  border-radius: var(--radius-full);
  padding: 2px 10px;
}
.meta {
  display: flex;
  gap: 16px;
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--ink-secondary);
}
.out-of-stock {
  color: #ff3b30;
}
.spec-hint {
  margin: 12px 0 0;
  font-size: 13px;
  color: #ff9500;
}
.fav-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 18px;
  padding: 7px 18px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--bg);
  color: var(--ink);
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}
.fav-btn:hover {
  border-color: var(--ink-secondary);
}
.fav-btn.favorited {
  color: #ff2d55;
  border-color: #ff2d55;
  background: #fff5f7;
}
.fav-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.heart {
  width: 15px;
  height: 15px;
}
.actions {
  display: flex;
  gap: 12px;
  margin-top: 22px;
}
.btn {
  padding: 12px 30px;
  border-radius: var(--radius-full);
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.btn.primary {
  border: none;
  background: var(--blue);
  color: #fff;
}
.btn.primary:hover {
  background: var(--blue-hover);
}
.btn.ghost {
  border: 1px solid var(--blue);
  background: var(--bg);
  color: var(--blue);
}
.btn.ghost:hover {
  background: var(--bg-gray);
}
.btn:disabled {
  cursor: not-allowed;
}
.btn.primary:disabled {
  background: var(--bg-gray);
  color: var(--ink-faint);
}
.btn.ghost:disabled {
  color: var(--ink-faint);
  border-color: var(--border);
  background: var(--bg-gray);
}
</style>