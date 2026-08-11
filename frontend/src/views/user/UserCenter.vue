<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import HomeHeader from '@/components/home/HomeHeader.vue'
import SidebarMenu from '@/components/user/SidebarMenu.vue'
import { fetchMemberOverview } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { useUserOrdersStore } from '@/stores/userOrders'
import { toast } from '@/utils/toast'
import { USER_MENU, ADMIN_MENU } from '@/config/userMenu'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 会员卡片（ch09）：等级/进度/距下一级，来自 GET /api/user/member/overview
const member = ref(null)

const vipCard = computed(() => {
  const m = member.value
  if (!m) return { name: '会员', pct: 0, hint: '' }
  const pct = Math.max(0, Math.min(100, m.progressPct ?? 0))
  const hint = m.nextLevel
    ? `再消费 ¥${Number(m.needAmount || 0).toLocaleString('zh-CN')} 升级${m.nextLevel.name}`
    : '已达最高等级'
  return { name: m.level?.name || '会员', pct, hint }
})

// 侧边栏「我的订单」：已处于订单模块时点击保留当前筛选（不导航、不重置）；
// 从其他模块进入时，用订单 store 记忆的筛选拼接 query —— 跨模块往返不丢筛选。
function goOrders() {
  if (route.name === 'user-orders') return
  const ordersStore = useUserOrdersStore()
  router.push({
    path: '/user/orders',
    query: ordersStore.tab === 'all' ? {} : { status: ordersStore.tab },
  })
}

const avatarChar = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username
  return name ? name.slice(0, 1) : '?'
})

function handleLogout() {
  userStore.logout()
  router.push('/')
}

function notifyTodo() {
  toast.info('该功能开发中，敬请期待')
}

// ch10：订单状态分类从侧栏移除（列表页 Tab + URL query 承担筛选），
// 菜单纯配置驱动（src/config/userMenu.js），增删项不改组件逻辑。
const menu = computed(() => (userStore.isAdmin ? [...USER_MENU, ...ADMIN_MENU] : USER_MENU))

onMounted(async () => {
  userStore.fetchUserInfo()
  try {
    member.value = await fetchMemberOverview()
  } catch {
    // 会员接口失败不影响页面主体
  }
})
</script>

<template>
  <div class="user-center">
    <HomeHeader />
    <div class="layout">
      <aside class="sidebar">
        <div class="user-card">
          <div class="avatar">{{ avatarChar }}</div>
          <div class="user-meta">
            <p class="nickname">{{ userStore.userInfo?.nickname || '未登录' }}</p>
            <p class="username">{{ userStore.userInfo?.username }}</p>
          </div>
        </div>

        <SidebarMenu
          class="menu-root"
          :menu="menu"
          @go-orders="goOrders"
          @todo="notifyTodo"
        />

        <div class="sidebar-foot">
          <button class="vip-card" @click="notifyTodo">
            <span class="vip-name">{{ vipCard.name }}</span>
            <span class="vip-track"><span class="vip-fill" :style="{ width: vipCard.pct + '%' }" /></span>
            <span class="vip-hint">{{ vipCard.hint }}</span>
          </button>
          <button class="logout-btn" @click="handleLogout">退出登录</button>
        </div>
      </aside>

      <main class="content">
        <!-- keep-alive 仅缓存订单模块：切走再切回保留滚动位置/筛选 tab/已加载数据，
             其余模块（资料/地址/收藏）不缓存，避免表单状态串扰 -->
        <router-view v-slot="{ Component }">
          <keep-alive :include="['UserOrders']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.user-center {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-gray);
}
.layout {
  flex: 1;
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 20px 48px;
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 28px;
  align-items: start;
}
.sidebar {
  position: sticky;
  top: 76px;
  background: var(--bg);
  border-radius: var(--radius-card);
  padding: 20px 16px;
  box-shadow: var(--shadow-card);
}
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 4px 6px 20px;
  border-bottom: 1px solid var(--border-line);
}
.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6e6e73, #1d1d1f);
  color: #fff;
  font-size: 24px;
  font-weight: 600;
  flex-shrink: 0;
}
.user-meta {
  min-width: 0;
}
.nickname {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.username {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--ink-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.menu-root {
  padding-top: 10px;
}
.sidebar-foot {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-line);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.vip-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: none;
  border-radius: 14px;
  background: var(--bg-gray);
  color: var(--ink);
  text-align: left;
  cursor: pointer;
  transition: background 0.2s;
}
.vip-card:hover {
  background: var(--border-line);
}
.vip-name {
  font-size: 13px;
  font-weight: 600;
}
.vip-track {
  height: 4px;
  border-radius: var(--radius-full);
  background: var(--border);
  overflow: hidden;
}
.vip-fill {
  display: block;
  width: 72%;
  height: 100%;
  border-radius: var(--radius-full);
  background: var(--blue);
}
.vip-hint {
  font-size: 11px;
  color: var(--ink-secondary);
}
.logout-btn {
  height: 44px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--bg);
  color: var(--ink);
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}
.logout-btn:hover {
  color: #ff3b30;
  border-color: #ff3b30;
}
.content {
  min-width: 0;
}
@media (max-width: 720px) {
  .layout {
    grid-template-columns: 1fr;
    padding-top: 20px;
  }
  .sidebar {
    position: static;
  }
}
</style>