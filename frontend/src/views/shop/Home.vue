<script setup>
import { computed, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import HomeHeader from '@/components/home/HomeHeader.vue'
import HomeHero from '@/components/home/HomeHero.vue'
import TrustBar from '@/components/home/TrustBar.vue'
import CategoryBar from '@/components/home/CategoryBar.vue'
import BentoPromises from '@/components/home/BentoPromises.vue'
import ProductGrid from '@/components/home/ProductGrid.vue'
import PromoBanner from '@/components/home/PromoBanner.vue'
import HomeFooter from '@/components/home/HomeFooter.vue'
import { useReveal } from '@/composables/useReveal'
import { fetchCategories, fetchProducts } from '@/api/product'

const router = useRouter()

const PAGE_SIZE = 12

const categories = shallowRef([])
const activeCategory = shallowRef(null)
const keyword = shallowRef('')
const products = shallowRef([])
const page = shallowRef(1)
const total = shallowRef(0)
const loading = shallowRef(false)
const error = shallowRef('')

const hasMore = computed(() => products.value.length < total.value)

async function loadProducts(reset = false) {
  if (reset) {
    page.value = 1
    products.value = []
  }
  loading.value = true
  error.value = ''
  try {
    const data = await fetchProducts({
      page: page.value,
      size: PAGE_SIZE,
      categoryId: activeCategory.value ?? undefined,
      keyword: keyword.value.trim() || undefined,
    })
    products.value = reset ? data.records : [...products.value, ...data.records]
    total.value = data.total
  } catch (e) {
    error.value = e.message || '网络异常'
  } finally {
    loading.value = false
  }
}

function loadMore() {
  page.value += 1
  loadProducts(false)
}

function handleSearch(kw) {
  keyword.value = kw
  loadProducts(true)
}

function switchCategory(id) {
  activeCategory.value = id
  loadProducts(true)
}

async function loadCategories() {
  try {
    categories.value = await fetchCategories()
  } catch (e) {
    console.warn('分类加载失败', e)
  }
}

onMounted(() => {
  loadProducts(true)
  loadCategories()
})

useReveal()

function goDetail(id) {
  router.push(`/products/${id}`)
}
</script>

<template>
  <div class="home">
    <HomeHeader @search="handleSearch" />

    <CategoryBar
      :categories="categories"
      :active-id="activeCategory"
      @change="switchCategory"
    />

    <HomeHero :product="products[0] || null" :loading="loading" />

    <TrustBar data-reveal :total="total" :category-count="categories.length" />

    <BentoPromises data-reveal />

    <main id="products" class="products" data-reveal>
      <div class="products-inner">
        <h2 class="section-title">精选产品</h2>
        <p class="section-sub">每一件都值得仔细看看</p>
        <ProductGrid
          :products="products"
          :loading="loading"
          :error="error"
          :has-more="hasMore"
          @retry="loadProducts(true)"
          @load-more="loadMore"
          @card-click="goDetail"
        />
      </div>
    </main>

    <PromoBanner data-reveal />

    <HomeFooter />
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}
.products {
  flex: 1;
  width: 100%;
  background: var(--bg-gray);
  padding: 56px 20px;
}
.products-inner {
  max-width: 1100px;
  margin: 0 auto;
}
.section-title {
  margin: 0;
  text-align: center;
  font-size: clamp(26px, 3vw, 34px);
  font-weight: 700;
  letter-spacing: -0.01em;
}
.section-sub {
  margin: 10px 0 32px;
  text-align: center;
  font-size: 15px;
  color: var(--ink-secondary);
}
</style>