<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchDashboardStats } from '@/api/user'
import { getToken } from '@/utils/auth'

// 账户数据概览（ch12 重构）：全部字段来自一个聚合接口 GET /api/user/dashboard-stats
// （累计消费 / 订单总数 / 待发货 / 待收货 / 积分 / 会员等级与进度）。
//
// 布局要点（ch13 UI 修复）：
//   · .overview 外层 flex + .stats-row 内 flex，各项 justify-content: space-between，flex: 1；
//   · 项目间用独立 .divider（40px 高、居中）而非全高 border-right，避免分割线拉满容器；
//   · 价值在上、标签在下，sub 文本强制单行省略（min-width:0 + nowrap + max-width + ellipsis），
//     数值 white-space: nowrap，杜绝「数字连在一起」与内容挤占。
//
// 缓存策略：个人中心首页数据变更频率低，模块级缓存 60s（按 token 区分用户）。
const CACHE_TTL = 60_000
const cache = new Map()

const loading = ref(true)
const data = ref(null)
const progressPct = ref(0)
let mounted = false

const fmtMoney = (v) =>
  Number.isFinite(Number(v))
    ? '¥' + Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '¥0.00'
const fmtInt = (v) => (Number.isFinite(Number(v)) ? Number(v || 0).toLocaleString('zh-CN') : '0')

async function load() {
  const token = getToken() || 'anonymous'
  const cached = cache.get(token)
  if (cached && Date.now() - cached.fetchedAt < CACHE_TTL) {
    apply(cached.data)
    return
  }
  loading.value = true
  try {
    const res = await fetchDashboardStats()
    cache.set(token, { fetchedAt: Date.now(), data: res })
    apply(res)
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
}

function apply(res) {
  data.value = res
  if (mounted) {
    progressPct.value = 0
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        progressPct.value = Math.max(0, Math.min(100, Number(res?.progressPct) || 0))
      })
    })
  } else {
    progressPct.value = Math.max(0, Math.min(100, Number(res?.progressPct) || 0))
  }
}

const stats = computed(() => {
  const d = data.value || {}
  const hot = (n) => n != null && Number(n) > 0
  return [
    { label: '累计消费', value: fmtMoney(d.totalSpent), sub: '已支付订单合计' },
    { label: '订单总数', value: fmtInt(d.totalOrders), sub: `待收货 ${fmtInt(d.pendingReceive)} · 待发货 ${fmtInt(d.pendingShipment)}` },
    { label: '待收货', value: fmtInt(d.pendingReceive), hot: hot(d.pendingReceive), to: { path: '/user/orders', query: { status: '2' } } },
    { label: '待发货', value: fmtInt(d.pendingShipment), hot: hot(d.pendingShipment), to: { path: '/user/orders', query: { status: '1' } } },
    // ch13：积分项改为商城入口（点击跳积分商城，sub 提示文案）
    { label: '积分', value: fmtInt(d.points), sub: '商城兑换 ›', to: { path: '/user/points-mall' }, hot: hot(d.points) },
  ]
})

const level = computed(() => {
  const d = data.value
  if (!d) return null
  const pct = Math.max(0, Math.min(100, Number(d.progressPct) || 0))
  const hint =
    d.nextLevelThreshold != null
      ? `再消费 ¥${Number(d.needAmount || 0).toLocaleString('zh-CN')} 升级${d.levelName || '下一等级'}`
      : '已达最高等级'
  return { name: d.levelName || '会员', pct, hint }
})

onMounted(() => {
  mounted = true
  load()
})
</script>

<template>
  <section class="overview">
    <template v-if="loading && !data">
      <div class="stats-row">
        <div class="item" v-for="n in 5" :key="n">
          <span class="skel-bar w-6" />
          <span class="skel-bar w-4" />
        </div>
      </div>
      <div class="level">
        <span class="skel-bar w-5" />
        <span class="skel-track" />
      </div>
    </template>

    <template v-else>
      <div class="stats-row">
        <template v-for="(s, i) in stats" :key="s.label">
          <div class="item">
            <router-link v-if="s.to" class="item-link" :to="s.to">
              <span class="value" :class="s.hot ? 'stat-hot' : 'stat-cold'">{{ s.value }}</span>
              <span class="label">{{ s.label }}</span>
              <span v-if="s.sub" class="sub">{{ s.sub }}</span>
            </router-link>
            <div v-else class="item-static">
              <span class="value">{{ s.value }}</span>
              <span class="label">{{ s.label }}</span>
              <span v-if="s.sub" class="sub">{{ s.sub }}</span>
            </div>
          </div>
          <span v-if="i < stats.length - 1" class="divider" aria-hidden="true" />
        </template>
      </div>

      <div class="level">
        <div class="level-head">
          <span class="label level-name">{{ level?.name || '会员' }}</span>
          <span v-if="level" class="level-pct">{{ level.pct }}%</span>
        </div>
        <div class="level-track">
          <div class="level-fill" :style="{ width: progressPct + '%' }" />
        </div>
        <span class="level-hint">{{ level?.hint || '加载中…' }}</span>
      </div>
    </template>
  </section>
</template>

<style scoped>
/* ===== 容器：Flex 布局，justify-content: space-between，垂直居中 ===== */
.overview {
  background: var(--bg);
  border-radius: var(--radius-card);
  padding: 20px 24px;
  box-shadow: var(--shadow-card);
}

/* 数据行：各项 flex:1 等分布局，基线/顶对齐，divider 垂直居中 */
.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.item {
  flex: 1;
  min-width: 0; /* 关键：允许子项收缩，长数值不再把兄弟块挤出容器 */
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.item-link {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  text-decoration: none;
  min-width: 0;
}
.item-static {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-width: 0;
}

/* 分割线：独立元素，40px 高，垂直居中；最后一个项目后不渲染 */
.divider {
  flex: none;
  width: 1px;
  height: 40px;
  background: #eeeeee;
  margin: 0 24px; /* 左右间距 ≥ 24px，视觉节奏均匀 */
}

/* ---- 文字层级：数值在上、标签在下、辅助说明截断 ---- */
.value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.25;
  font-variant-numeric: tabular-nums;
  color: #333333;
  white-space: nowrap; /* 关键：数字永不换行，避免与临近块数字粘连 */
}
.label {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.3;
  color: #666666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}
/* 关键截断类：长辅助文案强制单行省略号，且受容器宽度钳制 */
.sub {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.3;
  color: #999999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

/* 待收货/待发货角标：>0 橙色高亮，=0 灰色弱化 */
.stat-hot {
  color: #ff5000;
}
.stat-cold {
  color: #b8b8b8;
}

/* ---- 会员进度条：独立整行，与前排数据保持层级区分 ---- */
.level {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.level-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}
.level-name {
  margin-top: 0;
}
.level-pct {
  font-size: 12px;
  font-weight: 600;
  color: var(--blue);
  white-space: nowrap;
}
.level-track {
  height: 4px;
  border-radius: var(--radius-full);
  background: var(--bg-gray);
  overflow: hidden;
}
.level-fill {
  height: 100%;
  width: 0;
  border-radius: var(--radius-full);
  background: var(--blue);
  transition: width 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}
.level-hint {
  display: block;
  margin-top: 8px;
  font-size: 11px;
  color: #999999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

/* ---- 骨架屏（与真实布局同构，防加载完成时跳动） ---- */
.skel-bar {
  display: block;
  height: 12px;
  border-radius: 6px;
  background: linear-gradient(90deg, var(--bg-gray) 25%, var(--border-line) 50%, var(--bg-gray) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}
.w-6 { width: 60%; }
.w-5 { width: 50%; }
.w-4 { width: 40%; }
.skel-track {
  height: 4px;
  border-radius: var(--radius-full);
  background: var(--bg-gray);
}
@keyframes shimmer {
  from { background-position: 200% 0; }
  to { background-position: -200% 0; }
}

/* ---- 窄屏：一行放两个，其余换行，分割线隐藏保持整洁 ---- */
@media (max-width: 760px) {
  .stats-row {
    flex-wrap: wrap;
    row-gap: 16px;
  }
  .item {
    flex: 1 1 44%;
  }
  .divider {
    display: none;
  }
}
</style>