<script setup>
// ============================================================================
// SidebarMenu —— 个人中心侧边菜单（纯展示/配置驱动组件，无业务逻辑）
// ----------------------------------------------------------------------------
// 由 menuConfig 数组渲染；点击按钮型入口（orders/todo）通过事件上抛由用户中心处理，
// 组件自身不持状态，保证「删除菜单项只改配置不动组件」。
// ============================================================================
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps({
  /** USER_MENU 数组结构：[{ group, items: [{ type:'link'|'orders'|'todo', to, label }] }] */
  menu: { type: Array, required: true },
})
const emit = defineEmits(['go-orders', 'todo'])

const route = useRoute()
const ordersActive = computed(() => route.name === 'user-orders')

/** index 上抛事件（含 item 供上层复用，组件不解业务语义） */
function onItemClick(item) {
  if (item.type === 'orders') emit('go-orders')
  else if (item.type === 'todo') emit('todo')
}
</script>

<template>
  <nav class="menu">
    <template v-for="group in menu" :key="group.group">
      <p class="menu-group">{{ group.group }}</p>
      <template v-for="item in group.items" :key="item.label">
        <router-link
          v-if="item.type === 'link'"
          :to="item.to"
          class="menu-item"
          active-class="active"
        >{{ item.label }}</router-link>
        <!-- button：orders/todo 非路由跳转，避免 router-link 的默认导航覆盖 goOrders 的 store 记忆逻辑 -->
        <button
          v-else
          class="menu-item"
          :class="{ active: item.type === 'orders' && ordersActive }"
          @click="onItemClick(item)"
        >{{ item.label }}</button>
      </template>
    </template>
  </nav>
</template>

<style scoped>
.menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 10px;
}
.menu-group {
  margin: 24px 12px 6px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  color: var(--ink-faint);
}
.menu-group:first-of-type {
  margin-top: 10px;
}
.menu-item {
  padding: 9px 12px;
  border: none;
  border-radius: var(--radius);
  background: transparent;
  font-size: 14px;
  font-family: inherit;
  text-align: left;
  color: var(--ink);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.menu-item:hover {
  background: var(--bg-gray);
}
.menu-item.active {
  background: var(--bg-gray);
  font-weight: 600;
  color: var(--blue);
}
@media (max-width: 720px) {
  /* 触控目标 ≥44px：窄屏下侧边菜单加高，避免误触相邻入口 */
  .menu-item {
    padding: 12px 12px;
  }
}
</style>