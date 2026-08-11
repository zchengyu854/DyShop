<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { exchangePoints, fetchPointsMall } from '@/api/user'
import { toast } from '@/utils/toast'

const router = useRouter()

const loading = ref(true)
const mall = ref(null) // { myPoints, goods: [] }
const confirmGoods = ref(null) // 兑换确认弹窗中的商品
const exchanging = ref(false)
const success = ref(null) // 兑换成功结果 + 商品名

function fmtStock(g) {
  return Number(g.stock) === -1 ? '不限量' : `剩余 ${g.stock} 件`
}

function limitText(g) {
  if (Number(g.limitPerUser) > 0) return `每人限兑 ${g.limitPerUser} 件 · 已兑 ${g.exchangedCount} 件`
  return `已兑 ${g.exchangedCount} 件`
}

/** 兑换按钮状态：disabled-limit / disabled-stock / disabled-points / 可兑换 */
function btnState(g) {
  if (Number(g.limitPerUser) > 0 && Number(g.exchangedCount) >= Number(g.limitPerUser)) {
    return { key: 'limit', label: '已达兑换上限', disabled: true }
  }
  if (Number(g.stock) === 0) {
    return { key: 'stock', label: '已兑完', disabled: true }
  }
  const balance = mall.value ? Number(mall.value.myPoints) : 0
  const cost = Number(g.pointCost)
  if (balance < cost) {
    return { key: 'points', label: `差 ${(cost - balance).toLocaleString('zh-CN')} 积分`, disabled: true }
  }
  return { key: 'ok', label: '立即兑换', disabled: false }
}

function openConfirm(g) {
  confirmGoods.value = g
}

async function doExchange() {
  if (exchanging.value || !confirmGoods.value) return
  exchanging.value = true
  try {
    const res = await exchangePoints(confirmGoods.value.id)
    success.value = { ...res, goodsName: confirmGoods.value.name }
    confirmGoods.value = null
    await load()
  } catch (e) {
    toast.error(e.message || '兑换失败')
    confirmGoods.value = null
  } finally {
    exchanging.value = false
  }
}

function goCoupons() {
  router.push('/user/coupons')
}

function closeSuccess() {
  success.value = null
}

async function copyCode() {
  if (!success.value?.code) return
  try {
    await navigator.clipboard.writeText(success.value.code)
    toast.success('兑换码已复制')
  } catch {
    toast.error('复制失败，请手动选择复制')
  }
}

async function load() {
  loading.value = true
  try {
    mall.value = await fetchPointsMall()
  } catch (e) {
    toast.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="points-mall">
    <div class="head">
      <h2 class="title">积分商城</h2>
      <router-link to="/user/points" class="mine-link">积分明细 ›</router-link>
    </div>

    <div class="balance-card">
      <span class="balance-label">我的可用积分</span>
      <span class="balance-value">{{ mall ? Number(mall.myPoints).toLocaleString('zh-CN') : '…' }}</span>
      <span class="balance-hint">积分自到账起 12 个月有效，过期自动清零</span>
    </div>

    <div v-if="loading" class="state">加载中…</div>
    <div v-else-if="!mall?.goods?.length" class="state">商城暂无在售商品，敬请期待</div>

    <div v-else class="grid">
      <div v-for="g in mall.goods" :key="g.id" class="card">
        <div class="cover">
          <img v-if="g.coverImage" :src="g.coverImage" :alt="g.name" />
          <span v-else class="cover-placeholder">{{ g.name.slice(0, 1) }}</span>
          <span class="type-tag">{{ g.goodsType === 'COUPON' ? '优惠券' : '兑换码' }}</span>
        </div>
        <p class="name">{{ g.name }}</p>
        <p class="desc">{{ g.description || '积分专享好礼' }}</p>
        <div class="meta">
          <p class="cost">
            <span class="cost-num">{{ Number(g.pointCost).toLocaleString('zh-CN') }}</span>
            积分
          </p>
          <p class="sub">{{ limitText(g) }} · {{ fmtStock(g) }}</p>
        </div>
        <button
          class="buy-btn"
          :class="{ muted: btnState(g).disabled }"
          :disabled="btnState(g).disabled || exchanging"
          @click="openConfirm(g)"
        >
          {{ btnState(g).label }}
        </button>
      </div>
    </div>

    <!-- 兑换确认弹窗 -->
    <div v-if="confirmGoods" class="mask" @click.self="exchanging ? null : (confirmGoods = null)">
      <div class="dialog">
        <h3 class="dialog-title">确认兑换</h3>
        <p class="dialog-text">将以 <b>{{ Number(confirmGoods.pointCost).toLocaleString('zh-CN') }} 积分</b> 兑换「{{ confirmGoods.name }}」</p>
        <p class="dialog-sub">
          {{ confirmGoods.goodsType === 'COUPON' ? '兑换成功后发放到「我的优惠券」' : '兑换成功后展示兑换码，请及时保存' }}
        </p>
        <div class="dialog-ops">
          <button class="op ghost" :disabled="exchanging" @click="confirmGoods = null">取消</button>
          <button class="op primary" :disabled="exchanging" @click="doExchange">
            {{ exchanging ? '兑换中…' : '确认兑换' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 成功反馈弹窗 -->
    <div v-if="success" class="mask" @click.self="closeSuccess">
      <div class="dialog success-dialog">
        <h3 class="dialog-title">兑换成功</h3>
        <template v-if="success.goodsType === 'CODE'">
          <p class="code-label">兑换码（已扣 {{ Number(success.pointCost).toLocaleString('zh-CN') }} 积分）</p>
          <p class="code-value">{{ success.code }}</p>
          <p class="dialog-sub">兑换码仅展示一次，请复制保存</p>
          <button class="op primary block" @click="copyCode">一键复制</button>
        </template>
        <template v-else>
          <p class="dialog-text">优惠券已发放到您的券包</p>
          <button class="op primary block" @click="goCoupons">查看我的优惠券</button>
        </template>
        <button class="op ghost block" @click="closeSuccess">继续逛逛</button>
      </div>
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
.mine-link {
  font-size: 13px;
  color: var(--ink-secondary);
  text-decoration: none;
}
.mine-link:hover {
  color: var(--blue);
}

.balance-card {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 18px;
  padding: 18px 22px;
  border-radius: var(--radius-card);
  background: linear-gradient(120deg, #fff7ec, #fff);
  border: 1px solid #ffe2bd;
}
.balance-label {
  font-size: 13px;
  color: var(--ink-secondary);
}
.balance-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  color: #ff5000;
  font-variant-numeric: tabular-nums;
}
.balance-hint {
  margin-left: auto;
  font-size: 12px;
  color: var(--ink-faint);
}

.state {
  padding: 48px 0;
  text-align: center;
  color: var(--ink-faint);
  font-size: 14px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 16px;
}
.card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  background: var(--bg);
  border: 1px solid var(--border-line);
  border-radius: var(--radius-card);
}
.cover {
  position: relative;
  height: 132px;
  border-radius: 10px;
  overflow: hidden;
  background: var(--bg-gray);
  display: flex;
  align-items: center;
  justify-content: center;
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-placeholder {
  font-size: 34px;
  font-weight: 700;
  color: #d8d8d8;
}
.type-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
}
.name {
  margin: 2px 0 0;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.desc {
  margin: 0;
  font-size: 12px;
  color: var(--ink-faint);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.cost {
  margin: 0;
  font-size: 12px;
  color: var(--ink-secondary);
}
.cost-num {
  font-size: 20px;
  font-weight: 700;
  color: #ff5000;
  font-variant-numeric: tabular-nums;
}
.sub {
  margin: 0;
  font-size: 11px;
  color: var(--ink-faint);
  text-align: right;
}
.buy-btn {
  margin-top: auto;
  height: 38px;
  border: none;
  border-radius: var(--radius-full);
  background: #ff5000;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.buy-btn:hover {
  opacity: 0.9;
}
.buy-btn.muted {
  background: var(--bg-gray);
  color: var(--ink-faint);
  cursor: not-allowed;
}

/* 弹窗 */
.mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.dialog {
  width: min(360px, 100%);
  padding: 22px;
  border-radius: var(--radius-card);
  background: var(--bg);
  text-align: center;
}
.dialog-title {
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 700;
}
.dialog-text {
  margin: 0;
  font-size: 14px;
  color: var(--ink);
}
.dialog-sub {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--ink-faint);
}
.code-label {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--ink-secondary);
}
.code-value {
  margin: 0;
  padding: 14px;
  border: 1px dashed #ffb27a;
  border-radius: 10px;
  background: #fff7ec;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #ff5000;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  word-break: break-all;
}
.dialog-ops {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}
.op {
  flex: 1;
  height: 42px;
  border: none;
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.op.primary {
  background: #ff5000;
  color: #fff;
}
.op.ghost {
  background: var(--bg-gray);
  color: var(--ink);
}
.op:disabled {
  opacity: 0.6;
}
.block {
  width: 100%;
  margin-top: 12px;
}
.success-dialog .op:last-child {
  background: var(--bg);
  border: 1px solid var(--border);
  color: var(--ink-secondary);
}
</style>