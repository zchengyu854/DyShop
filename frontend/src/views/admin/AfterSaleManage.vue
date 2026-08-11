<script setup>
import { onMounted, ref } from 'vue'
import { approveAfterSale, fetchAdminAfterSales, rejectAfterSale } from '@/api/admin/after-sale'
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
const keyword = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const records = ref([])
const loading = ref(false)

const auditTarget = ref(null) // { type: 'approve' | 'reject', row }
const rejectReason = ref('')
const auditBusy = ref(false)

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (tab.value !== 'all') params.status = Number(tab.value)
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await fetchAdminAfterSales(params)
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

function search() {
  page.value = 1
  load()
}

function goPage(p) {
  if (p < 1 || p > totalPages()) return
  page.value = p
  load()
}

const totalPages = () => Math.max(1, Math.ceil(total.value / size))

function openApprove(row) {
  auditTarget.value = { type: 'approve', row }
}

function openReject(row) {
  auditTarget.value = { type: 'reject', row }
  rejectReason.value = ''
}

async function doAudit() {
  if (auditBusy.value) return
  const target = auditTarget.value
  if (target.type === 'reject' && !rejectReason.value.trim()) {
    return toast.error('请填写拒绝理由')
  }
  auditBusy.value = true
  try {
    if (target.type === 'approve') {
      await approveAfterSale(target.row.id)
      toast.success('已同意，模拟退款完成')
    } else {
      await rejectAfterSale(target.row.id, rejectReason.value.trim())
      toast.success('已拒绝')
    }
    auditTarget.value = null
    load()
  } catch (e) {
    toast.error(e.message || '操作失败')
  } finally {
    auditBusy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page admin-page">
    <div class="head">
      <h1 class="title">售后管理</h1>
      <div class="tabs">
        <button v-for="t in TABS" :key="t.key" class="tab" :class="{ active: tab === t.key }" @click="switchTab(t.key)">
          {{ t.label }}
        </button>
      </div>
    </div>

    <div class="toolbar">
      <input v-model="keyword" class="input" type="text" placeholder="订单号 / 用户名 / 商品名" @keyup.enter="search" />
      <button class="op" @click="search">搜索</button>
      <button v-if="keyword" class="op" @click="keyword = ''; search()">重置</button>
    </div>

    <div v-if="loading" class="hint">加载中…</div>
    <div v-else-if="records.length === 0" class="hint">暂无售后单</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>售后单号</th>
          <th>订单号</th>
          <th>用户</th>
          <th>商品</th>
          <th>退款金额</th>
          <th>原因</th>
          <th>状态</th>
          <th>申请时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in records" :key="r.id">
          <td class="mono">{{ r.id }}</td>
          <td class="mono">{{ r.afterSaleNo }}</td>
          <td class="mono">{{ r.orderNo }}</td>
          <td>{{ r.username }}</td>
          <td>
            {{ r.productName }}
            <span v-if="r.specText" class="cell-sub">{{ r.specText }}</span>
          </td>
          <td class="mono amount">¥{{ Number(r.refundAmount).toFixed(2) }}</td>
          <td class="cell-reason">{{ r.reason }}<span v-if="r.rejectReason" class="reject">（{{ r.rejectReason }}）</span></td>
          <td>
            <span class="status" :style="{ color: STATUS_COLOR[r.status], background: STATUS_COLOR[r.status] + '1a' }">
              {{ r.statusText }}
            </span>
          </td>
          <td class="mono">{{ fmtTime(r.createTime) }}</td>
          <td class="ops">
            <template v-if="r.status === 0">
              <button class="op" @click="openApprove(r)">同意退款</button>
              <button class="op danger" @click="openReject(r)">拒绝</button>
            </template>
            <span v-else class="noop">-</span>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="records.length" class="pager">
      <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span class="page-info">{{ page }} / {{ totalPages() }}（共 {{ total }} 单）</span>
      <button class="page-btn" :disabled="page >= totalPages()" @click="goPage(page + 1)">下一页</button>
    </div>

    <!-- 审核弹窗 -->
    <div v-if="auditTarget" class="mask" @click.self="auditTarget = null">
      <div class="confirm">
        <h3 class="confirm-title">{{ auditTarget.type === 'approve' ? '同意退款' : '拒绝售后' }}</h3>
        <p class="confirm-desc">
          售后单 {{ auditTarget.row.afterSaleNo }} · {{ auditTarget.row.productName }}
          · 退款 ¥{{ Number(auditTarget.row.refundAmount).toFixed(2) }}
          <template v-if="auditTarget.type === 'approve'">，确认同意并模拟退款？</template>
        </p>
        <textarea
          v-if="auditTarget.type === 'reject'"
          v-model="rejectReason"
          class="reject-input"
          rows="3"
          maxlength="200"
          placeholder="请填写拒绝理由（必填）"
        ></textarea>
        <div class="confirm-ops">
          <button class="confirm-btn cancel" @click="auditTarget = null">取消</button>
          <button
            class="confirm-btn"
            :class="auditTarget.type === 'reject' ? 'danger' : 'primary'"
            :disabled="auditBusy"
            @click="doAudit"
          >
            {{ auditBusy ? '处理中…' : auditTarget.type === 'approve' ? '同意退款' : '确认拒绝' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title {
  margin: 0;
}
.cell-sub {
  display: block;
  font-size: 12px;
  color: var(--ink-faint);
}
.cell-reason {
  max-width: 180px;
  font-size: 12px;
  color: var(--ink-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.reject {
  color: #e5484d;
}
.amount {
  color: #ff5000;
  font-weight: 600;
}
.op.danger {
  color: #e5484d;
}
.noop {
  color: var(--ink-faint);
  font-size: 13px;
}
.reject-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid var(--border-line);
  border-radius: var(--radius);
  font-size: 13px;
  font-family: inherit;
  resize: vertical;
  background: var(--bg);
  color: var(--ink);
}
</style>
