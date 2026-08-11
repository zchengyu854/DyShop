<script setup>
defineProps({
  favorite: { type: Object, required: true },
})

const emit = defineEmits(['remove', 'click'])
</script>

<template>
  <div class="fav-card">
    <div class="fav-img-wrap" @click="emit('click', favorite.productId)">
      <img class="fav-img" :src="favorite.mainImage" :alt="favorite.name" loading="lazy" />
    </div>
    <div class="fav-body">
      <p class="fav-name" @click="emit('click', favorite.productId)">{{ favorite.name }}</p>
      <p class="fav-subtitle">{{ favorite.subtitle }}</p>
      <div class="fav-foot">
        <span class="fav-price">¥{{ favorite.price }}</span>
        <button class="fav-remove" @click="emit('remove', favorite.productId)">移除</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fav-card {
  background: var(--bg);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-hover);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.fav-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}
.fav-img-wrap {
  cursor: pointer;
  overflow: hidden;
}
.fav-img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
  transition: transform 0.35s ease;
}
.fav-card:hover .fav-img {
  transform: scale(1.04);
}
.fav-body {
  padding: 12px 14px 14px;
}
.fav-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fav-name:hover {
  color: var(--blue);
}
.fav-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fav-foot {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.fav-price {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}
.fav-remove {
  padding: 5px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--ink-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}
.fav-remove:hover {
  color: #ff3b30;
  border-color: #ff3b30;
}
</style>
