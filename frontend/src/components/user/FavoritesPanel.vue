<script setup>
import { computed, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import FavoriteCard from './FavoriteCard.vue'
import { fetchFavorites, removeFavorite } from '@/api/user'

const router = useRouter()

const PAGE_SIZE = 8

const records = shallowRef([])
const total = shallowRef(0)
const page = shallowRef(1)
const loading = shallowRef(false)
const error = shallowRef('')

const hasMore = computed(() => records.value.length < total.value)

async function load(reset = false) {
  if (reset) {
    page.value = 1
    records.value = []
  }
  loading.value = true
  error.value = ''
  try {
    const data = await fetchFavorites({ page: page.value, size: PAGE_SIZE })
    records.value = reset ? data.records : [...records.value, ...data.records]
    total.value = data.total
  } catch (e) {
    error.value = e.message || '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

function loadMore() {
  page.value += 1
  load(false)
}

async function remove(id) {
  await removeFavorite(id)
  await load(true)
}

onMounted(() => load(true))

function goDetail(productId) {
  router.push(`/products/${productId}`)
}
</script>

<template>
  <section class="panel">
    <h2 class="panel-title">我的收藏</h2>

    <div v-if="loading && records.length === 0" class="state">正在加载…</div>

    <div v-else-if="error" class="state">
      <p>{{ error }}</p>
      <button class="retry-btn" @click="load(true)">重试</button>
    </div>

    <div v-else-if="records.length === 0" class="state">
      <p>还没有收藏的商品，去商品页逛逛吧</p>
    </div>

    <template v-else>
      <div class="grid">
        <FavoriteCard
          v-for="f in records"
          :key="f.favoriteId"
          :favorite="f"
          @remove="remove"
          @click="goDetail"
        />
      </div>

      <div class="load-more">
        <button v-if="hasMore" class="more-btn" :disabled="loading" @click="loadMore">
          {{ loading ? '正在加载…' : '加载更多' }}
        </button>
        <span v-else class="no-more">没有更多收藏了</span>
      </div>
    </template>
  </section>
</template>

<style scoped>
.panel-title {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.state {
  background: var(--bg);
  border-radius: var(--radius-lg);
  padding: 64px 20px;
  text-align: center;
  color: var(--ink-secondary);
  font-size: 14px;
  box-shadow: var(--shadow-hover);
}
.state p {
  margin: 0;
}
.retry-btn {
  margin-top: 14px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--link);
  font-size: 14px;
  cursor: pointer;
}
.retry-btn:hover {
  text-decoration: underline;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.load-more {
  text-align: center;
  padding: 24px 0 4px;
}
.more-btn {
  padding: 9px 30px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--blue);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}
.more-btn:hover {
  background: var(--blue-hover);
}
.more-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.no-more {
  font-size: 13px;
  color: var(--ink-faint);
}
</style>
