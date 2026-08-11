<script setup>
import { onBeforeUnmount, onMounted, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'

const emit = defineEmits(['search'])

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

const keyword = shallowRef('')

function handleSearch() {
  emit('search', keyword.value.trim())
}

function handleLogout() {
  userStore.logout()
  router.push('/')
}

// BFCache 恢复页面快照时 onMounted 不会重跑，徽标/昵称补一次刷新，避免图显旧购物车数量
function onPageShow(event) {
  if (event.persisted && userStore.isLogin) {
    cartStore.fetchCart().catch(() => {})
  }
}

onMounted(() => {
  window.addEventListener('pageshow', onPageShow)
  userStore.fetchUserInfo()
  if (userStore.isLogin) {
    cartStore.fetchCart().catch(() => {})
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('pageshow', onPageShow)
})
</script>

<template>
  <header class="header">
    <div class="header-inner">
      <router-link to="/" class="logo">dyshop</router-link>
      <div class="search">
        <input
          v-model="keyword"
          class="search-input"
          type="text"
          placeholder="搜索电子产品"
          @keyup.enter="handleSearch"
        />
        <button class="search-btn" @click="handleSearch">搜索</button>
      </div>
      <nav class="nav">
        <router-link to="/cart" class="nav-cart" aria-label="购物车">
          购物车
          <span
            v-if="userStore.isLogin && cartStore.totalQuantity > 0"
            class="cart-badge"
          >{{ cartStore.totalQuantity > 99 ? '99+' : cartStore.totalQuantity }}</span>
        </router-link>
        <template v-if="userStore.isLogin">
          <router-link to="/user/profile" class="nav-user">
            {{ userStore.userInfo?.nickname || '个人中心' }}
          </router-link>
          <a class="nav-action" @click="handleLogout">退出</a>
        </template>
        <template v-else>
          <router-link :to="{ path: '/login', query: { redirect: route.fullPath } }">登录</router-link>
          <router-link to="/register">注册</router-link>
        </template>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
}
.header-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 24px;
}
.logo {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--ink);
}
.search {
  flex: 1;
  max-width: 400px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 6px 6px 16px;
  background: var(--bg-gray);
  border-radius: var(--radius-full);
}
.search-input {
  flex: 1;
  min-width: 0;
  padding: 4px 0;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  color: var(--ink);
}
.search-input::placeholder {
  color: var(--ink-faint);
}
.search-btn {
  padding: 5px 16px;
  border: none;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--link);
  font-size: 13px;
  cursor: pointer;
}
.search-btn:hover {
  text-decoration: underline;
}
.nav {
  display: flex;
  gap: 24px;
  margin-left: auto;
  align-items: center;
}
.nav a {
  font-size: 13px;
  color: #333;
  transition: color 0.2s;
}
.nav a:hover {
  color: #000;
}
.nav-user {
  font-weight: 600;
}
.nav-cart {
  position: relative;
  display: inline-flex;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}
.nav-cart::before {
  content: "";
  position: absolute;
  inset: -11px;
}
.cart-badge {
  position: absolute;
  top: -4px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 9px;
  background: var(--blue);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  transform: translateY(-0.5px);
  pointer-events: none;
}
@media (prefers-color-scheme: dark) {
  .cart-badge {
    background: #2997ff;
  }
}
.nav-action {
  cursor: pointer;
}
/* 移动端收紧：防止导航溢出造成页面横向滚动 */
@media (max-width: 720px) {
  .header-inner {
    gap: 14px;
    padding: 10px 14px;
  }
  .search {
    max-width: none;
    min-width: 0;
  }
  .nav {
    gap: 14px;
  }
  .nav-user {
    max-width: 88px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
