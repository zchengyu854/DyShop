<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: String, default: '--' },
  hint: { type: String, default: '' },
  tone: { type: String, default: 'blue' },
})

const TONES = {
  blue: '#0071e3',
  green: '#34c759',
  orange: '#ff9500',
  purple: '#af52de',
  teal: '#30b0c7',
  ink: '#86868b',
}
</script>

<template>
  <article class="metric-card">
    <div class="metric-top">
      <span class="metric-label">{{ label }}</span>
      <span class="metric-dot" :style="{ backgroundColor: TONES[tone] }"></span>
    </div>
    <p class="metric-value" :title="value">{{ value }}</p>
    <p class="metric-hint">{{ hint }}</p>
  </article>
</template>

<style scoped>
.metric-card {
  height: 6.5rem;
  background: var(--bg);
  border: 1px solid var(--border-line);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.metric-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.metric-label {
  font-size: 0.8125rem;
  color: var(--ink-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.metric-dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}
.metric-value {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.metric-hint {
  margin: 0;
  font-size: 0.75rem;
  color: var(--ink-faint);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
