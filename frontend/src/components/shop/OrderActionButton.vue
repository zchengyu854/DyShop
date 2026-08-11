<script setup>
// ============================================================================
// OrderActionButton —— 订单操作按钮（状态机 + 事件接口）
// ----------------------------------------------------------------------------
// 状态机（由父级 useOrderAction.pending 驱动，单行内聚）：
//   idle ──点击──▶ pending（加载中，禁用）
//   pending ──成功──▶ idle（由父级乐观更新/移除按钮完成状态翻转）
//   pending ──失败──▶ idle（父级回滚 + toast，本组件仅负责还原可点击态）
// 交互规范：
//   1) 点击 50ms 内必有视觉响应：pending 立即显示 spinner + loading 文案。
//   2) busy/disabled 双保险禁点，杜绝连点重复提交（还原态由父级控制）。
//   3) 原生 <button> 自带键盘支持：Tab 聚焦、Enter/Space 触发。
// ============================================================================
const props = defineProps({
  /** 'primary' | 'ghost' | 'danger' | 'del' */
  variant: { type: String, default: 'ghost', validator: (v) => ['primary', 'ghost', 'danger', 'del'].includes(v) },
  /** 是否 pending（loading + 禁用），由 useOrderAction.isPending 驱动 */
  busy: { type: Boolean, default: false },
  /** 外部禁用（如倒计时超时 / 无操作权） */
  disabled: { type: Boolean, default: false },
  label: { type: String, required: true },
  loadingText: { type: String, default: '处理中…' },
  /** 触控目标外扩类（mini 按钮专用，如删除） */
  compact: { type: Boolean, default: false },
})

const emit = defineEmits(['click'])
</script>

<template>
  <button
    class="op-btn"
    :class="[variant, { compact, busy }]"
    :disabled="busy || disabled"
    :aria-busy="busy"
    :aria-disabled="busy || disabled"
    @click="emit('click')"
  >
    <span v-if="busy" class="spinner" aria-hidden="true"></span>
    {{ busy ? loadingText : label }}
  </button>
</template>

<style scoped>
.op-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  min-height: 2.75rem; /* 触控目标 ≥44px（WCAG 2.5.8） */
  min-width: 5.5rem;
  padding: 0 1.25rem;
  border-radius: var(--radius-full);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: background 0.15s, color 0.15s, border-color 0.15s, opacity 0.15s;
}
.op-btn:focus-visible {
  outline: 2px solid var(--link);
  outline-offset: 2px;
}
.op-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.op-btn.primary {
  background: var(--blue);
  color: #fff;
}
.op-btn.primary:hover:not(:disabled) {
  background: var(--blue-hover);
}
.op-btn.ghost {
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--ink);
}
.op-btn.ghost:hover:not(:disabled) {
  background: var(--bg-gray);
}
.op-btn.danger {
  border: 1px solid rgba(255, 59, 48, 0.35);
  background: var(--bg);
  color: #ff3b30;
}
.op-btn.danger:hover:not(:disabled) {
  background: rgba(255, 59, 48, 0.08);
}
/* 次要紧凑删除按钮 */
.op-btn.del {
  min-width: 0;
  min-height: 2rem;
  padding: 0 0.875rem;
  font-size: 0.8125rem;
  color: var(--ink-secondary);
  border: 1px solid transparent;
  background: transparent;
}
.op-btn.del:hover:not(:disabled) {
  color: #ff3b30;
  border-color: rgba(255, 59, 48, 0.35);
  background: rgba(255, 59, 48, 0.06);
}
.op-btn.compact {
  min-width: 5.5rem;
  min-height: 2rem;
  padding: 0 0.875rem;
  font-size: 0.8125rem;
}

.spinner {
  width: 14px;
  height: 14px;
  flex: none;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: oa-spin 0.7s linear infinite;
}
@keyframes oa-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>