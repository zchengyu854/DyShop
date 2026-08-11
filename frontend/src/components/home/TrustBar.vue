<script setup>
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'

const props = defineProps({
  // 在售商品总数（真实接口 total）
  total: { type: Number, default: 0 },
  categoryCount: { type: Number, default: 0 },
})

const sold = shallowRef(0)
const cats = shallowRef(0)
const bar = ref(null)
let observer = null
let raf = null

function easeOutExpo(t) {
  return t === 1 ? 1 : 1 - Math.pow(2, -10 * t)
}

function animate(field, to, from = 0, duration = 900) {
  const start = performance.now()
  const step = (now) => {
    const p = Math.min(1, (now - start) / duration)
    field.value = Math.round(from + (to - from) * easeOutExpo(p))
    if (p < 1 && to > 0) raf = requestAnimationFrame(step)
  }
  raf = requestAnimationFrame(step)
}

onMounted(() => {
  if (!('IntersectionObserver' in window) || !bar.value) {
    sold.value = props.total
    cats.value = props.categoryCount
    return
  }
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          animate(sold, props.total)
          animate(cats, props.categoryCount)
          observer.disconnect()
        }
      })
    },
    { threshold: 0.4 }
  )
  observer.observe(bar.value)
})

onBeforeUnmount(() => {
  if (raf) cancelAnimationFrame(raf)
  observer?.disconnect()
})
</script>

<template>
  <div ref="bar" class="trust-bar">
    <div class="trust-inner">
      <div class="trust-item">
        <p class="trust-num">{{ sold }}</p>
        <p class="trust-label">件在售商品</p>
      </div>
      <div class="trust-item">
        <p class="trust-num">{{ cats }}</p>
        <p class="trust-label">个精选分类</p>
      </div>
      <div class="trust-item">
        <p class="trust-num">48h</p>
        <p class="trust-label">极速发货</p>
      </div>
      <div class="trust-item">
        <p class="trust-num">7天</p>
        <p class="trust-label">无理由退换</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.trust-bar {
  background: var(--bg);
  border-top: 1px solid var(--border-line);
  border-bottom: 1px solid var(--border-line);
  padding: 28px 20px;
}
.trust-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.trust-item {
  text-align: center;
}
.trust-num {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.trust-label {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--ink-secondary);
}
@media (max-width: 640px) {
  .trust-inner {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px 16px;
  }
}
</style>