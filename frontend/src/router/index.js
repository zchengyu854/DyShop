// 路由骨架 —— 页面文件均为最小占位壳，业务实现后续填充
//
// 权限约定（meta）：
//   requiresAuth: true  —— 需登录（C 端）
//   role: 'admin'       —— 需管理员角色（后台管理）
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // ---------- C 端 ----------
  { path: '/', name: 'home', component: () => import('@/views/shop/Home.vue') },
  { path: '/products', name: 'product-list', component: () => import('@/views/shop/ProductList.vue') },
  { path: '/products/:id', name: 'product-detail', component: () => import('@/views/shop/ProductDetail.vue') },
  { path: '/cart', name: 'cart', component: () => import('@/views/shop/Cart.vue'), meta: { requiresAuth: true } },
  { path: '/checkout', name: 'checkout', component: () => import('@/views/shop/Checkout.vue'), meta: { requiresAuth: true } },
  // 领券中心已并入个人中心左右布局（/user/coupon-center），旧地址重定向保持兼容
  { path: '/coupon/center', redirect: '/user/coupon-center', meta: { requiresAuth: true } },

  // ---------- 用户中心 ----------
  { path: '/login', name: 'login', component: () => import('@/views/user/Login.vue') },
  { path: '/register', name: 'register', component: () => import('@/views/user/Register.vue') },
  {
    path: '/user',
    component: () => import('@/views/user/UserCenter.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/user/profile' },
      { path: 'profile', name: 'user-profile', component: () => import('@/views/user/ProfileView.vue') },
      { path: 'password', name: 'user-password', component: () => import('@/components/user/PasswordPanel.vue') },
      { path: 'favorites', name: 'user-favorites', component: () => import('@/components/user/FavoritesPanel.vue') },
      // ch14 浏览历史：设备本地数据（localStorage），无需后端同步
      { path: 'history', name: 'user-history', component: () => import('@/components/user/RecentViewsPanel.vue') },
      { path: 'addresses', name: 'user-addresses', component: () => import('@/views/user/AddressList.vue') },
      // 订单模块内嵌（2026-08 重构：从独立顶级路由迁入个人中心，
      // 与「个人资料/地址/收藏」同构；组件级 lazy import，数据仅在激活时请求）
      {
        path: 'orders',
        name: 'user-orders',
        component: () => import('@/views/user/UserOrders.vue'),
      },
      { path: 'coupons', name: 'user-coupons', component: () => import('@/views/user/MyCoupons.vue') },
      { path: 'coupon-center', name: 'user-coupon-center', component: () => import('@/views/user/CouponCenter.vue') },
      { path: 'aftersales', name: 'user-aftersales', component: () => import('@/views/user/AfterSales.vue') },
      // 积分商城与积分明细（ch13）
      { path: 'points-mall', name: 'user-points-mall', component: () => import('@/views/user/PointsMall.vue') },
      { path: 'points', name: 'user-points', component: () => import('@/views/user/PointsDetail.vue') },
    ],
  },
  // 旧地址兼容：独立页迁移至 /user/addresses 后重定向
  { path: '/addresses', redirect: '/user/addresses', meta: { requiresAuth: true } },

  // ---------- 订单 ----------
  // 2026-08 重构：订单列表迁入个人中心 /user/orders；
  // /orders 保留为兼容重定向（旧书签/分享链接/外部入口直达新位置，不产生跳转断层）。
  {
    path: '/orders',
    name: 'order-list-legacy',
    redirect: '/user/orders',
    meta: { requiresAuth: true },
  },
  // 订单详情为独立聚焦阅读页，保留顶级路由（从列表 push 进入，back 返回列表，
  // keep-alive 保证列表状态/滚动不丢）
  { path: '/orders/:id', name: 'order-detail', component: () => import('@/views/order/OrderDetail.vue'), meta: { requiresAuth: true } },

  // ---------- 后台管理 ----------
  // 独立后台登录页（ch08 需求：管理员独立登录窗口；不加 meta.role，买家/游客均可访问该页做校验）
  { path: '/admin/login', name: 'admin-login', component: () => import('@/views/admin/AdminLogin.vue') },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'products', name: 'admin-products', component: () => import('@/views/admin/ProductManage.vue') },
      { path: 'orders', name: 'admin-orders', component: () => import('@/views/admin/OrderManage.vue') },
      { path: 'categories', name: 'admin-categories', component: () => import('@/views/admin/CategoryManage.vue') },
      { path: 'users', name: 'admin-users', component: () => import('@/views/admin/UserManage.vue') },
      // 会员管理并入用户管理模块（ch09 合并），旧地址重定向保持兼容
      { path: 'members', redirect: '/admin/users' },
      { path: 'coupons', name: 'admin-coupons', component: () => import('@/views/admin/CouponTemplateManage.vue') },
      { path: 'user-coupons', name: 'admin-user-coupons', component: () => import('@/views/admin/UserCouponManage.vue') },
      { path: 'after-sales', name: 'admin-after-sales', component: () => import('@/views/admin/AfterSaleManage.vue') },
      { path: 'points-goods', name: 'admin-points-goods', component: () => import('@/views/admin/PointsGoodsManage.vue') },
      { path: 'points-exchanges', name: 'admin-points-exchanges', component: () => import('@/views/admin/PointsExchangeManage.vue') },
    ],
  },

  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/shop/Home.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 交互加固：每次进入新路由回到顶部，防止从长页面（如个人中心）跳转后
  // 保留旧滚动位置，导致半透明 sticky 顶栏盖住上屏卡片的操作按钮（无法点击）。
  // 返回/前进则恢复浏览器保存的滚动位置。
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, top: 80 }
    return { top: 0 }
  },
})

// 全局前置守卫：登录校验 + 角色校验（ch08：meta.role='admin' 需 ROLE_ADMIN，未登录/非管理员一律引导到独立后台登录页 /admin/login）
// ch08.3：后台守卫基于独立后台 token（dyshop_admin_token）与 adminStore，与 C 端会话互不影响
// 注意：pinia 需在路由守卫执行前初始化（main.js 中 app.use(createPinia()) 先于 app.use(router)）
import { getAdminToken, getToken } from '@/utils/auth'
import { useAdminStore } from '@/stores/admin'
import { useUserStore } from '@/stores/user'

router.beforeEach(async (to) => {
  if (to.meta.role === 'admin') {
    const adminStore = useAdminStore()
    // 未登录后台 → 后台登录页（保留回跳地址）
    if (!getAdminToken()) {
      return { path: '/admin/login', query: { redirect: to.fullPath } }
    }
    // 已登录后台但缺用户信息（刷新后）：补拉；失败(401/网络) → 后台登录页
    if (!adminStore.adminInfo) {
      try {
        await adminStore.fetchAdminInfo()
      } catch (e) {
        adminStore.logout()
        return { path: '/admin/login', query: { redirect: to.fullPath } }
      }
    }
    // 非管理员访问后台 → 后台登录页并提示
    if (adminStore.adminInfo?.role !== 1) {
      adminStore.logout()
      return { path: '/admin/login', query: { redirect: to.fullPath, denied: '1' } }
    }
    return true
  }
  // 普通需登录页面（C 端）沿用原逻辑
  if (to.meta.requiresAuth && !getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
