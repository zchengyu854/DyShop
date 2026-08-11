<script setup>
import { onMounted, ref } from 'vue'
import { fetchDashboardStats, fetchMemberPoints } from '@/api/user'
import { toast } from '@/utils/toast'

const records = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const balance = ref(null)

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

const totalPages = () => Math.max(1, Math.ceil(total.value / size))

async function load() {
  loading.value = true
  try {
    const data = await fetchMemberPoints({ page: page.value, size })
    records.value = data.records
    total.value = Number(data.total)
  } catch (e) {
    toast.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function goPage(p) {
  if (p < 1 || p > totalPages()) return
  page.value = p
  load()
}

onMounted(async () => {
  load()
  try {
    const stats = await fetchDashboardStats()
    balance.value = stats?.points ?? null
  } catch {
    // 余额展示失败不影响流水列表
  }
})
</script>

<template>
  <div class="points-detail">
    <div class="head">
      <h2 class="title">积分明细</h2>
      <router-link to="/user/points-mall" class="mall-link">积分商城 ›</router-link>
    </div>

    <div v-if="loading && !records.length" class="state">加载中…</div>
    <div v-else-if="records.length === 0" class="state">暂无积分流水，完成订单确认收货后赠送积分</div>

    <div v-else class="list">
      <div v-for="(log, i) in records" :key="i" class="row">
        <div class="row-main">
          <p class="remark">{{ log.remark }}</p>
          <p class="time">{{ fmtTime(log.createTime) }}</p>
        </div>
        <p class="delta" :class="Number(log.points) >= 0 ? 'plus' : 'minus'">
          {{ Number(log.points) >= 0 ? '+' : '' }}{{ Number(log.points).toLocaleString('zh-CN') }}
        </p>
        <p class="after">余额 {{ Number(log.balance).toLocaleString('zh-CN') }}</p>
      </div>
    </div>

    <div v-if="records.length" class="pager">
      <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span class="page-info">{{ page }} / {{ totalPages() }}（共 {{ total }} 条）</span>
      <button class="page-btn" :disabled="page >= totalPages()" @click="goPage(page + 1)">下一页</button>
    </div>

    <div class="foot">
      <p class="foot-label">当前可用积分</p>
      <p class="foot-value">{{ balance == null ? '—' : Number(balance).toLocaleString('zh-CN') }}</p>
      <p class="foot-hint">积分自到账起 12 个月有效，过期批次将在每日凌晨自动清零</p>
    </div>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}
.mall-link {
  font-size: 13px;
  color: var(--ink-secondary);
  text-decoration: none;
}
.mall-link:hover {
  color: var(--blue);
}
.state {
  padding: 48px 0;
  text-align: center;
  color: var(--ink-faint);
  font-size: 14px;
}
.list {
  background: var(--bg);
  border: 1px solid var(--border-line);
  border-radius: var(--radius-card);
  overflow: hidden;
}
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-line);
}
.row:last-child {
  border-bottom: none;
}
.row-main {
  min-width: 0;
  flex: 1;
}
.remark {
  margin: 0;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.time {
  margin: 3px 0 0;
  font-size: 12px;
  color: var(--ink-faint);
}
.delta {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.plus {
  color: #ff5000;
}
.minus {
  color: var(--ink-secondary);
}
.after {
  margin: 0;
  min-width: 88px;
  text-align: right;
  font-size: 12px;
  color: var(--ink-faint);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 18px;
}
.page-btn {
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
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
.foot {
  margin-top: 18px;
  padding: 16px 22px;
  border-radius: var(--radius-card);
  background: linear-gradient(120deg, #fff7ec, #fff);
  border: 1px solid #ffe2bd;
  text-align: center;
}
.foot-label {
  margin: 0;
  font-size: 12px;
  color: var(--ink-secondary);
}
.foot-value {
  margin: 4px 0 0;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  color: #ff5000;
  font-variant-numeric: tabular-nums;
}
.foot-hint {
  margin: 6px 0 0;
  font-size: 11px;
  color: var(--ink-faint);
}
</style>