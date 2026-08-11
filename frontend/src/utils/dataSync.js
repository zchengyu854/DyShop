// 全局数据失效广播
// -------------------------------------------------------------------
// 故障根因（排查结论）：
//   订单数据只存在于各页面组件局部 ref，无全局 Store；下单/支付/取消成功
//   仅刷新「当前组件」，且组件只在 onMounted 拉取一次。当用户从下单成功页、
//   支付结果页经浏览器前进/后退（BFCache 恢复，onMounted 不触发）返回订单
//   列表，或另一视图操作成功后本视图仍挂载时，页面展示旧数据。
// -------------------------------------------------------------------
// 修复策略：
//   引入「领域命名空间」事件总线 —— 任一数据变更成功回调调用 notifyDataChanged(ns)，
//   所有已挂载且订阅该命名空间的视图收到后静默重拉（不清列表、不闪骨架屏），
//   配合 pageshow(persisted) / visibilitychange 覆盖「重新可见但不重新 mount」。
//   （2026-08 增强）广播同时写入 localStorage：window.CustomEvent 仅在同一
//   tab 内可达，双开/多 tab 时另一 tab 的订单/商品视图喝不到更新；storage
//   事件天然跨 tab（且不会回响到触发 tab 自身），消费端统一监听两者即可。
// -------------------------------------------------------------------
// 修复后数据流对比：
//   修复前：createOrder → 仅刷新本地 pendingOrder 与购物车 → 回列表 onMounted 时才拉取。
//   修复后：createOrder → notifyDataChanged('order') → 已挂载的订单列表/详情静默刷新，
//           （同 tab：CustomEvent；跨 tab：localStorage + storage 事件）
//           新进入的页面仍由 onMounted 拉取，但强制 no-store 保证拿到的就是最新 DB 数据。
export const DATA_CHANGED_EVENT = 'dyshop:data-changed'
/** localStorage 键：跨 tab 广播载体（值仅作信号，内容无意义） */
const STORAGE_KEY = 'dyshop:data-changed'
/** 订单领域：列表/详情/待支付等视图订阅 */
export const ORDER_NS = 'order'
/** 商品领域：详情页库存/价格视图订阅 */
export const PRODUCT_NS = 'product'

/**
 * 广播某领域数据已变更。
 * @param {string} namespace 领域命名空间（ORDER_NS / PRODUCT_NS）
 */
export function notifyDataChanged(namespace) {
  const detail = { namespace, ts: Date.now() }
  // 同 tab：CustomEvent 直达当前窗口所有已挂载视图
  window.dispatchEvent(new CustomEvent(DATA_CHANGED_EVENT, { detail }))
  // 跨 tab：写入 localStorage 触发其他标签页的 storage 事件（try 兜底隐私模式）
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(detail))
  } catch {
    /* 隐私模式/禁用存储时静默降级为仅同 tab 广播 */
  }
}

/**
 * 从事件对象中解析领域命名空间。兼容三种来源：
 *   1) window CustomEvent（同 tab）—— detail 挂在 event.detail
 *   2) storage 事件（跨 tab）      —— 载荷在 event.newValue（JSON 字符串）
 *   3) 占位：无 payload 的裸事件（容错）
 * @param {Event} event CustomEvent 或 StorageEvent
 * @returns {string|null} 命名空间；无法解析或为自身广播时返回 null
 */
export function parseNamespace(event) {
  if (event && event.detail && event.detail.namespace) {
    return event.detail.namespace
  }
  // storage 事件：newValue 为 JSON 字符串；旧值为 null（初始化）则忽略
  if (event && event.key === STORAGE_KEY && event.newValue) {
    try {
      const parsed = JSON.parse(event.newValue)
      return parsed && parsed.namespace ? parsed.namespace : null
    } catch {
      return null
    }
  }
  return null
}

/** 校验事件是否属于指定领域（兼容同 tab CustomEvent 与跨 tab StorageEvent） */
export function isNamespaceEvent(event, namespace) {
  return parseNamespace(event) === namespace
}
