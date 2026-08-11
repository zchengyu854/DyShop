<script setup>
// 金额展示（结算摘要/明细通用）：接收已格式化的 ¥X.XX 文本（格式化收敛在 utils/price.formatMoney）。
// 仅在「首次加载且尚未有后端权威值」时显示金额骨架屏，避免给用户动画闪烁（切券即时本地重算无 loading）。
defineProps({
  totalText: { type: String, default: '¥0.00' },
  memberBenefitText: { type: String, default: '¥0.00' },
  couponDiscountText: { type: String, default: '¥0.00' },
  payText: { type: String, default: '¥0.00' },
  showMemberBenefit: Boolean,
  showCoupon: Boolean,
  couponName: { type: String, default: '' },
  loading: Boolean, // preview 请求进行中
  initial: Boolean, // 尚无后端权威值（显示骨架）
})

// 优惠额展示为负值（文本已带负号则不再叠加）
function signed(text) {
  return text.startsWith('-') ? text : `-${text}`
}
</script>

<template>
  <div class="amount-card">
    <div class="summary-row">
      <span>商品总额</span>
      <span class="mono val">{{ totalText }}</span>
    </div>
    <div v-if="showCoupon" class="summary-row">
      <span class="benefit-label">优惠券{{ couponName ? ` · ${couponName}` : '' }}</span>
      <span class="mono save">{{ signed(couponDiscountText) }}</span>
    </div>
    <div v-else-if="showMemberBenefit" class="summary-row">
      <span class="benefit-label">会员优惠</span>
      <span class="mono save">{{ signed(memberBenefitText) }}</span>
    </div>
    <div class="summary-row">
      <span>运费</span>
      <span class="free-tag">免运费</span>
    </div>

    <div class="summary-divider"></div>

    <div class="pay-row" :class="{ skeleton: initial && loading }">
      <span class="pay-label">应付合计</span>
      <!-- 用 payText 做 key：切券/取消时同 tick 替换节点 → 文本立即更新，CSS 动画随之重播（闪现提示） -->
      <span v-if="initial && loading" key="skeleton" class="pay-skeleton" aria-label="价格计算中">
        <span class="skeleton-bar"></span>
      </span>
      <span v-else :key="payText" class="pay" :data-pay-text="payText">{{ payText }}</span>
    </div>
  </div>
</template>

<style scoped>
.amount-card {
  margin-top: 0.75rem;
}
/* 过程数据行：行距加大（约 40px）呼吸感，Label/Value 两端对齐 */
.summary-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 0.625rem 0;
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--sum-label, #6e6e73);
}
.summary-row .val {
  color: var(--sum-value, #1d1d1f);
}
.mono {
  font-variant-numeric: tabular-nums;
}
/* 抵扣行：标签弱化 + 品牌橙金额 */
.benefit-label {
  color: var(--sum-label, #6e6e73);
}
.save {
  color: var(--sum-save, #ff5000);
  font-weight: 600;
}
/* 免运费：绿色标签 */
.free-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.125rem 0.5rem;
  border-radius: 6px;
  background: rgba(52, 199, 89, 0.12);
  color: var(--sum-free, #34c759);
  font-size: 0.75rem;
  font-weight: 600;
}
/* 虚线分隔：过程数据 ↔ 最终结果 */
.summary-divider {
  height: 0;
  margin: 0.5rem 0 0.75rem;
  border-top: 1px dashed var(--sum-divider, #ececec);
}
/* 核心金额区：视觉重心，红字放大 + 柔和底色做差异感 */
.pay-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.875rem 1rem;
  border-radius: 8px;
  background: var(--sum-band, rgba(255, 77, 79, 0.04));
}
.pay-label {
  font-size: 0.875rem;
  color: var(--sum-label, #6e6e73);
}
.pay {
  font-size: 1.75rem;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.15;
  color: var(--sum-pay, #ff4d4f);
  animation: pay-flash 0.28s ease;
}
@keyframes pay-flash {
  0% {
    opacity: 0.15;
    color: #ff7875;
    transform: translateY(3px);
  }
  60% {
    color: #ff7875;
  }
  100% {
    opacity: 1;
    color: var(--sum-pay, #ff4d4f);
    transform: translateY(0);
  }
}
/* 骨架：仅首次加载展示 */
.pay-skeleton {
  display: inline-flex;
  align-items: center;
}
.skeleton-bar {
  display: inline-block;
  width: 5.5rem;
  height: 1.875rem;
  border-radius: 0.375rem;
  background: linear-gradient(90deg, var(--bg-gray) 25%, var(--border-line) 50%, var(--bg-gray) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}
@keyframes shimmer {
  from {
    background-position: 200% 0;
  }
  to {
    background-position: -200% 0;
  }
}
</style>