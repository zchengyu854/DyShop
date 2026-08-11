<script setup>
import { ref } from 'vue'

/**
 * 多维规格选择器（纯展示层）。
 * 联动算法在 useSpecSelector（父组件持有），本组件仅按 props 渲染 + emit select。
 * 无障碍：每维度一个 role=radiogroup，值项 role=radio + roving tabindex +
 * 方向键切换（WCAG 2.1 键盘操作）。
 */
const props = defineProps({
  specs: { type: Array, default: () => [] },
  // { [dimName]: { [value]: { selectable, soldOut } } }
  states: { type: Object, default: () => ({}) },
  // 已选 { [dimName]: value }
  selected: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['select'])

// 行内 radio 引用：groupRefs[dimIndex][valueIndex]
const groupRefs = ref([])

function isSelected(dim, value) {
  return props.selected[dim] === value
}

function stateOf(dim, value) {
  return props.states[dim]?.[value] || { selectable: true, soldOut: false }
}

function onSelect(dim, value) {
  const { selectable } = stateOf(dim, value)
  if (!selectable) return
  emit('select', { dim, value })
}

/** roving tabindex：同组内仅焦点项可 Tab，方向键移动焦点并选中 */
function onGroupKeydown(e, dimIndex, valueIndex) {
  const group = groupRefs.value[dimIndex]
  if (!group) return
  const values = props.specs[dimIndex]?.values || []
  const total = group.length

  const moveTo = (target) => {
    const next = group[target]
    if (!next) return
    next.focus()
    const value = values[target]
    const { selectable } = stateOf(props.specs[dimIndex].name, value)
    if (selectable) onSelect(props.specs[dimIndex].name, value)
    e.preventDefault()
  }

  switch (e.key) {
    case 'ArrowRight':
    case 'ArrowDown':
      moveTo((valueIndex + 1) % total)
      break
    case 'ArrowLeft':
    case 'ArrowUp':
      moveTo((valueIndex - 1 + total) % total)
      break
    case 'Home':
      moveTo(0)
      break
    case 'End':
      moveTo(total - 1)
      break
    case ' ':
    case 'Enter':
      e.preventDefault()
      onSelect(props.specs[dimIndex].name, values[valueIndex])
      break
  }
}

function tabIndexFor(dimIndex, valueIndex, dim, value) {
  if (!stateOf(dim, value).selectable) return -1
  // 同组内唯一 Tab 焦点：当前选中项或第一个可选值
  return isSelected(dim, value) || (!props.selected[dim] && valueIndex === firstSelectableIndex(dimIndex))
    ? 0
    : -1
}

function firstSelectableIndex(dimIndex) {
  const dim = props.specs[dimIndex]
  const values = dim?.values || []
  const idx = values.findIndex((v) => stateOf(dim.name, v).selectable)
  return idx === -1 ? 0 : idx
}
</script>

<template>
  <div class="spec-selector">
    <div
      v-for="(spec, dimIndex) in specs"
      :key="spec.name"
      class="spec-group"
    >
      <p class="spec-name">
        <span>{{ spec.name }}</span>
        <span v-if="!selected[spec.name]" class="spec-missing">未选择</span>
        <span v-else class="spec-picked">{{ selected[spec.name] }}</span>
      </p>
      <div
        class="spec-values"
        role="radiogroup"
        :aria-label="spec.name"
      >
        <button
          v-for="(value, valueIndex) in spec.values"
          :key="value"
          ref="groupRefs"
          type="button"
          class="spec-value"
          :class="{
            checked: isSelected(spec.name, value),
            disabled: !stateOf(spec.name, value).selectable,
            soldout: stateOf(spec.name, value).soldOut,
          }"
          role="radio"
          :aria-checked="isSelected(spec.name, value)"
          :aria-disabled="!stateOf(spec.name, value).selectable"
          :tabindex="tabIndexFor(dimIndex, valueIndex, spec.name, value)"
          :title="String(value)"
          @click="onSelect(spec.name, value)"
          @keydown="onGroupKeydown($event, dimIndex, valueIndex)"
        >
          <span class="value-text">{{ value }}</span>
          <span v-if="stateOf(spec.name, value).soldOut" class="soldout-tag">缺货</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.spec-selector {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 22px;
}
.spec-group {
  min-width: 0;
}
.spec-name {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.spec-missing {
  font-size: 12px;
  font-weight: 400;
  color: #ff9500;
}
.spec-picked {
  font-size: 13px;
  font-weight: 500;
  color: var(--blue);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 70%;
}
.spec-values {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  max-height: 168px;
  overflow-y: auto;
  padding: 2px;
}
.spec-value {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 44px;
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--bg);
  color: var(--ink);
  font-size: 14px;
  line-height: 1.3;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, color 0.15s, opacity 0.15s;
  outline-offset: 2px;
}
.spec-value:hover:not(.disabled):not(.checked) {
  border-color: var(--blue);
}
.spec-value:focus-visible {
  outline: 2px solid var(--blue);
}
.spec-value.checked {
  border-color: var(--blue);
  background: var(--blue-soft, rgba(10, 132, 255, 0.08));
  color: var(--blue);
  font-weight: 600;
}
.spec-value.disabled {
  opacity: 0.42;
  cursor: not-allowed;
}
.spec-value.disabled .value-text {
  text-decoration: line-through;
  text-decoration-color: rgba(0, 0, 0, 0.25);
}
.spec-value.soldout:not(.disabled) .value-text {
  text-decoration: line-through;
}
.soldout-tag {
  flex-shrink: 0;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
  font-size: 11px;
  font-weight: 600;
}
.value-text {
  overflow-wrap: anywhere;
}
</style>
