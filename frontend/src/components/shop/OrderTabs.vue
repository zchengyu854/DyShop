<script setup>
// ============================================================================
// OrderTabs —— 订单状态筛选分段控件（受控组件）
// ----------------------------------------------------------------------------
// 受控模式：选中态完全由父级 modelValue 驱动，组件内部不维护任何选中状态。
// 点击 → 仅 emit('update:modelValue'/'change')，由父级更新状态并同步 URL，
// 保证「高亮」与「列表数据」同源，杜绝状态漂移。
// ============================================================================
const props = defineProps({
  modelValue: { type: [String, Number], required: true },
  tabs: { type: Array, required: true }, // [{ key, label }]
  disabled: { type: Boolean, default: false },
  ariaLabel: { type: String, default: '订单状态筛选' },
})
const emit = defineEmits(['update:modelValue', 'change'])

function select(tab) {
  if (tab.key === props.modelValue || props.disabled) return
  emit('update:modelValue', tab.key)
  emit('change', tab.key)
}
</script>

<template>
  <div class="tabs" role="tablist" :aria-label="ariaLabel">
    <button
      v-for="t in tabs"
      :key="t.key"
      role="tab"
      :aria-selected="modelValue === t.key"
      class="tab"
      :class="{ active: modelValue === t.key }"
      :disabled="disabled"
      @click="select(t)"
    >
      {{ t.label }}
    </button>
  </div>
</template>

<style scoped>
.tabs {
  display: flex;
  gap: 8px;
  padding: 4px;
  margin-bottom: 20px;
  background: var(--bg-gray);
  border-radius: var(--radius-full);
  width: fit-content;
  max-width: 100%;
  overflow-x: auto;
}
.tab {
  flex: none;
  height: 34px;
  padding: 0 18px;
  border: none;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--ink-secondary);
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.tab.active {
  background: var(--ink);
  color: #fff;
  font-weight: 600;
}
.tab:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
@media (max-width: 720px) {
  .tabs {
    width: 100%;
  }
  /* 触控目标 ≥44px：窄屏下分段控件提高高度，避免误触相邻 tab */
  .tab {
    height: 44px;
  }
}
</style>