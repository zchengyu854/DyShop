<script setup>
// 15 分钟付款倒计时（UX 层）：
// - 基准 = 后端返回的 payDeadline（服务端按订单创建时间计算，不依赖前端本地时间）
// - setInterval(1s) + 每次 tick 以 Date.now() 重新计算剩余，避免后台标签页节流导致的累计漂移
// - 归零后一次性 emit('expired') 并自停
// 注意：前端倒计时仅供体验；库存释放必须由后端兜底（延迟队列 / Redis 过期 / 定时扫描），
// 在 payDeadline 到达时自动取消订单并回补库存。
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  deadline: { type: String, required: true },
  // 紧凑模式：用于订单列表卡片，缩小内边距与字号
  compact: { type: Boolean, default: false },
})

const emit = defineEmits(['expired'])

const now = ref(Date.now())
let timer = null
// 修复（2026-08）：expiredFired 必须是 ref —— 此前误用普通布尔量，
// tick() 中 expiredFired.value = true 会对 false 直接写 .value，
// 抛出 "Cannot create property 'value' on boolean 'false'"，导致 Vue 调度器
// 中断 flush 队列、整页响应式冻结（表现为订单页所有按钮不可点击）。
const expiredFired = ref(false)

const remainingMs = computed(() => {
  const target = new Date(props.deadline).getTime()
  return Math.max(0, target - now.value)
})
const expired = computed(() => remainingMs.value <= 0)
const warning = computed(() => !expired.value && remainingMs.value <= 3 * 60 * 1000)

const mm = computed(() => String(Math.floor(remainingMs.value / 60000)).padStart(2, '0'))
const ss = computed(() => String(Math.floor((remainingMs.value % 60000) / 1000)).padStart(2, '0'))

function tick() {
  now.value = Date.now()
  if (expired.value && !expiredFired.value) {
    expiredFired.value = true
    clearInterval(timer)
    timer = null
    emit('expired')
  }
}

onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div
    class="countdown"
    :class="{ warning, compact: props.compact }"
    role="timer"
    aria-live="polite"
    :aria-label="expired ? '订单已超时取消' : `付款剩余时间 ${mm} 分 ${ss} 秒，超时将自动取消`"
  >
    <svg class="clock" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3.5 2" stroke-linecap="round" stroke-linejoin="round" />
    </svg>
    <span v-if="!expired" class="text">
      剩余 <strong class="time">{{ mm }}:{{ ss }}</strong> 自动取消
    </span>
    <span v-else class="text">订单已超时取消</span>
  </div>
</template>

<style scoped>
.countdown {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  border-radius: var(--radius-full);
  background: rgba(255, 149, 0, 0.1);
  color: #ff9500;
  font-size: 0.9375rem;
  font-variant-numeric: tabular-nums;
  transition: background 0.2s ease, color 0.2s ease;
}
.countdown.warning {
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
}
.countdown.compact {
  padding: 0.375rem 0.75rem;
  font-size: 0.8125rem;
  border-radius: 0.625rem;
}
.countdown.compact .clock {
  width: 0.9375rem;
  height: 0.9375rem;
}
.countdown.compact .time {
  font-size: 0.875rem;
}
.clock {
  width: 1.125rem;
  height: 1.125rem;
}
.text {
  line-height: 1;
}
.time {
  font-weight: 700;
  font-size: 1.0625rem;
}
</style>
