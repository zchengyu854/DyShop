<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import HomeFooter from '@/components/home/HomeFooter.vue'
import HomeHeader from '@/components/home/HomeHeader.vue'
import CouponSelector from '@/components/shop/CouponSelector.vue'
import PriceDisplay from '@/components/shop/PriceDisplay.vue'
import { usePriceCalculator } from '@/composables/usePriceCalculator'
import { fetchAddresses } from '@/api/address'
import { createOrder, payOrder, previewOrder } from '@/api/order'
import { fetchProductDetail } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { notifyDataChanged, ORDER_NS, PRODUCT_NS } from '@/utils/dataSync'
import { toast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

// 立即购买模式：?buyNow=1&productId=X&quantity=N
const buyNow = computed(() => route.query.buyNow === '1' || !!route.query.productId)
const buyQty = computed(() => {
  const q = Number(route.query.quantity)
  return q >= 1 ? q : 1
})

const buyProduct = ref(null)
const addresses = ref([])
const selectedAddressId = ref(null)
const remark = ref('')
const submitting = ref(false)
const paying = ref(false)
const pendingOrder = ref(null)
const addressLoaded = ref(false)
const pickerOpen = ref(false) // 地址 slide-over 展开状态

const selectedAddress = computed(() =>
  addresses.value.find((a) => a.id === selectedAddressId.value)
)

// 立即购买规格：按 skuId 反查 SKU（价格以 SKU 为准），回退前端下发的 specText（展示用）
const buySku = computed(() => {
  const p = buyProduct.value
  const skuId = Number(route.query.skuId || 0)
  if (!p?.skus?.length || !skuId) return null
  return p.skus.find((s) => s.id === skuId) || null
})
const buySpecText = computed(() => {
  if (buySku.value) {
    return (buyProduct.value.specs || [])
      .map((s) => `${s.name}:${buySku.value.specs?.[s.name] ?? ''}`)
      .join(', ')
  }
  return String(route.query.specText || '')
})

// 结算行：购物车勾选项 / 立即购买单品
const lines = computed(() => {
  if (buyNow.value) {
    const p = buyProduct.value
    if (!p) return []
    const sku = buySku.value
    return [
      {
        productId: p.id,
        name: p.name,
        mainImage: p.mainImage,
        price: sku?.price ?? p.price,
        quantity: buyQty.value,
        skuId: sku?.id ?? 0,
        specText: buySpecText.value,
      },
    ]
  }
  return cartStore.checkedItems
})

const totalQuantity = computed(() => lines.value.reduce((sum, l) => sum + l.quantity, 0))

// ---- 价格计算（usePriceCalculator）：选券/取消券 → 事件回调内同步本地重算（纯函数 <16ms），
//      随后后端 preview 权威对账（竞态守卫只采纳最后一次响应）。金额统一整数分运算。 ----
// 解构为顶层绑定：Vue 模板对顶层 ref/computed 自动解包（嵌套在普通对象里不会解包）
const {
  selectCoupon,
  clearCoupon,
  selectedCouponId,
  displayLines,
  couponOptions,
  selectedCoupon,
  totalText,
  memberBenefitText,
  couponDiscountText,
  payText,
  memberBenefitCents,
  couponApplied,
  loading,
  hasInitialData,
  previewError,
} = usePriceCalculator({
  // 结算行（原价口径）：购物车勾选项 / 立即购买单品
  lines: () => lines.value,
  // preview 请求参数（couponId 由 hook 按选中券注入）
  buildParams: () => {
    const params = { source: buyNow.value ? 'buyNow' : 'cart' }
    if (buyNow.value) {
      params.productId = Number(route.query.productId)
      params.quantity = buyQty.value
      params.skuId = buySku.value?.id ?? 0
    }
    return params
  },
  fetchPreview: previewOrder,
})

function format(amount) {
  return Number(amount).toFixed(2)
}

function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone || ''
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

async function load() {
  if (buyNow.value) {
    try {
      const p = await fetchProductDetail(route.query.productId)
      buyProduct.value = p
    } catch (e) {
      toast.error(e.message || '商品不存在')
      router.replace('/')
      return
    }
  } else {
    await cartStore.fetchCart().catch(() => {})
  }
  // 预览请求由 usePriceCalculator 内部的 lines watcher 触发（immediate：true），无需手动调用
  try {
    addresses.value = await fetchAddresses()
    addressLoaded.value = true
    if (addresses.value.length) {
      selectedAddressId.value = addresses.value[0].id
    }
  } catch (e) {
    // 地址加载失败不阻塞页面
  }
}

// 地址选择：单选关闭 slide-over
function selectAddress(addr) {
  selectedAddressId.value = addr.id
  pickerOpen.value = false
}

async function handleSubmit() {
  if (submitting.value || !selectedAddress.value || !lines.value.length) return
  submitting.value = true
  try {
    const payload = {
      source: buyNow.value ? 'buyNow' : 'cart',
      addressId: selectedAddressId.value,
      remark: remark.value,
      couponId: selectedCouponId.value ?? undefined,
    }
    if (buyNow.value) {
      payload.productId = Number(route.query.productId)
      payload.quantity = buyQty.value
      payload.skuId = buySku.value?.id ?? 0
    }
    // 后端返回完整 OrderVO（含 items/payDeadline，无需二次查询即可渲染支付弹层）。
    // 数据同步：下单成功 = 订单领域数据变更 —— 广播失效事件，让已挂载的
    // 订单列表/详情视图静默刷新（配合 GET /api/orders 的 no-store 头）；
    // 同时广播商品领域 —— 下单占用库存，已挂载的商品详情页需同步最新库存/价格。
    const order = await createOrder(payload)
    pendingOrder.value = order
    // 下单成功：清空选券缓存，防止重复下单再带券（spec §7.2）
    clearCoupon()
    notifyDataChanged(ORDER_NS)
    notifyDataChanged(PRODUCT_NS)
    if (!buyNow.value) {
      await cartStore.fetchCart().catch(() => {})
    }
  } catch (e) {
    toast.error(e.message || '下单失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function confirmPay() {
  if (paying.value) return
  paying.value = true
  try {
    await payOrder(pendingOrder.value.id)
    toast.success('支付成功')
    // 支付成功同样是订单状态变更：广播失效，列表/详情/后台订单页即时同步
    notifyDataChanged(ORDER_NS)
    router.replace(`/orders/${pendingOrder.value.id}`)
  } catch (e) {
    toast.error(e.message || '支付失败，请重试')
    pendingOrder.value = null
  } finally {
    paying.value = false
  }
}

function closePay() {
  router.replace(`/orders/${pendingOrder.value.id}`)
}

onMounted(load)
</script>

<template>
  <div class="page-shell">
    <HomeHeader />
    <main class="checkout-page">
      <h1 class="title">确认订单</h1>
      <p v-if="!lines.length && !buyNow" class="hint">
        没有勾选要结算的商品，<router-link to="/cart" class="link">回购物车选择 ›</router-link>
      </p>

      <template v-else>
        <div class="layout">
          <!-- 左栏：主内容 60% -->
          <div class="main-col">
            <!-- 收货地址 -->
            <section class="card" aria-labelledby="addr-title">
              <div class="card-head">
                <h2 class="card-title" id="addr-title">收货地址</h2>
              </div>

              <!-- 空态兜底：无地址 -->
              <div v-if="addressLoaded && !addresses.length" class="addr-emptycard">
                <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                  <path d="M12 21c-4.5-3.6-7-6.6-7-9.5A7 7 0 0 1 12 4.5 7 7 0 0 1 19 11.5c0 2.9-2.5 5.9-7 9.5Z" />
                  <circle cx="12" cy="11.5" r="2.4" />
                </svg>
                <p class="empty-text">还没有收货地址</p>
                <router-link to="/user/addresses" class="empty-add">＋ 添加收货地址</router-link>
              </div>

              <!-- 默认状态：高亮当前选中地址 + 更改 -->
              <div v-else-if="selectedAddress" class="addr-summary" role="group" aria-label="当前收货地址">
                <div class="addr-info">
                  <div class="addr-top">
                    <span class="addr-name">{{ selectedAddress.receiverName }}</span>
                    <span class="addr-phone">{{ maskPhone(selectedAddress.receiverPhone) }}</span>
                    <span v-if="selectedAddress.isDefault === 1" class="addr-badge">默认</span>
                  </div>
                  <p class="addr-full">{{ selectedAddress.fullAddress }}</p>
                </div>
                <button
                  class="change-btn"
                  type="button"
                  aria-haspopup="dialog"
                  aria-controls="addr-picker"
                  :aria-expanded="pickerOpen ? 'true' : 'false'"
                  @click="pickerOpen = true"
                >
                  更改
                </button>
              </div>
            </section>

<!-- 商品清单 -->
              <section class="card" aria-labelledby="line-title">
                <div class="card-head">
                  <h2 class="card-title" id="line-title">商品清单</h2>
                  <span v-if="buyNow" class="buy-badge">立即购买</span>
                  <span v-else class="qty-hint">共 {{ totalQuantity }} 件</span>
                </div>
<div
                  v-for="line in displayLines"
                :key="`${line.productId}-${line.specText || 'none'}`"
                class="line"
              >
                <router-link :to="`/products/${line.productId}`" class="line-thumb" :aria-label="`查看 ${line.productName}`">
                  <img :src="line.productImage" :alt="line.productName" loading="lazy" />
                </router-link>
                <div class="line-info">
                  <router-link :to="`/products/${line.productId}`" class="line-name">
                    {{ line.productName }}
                  </router-link>
                  <p v-if="line.specText" class="line-spec">{{ line.specText }}</p>
                  <p class="line-meta">¥{{ format(line.price) }} × {{ line.quantity }}</p>
                </div>
                <span class="line-total">¥{{ format(line.subtotal) }}</span>
              </div>
            </section>

            <!-- 订单备注 -->
            <section class="card" aria-labelledby="remark-title">
              <div class="card-head">
                <h2 class="card-title" id="remark-title">订单备注（选填）</h2>
              </div>
              <textarea
                v-model="remark"
                class="remark"
                rows="3"
                maxlength="200"
                aria-label="订单备注，最多 200 字"
                placeholder="给商家留言，最多 200 字"
              ></textarea>
              <div class="remark-count">{{ remark.length }}/200</div>
            </section>
          </div>

          <!-- 右栏：订单摘要 40% -->
          <aside class="summary" aria-label="订单摘要">
            <div class="summary-card">
              <div class="summary-head">
                <h3 class="summary-title">订单摘要</h3>
                <span v-if="lines.length" class="summary-count">共 {{ totalQuantity }} 件</span>
              </div>
              <!-- 金额区（PriceDisplay）：整数分口径 + ¥X.XX 千分位；首次加载骨架屏 -->
              <PriceDisplay
                :total-text="totalText"
                :member-benefit-text="memberBenefitText"
                :coupon-discount-text="couponDiscountText"
                :pay-text="payText"
                :show-member-benefit="!couponApplied && memberBenefitCents > 0"
                :show-coupon="couponApplied"
                :coupon-name="selectedCoupon?.name"
                :loading="loading"
                :initial="!hasInitialData"
              />
              <!-- 优惠券（CouponSelector）：一单一券单选；onChange 上抛 → usePriceCalculator 即时重算 -->
              <CouponSelector
                :options="couponOptions"
                :selected-id="selectedCouponId"
                :applied="couponApplied"
                :loading="loading"
                @change="selectCoupon"
              />
              <button
                class="submit-btn"
                type="submit"
                :disabled="submitting || !selectedAddress || !lines.length"
                @click="handleSubmit"
              >
                {{ submitting ? '提交中…' : '提交订单' }}
              </button>
              <p class="mock-hint">演示项目 · 模拟支付，无需真实付款</p>
            </div>
          </aside>
        </div>
      </template>
    </main>
    <HomeFooter />

    <!-- 地址选择 Slide-over 抽屉 -->
    <Transition name="picker-fade">
      <div v-if="pickerOpen" class="picker" role="dialog" aria-modal="true" aria-label="选择收货地址" id="addr-picker">
        <div class="picker-mask" @click="pickerOpen = false" aria-hidden="true"></div>
        <aside class="picker-panel">
          <div class="picker-head">
            <h3 class="picker-title">选择收货地址</h3>
            <button class="picker-close" type="button" aria-label="关闭" @click="pickerOpen = false">×</button>
          </div>
          <div class="picker-list" role="radiogroup" aria-label="已保存地址列表">
            <button
              v-for="a in addresses"
              :key="a.id"
              type="button"
              class="picker-item"
              :class="{ active: a.id === selectedAddressId }"
              role="radio"
              :aria-checked="a.id === selectedAddressId ? 'true' : 'false'"
              @click="selectAddress(a)"
            >
              <span class="radio" :class="{ checked: a.id === selectedAddressId }" aria-hidden="true"></span>
              <span class="picker-item-main">
                <span class="picker-item-top">
                  <span class="addr-name">{{ a.receiverName }}</span>
                  <span class="addr-phone">{{ maskPhone(a.receiverPhone) }}</span>
                  <span v-if="a.isDefault === 1" class="addr-badge">默认</span>
                </span>
                <span class="addr-full">{{ a.fullAddress }}</span>
              </span>
              <svg v-if="a.id === selectedAddressId" class="pick-check" viewBox="0 0 12 10" aria-hidden="true">
                <path d="M1 5.5 4.2 8.5 11 1.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>
          </div>
          <router-link to="/user/addresses" class="picker-add">＋ 新增收货地址</router-link>
        </aside>
      </div>
    </Transition>

    <!-- 模拟支付弹层 -->
    <Transition name="picker-fade">
      <div v-if="pendingOrder" class="pay-mask" role="dialog" aria-modal="true" aria-label="模拟支付">
        <div class="pay-modal">
          <h3 class="pay-title">模拟支付</h3>
          <p class="pay-label">应付金额</p>
          <p class="pay-amount">¥{{ format(pendingOrder.payAmount) }}</p>
          <p class="pay-hint">演示项目 · 点击「确认支付」即视为支付成功</p>
          <button class="pay-btn" :disabled="paying" @click="confirmPay">
            {{ paying ? '支付中…' : '确认支付' }}
          </button>
          <button class="pay-later" @click="closePay">稍后支付</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.page-shell {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}
.checkout-page {
  flex: 1;
  width: min(1140px, 100%);
  margin: 0 auto;
  padding: 3rem 2rem 5rem;
}
.title {
  margin: 0 0 2.5rem;
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.hint {
  padding: 4rem 0;
  text-align: center;
  font-size: 0.95rem;
  color: var(--ink-faint);
}
.link {
  color: var(--link);
}

/* 双栏布局：左主内容约 62%，右摘要约 38% */
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1.62fr) minmax(300px, 1fr);
  gap: 2.5rem;
  align-items: start;
}
.main-col {
  display: flex;
  flex-direction: column;
  gap: 2.25rem; /* 卡片垂直间距 36px */
  min-width: 0;
}

/* 卡片：去阴影，淡背景/细边框 + 圆角 16px */
.card {
  background: var(--bg);
  border: 1px solid var(--border-line);
  border-radius: 1rem;
  padding: 2rem 2rem; /* 内部 32px */
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.125rem; /* 标题与内容间距 ≥16px */
}
.card-title {
  margin: 0;
  font-size: 1.0625rem;
  font-weight: 600;
  letter-spacing: -0.01em;
}

/* --- 收货地址：默认展示 + 更改 --- */
.addr-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
}
.addr-info {
  min-width: 0;
}
.addr-top {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  flex-wrap: wrap;
}
.addr-name {
  font-size: 1.0625rem;
  font-weight: 600;
}
.addr-phone {
  font-size: 0.875rem;
  color: var(--ink-secondary);
  font-variant-numeric: tabular-nums;
}
.addr-badge {
  padding: 0.125rem 0.5rem;
  border-radius: var(--radius-full);
  background: rgba(0, 113, 227, 0.12);
  color: #0063c1;
  font-size: 0.75rem;
  font-weight: 600;
}
.addr-full {
  margin: 0.375rem 0 0;
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--ink-secondary);
}
.change-btn {
  flex: none;
  padding: 0.25rem 0;
  border: none;
  background: transparent;
  color: var(--link);
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.15s;
}
.change-btn:hover {
  color: var(--blue);
  text-decoration: underline;
}

/* 空态：无地址引导卡 */
.addr-emptycard {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  border: 1px dashed var(--border);
  border-radius: 0.875rem;
  background: var(--bg-gray);
  text-align: center;
}
.empty-icon {
  width: 2rem;
  height: 2rem;
  color: var(--ink-faint);
}
.empty-text {
  margin: 0;
  font-size: 0.9375rem;
  color: var(--ink-secondary);
}
.empty-add {
  margin-top: 0.5rem;
  color: var(--link);
  font-size: 0.9375rem;
  font-weight: 500;
}
.empty-add:hover {
  text-decoration: underline;
}

/* --- 商品清单 --- */
.qty-hint {
  font-size: 0.8125rem;
  color: var(--ink-faint);
}
.buy-badge {
  padding: 0.125rem 0.625rem;
  border-radius: var(--radius-full);
  background: rgba(0, 113, 227, 0.1);
  color: var(--blue);
  font-size: 0.75rem;
  font-weight: 600;
}
.line {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.875rem 0;
  border-bottom: 1px solid var(--border-line);
}
.line:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.line-thumb {
  flex-shrink: 0;
  width: 4.5rem;
  height: 4.5rem;
  border-radius: 0.75rem;
  overflow: hidden;
  background: var(--bg-gray);
}
.line-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.line-info {
  flex: 1;
  min-width: 0;
}
.line-name {
  display: inline-block;
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--ink);
}
.line-name:hover {
  color: var(--link);
}
.line-meta {
  margin: 0.25rem 0 0;
  font-size: 0.8125rem;
  color: var(--ink-secondary);
}
.line-price {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0.25rem 0 0;
  font-size: 0.8125rem;
}
.member-tag {
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: rgba(0, 113, 227, 0.1);
  color: var(--blue);
  font-size: 0.6875rem;
  font-weight: 600;
}
.price-now {
  font-weight: 600;
  color: var(--blue);
  font-variant-numeric: tabular-nums;
}
.price-original {
  color: var(--ink-faint);
  text-decoration: line-through;
}
.price-qty {
  color: var(--ink-secondary);
}
.line-spec {
  margin: 0.25rem 0 0;
  font-size: 0.8125rem;
  color: var(--ink-secondary);
}
.line-spec::before {
  content: '规格：';
  color: var(--ink-faint);
}
.line-total {
  font-size: 0.9375rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* --- 备注 --- */
.remark {
  width: 100%;
  box-sizing: border-box;
  padding: 0.75rem 0.875rem;
  border: 1px solid var(--border);
  border-radius: 0.625rem;
  background: var(--bg);
  font-family: inherit;
  font-size: 0.875rem;
  line-height: 1.6;
  resize: vertical;
  color: var(--ink);
}
.remark:focus {
  outline: none;
  border-color: var(--blue);
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.15);
}
.remark-count {
  margin-top: 0.375rem;
  text-align: right;
  font-size: 0.75rem;
  color: var(--ink-faint);
  font-variant-numeric: tabular-nums;
}

/* --- 右栏摘要：sticky 固定 + 卡片化 --- */
.summary {
  position: sticky;
  top: 1.5rem;
  min-width: 0;
}
/* 摘要卡片设计令牌：随 .summary-card 下发，PriceDisplay / CouponSelector 通过继承读取 */
.summary-card {
  --sum-bg: #ffffff;
  --sum-border: #f0f0f0;
  --sum-divider: #ececec;
  --sum-band: rgba(255, 77, 79, 0.04);
  --sum-title: #8c8c8c;
  --sum-label: #6e6e73;
  --sum-value: #1d1d1f;
  --sum-save: #ff5000;
  --sum-free: #34c759;
  --sum-pay: #ff4d4f;

  padding: 1.5rem;
  background: var(--sum-bg);
  border: 1px solid var(--sum-border);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}
.summary-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 0.25rem;
}
.summary-title {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--sum-title);
  letter-spacing: 0.02em;
}
.summary-count {
  font-size: 0.8125rem;
  color: var(--sum-title);
  font-variant-numeric: tabular-nums;
}
/* 优惠券选券（ch11）已抽离至 components/shop/CouponSelector.vue */
.submit-btn {
  width: 100%;
  height: 3rem;
  margin-top: 1.5rem;
  border: none;
  border-radius: 8px;
  background: linear-gradient(180deg, #3a8bff 0%, var(--blue) 100%);
  color: #fff;
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(0, 113, 227, 0.28);
  transition: background 0.2s, box-shadow 0.2s, transform 0.08s;
}
.submit-btn:hover:not(:disabled) {
  background: linear-gradient(180deg, #2278e6 0%, #005fd3 100%);
  box-shadow: 0 8px 22px rgba(0, 113, 227, 0.34);
}
.submit-btn:active:not(:disabled) {
  transform: translateY(1px);
}
.submit-btn:focus-visible {
  outline: 2px solid var(--blue-hover);
  outline-offset: 2px;
}
.submit-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.mock-hint {
  margin: 1rem 0 0;
  text-align: center;
  font-size: 0.75rem;
  color: #bfbfbf;
}

/* 深色模式：卡片令牌整体翻转，避免硬编码浅色造成割裂 */
@media (prefers-color-scheme: dark) {
  .summary-card {
    --sum-bg: #1c1c1f;
    --sum-border: #2c2c2f;
    --sum-divider: #3a3a3f;
    --sum-band: rgba(255, 77, 79, 0.14);
    --sum-label: #a0a0a5;
    --sum-value: #f5f5f7;
    --sum-pay: #ff6b6e;
  }
  .submit-btn {
    box-shadow: 0 6px 16px rgba(0, 113, 227, 0.35);
  }
}

/* --- 地址选择 Slide-over --- */
.picker {
  position: fixed;
  inset: 0;
  z-index: 120;
}
.picker-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
}
.picker-panel {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: min(26rem, 90vw);
  display: flex;
  flex-direction: column;
  background: var(--bg);
  box-shadow: -1.5rem 0 3rem rgba(0, 0, 0, 0.14);
}
.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.375rem 1.5rem;
  border-bottom: 1px solid var(--border-line);
}
.picker-title {
  margin: 0;
  font-size: 1.0625rem;
  font-weight: 600;
}
.picker-close {
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 50%;
  background: var(--bg-gray);
  color: var(--ink-secondary);
  font-size: 1.375rem;
  line-height: 1;
  cursor: pointer;
}
.picker-close:hover {
  color: var(--ink);
}
.picker-list {
  flex: 1;
  overflow-y: auto;
  padding: 1rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.picker-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  width: 100%;
  padding: 1.125rem 1rem;
  border: 1px solid var(--border-line);
  border-radius: 0.875rem;
  background: var(--bg);
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease; /* 展开/选中过渡 */
}
.picker-item:hover {
  border-color: var(--blue);
  background: rgba(0, 113, 227, 0.04);
}
.picker-item.active {
  border-color: var(--blue);
  background: rgba(0, 113, 227, 0.06);
  box-shadow: 0 0 0 1px var(--blue);
}
.radio {
  flex-shrink: 0;
  width: 1.125rem;
  height: 1.125rem;
  margin-top: 0.125rem;
  border-radius: 50%;
  border: 1.5px solid #c7c7cc;
  background: var(--bg);
  transition: all 0.2s ease;
}
.radio.checked {
  border-color: var(--blue);
  background: var(--blue);
  box-shadow: inset 0 0 0 3px var(--bg);
}
.picker-item-main {
  flex: 1;
  min-width: 0;
}
.picker-item-top {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.pick-check {
  flex-shrink: 0;
  width: 0.875rem;
  height: 0.75rem;
  margin-top: 0.25rem;
  color: var(--blue);
}
.picker-add {
  display: block;
  text-align: center;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--border-line);
  color: var(--link);
  font-size: 0.9375rem;
  font-weight: 500;
  text-decoration: none;
}
.picker-add:hover {
  background: var(--bg-gray);
}

/* 过渡动画：遮罩淡入 + 面板从右滑入（0.2s ease） */
.picker-fade-enter-active,
.picker-fade-leave-active {
  transition: opacity 0.2s ease;
}
.picker-fade-enter-active .picker-panel,
.picker-fade-leave-active .picker-panel {
  transition: transform 0.2s ease;
}
.picker-fade-enter-from,
.picker-fade-leave-to {
  opacity: 0;
}
.picker-fade-enter-from .picker-panel,
.picker-fade-leave-to .picker-panel {
  transform: translateX(100%);
}

/* --- 模拟支付弹层 --- */
.pay-mask {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
}
.pay-modal {
  width: min(22rem, calc(100vw - 2.5rem));
  padding: 1.5rem;
  border-radius: 1.25rem;
  background: var(--bg);
  box-shadow: 0 1.5rem 3.75rem rgba(0, 0, 0, 0.25);
  text-align: center;
}
.pay-title {
  margin: 0 0 0.875rem;
  font-size: 1.0625rem;
  font-weight: 600;
}
.pay-label {
  margin: 0 0 0.125rem;
  font-size: 0.8125rem;
  color: var(--ink-secondary);
}
.pay-amount {
  margin: 0 0 0.5rem;
  font-size: 2.125rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.pay-hint {
  margin: 0 0 1.125rem;
  font-size: 0.75rem;
  color: var(--ink-faint);
}
.pay-btn {
  width: 100%;
  height: 2.75rem;
  border: none;
  border-radius: var(--radius-full);
  background: var(--blue);
  color: #fff;
  font-size: 0.9375rem;
  font-weight: 600;
  cursor: pointer;
}
.pay-btn:hover:not(:disabled) {
  background: var(--blue-hover);
}
.pay-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.pay-later {
  width: 100%;
  height: 2.5rem;
  margin-top: 0.5rem;
  border: none;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--ink-secondary);
  font-size: 0.875rem;
  cursor: pointer;
}
.pay-later:hover {
  color: var(--ink);
}

/* 响应式：移动端单栏，摘要沉底 */
@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }
  .summary {
    position: static;
    order: 2;
    grid-row: auto;
  }
  .checkout-page {
    padding: 2rem 1.25rem 3.5rem;
  }
  .card {
    padding: 1.5rem 1.25rem;
  }
  .main-col {
    gap: 1.5rem;
  }
}
</style>