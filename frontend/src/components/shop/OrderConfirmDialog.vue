<script setup>
// ============================================================================
// OrderConfirmDialog —— 高风险操作二次确认弹窗（键盘可用）
// ----------------------------------------------------------------------------
// 交互约束（P0：杜绝误触/重复确认）：
//   1) 高风险操作（取消/确认/删除）必须经本组件拦截方可执行。
//   2) busy 时锁定：确认按钮显示 loading + 禁用，忽略点击/Esc（防重复提交）。
//   3) 键盘：Tab 聚焦弹窗内按钮，Enter/Space 触发（原生 button），Esc 关闭
//      （busy 期间 Esc 无效，防止操作进行中误关悬置状态）。
// ============================================================================
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  message: { type: String, default: '' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '再想想' },
  /** danger=true → 确认按钮红色；false 使用主色 */
  danger: { type: Boolean, default: false },
  /** 确认中：按钮 loading 锁定 */
  busy: { type: Boolean, default: false },
})

const emit = defineEmits(['confirm', 'close'])

const rootRef = ref(null)
const confirmBtnRef = ref(null)

// 打开时自动聚焦确认按钮（键盘用户 Tab/Enter 即可确认，免二次 Tab 导航）
watch(
  () => props.busy,
  async (v) => {
    if (!v) await nextTick()
    confirmBtnRef.value?.focus()
  },
  { immediate: true },
)

// Esc 关闭（非 busy 时）；焦点保持在弹窗内（简易焦点圈定）
function onKeydown(e) {
  if (props.busy) return
  if (e.key === 'Escape') emit('close')
  if (e.key === 'Tab') trapFocus(e)
}

let lastActive = null
function trapFocus(e) {
  const focusables = rootRef.value?.querySelectorAll('button:not(:disabled)')
  if (!focusables || focusables.length === 0) return
  const first = focusables[0]
  const last = focusables[focusables.length - 1]
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault()
    last.focus()
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault()
    first.focus()
  }
}

onMounted(() => {
  lastActive = document.activeElement
  window.addEventListener('keydown', onKeydown, true)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown, true)
  // 关闭后焦点归还触发元素，键盘操作不中断
  if (lastActive && typeof lastActive.focus === 'function') lastActive.focus()
})

</script>

<template>
  <div ref="rootRef" class="mask" role="dialog" aria-modal="true" :aria-label="title" @click.self="!busy && emit('close')">
    <div class="confirm-modal">
      <h3 class="confirm-title">{{ title }}</h3>
      <p class="confirm-desc">{{ message }}</p>
      <div class="confirm-ops">
        <button class="confirm-btn cancel" :disabled="busy" @click="emit('close')">
          {{ cancelText }}
        </button>
        <button
          ref="confirmBtnRef"
          class="confirm-btn"
          :class="danger ? 'danger' : 'primary'"
          :disabled="busy"
          :aria-busy="busy"
          @click="emit('confirm')"
        >
          <span v-if="busy" class="spinner" aria-hidden="true"></span>
          {{ busy ? '处理中…' : confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
}
.confirm-modal {
  width: 340px;
  max-width: calc(100vw - 40px);
  padding: 24px;
  border-radius: 20px;
  background: var(--bg);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.25);
}
.confirm-title {
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 600;
  text-align: center;
}
.confirm-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--ink-secondary);
  text-align: center;
}
.confirm-ops {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}
.confirm-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 44px;
  border: none;
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.confirm-btn.cancel {
  background: var(--bg-gray);
  color: var(--ink);
}
.confirm-btn.primary {
  background: var(--blue);
  color: #fff;
}
.confirm-btn.danger {
  background: #ff3b30;
  color: #fff;
}
.confirm-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: ocd-spin 0.7s linear infinite;
}
@keyframes ocd-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>