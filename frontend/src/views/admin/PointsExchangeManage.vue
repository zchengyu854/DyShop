<script setup>
import { onMounted, ref } from 'vue'
import { fetchPointsExchanges, fetchPointsGoods } from '@/api/admin/points'
import { toast } from '@/utils/toast'

const keyword = ref('')
const goodsFilter = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const records = ref([])
const loading = ref(false)

// 兑换商品筛选下拉
const goodsOptions = ref([])

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (goodsFilter.value) params.goodsId = Number(goodsFilter.value)
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await fetchPointsExchanges(params)
    records.value = data.records
    total.value = Number(data.total)
  } catch (e) {
    toast.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
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

onMounted(async () => {
  load()
  try {
    const data = await fetchPointsGoods({ page: 1, size: 100 })
    goodsOptions.value = data.records
  } catch {
    goodsOptions.value = []
  }
})
</script>

<template>
  <div class="page admin-page">
    <div class="head">
      <h1 class="title">积分兑换记录</h1>
      <router-link class="op link-op" to="/admin/points-goods">商品管理</router-link>
    </div>

    <div class="toolbar">
      <input v-model="keyword" class="input" type="text" placeholder="用户名 / 昵称" @keyup.enter="search" />
      <select v-model="goodsFilter" class="input select" @change="search">
        <option value="">全部商品</option>
        <option v-for="g in goodsOptions" :key="g.id" :value="g.id">{{ g.name }}</option>
      </select>
      <button class="op" @click="search">搜索</button>
      <button v-if="keyword || goodsFilter" class="op" @click="keyword = ''; goodsFilter = ''; search()">重置</button>
    </div>

    <div v-if="loading" class="hint">加载中…</div>
    <div v-else-if="records.length === 0" class="hint">暂无兑换记录</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>兑换单号</th>
          <th>用户</th>
          <th>商品</th>
          <th>类型</th>
          <th>消耗积分</th>
          <th>兑换码</th>
          <th>优惠券ID</th>
          <th>兑换时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="e in records" :key="e.id">
          <td class="mono">{{ e.exchangeNo }}</td>
          <td>
            {{ e.nickname || '—' }}
            <span class="sub-name">{{ e.username }}</span>
          </td>
          <td>{{ e.goodsName }}</td>
          <td>
            <span class="type-tag" :class="e.goodsType === 'COUPON' ? 't-coupon' : 't-code'">
              {{ e.goodsType === 'COUPON' ? '优惠券' : '兑换码' }}
            </span>
          </td>
          <td class="mono">{{ e.pointCost }}</td>
          <td class="mono">{{ e.code || '-' }}</td>
          <td class="mono">{{ e.couponId || '-' }}</td>
          <td class="mono">{{ fmtTime(e.createTime) }}</td>
        </tr>
      </tbody>
    </table>

    <div v-if="records.length" class="pager">
      <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span class="page-info">{{ page }} / {{ totalPages() }}（共 {{ total }} 条）</span>
      <button class="page-btn" :disabled="page >= totalPages()" @click="goPage(page + 1)">下一页</button>
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
  margin-right: auto;
}
.link-op {
  text-decoration: none;
}
.sub-name {
  margin-left: 6px;
  font-size: 12px;
  color: var(--ink-faint);
}
.type-tag {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
}
.t-coupon {
  color: #0071e3;
  background: rgba(0, 113, 227, 0.08);
}
.t-code {
  color: #ff9500;
  background: rgba(255, 149, 0, 0.12);
}
</style>