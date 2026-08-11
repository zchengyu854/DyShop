import { request } from '@/utils/request'

// 订单操作接口：动作类请求使用更紧的超时（≤8s，弱网下前端快速失败回滚），
// 与列表/详情读取（10s 默认）区分 —— 动作超时后由 useOrderAction 统一
// 提示「网络较慢，请稍后重试」并回滚乐观状态。
const ACTION_TIMEOUT = 8000

// 创建订单：{ source: 'cart'|'buyNow', addressId, remark, productId?, quantity?, couponId? }
export function createOrder(data) {
  return request.post('/orders', data)
}

// 结算价预览（ch11）：{ source, couponId?, productId?, skuId?, quantity? }
export function previewOrder(params) {
  return request.get('/orders/preview', { params })
}

// 我的订单列表（status 缺省=全部）
export function fetchOrders(status) {
  return request.get('/orders', { params: status != null ? { status } : {} })
}

// 订单详情
export function fetchOrder(id) {
  return request.get(`/orders/${id}`)
}

// 取消订单（仅待支付；幂等：已取消订单重复调用返回成功）
export function cancelOrder(id) {
  return request.post(`/orders/${id}/cancel`, null, { timeout: ACTION_TIMEOUT })
}

// 模拟支付（仅待支付；幂等：已支付订单重复调用返回成功）
export function payOrder(id) {
  return request.post(`/orders/${id}/pay`, null, { timeout: ACTION_TIMEOUT })
}

// 确认收货（仅待收货；幂等：已完成订单重复调用返回成功）
export function confirmOrder(id) {
  return request.post(`/orders/${id}/confirm`, null, { timeout: ACTION_TIMEOUT })
}

// 删除订单（仅已完成/已取消，逻辑删除；幂等：已删除订单重复调用返回成功）
export function deleteOrder(id) {
  return request.delete(`/orders/${id}`, { timeout: ACTION_TIMEOUT })
}
