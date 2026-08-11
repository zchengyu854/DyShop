<script setup>
import { useRouter } from 'vue-router'
import { useRecentViewed } from '@/composables/useRecentViewed'
import { toast } from '@/utils/toast'

const router = useRouter()
const { list } = useRecentViewed()

// 快捷入口 → 订单列表对应状态筛选（OrderList 的 tab 已与 /orders?status= 双向同步）。
// 故障根因（2026-08 排查「个人中心按钮点击无跳转」）：
//   此处此前为 <button @click="handleAction"> 且 handler 仅 toast「订单模块开发中」，
//   点击无任何跳转 —— 用户期望直达待支付/待收货订单。
// 修复：改为 router-link 声明式导航（无需 JS 逻辑、SEO/分享/前进后退均可达）；
//   无对应模块的入口（售后/退款）保留明确提示而非静默。
const entries = [
  { label: '待付款', to: { path: '/user/orders', query: { status: '0' } } },
  { label: '待收货', to: { path: '/user/orders', query: { status: '2' } } },
  { label: '待评价', to: { path: '/user/orders', query: { status: '3' } } },
  { label: '售后/退款', to: { path: '/user/aftersales' } },
]

const recent = list.value.slice(0, 4)

function handleTodo(label) {
  toast.info(`${label}模块开发中，敬请期待`)
}

function goDetail(id) {
  try {
    router.push(`/products/${id}`)
  } catch (e) {
    toast.error('页面跳转失败，请重试')
  }
}
</script>

<template>
  <section class="activity">
    <h3 class="card-title">快捷操作与最近动态</h3>

    <div class="quick-row">
      <router-link
        v-for="a in entries.filter((e) => e.to)"
        :key="a.label"
        :to="a.to"
        class="quick-btn"
      >
        {{ a.label }}
      </router-link>
      <button
        v-if="entries.find((e) => !e.to)"
        class="quick-btn"
        @click="handleTodo('售后/退款')"
      >
        售后/退款
      </button>
    </div>

    <div class="recent">
      <p class="recent-label">最近浏览</p>
      <div v-if="recent.length" class="recent-list">
        <button
          v-for="r in recent"
          :key="r.id"
          class="recent-item"
          @click="goDetail(r.id)"
        >
          <img :src="r.mainImage" :alt="r.name" loading="lazy" />
          <span class="recent-price">¥{{ Number(r.price).toFixed(2) }}</span>
        </button>
      </div>
      <p v-else class="recent-empty">暂无浏览记录，去逛逛吧</p>
    </div>
  </section>
</template>

<style scoped>
.activity {
  background: var(--bg);
  border-radius: var(--radius-card);
  padding: 20px 24px;
  box-shadow: var(--shadow-card);
}
.card-title {
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--ink);
}
.quick-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap; /* 窄屏换行，避免溢出 */
}
/* router-link 版快捷按钮：与旧 button 样式一致，保证视觉无回归 */
.quick-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 44px; /* 移动端触控目标 ≥44px */
  padding: 0 18px;
  border: 1px solid var(--border-line);
  border-radius: var(--radius-full);
  background: var(--bg);
  color: var(--ink);
  font-size: 13px;
  text-decoration: none;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}
.quick-btn:hover {
  border-color: var(--blue);
  color: var(--blue);
}
.quick-badge {
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--blue);
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
}
.recent {
  margin-top: 18px;
}
.recent-label {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--ink-secondary);
}
.recent-list {
  display: flex;
  gap: 12px;
}
.recent-item {
  position: relative;
  width: 64px;
  height: 64px;
  padding: 0;
  border: none;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: var(--bg-gray);
}
.recent-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.recent-price {
  position: absolute;
  right: 3px;
  bottom: 3px;
  padding: 1px 5px;
  border-radius: 5px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 10px;
}
.recent-empty {
  margin: 0;
  font-size: 13px;
  color: var(--ink-faint);
}
</style>
