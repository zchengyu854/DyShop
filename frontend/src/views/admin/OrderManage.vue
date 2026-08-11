<script setup>
import { ref } from 'vue'
import { fetchOrders, shipOrder } from '@/api/admin/order'
import { useDataRefresh } from '@/composables/useDataRefresh'
import { ORDER_NS } from '@/utils/dataSync'
import { toast } from '@/utils/toast'

const TABS = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待支付' },
  { key: '1', label: '待发货' },
  { key: '2', label: '待收货' },
  { key: '3', label: '已完成' },
  { key: '4', label: '已取消' },
]

const STATUS_COLOR = {
  0: '#ff9500',
  1: '#0071e3',
  2: '#0071e3',
  3: '#34c759',
  4: '#86868b',
}

const tab = ref('all')
const keyword = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const records = ref([])
const loading = ref(false)
const detail = ref(null)
const shipTarget = ref(null)

function format(amount) {
  return Number(amount).toFixed(2)
}

function fmtTime(time) {
  if (!time) return '-'
  return String(time).replace('T', ' ').slice(0, 19)
}

async function load({ silent = false } = {}) {
  if (!silent) loading.value = true
  try {
    const status = tab.value === 'all' ? undefined : Number(tab.value)
    const params = { status, page: page.value, size }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await fetchOrders(params)
    records.value = data.records
    total.value = Number(data.total)
  } catch (e) {
    toast.error(e.message || '订单加载失败')
  } finally {
    if (!silent) loading.value = false
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
  if (p < 1 || p > totalPages.value) return
  page.value = p
  load()
}

const totalPages = () => Math.max(1, Math.ceil(total.value / size))

async function doShip() {
  const target = shipTarget.value
  shipTarget.value = null
  try {
    await shipOrder(target.id)
    toast.success('已发货')
    load()
  } catch (e) {
    toast.error(e.message || '发货失败')
  }
}

// 首次加载 + C 端下单/支付/取消/确认成功广播 order 失效事件时自动同步最新列表
useDataRefresh(ORDER_NS, load)
</script>

<template>
  <div class="page admin-page">
    <div class="head">
      <h1 class="title">订单管理</h1>
      <div class="tabs">
        <button
          v-for="t in TABS"
          :key="t.key"
          class="tab"
          :class="{ active: tab === t.key }"
          @click="switchTab(t.key)"
        >
          {{ t.label }}
        </button>
      </div>
    </div>

    <div class="toolbar">
      <input
        v-model="keyword"
        class="input"
        type="text"
        placeholder="订单号 / 收货手机号"
        @keyup.enter="search"
      />
      <button class="op" @click="search">搜索</button>
      <button v-if="keyword" class="op" @click="keyword = ''; search()">重置</button>
    </div>

    <div v-if="loading" class="hint">加载中…</div>
    <div v-else-if="records.length === 0" class="hint">暂无订单</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>订单号</th>
          <th>用户</th>
          <th>收货手机</th>
          <th>金额</th>
          <th>状态</th>
          <th>下单时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="o in records" :key="o.id">
          <td class="mono">{{ o.orderNo }}</td>
          <td>{{ o.userName }}</td>
          <td class="mono">{{ o.receiverPhone }}</td>
          <td class="mono">¥{{ format(o.payAmount) }}</td>
          <td>
            <span class="status" :style="{ color: STATUS_COLOR[o.status], background: STATUS_COLOR[o.status] + '1a' }">
              {{ o.statusText }}
            </span>
          </td>
          <td class="mono">{{ fmtTime(o.createTime) }}</td>
          <td class="ops">
            <button class="op" @click="detail = o">详情</button>
            <button v-if="o.status === 1" class="op primary" @click="shipTarget = o">发货</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="records.length" class="pager">
      <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span class="page-info">{{ page }} / {{ totalPages() }}（共 {{ total }} 单）</span>
      <button class="page-btn" :disabled="page >= totalPages()" @click="goPage(page + 1)">下一页</button>
    </div>

    <!-- 详情抽屉 -->
    <div v-if="detail" class="mask" @click.self="detail = null">
      <aside class="drawer detail">
        <div class="drawer-head">
          <h2>订单详情</h2>
          <button class="close" @click="detail = null">×</button>
        </div>
        <p class="mono no">订单号 {{ detail.orderNo }}</p>
        <div class="row"><span>下单用户</span><span>{{ detail.userName }}</span></div>
        <div class="row"><span>状态</span><span>{{ detail.statusText }}</span></div>
        <div class="row"><span>应付金额</span><span>¥{{ format(detail.payAmount) }}</span></div>
        <div class="row"><span>备注</span><span>{{ detail.remark || '-' }}</span></div>
        <h3 class="section">收货信息</h3>
        <div class="row"><span>收货人</span><span>{{ detail.receiverName }}</span></div>
        <div class="row"><span>手机号</span><span class="mono">{{ detail.receiverPhone }}</span></div>
        <div class="row addr"><span>地址</span><span>{{ detail.receiverAddr }}</span></div>
        <h3 class="section">商品明细</h3>
        <div v-for="it in detail.items" :key="it.productId" class="item-line">
          <img :src="it.productImage" :alt="it.productName" class="item-img" />
          <div class="item-info">
            <p class="item-name">{{ it.productName }}</p>
            <p v-if="it.specText" class="item-spec">{{ it.specText }}</p>
            <p class="item-meta">¥{{ format(it.price) }} × {{ it.quantity }}</p>
          </div>
          <span class="item-total">¥{{ format(it.subtotal) }}</span>
        </div>
        <h3 class="section">时间线</h3>
        <div class="row"><span>下单</span><span>{{ fmtTime(detail.createTime) }}</span></div>
        <div class="row"><span>支付</span><span>{{ fmtTime(detail.payTime) }}</span></div>
        <div class="row"><span>发货</span><span>{{ fmtTime(detail.shipTime) }}</span></div>
        <div class="row"><span>完成</span><span>{{ fmtTime(detail.finishTime) }}</span></div>
        <div class="row"><span>取消</span><span>{{ fmtTime(detail.cancelTime) }}</span></div>
        <button
          v-if="detail.status === 1"
          class="ship-btn"
          @click="detail = null; shipTarget = { id: detail.id }"
        >
          发货
        </button>
      </aside>
    </div>

    <!-- 发货确认 -->
    <div v-if="shipTarget" class="mask" @click.self="shipTarget = null">
      <div class="confirm">
        <h3 class="confirm-title">确认发货</h3>
        <p class="confirm-desc">确认后订单将变为「待收货」，确定发货吗？</p>
        <div class="confirm-ops">
          <button class="confirm-btn cancel" @click="shipTarget = null">再想想</button>
          <button class="confirm-btn primary" @click="doShip">确定发货</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 共享样式见 assets/admin.css（.admin-page 包装类），此处仅保留页面独有样式 */
.tab {
  height: 2rem;
  padding: 0 0.875rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--bg);
  color: var(--ink-secondary);
  font-size: 0.8125rem;
  cursor: pointer;
}
.tab.active {
  border-color: var(--blue);
  background: rgba(0, 113, 227, 0.08);
  color: var(--blue);
  font-weight: 600;
}
.drawer.detail {
  width: 28.75rem;
}
.no {
  margin: 0.75rem 0 0.5rem;
  font-size: 0.8125rem;
  color: var(--ink-faint);
}
.row {
  display: flex;
  justify-content: space-between;
  gap: 1.25rem;
  padding: 0.375rem 0;
  font-size: 0.875rem;
}
.row > span:first-child {
  color: var(--ink-secondary);
  flex-shrink: 0;
}
.row.addr > span:last-child {
  text-align: right;
}
.section {
  margin: 1.125rem 0 0.5rem;
  font-size: 0.9375rem;
  font-weight: 600;
}
.item-line {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--border-line);
}
.item-img {
  width: 3rem;
  height: 3rem;
  border-radius: 0.5rem;
  object-fit: cover;
  background: var(--bg-gray);
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  margin: 0;
  font-size: 0.875rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-spec {
  margin: 0.1875rem 0 0;
  font-size: 0.75rem;
  color: var(--ink-secondary);
}
.item-spec::before {
  content: "规格：";
  color: var(--ink-faint);
}
.item-meta {
  margin: 0.1875rem 0 0;
  font-size: 0.75rem;
  color: var(--ink-secondary);
}
.item-total {
  font-size: 0.875rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.ship-btn {
  width: 100%;
  height: 2.625rem;
  margin-top: 1.25rem;
  border: none;
  border-radius: 0.625rem;
  background: var(--blue);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
}
.ship-btn:hover {
  background: var(--blue-hover);
}
</style>