<script setup>
import { onMounted, ref } from 'vue'
import { fetchUserCoupons, voidUserCoupon } from '@/api/admin/coupon'
import { toast } from '@/utils/toast'

const TABS = [
  { key: 'all', label: '全部' },
  { key: '0', label: '未使用' },
  { key: '1', label: '已使用' },
  { key: '2', label: '已过期/作废' },
]

const STATUS_COLOR = { 0: '#34c759', 1: '#86868b', 2: '#ff9500' }
const STATUS_TEXT = { 0: '未使用', 1: '已使用', 2: '已过期' }

const tab = ref('all')
const keyword = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const records = ref([])
const loading = ref(false)
const voidTarget = ref(null)

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
    const data = await fetchUserCoupons(params)
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

async function doVoid() {
  const target = voidTarget.value
  voidTarget.value = null
  try {
    await voidUserCoupon(target.id)
    toast.success('已作废')
    load()
  } catch (e) {
    toast.error(e.message || '作废失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page admin-page">
    <div class="head">
      <h1 class="title">用户优惠券</h1>
      <div class="tabs">
        <button v-for="t in TABS" :key="t.key" class="tab" :class="{ active: tab === t.key }" @click="switchTab(t.key)">
          {{ t.label }}
        </button>
      </div>
    </div>

    <div class="toolbar">
      <input v-model="keyword" class="input" type="text" placeholder="用户名 / 手机号" @keyup.enter="search" />
      <button class="op" @click="search">搜索</button>
      <button v-if="keyword" class="op" @click="keyword = ''; search()">重置</button>
    </div>

    <div v-if="loading" class="hint">加载中…</div>
    <div v-else-if="records.length === 0" class="hint">暂无用户券</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>用户</th>
          <th>券名称</th>
          <th>来源</th>
          <th>状态</th>
          <th>领取/发放时间</th>
          <th>到期时间</th>
          <th>使用订单</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="u in records" :key="u.id">
          <td class="mono">{{ u.id }}</td>
          <td>
            {{ u.username }}
            <span class="phone mono">{{ u.phone || '' }}</span>
          </td>
          <td>{{ u.templateName }}</td>
          <td>{{ u.source === 'CENTER' ? '领取' : '发放' }}</td>
          <td>
            <span class="status" :style="{ color: STATUS_COLOR[u.status], background: STATUS_COLOR[u.status] + '1a' }">
              {{ STATUS_TEXT[u.status] }}
            </span>
          </td>
          <td class="mono">{{ fmtTime(u.receivedAt) }}</td>
          <td class="mono">{{ fmtTime(u.expireAt) }}</td>
          <td class="mono">{{ u.usedOrderId || '-' }}</td>
          <td class="ops">
            <button v-if="u.status === 0" class="op danger" @click="voidTarget = u">作废</button>
            <span v-else class="noop">-</span>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="records.length" class="pager">
      <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span class="page-info">{{ page }} / {{ totalPages() }}（共 {{ total }} 张）</span>
      <button class="page-btn" :disabled="page >= totalPages()" @click="goPage(page + 1)">下一页</button>
    </div>

    <div v-if="voidTarget" class="mask" @click.self="voidTarget = null">
      <div class="confirm">
        <h3 class="confirm-title">作废用户券</h3>
        <p class="confirm-desc">
          确定作废「{{ voidTarget.templateName }}」（{{ voidTarget.username }}）？作废后不可使用。
        </p>
        <div class="confirm-ops">
          <button class="confirm-btn cancel" @click="voidTarget = null">取消</button>
          <button class="confirm-btn primary" @click="doVoid">确定作废</button>
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
.phone {
  margin-left: 6px;
  font-size: 12px;
  color: var(--ink-faint);
}
.op.danger {
  color: #e5484d;
}
.noop {
  color: var(--ink-faint);
  font-size: 13px;
}
</style>
