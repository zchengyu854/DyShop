<script setup>
// 2026-08 数据同步修复（同 OrderList）：
//   根因：仅 onMounted 拉取 + 不响应 route.params 变化（同组件 /orders/A→/orders/B
//         复用实例时展示旧订单）+ BFCache/前后台切换不刷新。
//   修复：watch(route.params.id) 触发重载；useDataRefresh 接管 pageshow/可见性/
//         订单失效广播；取消/支付/确认乐观更新 + 失败回滚，服务端成功后台校正。
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import HomeFooter from '@/components/home/HomeFooter.vue'
import HomeHeader from '@/components/home/HomeHeader.vue'
import PayCountdown from '@/components/shop/PayCountdown.vue'
import PayModal from '@/components/shop/PayModal.vue'
import { addCartItem } from '@/api/cart'
import { cancelOrder, confirmOrder, deleteOrder, fetchOrder } from '@/api/order'
import { createAfterSale } from '@/api/after-sale'
import { useDataRefresh } from '@/composables/useDataRefresh'
import { notifyDataChanged, ORDER_NS } from '@/utils/dataSync'
import { toast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const loading = ref(true)
const payModal = ref(false)
const confirmState = ref(null)
// 本地超时标记：倒计时归零后禁用支付并弹出超时提示（库存释放由后端兜底）
const expired = ref(false)
const expiredModal = ref(false)
const reorderBusy = ref(false)

// 申请售后（ch12）：仅已完成订单，按商品行提交
const afterSaleTarget = ref(null)
const afterSaleReason = ref('')
const afterSaleBusy = ref(false)

function openAfterSale(item) {
  afterSaleTarget.value = item
  afterSaleReason.value = ''
}

async function submitAfterSale() {
  if (afterSaleBusy.value) return
  afterSaleBusy.value = true
  try {
    await createAfterSale({ orderItemId: afterSaleTarget.value.id, reason: afterSaleReason.value.trim() })
    toast.success('售后申请已提交')
    afterSaleTarget.value = null
    router.push('/user/aftersales')
  } catch (e) {
    toast.error(e.message || '申请失败')
  } finally {
    afterSaleBusy.value = false
  }
}

const STATUS_TEXT = ['待支付', '待发货', '待收货', '已完成', '已取消']

function format(amount) {
  return Number(amount).toFixed(2)
}

function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone || ''
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

function fmtTime(time) {
  if (!time) return '-'
  return String(time).replace('T', ' ').slice(0, 19)
}

// 竞态守卫：连续触发只采纳最后一次请求（快速切换订单详情/并发静默刷新）
let requestSeq = 0

// 支付截止时间已过（按服务端 payDeadline 判定，不依赖本地计时器）
function isDeadlinePassed(deadline) {
  if (!deadline) return false
  return Date.now() >= new Date(deadline).getTime()
}

async function load({ silent = false } = {}) {
  const seq = ++requestSeq
  if (!silent) loading.value = true
  try {
    const data = await fetchOrder(route.params.id)
    if (seq !== requestSeq) return
    order.value = data
    // expired 由服务端真实数据驱动：仅「仍待支付且截止已过」保持超时态，
    // 服务端自动取消（status=4）后自然解除，防止过期刷新后倒计时重挂载
    // → 立即 emit expired → 再次刷新 的无限循环；expiredModal 仍只由 onExpired 置位。
    expired.value = data.status === 0 && isDeadlinePassed(data.payDeadline)
  } catch (e) {
    if (seq !== requestSeq) return
    toast.error(e.message || '订单加载失败')
    // 仅首次进入且无本地数据时回列表；静默刷新失败保留旧数据展示
    if (!order.value) router.replace('/user/orders')
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

// 乐观更新辅助：本地瞬时回填目标状态，失败按快照回滚
function applyLocalStatus(targetStatus) {
  if (!order.value) return
  order.value.__prevStatus = order.value.status
  order.value.status = targetStatus
  order.value.statusText = STATUS_TEXT[targetStatus] || order.value.statusText
  if (targetStatus !== 0) order.value.payDeadline = null
}

function rollbackStatus() {
  if (!order.value || order.value.__prevStatus === undefined) return
  const prev = order.value.__prevStatus
  order.value.status = prev
  order.value.statusText = STATUS_TEXT[prev] || ''
  delete order.value.__prevStatus
}

// 倒计时归零：本地禁用支付 + 弹超时提示，并静默重拉一次——
// 若后端自动取消任务已执行（订单变已取消），界面立即展示真实状态；
// 若尚未执行（仍待支付），expired 由 load 按 payDeadline 判定保持超时态，不会循环。
function onExpired() {
  expired.value = true
  expiredModal.value = true
  load({ silent: true })
}

async function doAction() {
  const { type } = confirmState.value
  confirmState.value = null
  const targetStatus = type === 'cancel' ? 4 : 3
  // 删除：终态清理操作，成功即离开详情页回列表（订单已被逻辑删除）
  if (type === 'delete') {
    try {
      await deleteOrder(order.value.id)
      toast.success('订单已删除')
      notifyDataChanged(ORDER_NS) // 列表等已挂载视图同步清除
      router.replace('/user/orders')
    } catch (e) {
      toast.error(e.message || '删除失败，请重试')
    }
    return
  }
  applyLocalStatus(targetStatus) // 乐观更新：先反馈结果
  try {
    if (type === 'cancel') {
      await cancelOrder(order.value.id)
      toast.success('订单已取消')
    } else {
      await confirmOrder(order.value.id)
      toast.success('已确认收货')
    }
    load({ silent: true }) // 服务端真实数据后台校正
  } catch (e) {
    rollbackStatus() // 失败回滚本地乐观状态
    toast.error(e.message || '操作失败，请重试')
  }
}

function onPaySuccess() {
  payModal.value = false
  toast.success('支付成功')
  applyLocalStatus(1) // 乐观置待发货，后台校正
  load({ silent: true })
}

function onPayClose() {
  payModal.value = false
  load({ silent: true })
}

// 超时后重新下单：原订单商品逐件加回购物车（跳过失败项）→ 跳结算页
async function reorder() {
  if (reorderBusy.value) return
  reorderBusy.value = true
  try {
    let added = 0
    for (const item of order.value.items) {
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
  } finally {
    reorderBusy.value = false
  }
}

// 路由参数变化：同组件实例复用（/orders/A → /orders/B）时强制重载
watch(
  () => route.params.id,
  (id) => {
    if (id) load()
  }
)

// 挂载后接管自动刷新：首次(+骨架) / 订单失效广播 / BFCache / 前后台切换（静默）
const { refresh } = useDataRefresh(ORDER_NS, load)
</script>

<template>
  <div class="page-shell">
    <HomeHeader />
    <main v-if="loading" class="detail-page">
      <div class="hint">加载中…</div>
    </main>

    <main v-else-if="order" class="detail-page">
      <!-- 状态区：大标题 + 副文案 + 金额；待支付展示倒计时 -->
      <div class="status-bar">
        <div class="status-left">
          <h1 class="status-title">{{ order.statusText }}</h1>
          <p class="status-sub">
            {{
              order.status === 0
                ? '请尽快完成支付，超时将自动取消'
                : order.status === 1
                  ? '商家正在为你备货'
                  : order.status === 2
                    ? '商品已发出，注意查收'
                    : order.status === 3
                      ? '订单已完成，感谢购买'
                      : '订单已取消'
            }}
          </p>
          <PayCountdown
            v-if="order.status === 0 && order.payDeadline && !expired"
            class="countdown-wrap"
            :deadline="order.payDeadline"
            @expired="onExpired"
          />
        </div>
        <p class="status-amount">¥{{ format(order.payAmount) }}</p>
      </div>

      <section class="card">
        <div class="card-title">收货信息</div>
        <p class="info-line">
          <span class="info-name">{{ order.receiverName }}</span>
          <span class="info-phone">{{ maskPhone(order.receiverPhone) }}</span>
        </p>
        <p class="info-addr">{{ order.receiverAddr }}</p>
      </section>

      <section class="card">
        <div class="card-title">商品清单</div>
        <div v-for="item in order.items" :key="item.productId" class="line">
          <router-link :to="`/products/${item.productId}`" class="line-thumb">
            <img :src="item.productImage" :alt="item.productName" loading="lazy" />
          </router-link>
          <div class="line-info">
            <router-link :to="`/products/${item.productId}`" class="line-name">
              {{ item.productName }}
            </router-link>
            <p v-if="item.specText" class="line-spec">{{ item.specText }}</p>
            <p class="line-meta">¥{{ format(item.price) }} × {{ item.quantity }}</p>
          </div>
          <span class="line-total">¥{{ format(item.subtotal) }}</span>
          <button v-if="order.status === 3" class="as-btn" @click="openAfterSale(item)">申请售后</button>
        </div>
      </section>

      <section class="card">
        <div class="amount-row"><span>商品总额</span><span class="mono">¥{{ format(order.totalAmount) }}</span></div>
        <div v-if="order.coupon" class="amount-row discount">
          <span>优惠券{{ order.coupon.templateName ? '（' + order.coupon.templateName + '）' : '' }}</span>
          <span class="mono save">-¥{{ format(order.coupon.discountAmount) }}</span>
        </div>
        <div v-else-if="order.discountAmount > 0" class="amount-row discount">
          <span>会员优惠</span>
          <span class="mono save">-¥{{ format(order.discountAmount) }}</span>
        </div>
        <div class="amount-row"><span>运费</span><span class="free">免运费</span></div>
        <div class="amount-row total"><span>应付合计</span><span class="mono strong">¥{{ format(order.payAmount) }}</span></div>
        <p v-if="order.remark" class="remark">备注：{{ order.remark }}</p>
      </section>

      <section class="card">
        <div class="card-title">订单信息</div>
        <div class="info-row"><span>订单号</span><span class="mono">{{ order.orderNo }}</span></div>
        <div class="info-row"><span>下单时间</span><span>{{ fmtTime(order.createTime) }}</span></div>
        <div class="info-row"><span>支付时间</span><span>{{ fmtTime(order.payTime) }}</span></div>
        <div class="info-row"><span>发货时间</span><span>{{ fmtTime(order.shipTime) }}</span></div>
        <div class="info-row"><span>完成时间</span><span>{{ fmtTime(order.finishTime) }}</span></div>
        <div class="info-row"><span>取消时间</span><span>{{ fmtTime(order.cancelTime) }}</span></div>
      </section>

      <div class="ops">
        <button class="op-btn back" @click="router.back()">返回</button>
        <button
          class="op-btn ghost"
          :aria-label="loading ? '刷新中' : '刷新订单详情'"
          @click="refresh({ silent: false })"
        >
          刷新
        </button>
        <button
          v-if="order.status === 0"
          class="op-btn danger"
          @click="confirmState = { type: 'cancel' }"
        >
          取消订单
        </button>
        <button
          v-if="order.status === 0"
          class="op-btn primary pay"
          :disabled="expired"
          @click="payModal = true"
        >
          {{ expired ? '已超时' : '去支付' }}
        </button>
        <button
          v-if="order.status === 2"
          class="op-btn primary"
          @click="confirmState = { type: 'confirm' }"
        >
          确认收货
        </button>
        <button
          v-if="order.status === 3 || order.status === 4"
          class="op-btn ghost del"
          @click="confirmState = { type: 'delete' }"
        >
          删除订单
        </button>
      </div>
    </main>

    <HomeFooter />

    <!-- 确认弹窗 -->
    <div v-if="confirmState" class="mask" @click.self="confirmState = null">
      <div class="confirm-modal">
        <h3 class="confirm-title">
          {{
            confirmState.type === 'delete'
              ? '删除订单'
              : confirmState.type === 'cancel'
                ? '取消订单'
                : '确认收货'
          }}
        </h3>
        <p v-if="confirmState.type === 'delete'" class="confirm-desc">
          删除后订单记录将从列表中移除（历史数据保留在后台），确定删除吗？
        </p>
        <p v-else-if="confirmState.type === 'cancel'" class="confirm-desc">
          取消后订单将关闭，商品库存会自动恢复，确定取消吗？
        </p>
        <p v-else class="confirm-desc">确认已收到商品？确认后订单将变为已完成。</p>
        <div class="confirm-ops">
          <button class="confirm-btn cancel" @click="confirmState = null">再想想</button>
          <button
            class="confirm-btn"
            :class="confirmState.type === 'cancel' || confirmState.type === 'delete' ? 'danger' : 'primary'"
            @click="doAction"
          >
            确定
          </button>
        </div>
      </div>
    </div>

    <!-- 申请售后弹窗（ch12）：仅已完成订单可见入口 -->
    <div v-if="afterSaleTarget" class="mask" @click.self="afterSaleTarget = null">
      <div class="confirm-modal">
        <h3 class="confirm-title">申请售后</h3>
        <p class="confirm-desc">
          {{ afterSaleTarget.productName }}{{ afterSaleTarget.specText ? '（' + afterSaleTarget.specText + '）' : '' }}
          · 退款金额 ¥{{ format(afterSaleTarget.subtotal) }}（系统按成交价自动计算）
        </p>
        <textarea
          v-model="afterSaleReason"
          class="as-reason"
          rows="3"
          maxlength="200"
          placeholder="请填写申请原因（最多 200 字）"
        ></textarea>
        <div class="confirm-ops">
          <button class="confirm-btn cancel" @click="afterSaleTarget = null">取消</button>
          <button class="confirm-btn primary" :disabled="afterSaleBusy" @click="submitAfterSale">
            {{ afterSaleBusy ? '提交中…' : '提交申请' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 超时提示：订单已自动取消（后端兜底释放库存），提供重新下单 -->
    <div v-if="expiredModal" class="mask" @click.self="expiredModal = false">
      <div class="confirm-modal">
        <h3 class="confirm-title">订单已超时取消</h3>
        <p class="confirm-desc">
          超过 15 分钟未支付，订单已自动取消，占用的库存已释放。
        </p>
        <div class="confirm-ops">
          <button class="confirm-btn cancel" @click="router.replace('/user/orders')">返回列表</button>
          <button class="confirm-btn primary" :disabled="reorderBusy" @click="reorder">
            {{ reorderBusy ? '处理中…' : '重新下单' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 模拟支付 -->
    <PayModal v-if="payModal" :order="order" @success="onPaySuccess" @close="onPayClose" />
  </div>
</template>

<style scoped>
.page-shell {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}
.detail-page {
  flex: 1;
  width: min(920px, 100%);
  margin: 0 auto;
  padding: 3rem 1.5rem 5rem;
}
.hint {
  padding: 5rem 0;
  text-align: center;
  font-size: 0.9375rem;
  color: var(--ink-faint);
}

/* 状态区：宽松、分层级 */
.status-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 2rem;
  margin-bottom: 2rem;
}
.status-left {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.5rem;
}
.status-title {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.status-sub {
  margin: 0;
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--ink-secondary);
}
.countdown-wrap {
  margin-top: 0.375rem;
}
.status-amount {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* 卡片：间距 ≥32px、内边距 ≥24px、行高 1.5~1.6 */
.card {
  margin-bottom: 2rem;
  padding: 1.75rem 2rem;
  background: var(--bg);
  border: 1px solid var(--border-line);
  border-radius: 1rem;
}
.card-title {
  margin-bottom: 1rem;
  font-size: 1.0625rem;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.info-line {
  display: flex;
  gap: 0.75rem;
  margin: 0;
  font-size: 0.9375rem;
  line-height: 1.5;
}
.info-name {
  font-weight: 600;
}
.info-phone {
  color: var(--ink-secondary);
  font-variant-numeric: tabular-nums;
}
.info-addr {
  margin: 0.375rem 0 0;
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--ink-secondary);
}

.line {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.875rem 0;
  border-bottom: 1px solid var(--border-line);
}
.line:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.line-thumb {
  flex-shrink: 0;
  width: 4rem;
  height: 4rem;
  border-radius: 0.625rem;
  overflow: hidden;
  background: var(--bg-gray);
}
.line-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.line-info {
  flex: 1;
  min-width: 0;
}
.line-name {
  display: inline-block;
  font-size: 0.9375rem;
  font-weight: 500;
  line-height: 1.5;
  color: var(--ink);
}
.line-name:hover {
  color: var(--link);
}
.line-spec {
  margin: 0.25rem 0 0;
  font-size: 0.8125rem;
  color: var(--ink-secondary);
}
.line-spec::before {
  content: "规格：";
  color: var(--ink-faint);
}
.line-meta {
  margin: 0.25rem 0 0;
  font-size: 0.8125rem;
  line-height: 1.5;
  color: var(--ink-secondary);
}
.line-total {
  font-size: 0.9375rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 0.4375rem 0;
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--ink-secondary);
}
.amount-row .free {
  color: #34c759;
}
.amount-row.discount .save {
  color: #ff5000;
  font-weight: 600;
}
.amount-row.total {
  margin-top: 0.375rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--border-line);
  font-size: 1rem;
  color: var(--ink);
}
.mono {
  font-variant-numeric: tabular-nums;
  color: var(--ink);
}
.mono.strong {
  font-weight: 700;
  font-size: 1.125rem;
}
.remark {
  margin: 0.75rem 0 0;
  font-size: 0.8125rem;
  line-height: 1.6;
  color: var(--ink-faint);
}

/* 订单信息：次要信息灰色小字 */
.info-row {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 0.375rem 0;
  font-size: 0.875rem;
  line-height: 1.6;
}
.info-row > span:first-child {
  color: var(--ink-faint);
  flex-shrink: 0;
}
.info-row > span:last-child {
  color: var(--ink-secondary);
  text-align: right;
}
.info-row .mono {
  color: var(--ink-secondary);
  font-variant-numeric: tabular-nums;
}

/* 操作区：核心操作 ≥48px 高度 */
.ops {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.5rem;
}
.op-btn {
  height: 3rem;
  min-width: 6.5rem;
  padding: 0 1.5rem;
  border-radius: var(--radius-full);
  font-size: 0.9375rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, opacity 0.15s;
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
.op-btn.danger {
  border: 1px solid rgba(255, 59, 48, 0.35);
  background: var(--bg);
  color: #ff3b30;
}
.op-btn.danger:hover {
  background: rgba(255, 59, 48, 0.08);
}
.op-btn.back {
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--ink);
}
.op-btn.back:hover {
  background: var(--bg-gray);
}
/* 申请售后（ch12） */
.as-btn {
  align-self: center;
  flex-shrink: 0;
  padding: 5px 12px;
  border: 1px solid #ff5000;
  border-radius: var(--radius);
  background: transparent;
  color: #ff5000;
  font-size: 12px;
  cursor: pointer;
}
.as-btn:hover {
  background: rgba(255, 80, 0, 0.08);
}
.as-reason {
  width: 100%;
  box-sizing: border-box;
  margin-top: 4px;
  padding: 10px 12px;
  border: 1px solid var(--border-line);
  border-radius: var(--radius);
  font-size: 13px;
  font-family: inherit;
  resize: vertical;
  background: var(--bg);
  color: var(--ink);
}
.op-btn.ghost {
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--ink-secondary);
}
.op-btn.ghost:hover {
  background: var(--bg-gray);
}
.op-btn.ghost.del {
  min-width: 0;
  padding: 0 1rem;
  font-size: 0.8125rem;
  color: var(--ink-secondary);
}
.op-btn.ghost.del:hover {
  color: #ff3b30;
  border-color: rgba(255, 59, 48, 0.35);
  background: rgba(255, 59, 48, 0.06);
}

.mask {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
}
.confirm-modal {
  width: min(22rem, calc(100vw - 2.5rem));
  padding: 1.5rem;
  border-radius: 1.25rem;
  background: var(--bg);
  box-shadow: 0 1.5rem 3.75rem rgba(0, 0, 0, 0.25);
}
.confirm-title {
  margin: 0 0 0.625rem;
  font-size: 1.0625rem;
  font-weight: 600;
  text-align: center;
}
.confirm-desc {
  margin: 0;
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--ink-secondary);
  text-align: center;
}
.confirm-ops {
  display: flex;
  gap: 0.625rem;
  margin-top: 1.25rem;
}
.confirm-btn {
  flex: 1;
  height: 2.75rem;
  border: none;
  border-radius: var(--radius-full);
  font-size: 0.9375rem;
  font-weight: 600;
  cursor: pointer;
}
.confirm-btn.cancel {
  background: var(--bg-gray);
  color: var(--ink);
}
.confirm-btn.primary {
  background: var(--blue);
  color: #fff;
}
.confirm-btn.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.confirm-btn.danger {
  background: #ff3b30;
  color: #fff;
}

@media (max-width: 640px) {
  .status-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  .card {
    padding: 1.5rem 1.25rem;
    margin-bottom: 1.5rem;
  }
  .ops {
    flex-wrap: wrap;
  }
  .op-btn {
    flex: 1;
    min-width: 0;
  }
}
</style>
