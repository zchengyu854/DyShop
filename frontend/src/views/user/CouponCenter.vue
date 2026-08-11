<script setup>
import { onMounted, ref } from 'vue'
import { claimCoupon, fetchCouponCenter } from '@/api/coupon'
import { toast } from '@/utils/toast'

const records = ref([])
const loading = ref(false)
const claimingId = ref(null)

function fmtValid(tpl) {
  if (tpl.validType === 'FIXED') {
    return tpl.startAt ? `有效期 ${String(tpl.startAt).slice(0, 10)} ~ ${tpl.endAt ? String(tpl.endAt).slice(0, 10) : '长期'}` : '长期有效'
  }
  return tpl.validDays > 0 ? `领取后 ${tpl.validDays} 天内有效` : '长期有效'
}

function remainingText(tpl, item) {
  return item.remaining === -1 ? '不限量' : `剩余 ${item.remaining} 张`
}

async function load() {
  loading.value = true
  try {
    records.value = await fetchCouponCenter()
  } catch (e) {
    toast.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function doClaim(item) {
  if (claimingId.value) return
  if (item.claimed) return
  if (item.remaining === 0) return toast.info('该券已抢光')
  claimingId.value = item.template.id
  try {
    await claimCoupon(item.template.id)
    toast.success('领取成功，可在「我的优惠券」查看')
    load()
  } catch (e) {
    toast.error(e.message || '领取失败')
  } finally {
    claimingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="coupon-center">
    <div class="head">
      <h2 class="title">领券中心</h2>
      <router-link to="/user/coupons" class="mine-link">我的优惠券 ›</router-link>
    </div>

    <div v-if="loading" class="state">加载中…</div>
    <div v-else-if="records.length === 0" class="state">暂无可用优惠券</div>

    <div v-else class="grid">
      <div v-for="item in records" :key="item.template.id" class="card">
        <div class="card-top">
          <p class="amount">-¥{{ Number(item.template.discountAmount).toFixed(0) }}</p>
          <div class="info">
            <p class="name">{{ item.template.name }}</p>
            <p class="cond">
              {{ Number(item.template.minAmount) > 0 ? `满 ¥${Number(item.template.minAmount)} 可用` : '无门槛' }}
            </p>
          </div>
        </div>
        <div class="card-meta">
          <p>{{ fmtValid(item.template) }}</p>
          <p>{{ remainingText(item.template, item) }}</p>
        </div>
        <button
          class="claim-btn"
          :class="{ claimed: item.claimed, soldout: item.remaining === 0 }"
          :disabled="item.claimed || item.remaining === 0 || claimingId === item.template.id"
          @click="doClaim(item)"
        >
          {{ claimingId === item.template.id ? '领取中…' : item.claimed ? '已领取' : item.remaining === 0 ? '已抢光' : '立即领取' }}
        </button>
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
.state {
  padding: 48px 0;
  text-align: center;
  color: var(--ink-faint);
  font-size: 14px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.card {
  background: var(--bg);
  border: 1px solid var(--border-line);
  border-radius: var(--radius-card);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.card-top {
  display: flex;
  align-items: center;
  gap: 16px;
}
.amount {
  margin: 0;
  min-width: 80px;
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #ff5000;
}
.info {
  min-width: 0;
}
.name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cond {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--ink-secondary);
}
.card-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 0;
  border-top: 1px dashed var(--border-line);
  font-size: 12px;
  color: var(--ink-faint);
}
.claim-btn {
  height: 40px;
  border: none;
  border-radius: var(--radius-full);
  background: #ff5000;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.claim-btn:hover {
  opacity: 0.9;
}
.claim-btn.claimed,
.claim-btn.soldout {
  background: var(--bg-gray);
  color: var(--ink-faint);
  cursor: not-allowed;
}
</style>
