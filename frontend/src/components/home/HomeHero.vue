<script setup>
defineProps({
  product: { type: Object, default: null },
  loading: { type: Boolean, default: false },
})

function scrollToProducts() {
  document.getElementById('products')?.scrollIntoView({ behavior: 'smooth' })
}
</script>

<template>
  <section class="hero">
    <div class="hero-inner">
      <p class="hero-eyebrow">dyshop · 严选电子好物</p>
      <h1 class="hero-title">好产品，恰到好处。</h1>
      <p class="hero-subtitle">
        从设计、性能到售后，每一件商品都经过严选与打磨。
      </p>
      <div class="hero-cta">
        <button class="hero-btn" @click="scrollToProducts">选购精选产品</button>
        <button class="hero-link" @click="scrollToProducts">浏览全部分类 →</button>
      </div>
    </div>

    <div class="hero-visual" :class="{ 'hero-visual--loading': loading || !product }">
      <img
        v-if="product"
        :src="product.mainImage"
        :alt="product.name"
        class="hero-img"
      />
      <div v-if="product" class="hero-tag">
        <span class="hero-tag-price">¥{{ Number(product.price).toFixed(2) }}</span>
        <span class="hero-tag-sales">已售 {{ product.sales }} 件</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  padding: 40px 20px 64px;
  text-align: center;
  background:
    radial-gradient(ellipse 60% 50% at 50% 0%, rgba(0, 113, 227, 0.07), transparent),
    var(--bg);
}
.hero-inner {
  max-width: 720px;
  margin: 0 auto;
}
.hero-eyebrow {
  margin: 0 0 14px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--blue);
}
.hero-title {
  margin: 0;
  font-size: clamp(36px, 6vw, 60px);
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: -0.02em;
  color: var(--ink);
}
.hero-subtitle {
  margin: 18px auto 0;
  max-width: 460px;
  font-size: 17px;
  line-height: 1.6;
  color: var(--ink-secondary);
}
.hero-cta {
  margin-top: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 22px;
}
.hero-btn {
  height: 44px;
  padding: 0 28px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--blue);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
}
.hero-btn:hover {
  background: #0077ed;
  transform: translateY(-1px);
}
.hero-link {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--blue);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
}
.hero-link:hover {
  color: #0077ed;
}
.hero-visual {
  position: relative;
  max-width: 560px;
  margin: 44px auto 0;
  aspect-ratio: 1;
  border-radius: 24px;
  background: var(--bg-gray);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.hero-visual--loading {
  animation: hero-pulse 1.6s ease-in-out infinite;
}
.hero-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.hero-tag {
  position: absolute;
  right: 16px;
  bottom: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
  backdrop-filter: blur(8px);
}
.hero-tag-price {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
}
.hero-tag-sales {
  font-size: 12px;
  color: var(--ink-secondary);
}
@keyframes hero-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}
@media (max-width: 640px) {
  .hero {
    padding: 44px 20px 48px;
  }
  .hero-cta {
    flex-direction: column;
    gap: 14px;
  }
}
</style>
