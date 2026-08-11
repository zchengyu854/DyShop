<script setup>
import { computed, ref } from 'vue'
import DashboardHeader from '@/components/admin/DashboardHeader.vue'
import MetricCard from '@/components/admin/MetricCard.vue'
import ChartSection from '@/components/admin/ChartSection.vue'
import { fetchOverview, fetchTrend } from '@/api/admin/stats'
import { toast } from '@/utils/toast'
import { fmtAmount, fmtCount, fmtDateTime } from '@/utils/formatters'

const RANGES = [
  { key: '7', label: '近7天' },
  { key: '30', label: '近30天' },
  { key: 'all', label: '全部历史' },
]

const overview = ref(null)
const trend = ref(null)
const range = ref('7')
const loading = ref(false)
const updatedAt = ref('')

function touch() {
  updatedAt.value = fmtDateTime(new Date())
}

async function loadOverview() {
  try {
    overview.value = await fetchOverview()
    touch()
  } catch (e) {
    toast.error(e.message || '概览加载失败')
  }
}

async function loadTrend() {
  loading.value = true
  try {
    trend.value = await fetchTrend(range.value)
    touch()
  } catch (e) {
    toast.error(e.message || '趋势加载失败')
  } finally {
    loading.value = false
  }
}

async function refresh() {
  loading.value = true
  try {
    const [ov, tr] = await Promise.all([fetchOverview(), fetchTrend(range.value)])
    overview.value = ov
    trend.value = tr
    touch()
    toast.success('数据已刷新')
  } catch (e) {
    toast.error(e.message || '刷新失败')
  } finally {
    loading.value = false
  }
}

function switchRange(key) {
  if (key === range.value) return
  range.value = key
  loadTrend()
}

function exportCsv() {
  const ov = overview.value
  const tr = trend.value
  if (!ov || !tr) {
    toast.error('暂无数据可导出')
    return
  }
  const rangeLabel = RANGES.find((r) => r.key === range.value)?.label || range.value
  const lines = [
    'dyshop 后台统计导出',
    `导出时间,${fmtDateTime(new Date())}`,
    '',
    '【概览】',
    '指标,数值',
    `今日订单数,${ov.todayOrderCount}`,
    `今日交易额(元),${ov.todayPaidAmount}`,
    `待支付订单,${ov.waitPayCount}`,
    `待发货订单,${ov.waitShipCount}`,
    `商品总数,${ov.productCount}`,
    `用户总数,${ov.userCount}`,
    '',
    `【趋势】时间范围,${rangeLabel}`,
    '日期,订单数,交易金额(元)',
  ]
  tr.dates.forEach((d, i) => {
    lines.push(`${d},${tr.orderCounts[i]},${tr.paidAmounts[i]}`)
  })
  const blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `dyshop-仪表盘-${fmtDateTime(new Date()).replace(/[:\s]/g, '')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  toast.success('导出数据成功')
}

const metrics = computed(() => [
  { label: '今日订单数', value: fmtCount(overview.value?.todayOrderCount), hint: '今日 00:00 起新下单', tone: 'blue' },
  { label: '今日交易额', value: '¥' + fmtAmount(overview.value?.todayPaidAmount), hint: '已支付订单合计', tone: 'green' },
  { label: '待支付订单', value: fmtCount(overview.value?.waitPayCount), hint: '等待用户支付', tone: 'orange' },
  { label: '待发货订单', value: fmtCount(overview.value?.waitShipCount), hint: '等待商家发货', tone: 'purple' },
  { label: '商品总数', value: fmtCount(overview.value?.productCount), hint: '在售与下架商品合计', tone: 'teal' },
  { label: '用户总数', value: fmtCount(overview.value?.userCount), hint: '注册用户数', tone: 'ink' },
])

const axisOption = (dates) => ({
  type: 'category',
  data: dates,
  axisTick: { show: false },
  axisLine: { lineStyle: { color: '#d2d2d7' } },
  axisLabel: { color: '#86868b' },
})

const orderOption = computed(() => {
  const data = trend.value
  if (!data) return null
  const graphic = data.orderCounts.some((n) => Number(n) > 0)
    ? undefined
    : { type: 'text', left: 'center', top: 'middle', style: { text: '暂无交易数据', fill: '#86868b', fontSize: 14 } }
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 20, top: 32, bottom: 44 },
    xAxis: axisOption(data.dates),
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#e8e8ed' } }, axisLabel: { color: '#86868b' } },
    series: [
      { name: '订单数', type: 'bar', data: data.orderCounts, barMaxWidth: 28, itemStyle: { borderRadius: [4, 4, 0, 0], color: '#0071e3' } },
    ],
    graphic,
  }
})

const amountOption = computed(() => {
  const data = trend.value
  if (!data) return null
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v) => '¥' + fmtAmount(v) },
    grid: { left: 64, right: 20, top: 32, bottom: 44 },
    xAxis: axisOption(data.dates),
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#e8e8ed' } }, axisLabel: { color: '#86868b' } },
    series: [
      { name: '交易金额', type: 'line', data: data.paidAmounts, smooth: true, symbolSize: 6, lineStyle: { width: 3, color: '#34c759' }, itemStyle: { color: '#34c759' }, areaStyle: { color: 'rgba(52,199,89,0.12)' } },
    ],
  }
})

loadTrend()
loadOverview()
</script>

<template>
  <div class="page">
    <DashboardHeader :updated-at="updatedAt" :loading="loading" @refresh="refresh" @export="exportCsv" />

    <div class="metric-grid">
      <MetricCard v-for="m in metrics" :key="m.label" v-bind="m" />
    </div>

    <section class="chart-area">
      <div class="chart-toolbar">
        <span class="toolbar-label">时间范围</span>
        <div class="range-tabs">
          <button
            v-for="r in RANGES"
            :key="r.key"
            class="range-tab"
            :class="{ active: range === r.key }"
            :disabled="loading"
            @click="switchRange(r.key)"
          >
            {{ r.label }}
          </button>
        </div>
      </div>
      <div class="chart-row">
        <ChartSection title="订单数趋势" :option="orderOption" />
        <ChartSection title="交易金额趋势" :option="amountOption" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: 100%;
  max-width: 85rem;
  margin: 0 auto;
  padding: 1.5rem;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(15rem, 1fr));
  gap: 1rem;
}
.chart-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}
.toolbar-label {
  font-size: 0.8125rem;
  color: var(--ink-secondary);
}
.range-tabs {
  display: flex;
  gap: 0.375rem;
}
.range-tab {
  height: 1.75rem;
  padding: 0 0.875rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--bg);
  color: var(--ink-secondary);
  font-size: 0.75rem;
  cursor: pointer;
}
.range-tab.active {
  border-color: var(--blue);
  background: rgba(0, 113, 227, 0.08);
  color: var(--blue);
  font-weight: 600;
}
.range-tab:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
@media (max-width: 1199.98px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(15rem, 1fr));
  }
  .chart-row {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 767.98px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
  .page {
    padding: 1rem;
  }
}
</style>
