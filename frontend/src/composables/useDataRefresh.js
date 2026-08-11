import { onBeforeUnmount, onMounted } from 'vue'
import { DATA_CHANGED_EVENT, isNamespaceEvent } from '@/utils/dataSync'

// 组件「重新可见」数据刷新组合式函数
// -------------------------------------------------------------------
// 故障根因：OrderList/OrderDetail 仅在 onMounted 拉取数据，缺少以下
//           触发时机，导致返回页面时仍展示旧数据：
//   1) BFCache 恢复（pageshow + persisted）：浏览器前进/后退从缓存还原整页
//      快照，Vue 生命周期与网络请求都不会执行，onMounted 不触发。
//   2) 页签切回/App 切后台回前台（visibilitychange → visible）。
//   3) 其他视图变更成功（notifyDataChanged 广播），本视图仍挂载时喝不到更新。
//   4) （2026-08 增强）多 tab 场景：另一标签页下单/支付成功后，本标签页
//      的订单视图收不到 CustomEvent —— 需监听跨 tab 的 storage 事件。
// -------------------------------------------------------------------
// 用法：
//   const { refresh } = useDataRefresh(ORDER_NS, ({ silent }) => load({ silent }))
//   load({ silent: false }) —— 首次/手动：置 loading 展示骨架屏
//   load({ silent: true  }) —— 静默：后台取最新数据，不打断当前展示
//   竞态守卫由调用方 load 内部通过自增 seq 实现（各页面均已内置）。
export function useDataRefresh(namespace, loadFn, { immediate = true } = {}) {
  function onDataChanged(event) {
    if (isNamespaceEvent(event, namespace)) loadFn({ silent: true })
  }

  // 跨 tab 广播：storage 事件（isNamespaceEvent 已兼容 StorageEvent 载荷解析）
  function onStorageChanged(event) {
    if (isNamespaceEvent(event, namespace)) loadFn({ silent: true })
  }

  // BFCache 恢复页面快照后强制重拉，杜绝「回列表还是旧订单」
  function onPageShow(event) {
    if (event.persisted) loadFn({ silent: true })
  }

  function onVisibility() {
    if (document.visibilityState === 'visible') loadFn({ silent: true })
  }

  onMounted(() => {
    window.addEventListener(DATA_CHANGED_EVENT, onDataChanged)
    window.addEventListener('storage', onStorageChanged)
    window.addEventListener('pageshow', onPageShow)
    document.addEventListener('visibilitychange', onVisibility)
    if (immediate) loadFn({ silent: false })
  })

  onBeforeUnmount(() => {
    window.removeEventListener(DATA_CHANGED_EVENT, onDataChanged)
    window.removeEventListener('storage', onStorageChanged)
    window.removeEventListener('pageshow', onPageShow)
    document.removeEventListener('visibilitychange', onVisibility)
  })

  return { refresh: (opts) => loadFn(opts || {}) }
}