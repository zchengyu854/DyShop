<script setup>
import { onMounted, ref } from 'vue'
import { fetchMyCoupons } from '@/api/coupon'
import { toast } from '@/utils/toast'

const TABS = [
  { key: '0', label: '未使用' },
  { key: '1', label: '已使用' },
  { key: '2', label: '已过期' },
]

const STATUS_COLOR = { 0: '#ff9500', 1: '#86868b', 2: '#c7c7cc' }
const STATUS_TEXT = { 0: '可使用', 1: '已使用', 2: '已过期' }

const tab = ref('0')
const page = ref(1)
const size = 10
const total = ref(0)
const records = ref([])
const loading = ref(false)

function fmtDate(t) {
  if (!t) return '长期有效'
  return String(t).replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    const data = await fetchMyCoupons({ status: Number(tab.value), page: page.value, size })
    records.value = data.records
    total.value = Number(data.total)
  } catch (e) {
    toast.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function switchTab(key) {
  tab.value = key
  page.value = 1
  load()
}

function goPage(p) {
  if (p < 1 || p > totalPages()) return
  page.value = p
  load()
}

const totalPages = () => Math.max(1, Math.ceil(total.value / size))

onMounted(load)
</script>

<template>
  <div class="my-coupons">
    <div class="tabs">
      <button v-for="t in TABS" :key="t.key" class="tab" :class="{ active: tab === t.key }" @click="switchTab(t.key)">
        {{ t.label }}
      </button>
    </div>

    <div v-if="loading" class="state">加载中…</div>
    <div v-else-if="records.length === 0" class="state">
      暂无{{ TABS.find((t) => t.key === tab)?.label }}优惠券
    </div>

    <div v-else class="list">
      <div v-for="c in records" :key="c.id" class="coupon">
        <div class="coupon-main">
          <p class="amount">-¥{{ Number(c.discountAmount).toFixed(0) }}</p>
          <div class="info">
            <p class="name">{{ c.name }}</p>
            <p class="cond">{{ Number(c.minAmount) > 0 ? `满 ¥${Number(c.minAmount)} 可用` : '无门槛' }}</p>
            <p class="meta">有效期至 {{ fmtDate(c.expireAt) }} · {{ c.source === 'CENTER' ? '领取' : '发放' }}</p>
          </div>
        </div>
        <span class="status" :style="{ color: STATUS_COLOR[c.status] }">{{ STATUS_TEXT[c.status] }}</span>
      </div>
    </div>

    <div v-if="records.length && totalPages() > 1" class="pager">
      <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span class="page-info">{{ page }} / {{ totalPages() }}</span>
      <button class="page-btn" :disabled="page >= totalPages()" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.tab {
  height: 2.25rem;
  padding: 0 1rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--bg);
  color: var(--ink-secondary);
  font-size: 13px;
  cursor: pointer;
}
.tab.active {
  border-color: var(--blue);
  background: rgba(0, 113, 227, 0.08);
  color: var(--blue);
  font-weight: 600;
}
.state {
  padding: 48px 0;
  text-align: center;
  color: var(--ink-faint);
  font-size: 14px;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.coupon {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--bg);
  border: 1px solid var(--border-line);
  border-radius: var(--radius-card);
}
.coupon-main {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}
.amount {
  margin: 0;
  min-width: 88px;
  text-align: center;
  font-size: 26px;
  font-weight: 700;
  color: #ff5000;
  font-variant-numeric: tabular-nums;
}
.info {
  min-width: 0;
}
.name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cond {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--ink-secondary);
}
.meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-faint);
}
.status {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
}
.page-btn {
  height: 2rem;
  padding: 0 0.875rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--bg);
  color: var(--ink);
  font-size: 13px;
  cursor: pointer;
}
.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.page-info {
  font-size: 13px;
  color: var(--ink-secondary);
}
</style>
