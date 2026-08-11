<script setup>
// 服务承诺 Bento：1 大卡（品质保障）+ 5 小卡，3 列 3 行网格
const mainCard = {
  title: '品质保障',
  desc: '每一件商品都经过严选与上架审核，买得放心，用得安心。',
  points: ['严选供应链', '官方正品', '售后无忧'],
}

const cards = [
  { icon: 'truck', title: '极速发货', desc: '当日 16:00 前下单，最快次日送达。' },
  { icon: 'refresh', title: '7 天无理由', desc: '签收后 7 天内不满意，无理由退换。' },
  { icon: 'lock', title: '安全支付', desc: '全站 HTTPS 加密，资金渠道安全可靠。' },
  { icon: 'chat', title: '专属客服', desc: '工作日 9:00-21:00 在线，人工应答。' },
  { icon: 'gift', title: '会员好礼', desc: '注册即享收藏、购物车云同步与优惠。' },
]

const icons = {
  truck: 'M3 17V7a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v10M15 9h3l3 3v5h-3M6 19a2 2 0 1 1 0-4 2 2 0 0 1 0 4Zm10 0a2 2 0 1 1 0-4 2 2 0 0 1 0 4Z',
  refresh: 'M4 12a8 8 0 0 1 14.5-4.9M20 12a8 8 0 0 1-14.5 4.9M15 3h5v5M9 21H4v-5',
  lock: 'M6 11h12v9H6v-9ZM8 11V8a4 4 0 0 1 8 0v3M12 15v2',
  chat: 'M4 5h16v11H9l-5 4V5Z',
  gift: 'M4 10h16v11H4V10ZM2 6h20v4H2V6Zm10 4v11M12 6s-3-5-6-3c0 3 3 3 6 3Zm0 0s3-5 6-3c0 3-3 3-6 3Z',
  shield: 'M12 3l8 3v6c0 4.5-3.2 7.7-8 9-4.8-1.3-8-4.5-8-9V6l8-3Zm-3 9 2 2 4-4',
}
</script>

<template>
  <section class="bento">
    <div class="bento-inner">
      <article class="bento-main">
        <span class="main-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path :d="icons.shield" />
          </svg>
        </span>
        <h3 class="main-title">{{ mainCard.title }}</h3>
        <p class="main-desc">{{ mainCard.desc }}</p>
        <ul class="main-points">
          <li v-for="p in mainCard.points" :key="p">{{ p }}</li>
        </ul>
      </article>

      <article v-for="c in cards" :key="c.title" class="bento-card">
        <span class="card-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path :d="icons[c.icon]" />
          </svg>
        </span>
        <h3 class="card-title">{{ c.title }}</h3>
        <p class="card-desc">{{ c.desc }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.bento {
  background: var(--bg-gray);
  padding: 48px 20px;
}
.bento-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.bento-main {
  grid-column: span 2;
  grid-row: span 2;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 36px;
  border-radius: var(--radius-card);
  background:
    linear-gradient(160deg, rgba(0, 113, 227, 0.07), transparent 55%),
    var(--bg);
  box-shadow: var(--shadow-card);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.bento-main:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}
.bento-card {
  padding: 24px;
  border-radius: var(--radius-card);
  background: var(--bg);
  box-shadow: var(--shadow-card);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.bento-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}
.main-icon,
.card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(0, 113, 227, 0.08);
  color: var(--blue);
}
.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
}
.main-icon svg,
.card-icon svg {
  width: 22px;
  height: 22px;
}
.main-title {
  margin: 18px 0 8px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--ink);
}
.main-desc {
  margin: 0;
  max-width: 420px;
  font-size: 15px;
  line-height: 1.6;
  color: var(--ink-secondary);
}
.main-points {
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.main-points li {
  padding: 5px 12px;
  border-radius: var(--radius-full);
  background: rgba(0, 113, 227, 0.08);
  color: var(--blue);
  font-size: 13px;
  font-weight: 500;
}
.card-title {
  margin: 14px 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
}
.card-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--ink-secondary);
}
@media (max-width: 900px) {
  .bento-inner {
    grid-template-columns: repeat(2, 1fr);
  }
  .bento-main {
    grid-column: 1 / -1;
  }
}
@media (max-width: 640px) {
  .bento-inner {
    grid-template-columns: 1fr;
  }
  .bento-main {
    grid-column: auto;
    grid-row: auto;
    padding: 28px;
  }
}
</style>