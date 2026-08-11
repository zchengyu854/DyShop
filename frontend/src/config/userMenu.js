// ============================================================================
// 个人中心侧边菜单配置（ch10 布局精简）
// ----------------------------------------------------------------------------
// 单一数据源：右侧订单页 Tab 已覆盖全部订单状态筛选，
// 删除侧边栏六个状态分类入口（全部订单/待支付/待发货/待收货/已完成/已取消），
// 仅保留模块入口与非订单类导航，避免路径重复与认知干扰。
// 调整菜单 = 增删此项数组，无需改动 SidebarMenu 组件逻辑。
// ============================================================================

/** item.type: 'link' 路由跳转 | 'orders' 订单模块入口 | 'todo' 占位 */
export const USER_MENU = [
  {
    group: '账户管理',
    items: [
      { type: 'link', to: '/user/profile', label: '个人资料' },
      { type: 'link', to: '/user/password', label: '安全设置' },
      { type: 'link', to: '/user/addresses', label: '收货地址' },
    ],
  },
  {
    group: '交易记录',
    items: [
      // 「我的订单」= 订单模块唯一入口（进入后由页内 Tab 承载 全部/待支付/待发货/待收货/已完成 筛选）。
      // ch10 已移除下列冗余状态入口（列表页 Tab 与 URL query 均支持 ?status=0..3 直达）：
      // { type: 'link', to: '/user/orders?status=0', label: '待支付' },
      // { type: 'link', to: '/user/orders?status=1', label: '待发货' },
      // { type: 'link', to: '/user/orders?status=2', label: '待收货' },
      // { type: 'link', to: '/user/orders?status=3', label: '已完成' },
      // { type: 'link', to: '/user/orders?status=4', label: '已取消' },
      { type: 'orders', label: '我的订单' },
      { type: 'link', to: '/user/coupons', label: '我的优惠券' },
      { type: 'link', to: '/user/coupon-center', label: '领券中心' },
      { type: 'link', to: '/user/aftersales', label: '我的售后' },
      // ch13 积分商城：积分消费出口（兑换优惠券 / 兑换码）+ 明细查询
      { type: 'link', to: '/user/points-mall', label: '积分商城' },
      { type: 'link', to: '/user/points', label: '积分明细' },
    ],
  },
  {
    group: '偏好设置',
    items: [
      { type: 'link', to: '/user/favorites', label: '我的收藏' },
      // ch14 浏览历史：占用型 todo 占位替换为真实入口（数据层 useRecentViewed 已接入详情页记录）
      { type: 'link', to: '/user/history', label: '浏览历史' },
    ],
  },
]

/** 管理后台专属组：仅管理员追加渲染（拆分导出便于 SidebarMenu 按需合并） */
export const ADMIN_MENU = [
  {
    group: '管理后台',
    adminOnly: true,
    items: [{ type: 'link', to: '/admin/dashboard', label: '后台管理' }],
  },
]