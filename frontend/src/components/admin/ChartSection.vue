<script setup>
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'

echarts.use([BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  title: { type: String, required: true },
  option: { type: Object, default: null },
})

const chartRef = shallowRef(null)
let chart = null
let resizeHandler = null

function bindRef(el) {
  chartRef.value = el
}

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  resizeHandler = () => chart?.resize()
  window.addEventListener('resize', resizeHandler)
}

function setOption(option) {
  if (!chart && chartRef.value) initChart()
  chart?.setOption(option, { notMerge: true })
}

onMounted(initChart)

onBeforeUnmount(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  if (chart) {
    chart.dispose()
    chart = null
  }
})

const pendingOption = ref(props.option)

watch(
  () => props.option,
  (option) => {
    if (!option) return
    pendingOption.value = option
    setOption(option)
  },
  { immediate: true },
)

onMounted(() => {
  if (pendingOption.value) setOption(pendingOption.value)
})
</script>

<template>
  <section class="chart-section">
    <h2 class="chart-title">{{ title }}</h2>
    <div class="chart-body" :ref="bindRef"></div>
  </section>
</template>

<style scoped>
.chart-section {
  background: var(--bg);
  border: 1px solid var(--border-line);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: 1.25rem;
}
.chart-title {
  margin: 0 0 0.75rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--ink);
}
.chart-body {
  width: 100%;
  height: 280px;
}
</style>
