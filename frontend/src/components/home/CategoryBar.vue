<script setup>
defineProps({
  categories: { type: Array, required: true },
  activeId: { type: [Number, String, null], default: null },
})

const emit = defineEmits(['change'])

function select(id) {
  emit('change', id)
}
</script>

<template>
  <nav class="category-bar">
    <div class="category-inner">
      <span
        class="category-item"
        :class="{ active: activeId === null }"
        @click="select(null)"
      >全部</span>
      <span
        v-for="c in categories"
        :key="c.id"
        class="category-item"
        :class="{ active: activeId === c.id }"
        @click="select(c.id)"
      >{{ c.name }}</span>
    </div>
  </nav>
</template>

<style scoped>
.category-bar {
  background: var(--bg);
  padding: 20px 20px 4px;
}
.category-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  gap: 26px;
  flex-wrap: wrap;
}
.category-item {
  padding: 4px 2px;
  font-size: 13px;
  color: var(--ink-secondary);
  cursor: pointer;
  user-select: none;
  border-bottom: 1px solid transparent;
  transition: color 0.2s, border-color 0.2s;
}
.category-item:hover {
  color: var(--ink);
}
.category-item.active {
  color: var(--ink);
  font-weight: 600;
  border-bottom-color: var(--link);
}
</style>
