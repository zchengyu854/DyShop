<script setup>
// 优惠券选择器（ch11）：一单一券单选；不可用券置灰并展示原因（title tooltip 兜底）；
// 选券/取消统一通过 onChange(userCouponId | null) 上抛，由父组件（usePriceCalculator）即时重算。
import { computed, ref } from 'vue'

const props = defineProps({
  options: { type: Array, default: () => [] }, // [{userCouponId,name,minAmount,discount,applicable,reason}]
  selectedId: { type: [Number, String], default: null },
  applied: { type: Boolean, default: false }, // 选中券是否实际生效（false=自动采用更优会员价）
  discountText: { type: String, default: '' }, // 生效券的 -¥X.XX 文本
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['change'])

const panelOpen = ref(false)

const selectedOption = computed(() =>
  props.options.find((o) => o.userCouponId === props.selectedId) || null
)
const availableCount = computed(() => props.options.filter((o) => o.applicable).length)

function pick(ucId) {
  emit('change', props.selectedId === ucId ? null : ucId)
}

function pickNone() {
  emit('change', null)
}

function formatMoney(yuan) {
  const n = Number(yuan) || 0
  const fixed = n.toFixed(2)
  return fixed.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}
</script>

<template>
  <div class="coupon-block">
    <!-- 胶囊入口：弱化标签 + 主操作链接 -->
    <div class="coupon-trigger">
      <span class="trigger-label">优惠券</span>
      <button
        type="button"
        class="trigger-btn"
        :class="{ open: panelOpen, has: !!selectedOption, una: !options.length }"
        :aria-expanded="panelOpen ? 'true' : 'false'"
        @click="panelOpen = !panelOpen"
      >
        <template v-if="selectedOption">
          <span class="t-name" :title="selectedOption.name">{{ selectedOption.name }}</span>
          <span v-if="applied" class="t-save">-¥{{ formatMoney(selectedOption.discount) }}</span>
          <span v-else class="t-note">会员价更优惠</span>
        </template>
        <template v-else>
          <span class="t-hint">
            {{ options.length ? `查看 ${availableCount} 张可用优惠券` : '暂无可用优惠券' }}
          </span>
        </template>
        <svg class="t-chevron" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M5 3.5 10 8 5 12.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
    </div>

    <!-- 选券面板：滑出式淡入，卡片化选项列表 -->
    <Transition name="panel">
      <div v-if="panelOpen" class="coupon-panel" role="radiogroup" aria-label="选择优惠券">
        <p v-if="!options.length" class="coupon-empty">暂无可用优惠券</p>
        <template v-else>
          <label
            v-for="o in options"
            :key="o.userCouponId"
            class="coupon-opt"
            :class="{ disabled: !o.applicable, active: selectedId === o.userCouponId }"
            :title="!o.applicable ? o.reason : ''"
          >
            <input
              type="radio"
              name="checkout-coupon"
              :checked="selectedId === o.userCouponId"
              :disabled="!o.applicable"
              @change="pick(o.userCouponId)"
            />
            <span class="opt-main">
              <span class="opt-name">{{ o.name }}</span>
              <span class="opt-cond">{{ Number(o.minAmount) > 0 ? `满 ¥${formatMoney(o.minAmount)}` : '无门槛' }}</span>
            </span>
            <span class="opt-right">
              <span v-if="o.applicable" class="opt-discount">-¥{{ formatMoney(o.discount) }}</span>
              <span v-else class="opt-reason">{{ o.reason }}</span>
            </span>
          </label>
          <label class="coupon-opt none-opt" :class="{ active: !selectedId }">
            <input
              type="radio"
              name="checkout-coupon"
              :checked="!selectedId"
              :disabled="loading"
              @change="pickNone"
            />
            <span class="opt-name">不使用优惠券</span>
          </label>
        </template>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.coupon-block {
  margin-top: 0.5rem;
}
/* 触发器：标签 + 胶囊/链接样式按钮 */
.coupon-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.625rem 0;
}
.trigger-label {
  font-size: 0.875rem;
  color: var(--sum-label, #6e6e73);
}
.trigger-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  min-width: 0;
  max-width: 16rem;
  padding: 0.25rem 0.125rem;
  border: none;
  background: none;
  font-family: inherit;
  font-size: 0.875rem;
  color: var(--sum-label, #6e6e73);
  cursor: pointer;
  transition: color 0.15s;
}
.trigger-btn:hover {
  color: var(--sum-save, #ff5000);
}
.trigger-btn.open {
  color: var(--sum-save, #ff5000);
}
.trigger-btn.una {
  cursor: default;
}
.trigger-btn.una:hover {
  color: var(--sum-label, #6e6e73);
}
.t-name {
  max-width: 9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--sum-value, #1d1d1f);
  font-weight: 600;
}
.t-save {
  color: var(--sum-save, #ff5000);
  font-weight: 700;
}
.t-note {
  font-size: 0.75rem;
  color: var(--sum-label, #6e6e73);
}
.t-hint {
  white-space: nowrap;
}
.t-chevron {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
  transition: transform 0.18s ease;
}
.trigger-btn.open .t-chevron {
  transform: rotate(90deg);
}

/* 面板：淡入 + 轻微位移，内嵌卡片质感 */
.coupon-panel {
  margin: 0.375rem 0 0.25rem;
  padding: 0.375rem;
  border: 1px solid var(--sum-border, #f0f0f0);
  border-radius: 10px;
  background: var(--sum-bg, #ffffff);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  max-height: 300px;
  overflow-y: auto;
}
.coupon-empty {
  margin: 0;
  padding: 0.875rem 0.5rem;
  font-size: 0.8125rem;
  color: var(--sum-label, #6e6e73);
  text-align: center;
}
.coupon-opt {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.625rem 0.625rem;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.coupon-opt:hover:not(.disabled) {
  background: var(--bg-gray);
}
.coupon-opt.active:not(.disabled) {
  background: rgba(255, 80, 0, 0.07);
}
.coupon-opt.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.coupon-opt input[type='radio'] {
  accent-color: var(--sum-save, #ff5000);
  flex-shrink: 0;
}
.opt-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}
.opt-name {
  font-size: 0.8125rem;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--sum-value, #1d1d1f);
}
.opt-cond {
  font-size: 0.75rem;
  color: var(--sum-label, #6e6e73);
}
.opt-right {
  flex-shrink: 0;
  font-size: 0.8125rem;
}
.opt-discount {
  color: var(--sum-save, #ff5000);
  font-weight: 700;
}
.opt-reason {
  color: var(--sum-label, #6e6e73);
  font-size: 0.75rem;
}
.none-opt {
  border-top: 1px dashed var(--sum-divider, #ececec);
  border-radius: 0 0 8px 8px;
}
.none-opt.active {
  background: rgba(52, 199, 89, 0.08);
}

/* 面板展开/收起 */
.panel-enter-active,
.panel-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}
.panel-enter-from,
.panel-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
