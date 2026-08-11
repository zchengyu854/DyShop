import { defineStore } from 'pinia'

// 订单模块筛选状态（跨路由/跨组件保持）
// -------------------------------------------------------------------
// 故障根因（2026-08 内嵌重构排查）：
//   订单列表迁入 /user/orders 后，切到「个人资料」再切回时，侧边栏入口
//   push 无 query 的 URL，watch(route.query.status) 把筛选重置为「全部」，
//   keep-alive 缓存的组件状态被 URL 变化覆盖。
// 修复：tab 状态收敛到 Pinia ——
//   · switchTab / watch(query) 写入 store，URL 与 store 双向一致；
//   · 从其他模块进入订单列表时（UserCenter.goOrders）用 store 拼接 query，
//     筛选跨模块往返保持；
//   · URL 有 query 时以 URL 为准（外部入口/分享链接），无 query 时以 store 兜底。
export const useUserOrdersStore = defineStore('userOrders', {
  state: () => ({
    tab: 'all', // 'all' | '0' | '1' | '2' | '3' | '4'
  }),
})
