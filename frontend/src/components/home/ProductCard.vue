<script setup>
defineProps({
  product: { type: Object, required: true },
})

const emit = defineEmits(['click'])
</script>

<template>
  <div class="card" @click="emit('click', product.id)">
    <img class="card-img" :src="product.mainImage" :alt="product.name" loading="lazy" />
    <div class="card-body">
      <p class="card-name">{{ product.name }}</p>
      <p class="card-subtitle">{{ product.subtitle }}</p>
      <p class="card-price">
        <span class="price">¥{{ product.price }}</span>
        <span v-if="product.originalPrice" class="original">¥{{ product.originalPrice }}</span>
        <span class="sales">已售 {{ product.sales }}</span>
      </p>
    </div>
  </div>
</template>

<style scoped>
.card {
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}
.card-img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
  transition: transform 0.35s ease;
}
.card:hover .card-img {
  transform: scale(1.03);
}
.card-body {
  padding: 14px 16px 16px;
}
.card-name {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-subtitle {
  font-size: 13px;
  color: var(--ink-secondary);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-price {
  margin-top: 10px;
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.price {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}
.original {
  font-size: 12px;
  color: var(--ink-faint);
  text-decoration: line-through;
}
.sales {
  margin-left: auto;
  font-size: 12px;
  color: var(--ink-faint);
}
</style>
