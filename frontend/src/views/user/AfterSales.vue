<script setup>
import { onMounted, ref } from 'vue'
import { cancelAfterSale, fetchAfterSales } from '@/api/after-sale'
import { toast } from '@/utils/toast'

const TABS = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待处理' },
  { key: '2', label: '已退款' },
  { key: '3', label: '已拒绝' },
  { key: '4', label: '已取消' },
]

const STATUS_COLOR = { 0: '#ff9500', 1: '#0071e3', 2: '#34c759', 3: '#e5484d', 4: '#86868b' }

const tab = ref('all')
const page = ref(1)
const size = 10
const total = ref(0)
const records = ref([])
const loading = ref(false)

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (tab.value !== 'all') params.status = Number(tab.value)
    const data = await fetchAfterSales(params)
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

async function doCancel(row) {
  if (!window.confirm('确定取消该售后申请？')) return
  try {
    await cancelAfterSale(row.id)
    toast.success('已取消')
    load()
  } catch (e) {
    toast.error(e.message || '取消失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="after-sales">
    <div class="tabs">
      <button v-for="t in TABS" :key="t.key" class="tab" :class="{ active: tab === t.key }" @click="switchTab(t.key)">
        {{ t.label }}
      </button>
    </div>

    <div v-if="loading" class="state">加载中…</div>
    <div v-else-if="records.length === 0" class="state">暂无售后记录</div>

    <div v-else class="list">
      <div v-for="r in records" :key="r.id" class="card">
        <div class="card-main">
          <img v-if="r.productImage" :src="r.productImage" :alt="r.productName" class="thumb" loading="lazy" />
          <div class="info">
            <p class="name">{{ r.productName }}</p>
            <p v-if="r.specText" class="spec">{{ r.specText }}</p>
            <p class="meta">
              售后单 {{ r.afterSaleNo }} · {{ fmtTime(r.createTime) }}<br />
              原因：{{ r.reason }}
              <span v-if="r.rejectReason" class="reject">（拒绝理由：{{ r.rejectReason }}）</span>
            </p>
          </div>
          <div class="right">
            <p class="amount">退款 ¥{{ Number(r.refundAmount).toFixed(2) }}</p>
            <p class="status" :style="{ color: STATUS_COLOR[r.status] }">{{ r.statusText }}</p>
            <button v-if="r.status === 0" class="cancel-btn" @click="doCancel(r)">取消申请</button>
          </div>
        </div>
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
  flex-wrap: wrap;
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
.card {
  padding: 16px;
  background: var(--bg);
  border: 1px solid var(--border-line);
  border-radius: var(--radius-card);
}
.card-main {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.thumb {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}
.info {
  flex: 1;
  min-width: 0;
}
.name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}
.spec {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-secondary);
}
.meta {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-faint);
}
.reject {
  color: #e5484d;
}
.right {
  flex-shrink: 0;
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-end;
}
.amount {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #ff5000;
}
.status {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
}
.cancel-btn {
  padding: 4px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--bg);
  color: var(--ink-secondary);
  font-size: 12px;
  cursor: pointer;
}
.cancel-btn:hover {
  color: #e5484d;
  border-color: #e5484d;
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
