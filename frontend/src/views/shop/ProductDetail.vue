<script setup>
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import HomeHeader from '@/components/home/HomeHeader.vue'
import HomeFooter from '@/components/home/HomeFooter.vue'
import ProductCard from '@/components/home/ProductCard.vue'
import ProductGallery from '@/components/shop/ProductGallery.vue'
import ProductInfoPanel from '@/components/shop/ProductInfoPanel.vue'
import { fetchProductDetail, fetchProducts } from '@/api/product'
import { useRecentViewed } from '@/composables/useRecentViewed'
import { useDataRefresh } from '@/composables/useDataRefresh'
import { PRODUCT_NS } from '@/utils/dataSync'

const route = useRoute()
const router = useRouter()
const { record: recordViewed } = useRecentViewed()

const loading = shallowRef(true)
const notFound = shallowRef(false)
const product = shallowRef(null)
// 规格 SKU 主图覆盖（选中带图 SKU 时传给相册）
const skuImage = shallowRef('')

const recommends = shallowRef([])
const recLoading = shallowRef(false)
const recError = shallowRef('')

// 竞态守卫：连续触发只采纳最后一次请求（切商品 / 静默刷新并发）
let requestSeq = 0

async function loadDetail(id, { silent = false } = {}) {
  const seq = ++requestSeq
  // 静默刷新：不闪 loading、不清空已展示内容（下单后库存/价格变化时后台同步）
  if (!silent) {
    loading.value = true
    notFound.value = false
    product.value = null
    recommends.value = []
  }
  try {
    const p = await fetchProductDetail(id)
    if (seq !== requestSeq) return
    product.value = p
    if (!silent) {
      document.title = `${p.name} - dyshop`
      recordViewed(p)
    }
    loadRecommends(p, seq)
  } catch (e) {
    if (seq !== requestSeq) return
    if (e.code === 404) {
      notFound.value = true
      document.title = '商品不存在 - dyshop'
    }
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

async function loadRecommends(p, seq) {
  recLoading.value = true
  recError.value = ''
  try {
    const data = await fetchProducts({ categoryId: p.categoryId, size: 8 })
    if (seq !== requestSeq) return
    recommends.value = data.records.filter((item) => item.id !== p.id)
  } catch (e) {
    if (seq !== requestSeq) return
    recError.value = e.message || '推荐加载失败'
  } finally {
    if (seq === requestSeq) recLoading.value = false
  }
}

function goDetail(id) {
  if (id !== route.params.id) {
    router.push(`/products/${id}`)
  }
}

function retryRecommends() {
  if (product.value) {
    loadRecommends(product.value, requestSeq)
  }
}

watch(
  () => route.params.id,
  (id) => {
    if (id) loadDetail(id)
  },
  { immediate: true },
)

// BFCache 恢复（前进/后退回到本页）时刷新详情：库存/价格等变动数据即时同步，
// 静默拉取，不闪 loading 也不清空已展示内容（复用 loadDetail 的竞态守卫）
function onPageShow(event) {
  if (!event.persisted || !route.params.id) return
  loadDetail(route.params.id, { silent: true })
}

// 商品领域失效广播（下单成功/后台改价等）→ 静默刷新库存/价格；
// 配合 pageshow/visibility 覆盖「重新可见但不重新 mount」场景
const { refresh } = useDataRefresh(PRODUCT_NS, (opts) => {
  if (route.params.id) loadDetail(route.params.id, opts)
})
onMounted(() => {
  window.addEventListener('pageshow', onPageShow)
})
onBeforeUnmount(() => {
  window.removeEventListener('pageshow', onPageShow)
})
</script>

<template>
  <div class="detail-page">
    <HomeHeader />

    <div v-if="loading" class="state-wrap">
      <p class="state-text">正在加载…</p>
    </div>

    <div v-else-if="notFound" class="state-wrap">
      <p class="state-title">商品不存在</p>
      <p class="state-text">该商品可能已下架或已被移除</p>
      <button class="back-btn" @click="router.push('/')">返回首页</button>
    </div>

    <template v-else-if="product">
      <div class="container">
        <nav class="breadcrumb">
          <router-link to="/" class="crumb">首页</router-link>
          <span class="sep">›</span>
          <span class="crumb current">{{ product.name }}</span>
        </nav>

        <section class="main">
          <div class="gallery-col">
            <ProductGallery
              :images="product.images"
              :main-image="product.mainImage"
              :name="product.name"
              :sku-image="skuImage"
            />
          </div>
          <div class="info-col">
            <ProductInfoPanel :product="product" @sku-image="(url) => (skuImage = url)" />
          </div>
        </section>

        <section class="detail">
          <h2 class="section-title">商品详情</h2>
          <div class="detail-body">
            <div v-if="product.detail" class="detail-html" v-html="product.detail" />
            <p v-else class="detail-empty">暂无详情</p>
          </div>
        </section>
      </div>

      <section v-if="recommends.length || recLoading" class="recommend">
        <div class="container">
          <h2 class="section-title">同类推荐</h2>
          <div v-if="recLoading" class="state-text">正在加载…</div>
          <div v-else-if="recError" class="state-text">
            <p>{{ recError }}</p>
            <button class="back-btn" @click="retryRecommends">重试</button>
          </div>
          <div v-else class="rec-grid">
            <ProductCard
              v-for="p in recommends"
              :key="p.id"
              :product="p"
              @click="goDetail"
            />
          </div>
        </div>
      </section>
    </template>

    <HomeFooter />
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}
.container {
  flex: 1;
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 20px;
}
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 0 4px;
  font-size: 13px;
  color: var(--ink-secondary);
}
.crumb {
  transition: color 0.2s;
}
.crumb:hover {
  color: var(--link);
}
.current {
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px;
}
.sep {
  color: var(--ink-faint);
}
.main {
  display: grid;
  grid-template-columns: minmax(0, 5fr) minmax(0, 4fr);
  gap: 48px;
  padding: 24px 0 56px;
  align-items: start;
}
.info-col {
  position: sticky;
  top: 100px;
}
.section-title {
  margin: 0 0 18px;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.detail {
  border-top: 1px solid var(--border);
  padding: 40px 0 56px;
}
.detail-body {
  max-width: 800px;
  margin: 0 auto;
}
.detail-empty {
  text-align: center;
  color: var(--ink-faint);
  font-size: 14px;
}
.detail-html {
  font-size: 15px;
  line-height: 1.8;
  color: var(--ink);
}
.detail-html :deep(p) {
  margin: 0 0 16px;
}
.detail-html :deep(img) {
  max-width: 100%;
  border-radius: var(--radius);
}
.recommend {
  background: var(--bg-gray);
  padding: 40px 0 56px;
}
.rec-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 20px;
}
.state-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 120px 20px;
}
.state-title {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
}
.state-text {
  margin: 0;
  color: var(--ink-secondary);
  font-size: 14px;
}
.back-btn {
  margin-top: 10px;
  padding: 9px 28px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--blue);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}
.back-btn:hover {
  background: var(--blue-hover);
}
@media (max-width: 860px) {
  .main {
    grid-template-columns: 1fr;
    gap: 28px;
  }
  .info-col {
    position: static;
  }
}
</style>