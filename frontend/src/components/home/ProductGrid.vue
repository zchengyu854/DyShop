<script setup>
import ProductCard from './ProductCard.vue'

defineProps({
  products: { type: Array, required: true },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  hasMore: { type: Boolean, default: false },
})

const emit = defineEmits(['retry', 'load-more', 'card-click'])
</script>

<template>
  <div v-if="loading && products.length === 0" class="state">正在加载…</div>

  <div v-else-if="error" class="state">
    <p>加载失败，请重试</p>
    <button class="retry-btn" @click="emit('retry')">重试</button>
  </div>

  <div v-else-if="products.length === 0" class="state">暂无商品</div>

  <template v-else>
    <div class="grid">
      <ProductCard
        v-for="p in products"
        :key="p.id"
        :product="p"
        @click="emit('card-click', $event)"
      />
    </div>

    <div class="load-more">
      <button v-if="hasMore" class="more-btn" :disabled="loading" @click="emit('load-more')">
        {{ loading ? '正在加载…' : '加载更多' }}
      </button>
      <span v-else class="no-more">没有更多产品了</span>
    </div>
  </template>
</template>

<style scoped>
.state {
  text-align: center;
  padding: 80px 0;
  color: var(--ink-secondary);
}
.state p {
  margin: 0;
}
.retry-btn {
  margin-top: 16px;
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
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 20px;
}
.load-more {
  text-align: center;
  padding: 32px 0 8px;
}
.more-btn {
  padding: 10px 34px;
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
