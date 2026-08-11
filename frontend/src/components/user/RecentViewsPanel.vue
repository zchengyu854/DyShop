<script setup>
// 浏览历史面板（ch14）：设备本地数据（localStorage），挂载时读取一次；
// 单条移除 / 一键清空（复用 OrderConfirmDialog 二次确认）→ 即时反映到界面与存储。
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import OrderConfirmDialog from '@/components/shop/OrderConfirmDialog.vue'
import { useRecentViewed } from '@/composables/useRecentViewed'

const router = useRouter()
const { list, remove, clear } = useRecentViewed()
const confirmingClear = ref(false)

function goDetail(id) {
  router.push(`/products/${id}`)
}

function doClear() {
  clear()
  confirmingClear.value = false
}
</script>

<template>
  <section class="panel">
    <div class="head">
      <h2 class="panel-title">浏览历史</h2>
      <div v-if="list.length" class="head-side">
        <span class="count">共 {{ list.length }} 条</span>
        <button class="clear-btn" @click="confirmingClear = true">清空</button>
      </div>
    </div>

    <div v-if="list.length" class="grid">
      <article v-for="r in list" :key="r.id" class="card" @click="goDetail(r.id)">
        <div class="thumb">
          <img :src="r.mainImage" :alt="r.name" loading="lazy" />
          <button class="remove" aria-label="移除该浏览记录" @click.stop="remove(r.id)">×</button>
        </div>
        <p class="name">{{ r.name }}</p>
        <p class="price">¥{{ Number(r.price).toFixed(2) }}</p>
      </article>
    </div>

    <div v-else class="state">
      <p>暂无浏览记录，去逛逛吧</p>
      <router-link to="/products" class="browse-link">去逛商品 →</router-link>
    </div>

    <OrderConfirmDialog
      v-if="confirmingClear"
      title="清空浏览历史"
      message="清空后将无法恢复，确定继续？"
      confirm-text="清空"
      danger
      @confirm="doClear"
      @close="confirmingClear = false"
    />
  </section>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.panel-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.head-side {
  display: flex;
  align-items: center;
  gap: 14px;
}
.count {
  font-size: 13px;
  color: var(--ink-secondary);
}
.clear-btn {
  padding: 6px 16px;
  border: 1px solid #ff3b30;
  border-radius: var(--radius-full);
  background: transparent;
  color: #ff3b30;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.clear-btn:hover {
  background: #ff3b30;
  color: #fff;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.card {
  background: var(--bg);
  border-radius: var(--radius-lg);
  padding: 12px;
  box-shadow: var(--shadow-card);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}
.card:hover {
  box-shadow: var(--shadow-hover);
  transform: translateY(-2px);
}
.thumb {
  position: relative;
  border-radius: var(--radius-card);
  overflow: hidden;
  aspect-ratio: 1;
  background: var(--bg-gray);
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.remove {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}
.card:hover .remove,
.card:focus-within .remove {
  opacity: 1;
}
.remove:hover {
  background: #ff3b30;
}
.name {
  margin: 10px 2px 4px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--ink);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.price {
  margin: 0 2px;
  font-size: 14px;
  font-weight: 600;
  color: #ff3b30;
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
.browse-link {
  display: inline-block;
  margin-top: 14px;
  color: var(--link);
  font-size: 14px;
  text-decoration: none;
}
.browse-link:hover {
  text-decoration: underline;
}
</style>