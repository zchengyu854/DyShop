<script setup>
import { onMounted, shallowRef, watch } from 'vue'
import { useRouter } from 'vue-router'
import ProductCard from '@/components/home/ProductCard.vue'
import HomeFooter from '@/components/home/HomeFooter.vue'
import HomeHeader from '@/components/home/HomeHeader.vue'
import { fetchProducts } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { toast } from '@/utils/toast'

const router = useRouter()
const cartStore = useCartStore()

const loading = shallowRef(true)
const recProducts = shallowRef([])
const recLoading = shallowRef(false)

function format(amount) {
  return Number(amount).toFixed(2)
}

function soldOut(item) {
  return (item.stock ?? 0) <= 0
}

function offShelf(item) {
  return (item.productStatus ?? 1) === 0
}

// 不可购买：售罄或商品已下架（均置灰展示、不可勾选/改量，仅可移除）
function unavailable(item) {
  return soldOut(item) || offShelf(item)
}

function maxQty(item) {
  return Math.min(99, item.stock ?? 0)
}

async function changeQty(item, quantity) {
  if (unavailable(item) || quantity < 1 || quantity > maxQty(item)) return
  const prev = item.quantity
  item.quantity = quantity
  try {
    await cartStore.updateQuantity(item.cartItemId, quantity)
  } catch (e) {
    item.quantity = prev
    toast.error(e.message || '操作失败，请重试')
  }
}

async function handleRemove(item) {
  try {
    await cartStore.removeItem(item.cartItemId)
  } catch (e) {
    toast.error(e.message || '移除失败，请重试')
  }
}

function isChecked(item) {
  return item.checked == null || item.checked === 1
}

async function toggleItem(item) {
  if (unavailable(item)) return
  try {
    await cartStore.toggleChecked(item.cartItemId, !isChecked(item))
  } catch (e) {
    toast.error(e.message || '操作失败，请重试')
  }
}

async function toggleAll() {
  try {
    await cartStore.setAllChecked(!cartStore.allChecked)
  } catch (e) {
    toast.error(e.message || '操作失败，请重试')
  }
}

function handleCheckout() {
  router.push('/checkout')
}

function goDetail(id) {
  router.push(`/products/${id}`)
}

// 单商品时填充「为你推荐」，避免左栏下方大面积空白
function loadRecommends() {
  if (cartStore.items.length !== 1 || recProducts.value.length) return
  recLoading.value = true
  fetchProducts({ size: 8 })
    .then((data) => {
      const inCart = new Set(cartStore.items.map((it) => it.productId))
      recProducts.value = data.records
        .filter((p) => !inCart.has(p.id))
        .slice(0, 4)
    })
    .catch(() => {
      recProducts.value = []
    })
    .finally(() => {
      recLoading.value = false
    })
}

onMounted(() => {
  cartStore
    .fetchCart()
    .catch(() => {})
    .finally(() => {
      loading.value = false
      if (!loading.value) loadRecommends()
    })
})

watch(
  () => cartStore.items.length,
  () => {
    recProducts.value = []
    loadRecommends()
  }
)
</script>

<template>
  <div class="page-shell">
    <HomeHeader />
    <main class="cart-page">
      <h1 class="title">购物车</h1>

      <div v-if="loading" class="hint">加载中…</div>
      <div v-else-if="cartStore.items.length === 0" class="empty">
        <p class="empty-title">购物车是空的</p>
        <router-link to="/" class="empty-link">去逛逛</router-link>
      </div>

      <template v-else>
        <div class="layout">
          <section class="list">
            <label class="select-all">
              <span
                class="checkbox"
                :class="{
                  checked: cartStore.allChecked,
                  partial: !cartStore.allChecked && cartStore.checkedItems.length > 0,
                }"
                role="checkbox"
                :aria-checked="cartStore.allChecked ? 'true' : 'false'"
                tabindex="0"
                @click.prevent="toggleAll"
                @keydown.enter.prevent="toggleAll"
              >
                <svg v-if="cartStore.allChecked" viewBox="0 0 12 10" class="check-icon">
                  <path d="M1 5.5 4.2 8.5 11 1.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                <span v-else-if="cartStore.checkedItems.length > 0" class="partial-line"></span>
              </span>
              <span class="select-all-text">全选</span>
            </label>
            <article
              v-for="it in cartStore.items"
              :key="it.cartItemId"
              class="item"
              :class="{ 'sold-out': unavailable(it) }"
            >
              <span
                class="checkbox"
                :class="{ checked: isChecked(it) && !unavailable(it), disabled: unavailable(it) }"
                role="checkbox"
                :aria-checked="isChecked(it) && !unavailable(it) ? 'true' : 'false'"
                :aria-disabled="unavailable(it) || undefined"
                :tabindex="unavailable(it) ? '-1' : '0'"
                @click.prevent="toggleItem(it)"
                @keydown.enter.prevent="toggleItem(it)"
              >
                <svg v-if="isChecked(it) && !unavailable(it)" viewBox="0 0 12 10" class="check-icon">
                  <path d="M1 5.5 4.2 8.5 11 1.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
              <router-link :to="`/products/${it.productId}`" class="item-thumb">
                <img :src="it.mainImage" :alt="it.name" loading="lazy" />
              </router-link>
              <div class="item-info">
                <span class="name-line">
                  <router-link :to="`/products/${it.productId}`" class="item-name">
                    {{ it.name }}
                  </router-link>
                  <span v-if="offShelf(it)" class="sold-badge">已下架</span>
                  <span v-else-if="soldOut(it)" class="sold-badge">已售罄</span>
                </span>
                <p v-if="it.specText" class="item-spec">{{ it.specText }}</p>
                <p v-if="it.subtitle && !it.specText" class="item-subtitle">{{ it.subtitle }}</p>
                <p class="item-price">¥{{ format(it.price) }}</p>
                <p v-if="offShelf(it)" class="sold-tip">该商品已下架，暂时无法购买</p>
                <p v-else-if="soldOut(it)" class="sold-tip">该商品已售罄，暂时无法购买</p>
                <button class="item-remove" @click="handleRemove(it)">移除</button>
              </div>
              <div class="item-right">
                <div class="stepper">
                  <button
                    class="step-btn"
                    :disabled="unavailable(it) || it.quantity <= 1"
                    @click="changeQty(it, it.quantity - 1)"
                  >
                    −
                  </button>
                  <span class="step-val">{{ it.quantity }}</span>
                  <button
                    class="step-btn"
                    :disabled="unavailable(it) || it.quantity >= maxQty(it)"
                    @click="changeQty(it, it.quantity + 1)"
                  >
                    +
                  </button>
                </div>
                <p class="line-total">¥{{ format(it.price * it.quantity) }}</p>
              </div>
            </article>

            <section
              v-if="cartStore.items.length === 1 && (recProducts.length || recLoading)"
              class="recommend"
            >
              <h2 class="recommend-title">为你推荐</h2>
              <div v-if="recLoading" class="hint">正在加载…</div>
              <div v-else class="rec-grid">
                <ProductCard
                  v-for="p in recProducts"
                  :key="p.id"
                  :product="p"
                  @click="goDetail"
                />
              </div>
            </section>
          </section>

          <aside class="summary">
              <div class="summary-card">
                <h3 class="summary-title">订单摘要</h3>
                <div class="summary-row">
                  <span>商品数量</span>
                  <span>{{ cartStore.checkedTotalQuantity }}</span>
                </div>
                <div class="summary-row total">
                  <span>合计</span>
                  <span>¥{{ format(cartStore.checkedTotalPrice) }}</span>
                </div>
                <button
                  class="checkout-btn"
                  :disabled="cartStore.checkedTotalQuantity === 0"
                  @click="handleCheckout"
                >
                  结算
                </button>
              </div>
          </aside>
        </div>
      </template>
    </main>
    <HomeFooter />
  </div>
</template>

<style scoped>
.page-shell {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}
.cart-page {
  flex: 1;
  width: 100%; /* 防 flex 容器内 fit-content 退化压窄布局（与订单页同款修复） */
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 32px 72px;
}
.title {
  margin: 0 0 32px;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.hint {
  padding: 60px 0;
  text-align: center;
  font-size: 15px;
  color: var(--ink-faint);
}
.layout {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}
.list {
  flex: 1;
  min-width: 0;
}
.item {
  display: flex;
  gap: 16px;
  padding: 24px 0;
  border-bottom: 1px solid #e8e8ed;
}
.item:first-child {
  padding-top: 4px;
}
.select-all {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e8e8ed;
  cursor: pointer;
}
.select-all-text {
  font-size: 14px;
  color: var(--ink-secondary);
}
.checkbox {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1.5px solid #c7c7cc;
  background: var(--bg);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.checkbox:hover {
  border-color: var(--blue);
}
.checkbox.checked {
  background: var(--blue);
  border-color: var(--blue);
  color: #fff;
}
.checkbox.partial {
  border-color: var(--blue);
  color: var(--blue);
}
.partial-line {
  width: 8px;
  height: 2px;
  border-radius: 1px;
  background: var(--blue);
}
.check-icon {
  width: 11px;
  height: 9px;
}
.item-thumb {
  flex-shrink: 0;
  width: 96px;
  height: 96px;
  border-radius: 14px;
  overflow: hidden;
  background: var(--bg-gray);
}
.item-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.name-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.sold-badge {
  flex-shrink: 0;
  padding: 2px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--bg-gray);
  color: var(--ink-faint);
  font-size: 12px;
}
.sold-tip {
  margin: 6px 0 0;
  font-size: 13px;
  color: #ff3b30;
}
.item.sold-out .item-thumb img {
  filter: grayscale(1);
  opacity: 0.55;
}
.item.sold-out .item-name,
.item.sold-out .item-name:hover,
.item.sold-out .item-spec,
.item.sold-out .item-subtitle,
.item.sold-out .item-price,
.item.sold-out .line-total {
  color: var(--ink-faint);
}
.item.sold-out .checkbox,
.item.sold-out .checkbox:hover {
  border-color: var(--border);
  background: var(--bg-gray);
  cursor: not-allowed;
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  display: inline-block;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--ink);
}
.item-name:hover {
  color: var(--link);
}
.item-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--ink-secondary);
}
.item-spec {
  margin: 5px 0 0;
  font-size: 13px;
  color: var(--ink-secondary);
}
.item-spec::before {
  content: '规格：';
  color: var(--ink-faint);
}
.item-price {
  margin: 8px 0 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}
.item-remove {
  margin-top: 10px;
  padding: 0;
  border: none;
  background: transparent;
  color: #ff3b30;
  font-size: 13px;
  cursor: pointer;
}
.item-remove:hover {
  text-decoration: underline;
}
.item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 14px;
}
.stepper {
  display: flex;
  align-items: center;
  height: 36px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--bg);
}
.step-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--ink);
  font-size: 18px;
  cursor: pointer;
}
.step-btn:disabled {
  color: var(--ink-faint);
  cursor: not-allowed;
}
.step-btn:hover:not(:disabled) {
  background: var(--bg-gray);
}
.step-val {
  min-width: 40px;
  text-align: center;
  font-size: 15px;
  font-weight: 600;
}
.line-total {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}
.recommend {
  margin-top: 40px;
}
.recommend-title {
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.rec-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 20px;
}
.summary {
  width: 320px;
  flex-shrink: 0;
  position: sticky;
  top: 96px;
}
.summary-card {
  padding: 20px 20px 22px;
  background: var(--bg-gray);
  border-radius: 16px;
}
.summary-title {
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 600;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 9px 0;
  font-size: 14px;
  line-height: 20px;
  color: var(--ink-secondary);
}
.summary-row.total {
  margin-top: 6px;
  padding: 14px 0 6px;
  border-top: 1px solid var(--border);
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}
.checkout-btn {
  width: 100%;
  height: 44px;
  margin-top: 18px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--blue);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.checkout-btn:hover:not(:disabled) {
  background: var(--blue-hover);
}
.checkout-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.empty {
  padding: 80px 0;
  text-align: center;
}
.empty-title {
  margin: 0 0 14px;
  font-size: 22px;
  font-weight: 600;
}
.empty-link {
  color: var(--link);
  font-size: 15px;
}
.empty-link:hover {
  text-decoration: underline;
}
@media (max-width: 768px) {
  .layout {
    flex-direction: column;
  }
  .summary {
    width: 100%;
    position: static;
  }
}
</style>
