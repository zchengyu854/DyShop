<script setup>
defineProps({
  updatedAt: { type: String, default: '' },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['refresh', 'export'])
</script>

<template>
  <header class="dash-header">
    <div class="dash-titles">
      <h1>仪表盘</h1>
      <p>实时业务概览<template v-if="updatedAt"> · 更新于 {{ updatedAt }}</template></p>
    </div>
    <div class="dash-actions">
      <button class="btn ghost" :disabled="loading" @click="emit('refresh')">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M21 12a9 9 0 1 1-2.64-6.36"></path>
          <path d="M21 3v6h-6"></path>
        </svg>
        刷新
      </button>
      <button class="btn primary" :disabled="loading || !updatedAt" @click="emit('export')">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M12 3v12"></path>
          <path d="m7 10 5 5 5-5"></path>
          <path d="M5 21h14"></path>
        </svg>
        导出数据
      </button>
    </div>
  </header>
</template>

<style scoped>
.dash-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
}
.dash-titles h1 {
  margin: 0;
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--ink);
}
.dash-titles p {
  margin: 0.25rem 0 0;
  font-size: 0.8125rem;
  color: var(--ink-faint);
}
.dash-actions {
  display: flex;
  gap: 0.5rem;
}
.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  height: 2rem;
  padding: 0 0.875rem;
  border-radius: var(--radius-full);
  border: 1px solid transparent;
  font-size: 0.8125rem;
  cursor: pointer;
}
.btn.ghost {
  border-color: var(--border);
  background: var(--bg);
  color: var(--ink);
}
.btn.ghost:hover {
  background: var(--bg-gray);
}
.btn.primary {
  background: var(--blue);
  color: #fff;
}
.btn.primary:hover {
  background: var(--blue-hover);
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>