<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const adminStore = useAdminStore()

const MENUS = [
  { to: '/admin/dashboard', label: '仪表盘' },
  { to: '/admin/orders', label: '订单管理' },
  { to: '/admin/products', label: '商品管理' },
  { to: '/admin/categories', label: '分类管理' },
  { to: '/admin/users', label: '用户管理' },
  { to: '/admin/coupons', label: '优惠券管理' },
  { to: '/admin/user-coupons', label: '用户券管理' },
  { to: '/admin/after-sales', label: '售后管理' },
  { to: '/admin/points-goods', label: '积分商城' },
]

const adminName = computed(() => adminStore.adminInfo?.nickname || adminStore.adminInfo?.username || '管理员')

function handleLogout() {
  // 仅清除后台会话，C 端登录态不受影响
  adminStore.logout()
  router.replace('/')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <router-link to="/admin/dashboard" class="brand">dyshop 管理后台</router-link>
      <nav class="menu">
        <router-link
          v-for="m in MENUS"
          :key="m.to"
          :to="m.to"
          class="menu-item"
          active-class="active"
        >
          {{ m.label }}
        </router-link>
      </nav>
      <router-link to="/" class="back">← 返回商城</router-link>
    </aside>

    <div class="main">
      <header class="topbar">
        <span class="crumb">后台管理</span>
        <div class="topbar-right">
          <span class="admin-name">{{ adminName }}</span>
          <button class="logout" @click="handleLogout">退出登录</button>
        </div>
      </header>
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  background: var(--bg-gray);
}
.sidebar {
  width: 208px;
  flex-shrink: 0;
  background: var(--bg);
  border-right: 1px solid var(--border-line);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 1.25rem 0.75rem;
}
.brand {
  display: block;
  padding: 0.25rem 0.75rem 1.25rem;
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
  text-decoration: none;
  border-bottom: 1px solid var(--border-line);
  margin-bottom: 0.875rem;
}
.menu {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
}
.menu-item {
  display: block;
  padding: 0.625rem 0.75rem;
  border-radius: 8px;
  font-size: 14px;
  color: var(--ink-secondary);
  text-decoration: none;
  transition: background 0.15s, color 0.15s;
}
.menu-item:hover {
  background: var(--bg-gray);
  color: var(--ink);
}
.menu-item.active {
  background: rgba(0, 113, 227, 0.08);
  color: var(--blue);
  font-weight: 600;
}
.back {
  display: block;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--ink-secondary);
  text-decoration: none;
}
.back:hover {
  color: var(--blue);
}
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.topbar {
  height: 56px;
  background: var(--bg);
  border-bottom: 1px solid var(--border-line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  position: sticky;
  top: 0;
  z-index: 10;
}
.crumb {
  font-size: 14px;
  color: var(--ink-faint);
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.admin-name {
  font-size: 14px;
  font-weight: 600;
}
.logout {
  height: 30px;
  padding: 0 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg);
  color: var(--ink);
  font-size: 13px;
  cursor: pointer;
}
.logout:hover {
  background: var(--bg-gray);
}
</style>
