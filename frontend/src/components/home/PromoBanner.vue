<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const isLogin = computed(() => userStore.isLogin)

function handleCta() {
  router.push(isLogin.value ? '/cart' : '/register')
}
</script>

<template>
  <section class="promo">
    <div class="promo-inner">
      <div class="promo-text">
        <h3 class="promo-title">
          {{ isLogin ? '欢迎回来，继续挑选好物' : '收藏、购物车，随时随地随身带' }}
        </h3>
        <p class="promo-desc">
          {{
            isLogin
              ? '收藏与购物车都在这里，接着上次的节奏，把心仪的好物带回家。'
              : '注册 dyshop 账号，收藏与购物车数据云端同步，续上上次的挑选。'
          }}
        </p>
      </div>
      <button class="promo-btn" @click="handleCta">
        {{ isLogin ? '去购物车看看' : '立即注册' }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.promo {
  padding: 40px 20px 64px;
  background: var(--bg-gray);
}
.promo-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 30px 36px;
  border-radius: var(--radius-card);
  background:
    linear-gradient(135deg, rgba(0, 113, 227, 0.09), rgba(0, 113, 227, 0.02) 60%),
    var(--bg);
  box-shadow: var(--shadow-card);
}
.promo-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--ink);
}
.promo-desc {
  margin: 6px 0 0;
  font-size: 14px;
  color: var(--ink-secondary);
}
.promo-btn {
  flex: none;
  height: 44px;
  padding: 0 30px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--blue);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
}
.promo-btn:hover {
  background: #0077ed;
  transform: translateY(-1px);
}
@media (max-width: 640px) {
  .promo-inner {
    flex-direction: column;
    align-items: flex-start;
    padding: 26px 24px;
  }
  .promo-btn {
    width: 100%;
  }
}
</style>