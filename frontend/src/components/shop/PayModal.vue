<script setup>
import { ref } from 'vue'
import { payOrder } from '@/api/order'
import { toast } from '@/utils/toast'

const props = defineProps({
  order: { type: Object, required: true },
})

const emit = defineEmits(['success', 'close'])

const paying = ref(false)

async function confirm() {
  if (paying.value) return
  paying.value = true
  try {
    await payOrder(props.order.id)
    toast.success('支付成功')
    emit('success')
  } catch (e) {
    toast.error(e.message || '支付失败，请重试')
    emit('close')
  } finally {
    paying.value = false
  }
}
</script>

<template>
  <div class="mask">
    <div class="modal">
      <h3 class="title">模拟支付</h3>
      <p class="label">应付金额</p>
      <p class="amount">¥{{ Number(order.payAmount).toFixed(2) }}</p>
      <p class="hint">演示项目 · 点击「确认支付」即视为支付成功</p>
      <button class="pay-btn" :disabled="paying" @click="confirm">
        {{ paying ? '支付中…' : '确认支付' }}
      </button>
      <button class="later" @click="$emit('close')">稍后支付</button>
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
.modal {
  width: 360px;
  max-width: calc(100vw - 40px);
  padding: 24px;
  border-radius: 20px;
  background: var(--bg);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.25);
  text-align: center;
}
.title {
  margin: 0 0 14px;
  font-size: 17px;
  font-weight: 600;
}
.label {
  margin: 0 0 2px;
  font-size: 13px;
  color: var(--ink-secondary);
}
.amount {
  margin: 0 0 8px;
  font-size: 34px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.hint {
  margin: 0 0 18px;
  font-size: 12px;
  color: var(--ink-faint);
}
.pay-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--blue);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.pay-btn:hover:not(:disabled) {
  background: var(--blue-hover);
}
.pay-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.later {
  width: 100%;
  height: 40px;
  margin-top: 8px;
  border: none;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--ink-secondary);
  font-size: 14px;
  cursor: pointer;
}
.later:hover {
  color: var(--ink);
}
</style>
