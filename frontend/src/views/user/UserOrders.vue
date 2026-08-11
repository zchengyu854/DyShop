<script setup>
// ===== 我的订单（个人中心内嵌版）=====
// 2026-08 重构：订单操作改为「命令模式 + 乐观更新 + 状态锁」（ch10）：
//   1) 状态同步统一收敛到 useOrderAction：点击 → 同步锁定(pending) → 品牌即时
//      loading/禁用 → 接口返回成功保持 / 失败回滚 → 解锁。杜绝「点了无反应」
//      与连点重复提交（P0 体验修复）。
//   2) 高风险操作（取消/确认/删除）统一走 OrderConfirmDialog 二次确认拦截，
//      弹窗内确认按钮自带 loading 锁定，Esc 可关（busy 期间无效），Tab 焦点圈定。
//   3) 取消/确认使用乐观更新（不可变 patchOrder，不 mutate 订单对象）：确认后
//      状态立即翻转 + 原按钮进入 loading 占位，成功后按钮消失；失败回滚还原。
//   4) 后端动作接口幂等（已处于目标状态重复调用返回成功），弱网超时重试安全。
defineOptions({ name: 'UserOrders' })

import { computed, onActivated, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDataRefresh } from '@/composables/useDataRefresh'
import { fetchOrders } from '@/api/order'
import { useUserOrdersStore } from '@/stores/userOrders'
import OrderActionButton from '@/components/shop/OrderActionButton.vue'
import OrderConfirmDialog from '@/components/shop/OrderConfirmDialog.vue'
import OrderTabs from '@/components/shop/OrderTabs.vue'
import PayCountdown from '@/components/shop/PayCountdown.vue'
import PayModal from '@/components/shop/PayModal.vue'
import { addCartItem } from '@/api/cart'
import { useOrderAction } from '@/composables/useOrderAction'
import { notifyDataChanged, ORDER_NS } from '@/utils/dataSync'
import { toast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const ordersStore = useUserOrdersStore()

const TABS = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待支付' },
  { key: '1', label: '待发货' },
  { key: '2', label: '待收货' },
  { key: '3', label: '已完成' },
  { key: '4', label: '已取消' },
]

// 本地状态文案映射：供乐观更新瞬时回填（服务端回到 boot 后以接口返回为准）
const STATUS_TEXT = ['待支付', '待发货', '待收货', '已完成', '已取消']

const STATUS_COLOR = {
  0: 'var(--ink)',
  1: 'var(--link)',
  2: 'var(--link)',
  3: '#34c759',
  4: 'var(--ink-faint)',
}

// 左区仅平铺前 N 件商品，其余以「共 N 件」折叠提示
const ORDER_ITEM_PREVIEW = 3

// 受控 Tab 值 + 订单列表由 useOrderList hook 供给（orders/loading/loadError）
const payOrder = ref(null)
// 二次确认弹窗：{ type: 'cancel' | 'confirm' | 'delete', order }；null=关闭
const confirmState = ref(null)
// 弹窗内确认中（跑操作时按钮 loading 锁定）
const confirming = ref(false)
// 倒计时已归零的订单 id 集合（超时后禁用支付，库存释放由后端定时任务兜底）
const timedOut = reactive(new Set())

function format(amount) {
  return Number(amount).toFixed(2)
}

// ---- Tab ↔ URL query 双向同步：URL 可分享/书签/前进后退，activeStatus 为唯一真实源 ----
// P0 修复：此前 switchTab 内先置 tab 再 replace，watch(route.query) 比对 target===tab
// 恒等短路跳过 load()，出现「高亮切换但列表不刷新」。现拉取完全由 useOrderList(tab)
// 的 watch(tab) 触发——tab 任何变化（点击/手势/前进后退/直达链接）唯一收敛到一次 refetch。
// URL 仅承担：写入（切 Tab 时 push，同路由 query 变更无整页刷新）+ 外部驱动（回写 tab）。
const STATUS_KEY = { all: undefined, '0': '0', '1': '1', '2': '2', '3': '3', '4': '4' }

function tabFromQuery() {
  const q = route.query.status
  if (q === undefined || q === null) return 'all'
  return STATUS_KEY[q] !== undefined ? String(q) : 'all'
}

// ---- 订单列表状态（原 useOrderList 内联）----
const CACHE_TTL = 30_000
const cache = new Map()
const orders = ref([])
const loading = ref(true)
const loadError = ref('')
let requestSeq = 0

function keyOf() {
  return String(tab.value)
}

async function loadOrders({ silent = false, force = false } = {}) {
  const key = keyOf()
  if (force) cache.delete(key)
  const cached = cache.get(key)
  const fresh = cached && Date.now() - cached.ts < CACHE_TTL

  if (!silent) {
    if (fresh) {
      orders.value = cached.list
      loadError.value = ''
      loading.value = false
    } else {
      loading.value = true
      loadError.value = ''
    }
  }

  const seq = ++requestSeq
  try {
    const data = await fetchOrders(key === 'all' ? undefined : Number(key))
    if (seq !== requestSeq) return
    cache.set(key, { list: data, ts: Date.now() })
    orders.value = data
    loadError.value = ''
    if (loading.value) loading.value = false
  } catch (e) {
    if (seq !== requestSeq) return
    loading.value = false
    if (!silent) {
      loadError.value = e.message || '加载失败'
      toast.error('加载失败，点击重试')
    }
  }
}

async function refetchOrders({ silent = false } = {}) {
  return loadOrders({ silent, force: true })
}

// 受控 Tab 值：以 URL 为初始种子（避免挂载先拉「全部」再跳目标态的双请求）
const tab = ref(tabFromQuery())

// tab 变化 → 自动 refetch；内置 30s 缓存/骨架屏/失败保留/竞态丢弃
watch(tab, () => loadOrders(), { immediate: true })

// URL query 变化（直达/前进后退/其他模块 push）= 外部驱动 → 回写 tab（值同则跳过），
// 请求由上方的 useOrderList watch 统一触发，此 watcher 不直接 load。
watch(
  () => route.query.status,
  () => {
    const target = tabFromQuery()
    ordersStore.tab = target
    if (target !== tab.value) tab.value = target
  },
  { immediate: true },
)

// 切 Tab：立即更新受控值（<100ms 内骨架/缓存可见）→ push 同步 URL（可后退/书签/分享）
function switchTab(key) {
  if (key === tab.value) return
  ordersStore.tab = key
  tab.value = key
  router.push({ query: key === 'all' ? {} : { status: key } })
}

// 移动端左右滑动手势切 tab（仅竖向滚动在顶部时启用，避免与浏览冲突）
let touchStartX = 0
let touchStartY = 0
function onTouchStart(e) {
  touchStartX = e.changedTouches[0].clientX
  touchStartY = e.changedTouches[0].clientY
}
function onTouchEnd(e) {
  if (window.scrollY > 0) return
  const dx = e.changedTouches[0].clientX - touchStartX
  const dy = e.changedTouches[0].clientY - touchStartY
  if (Math.abs(dx) < 60 || Math.abs(dx) < Math.abs(dy) * 1.5) return
  const order = TABS.map((t) => t.key)
  const idx = order.indexOf(tab.value)
  if (dx < 0 && idx >= 0 && idx < order.length - 1) switchTab(order[idx + 1]) // 左滑：下一个状态
  else if (dx > 0 && idx > 0) switchTab(order[idx - 1]) // 右滑：上一个状态
}

// ---- 订单操作：命令模式 + 乐观更新 + 状态锁（ch10）----
// patchOrder：不可变更新（map 替换对象，禁止 mutate），回滚 = 用快照字段重建新对象
function patchOrder(orderId, patch) {
  orders.value = orders.value.map((o) =>
    o.id === orderId ? { ...o, ...patch } : o,
  )
}
// 删除成功后的行移除（同样不可变）
function removeOrder(orderId) {
  orders.value = orders.value.filter((o) => o.id !== orderId)
}

const {
  isPending,
  anyPending,
  run: runOrderAction,
} = useOrderAction({
  patchOrder,
  removeOrder,
  // 每单成功：仅广播其它已挂载视图（本列表由下方 load({silent}) 自行校正，
  // 避免依赖事件回环时序）
  onSuccess: () => notifyDataChanged(ORDER_NS),
})

// 二次确认框元数据（决定 title/message/危险色）
const confirmMeta = computed(() => {
  const t = confirmState.value?.type
  if (t === 'delete') {
    return { title: '删除订单', message: '删除后订单记录将从列表中移除（历史数据保留在后台），确定删除吗？', danger: true }
  }
  if (t === 'cancel') {
    return { title: '取消订单', message: '取消后订单将关闭，商品库存会自动恢复，确定取消吗？', danger: true }
  }
  return { title: '确认收货', message: '确认已收到商品？确认后订单将变为已完成。', danger: false }
})

function openCancel(order) {
  confirmState.value = { type: 'cancel', order }
}
function openConfirmReceive(order) {
  confirmState.value = { type: 'confirm', order }
}
function openDelete(order) {
  confirmState.value = { type: 'delete', order }
}

// 弹窗确认：弹窗内按钮先 loading 锁定 → 执行动作 → 成功关闭 / 失败保持弹窗回滚后关闭
async function onDialogConfirm() {
  const { type, order } = confirmState.value
  if (!type || confirming.value) return
  confirming.value = true
  const ok = await runOrderAction(type, order)
  confirming.value = false
  if (ok) {
    confirmState.value = null
    // 服务端真实数据校正（乐观状态可能与 DB 略有偏差，但接口幂等，返回一致）
    loadOrders({ silent: true })
  } else {
    // 失败：回滚已完成，重新打开弹窗让用户再次决策/关闭
    confirmState.value = null
  }
}

function onDialogClose() {
  if (confirming.value) return // busy 期间不可关闭（防悬置）
  confirmState.value = null
}

function openDetail(order) {
  // 详情为独立阅读页（保留顶级路由 /orders/:id）；返回 back() 回本列表，
  // keep-alive 保留滚动/筛选 —— 用户无需手动导航即可回沉浸流
  try {
    router.push(`/orders/${order.id}`)
  } catch (e) {
    toast.error('页面跳转失败，请重试')
  }
}

function onPaySuccess() {
  const order = payOrder.value
  payOrder.value = null
  toast.success('支付成功')
  // 乐观置为待发货，随后服务端校正；失败保持列表不闪断（load 静默）
  if (order && order.status === 0) patchOrder(order.id, { status: 1, statusText: STATUS_TEXT[1], payDeadline: null })
  loadOrders({ silent: true })
  notifyDataChanged(ORDER_NS)
}

function onPayClose() {
  payOrder.value = null
  loadOrders({ silent: true })
}

// 倒计时归零：本地标记超时（禁用支付）+ 静默重拉一次（服务端定时任务兜底释放库存）
function onCountdownExpired(orderId) {
  timedOut.add(orderId)
  loadOrders({ silent: true })
}

// 超时后重新下单：原订单商品逐件加回购物车（跳过失败项）→ 跳结算页
async function reorder(order) {
  let added = 0
  for (const item of order.items) {
    try {
      await addCartItem(item.productId, item.quantity)
      added++
    } catch (e) {
      // 已下架/售罄的商品跳过，不阻塞整体
    }
  }
  if (added > 0) {
    toast.success(`已重新加入 ${added} 件商品到购物车`)
    router.push('/checkout')
  } else {
    toast.error('商品均已下架或售罄，无法重新下单')
  }
}

// keep-alive 复用实例时静默刷新：切回订单模块时数据保持新鲜
// （广播/pageshow/可见性刷新由 useDataRefresh 接管；immediate=false，
//  首次加载由 useOrderList(tab) 的 immediate watch 触发，避免重复请求）
useDataRefresh(ORDER_NS, () => loadOrders({ silent: true }), { immediate: false })

onActivated(() => {
  loadOrders({ silent: true })
})
</script>

<template>
  <div class="user-orders">
    <div class="orders-head">
      <h2 class="module-title">我的订单</h2>
      <!-- 手动刷新兜底：自动刷新（事件/BFCache/可见性）之外的用户可控入口；
           refetchOrders 强制清当前 Tab 缓存再拉，保证最新 -->
      <button
        class="refresh-btn"
        :class="{ spinning: loading }"
        :aria-label="loading ? '刷新中' : '刷新订单列表'"
        @click="refetchOrders()"
      >
        <svg class="refresh-icon" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path
            d="M13.5 8a5.5 5.5 0 1 1-1.6-3.9M13.5 1.5v3h-3"
            stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
          />
        </svg>
        刷新
      </button>
    </div>

    <!-- 受控 Tab：modelValue=tab（唯一真源），change → switchTab 更新 tab + push URL；
         外部 URL 变化（前进后退/直达）由 watch(route.query) 回写 tab，双向闭环 -->
    <OrderTabs :model-value="tab" :tabs="TABS" @change="switchTab" />

    <!-- 列表容器：移动端左右滑动手势切 tab（竖向在顶部时生效） -->
    <div
      class="orders-body"
      @touchstart.passive="onTouchStart"
      @touchend.passive="onTouchEnd"
    >
      <!-- 首次/手动/切 tab：骨架屏（轻量 loading，避免空白闪烁） -->
      <div v-if="loading" class="skeleton-list" aria-label="订单加载中">
        <div v-for="n in 3" :key="n" class="skeleton-card">
          <div class="sk-line w40"></div>
          <div class="sk-item-row">
            <div class="sk-thumb"></div>
            <div class="sk-line w55"></div>
          </div>
          <div class="sk-line w80"></div>
          <div class="sk-line w30"></div>
        </div>
      </div>

      <!-- 空态兜底：失败 vs 确实无数据，明确区分避免用户误判 -->
      <div v-else-if="orders.length === 0 && loadError" class="empty">
        <p class="empty-title">订单加载失败</p>
        <p class="empty-desc">{{ loadError }}</p>
        <button class="empty-link btn" @click="refetchOrders()">重试</button>
      </div>
      <div v-else-if="orders.length === 0" class="empty">
        <svg class="empty-illust" viewBox="0 0 96 96" fill="none" aria-hidden="true">
          <rect x="26" y="38" width="44" height="40" rx="6" fill="var(--bg-gray)" stroke="var(--border)" stroke-width="2" />
          <path d="M26 46h44M34 46v-8a14 14 0 0 1 28 0v8" stroke="var(--border)" stroke-width="3" stroke-linecap="round" />
          <circle cx="48" cy="60" r="3" fill="var(--ink-faint)" />
        </svg>
        <p class="empty-title">
          {{ tab === 'all' ? '还没有订单' : `暂无${TABS.find((t) => t.key === tab).label}的订单` }}
        </p>
        <p class="empty-desc">去看看心仪的商品，下一单不难</p>
        <router-link to="/" class="empty-link">去逛逛</router-link>
      </div>

      <!-- 有数据但刷新失败：保留当前内容 + 顶部细 banner，不整块打替换 -->
      <template v-else>
        <div v-if="loadError" class="error-banner" role="alert">
          <span class="error-msg">加载失败，点击重试</span>
          <button class="error-retry" @click="refetchOrders()">重试</button>
        </div>
        <!-- key=tab：强制重建列表 DOM，杜绝跨 Tab 复用时倒计时/滚动残留 -->
        <div class="order-list" :key="tab">
          <article v-for="order in orders" :key="order.id" class="order-card">
          <div class="card-body">
            <!-- 左区（≈66%）：订单元信息 + 商品清单，最多平铺 3 件 -->
            <div class="col-products">
              <div class="order-meta">
                <span class="order-no">订单号 {{ order.orderNo }}</span>
                <span class="order-time">{{ order.createTime?.replace('T', ' ').slice(0, 19) }}</span>
              </div>

              <ul class="items">
                <li v-for="item in order.items.slice(0, ORDER_ITEM_PREVIEW)" :key="item.productId" class="item">
                  <router-link
                    :to="`/products/${item.productId}`"
                    class="item-thumb"
                    :aria-label="`查看商品 ${item.productName}`"
                  >
                    <img :src="item.productImage" :alt="item.productName" loading="lazy" />
                  </router-link>
                  <div class="item-info">
                    <router-link
                      :to="`/products/${item.productId}`"
                      class="item-name"
                      :title="item.productName"
                    >
                      {{ item.productName }}
                    </router-link>
                    <p v-if="item.specText" class="item-spec">{{ item.specText }}</p>
                    <p class="item-price">¥{{ format(item.price) }} × {{ item.quantity }}</p>
                  </div>
                  <span class="item-sub">¥{{ format(item.subtotal) }}</span>
                </li>
              </ul>

              <p v-if="order.items.length > ORDER_ITEM_PREVIEW" class="more-tip">
                等共 {{ order.items.reduce((s, i) => s + i.quantity, 0) }} 件商品
              </p>
              <p v-else class="more-tip">
                {{ order.items.length }} 件商品
              </p>
              <p v-if="order.remark" class="remark" :title="order.remark">备注：{{ order.remark }}</p>
            </div>

            <!-- 右区（≈34%）：状态 + 倒计时 + 实付金额 + 操作组，垂直居中 -->
            <div class="col-ops">
              <div class="status-line">
                <span
                  class="order-status"
                  :style="{ color: STATUS_COLOR[order.status] }"
                >{{ order.statusText }}</span>
              </div>
              <PayCountdown
                v-if="order.status === 0 && order.payDeadline && !timedOut.has(order.id)"
                class="countdown-inline"
                compact
                :deadline="order.payDeadline"
                @expired="onCountdownExpired(order.id)"
              />
              <span v-else-if="order.status === 0 && timedOut.has(order.id)" class="expired-tag">
                订单已超时取消
              </span>

              <div class="amount-line">
                <span class="amount-label">实付金额</span>
                <span class="sum-num">¥{{ format(order.payAmount) }}</span>
              </div>

              <div class="ops">
                <button
                  class="op-btn ghost"
                  :aria-label="`查看订单 ${order.orderNo} 详情`"
                  @click="openDetail(order)"
                >
                  查看详情
                </button>
                <OrderActionButton
                  v-if="order.status === 0 && timedOut.has(order.id)"
                  variant="ghost"
                  :label="'重新下单'"
                  :disabled="anyPending"
                  @click="reorder(order)"
                />
                <OrderActionButton
                  v-if="order.status === 0"
                  variant="danger"
                  :label="'取消订单'"
                  :busy="isPending(order.id)"
                  :disabled="anyPending"
                  @click="openCancel(order)"
                />
                <OrderActionButton
                  v-if="order.status === 0"
                  variant="primary"
                  :label="timedOut.has(order.id) ? '已超时' : '去支付'"
                  :busy="isPending(order.id)"
                  :disabled="timedOut.has(order.id) || anyPending"
                  @click="payOrder = order"
                />
                <OrderActionButton
                  v-if="order.status === 2"
                  variant="primary"
                  :label="'确认收货'"
                  :busy="isPending(order.id)"
                  :disabled="anyPending"
                  @click="openConfirmReceive(order)"
                />
                <OrderActionButton
                  v-if="order.status === 3 || order.status === 4"
                  variant="del"
                  compact
                  :label="'删除'"
                  :busy="isPending(order.id)"
                  :disabled="anyPending"
                  @click="openDelete(order)"
                />
              </div>
            </div>
          </div>
        </article>
        </div>
      </template>
    </div>

    <!-- 确认弹窗（高危操作二次确认拦截：取消/确认收货/删除） -->
    <OrderConfirmDialog
      v-if="confirmState"
      :title="confirmMeta.title"
      :message="confirmMeta.message"
      :danger="confirmMeta.danger"
      :busy="confirming"
      confirm-text="确定"
      @confirm="onDialogConfirm"
      @close="onDialogClose"
    />

    <!-- 模拟支付 -->
    <PayModal v-if="payOrder" :order="payOrder" @success="onPaySuccess" @close="onPayClose" />
  </div>
</template>

<style scoped>
/* 设计令牌：与个人中心全局（var(--ink)/--bg/--radius-*）一致，无独立配色 */
.user-orders {
  min-width: 0;
}
.orders-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.module-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.refresh-btn {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--bg);
  color: var(--ink-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s, background 0.15s;
}
.refresh-btn:hover {
  color: var(--link);
  border-color: var(--link);
  background: var(--bg-gray);
}
.refresh-btn.spinning .refresh-icon {
  animation: spin 0.8s linear infinite;
}
.refresh-icon {
  width: 14px;
  height: 14px;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
/* .tabs/.tab 样式已迁入 components/shop/OrderTabs.vue（受控组件自含） */
.error-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  padding: 10px 14px;
  border: 1px solid rgba(255, 59, 48, 0.35);
  border-radius: var(--radius);
  background: rgba(255, 59, 48, 0.06);
  font-size: 13px;
  color: #ff3b30;
}
.error-retry {
  flex: none;
  border: 1px solid rgba(255, 59, 48, 0.4);
  border-radius: var(--radius-full);
  background: transparent;
  color: #ff3b30;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  padding: 6px 16px;
  cursor: pointer;
}
.error-retry:hover {
  background: rgba(255, 59, 48, 0.08);
}
.orders-body {
  min-width: 0;
}

/* 骨架屏：首次加载/切 tab/手动刷新时展示，静默刷新不触发（loading 不置位） */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}
.skeleton-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  border: 1px solid var(--border-line);
  border-radius: 1rem;
}
.sk-line,
.sk-thumb {
  border-radius: 6px;
  background: linear-gradient(90deg, var(--bg-gray) 25%, #ececf0 37%, var(--bg-gray) 63%);
  background-size: 400% 100%;
  animation: sk-shine 1.4s ease infinite;
}
.sk-line {
  height: 14px;
}
.sk-thumb {
  flex: none;
  width: 56px;
  height: 56px;
  border-radius: 10px;
}
.sk-item-row {
  display: flex;
  align-items: center;
  gap: 14px;
}
.w30 { width: 30%; }
.w40 { width: 40%; }
.w55 { width: 55%; }
.w80 { width: 80%; }
@keyframes sk-shine {
  from { background-position: 100% 50%; }
  to { background-position: 0 50%; }
}

.empty {
  padding: 80px 0;
  text-align: center;
}
.empty-illust {
  width: 72px;
  height: 72px;
  margin: 0 auto 18px;
  display: block;
}
.empty-title {
  margin: 0 0 14px;
  font-size: 22px;
  font-weight: 600;
}
.empty-desc {
  margin: 0 0 18px;
  font-size: 14px;
  color: var(--ink-secondary);
}
.empty-link {
  color: var(--link);
  font-size: 15px;
}
.empty-link.btn {
  display: inline-flex;
  align-items: center;
  height: 40px;
  padding: 0 24px;
  border: 1px solid var(--link);
  border-radius: var(--radius-full);
  background: transparent;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.15s;
}
.empty-link.btn:hover {
  background: rgba(0, 113, 227, 0.08);
}

/* 卡片：间距 2rem（≥16px 要求），内边距 1.5rem（≥24px 要求） */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}
.order-card {
  background: var(--bg);
  border: 1px solid var(--border-line);
  border-radius: 1rem;
  padding: 1.5rem;
}

/* 左右分栏：左 1fr（约 66%）/ 右 minmax(240px, 34%) */
.card-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 34%);
  gap: 1.5rem;
  align-items: center;
}
.col-products {
  min-width: 0;
  padding-right: 1.5rem;
  border-right: 1px solid var(--border-line);
}

/* ---- 左区：商品信息 ---- */
.order-meta {
  display: flex;
  align-items: baseline;
  gap: 0.875rem;
  padding-bottom: 0.75rem;
  margin-bottom: 0.25rem;
  border-bottom: 1px solid var(--border-line);
  font-size: 0.8125rem;
  line-height: 1.5;
  color: var(--ink-faint);
}
.order-no {
  font-variant-numeric: tabular-nums;
}
.items {
  list-style: none;
  margin: 0;
  padding: 0;
}
.item {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--border-line);
}
.item:last-child {
  border-bottom: none;
}
.item-thumb {
  flex-shrink: 0;
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 0.625rem;
  overflow: hidden;
  background: var(--bg-gray);
}
.item-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  display: inline-block;
  font-size: 0.9375rem;
  font-weight: 500;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-name:hover {
  color: var(--link);
}
.item-spec {
  margin: 3px 0 0;
  font-size: 12px;
  color: var(--ink-secondary);
}
.item-spec::before {
  content: "规格：";
  color: var(--ink-faint);
}
.item-price {
  margin: 0.25rem 0 0;
  font-size: 0.8125rem;
  line-height: 1.5;
  color: var(--ink-secondary);
  font-variant-numeric: tabular-nums;
}
.item-sub {
  flex-shrink: 0;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.more-tip {
  margin: 0.625rem 0 0;
  font-size: 0.8125rem;
  line-height: 1.5;
  color: var(--ink-faint);
}
.remark {
  margin: 0.5rem 0 0;
  font-size: 0.8125rem;
  line-height: 1.5;
  color: var(--ink-faint);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---- 右区：状态 + 金额 + 操作（右对齐，垂直居中） ---- */
.col-ops {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.5rem;
  min-width: 0;
}
.status-line {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}
.order-status {
  font-size: 0.9375rem;
  font-weight: 600;
  line-height: 1.5;
}
.countdown-inline {
  flex-shrink: 0;
}
.expired-tag {
  font-size: 0.8125rem;
  line-height: 1.5;
  color: var(--ink-faint);
}
.amount-line {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
  margin: 0.25rem 0 0.375rem;
}
.amount-label {
  font-size: 0.75rem;
  line-height: 1.4;
  color: var(--ink-faint);
}
.sum-num {
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.2;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.ops {
  display: flex;
  flex-wrap: wrap; /* 窄屏自动换行，防止按钮溢出 */
  justify-content: flex-end;
  gap: 0.5rem; /* 按钮间距 ≥8px */
}

/* ---- 弹窗（ch10：统一 OrderConfirmDialog 自含样式） ---- */
/* .mask/.confirm-modal/.confirm-btn 已迁入 components/shop/OrderConfirmDialog.vue */
.op-btn {
  min-height: 2.75rem; /* 触控目标 ≥44px（WCAG 2.5.8） */
  min-width: 5.5rem;
  padding: 0 1.25rem;
  border-radius: var(--radius-full);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s, opacity 0.15s;
}
.op-btn.primary {
  border: none;
  background: var(--blue);
  color: #fff;
}
.op-btn.primary:hover:not(:disabled) {
  background: var(--blue-hover);
}
.op-btn.primary:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.op-btn.ghost {
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--ink);
}
.op-btn.ghost:hover {
  background: var(--bg-gray);
}
.op-btn.danger {
  border: 1px solid rgba(255, 59, 48, 0.35);
  background: var(--bg);
  color: #ff3b30;
}
.op-btn.danger:hover {
  background: rgba(255, 59, 48, 0.08);
}
/* 删除小按钮：已完成/已取消订单的次要清理入口，比主操作更紧凑 */
.op-btn.ghost.del {
  min-width: 0;
  min-height: 2rem;
  padding: 0 0.875rem;
  font-size: 0.8125rem;
  color: var(--ink-secondary);
  border-color: transparent;
}
.op-btn.ghost.del:hover {
  color: #ff3b30;
  border-color: rgba(255, 59, 48, 0.35);
  background: rgba(255, 59, 48, 0.06);
}

/* ---- 移动端：回退单栏，右区内容移至卡片底部 ---- */
@media (max-width: 720px) {
  .module-title {
    font-size: 18px;
  }
  .card-body {
    grid-template-columns: 1fr;
    gap: 1.25rem;
  }
  .col-products {
    padding-right: 0;
    border-right: none;
  }
  .col-ops {
    align-items: stretch;
    padding-top: 1rem;
    border-top: 1px solid var(--border-line);
  }
  .status-line,
  .amount-line {
    align-items: flex-start;
  }
  .ops {
    justify-content: flex-start;
  }
}
</style>
