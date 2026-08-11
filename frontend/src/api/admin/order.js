import { request } from '@/utils/request'

// 后台订单分页列表
export function fetchOrders(params) {
  return request.get('/admin/orders', { params })
}

// 后台订单详情
export function fetchOrder(id) {
  return request.get(`/admin/orders/${id}`)
}

// 发货
export function shipOrder(id) {
  return request.post(`/admin/orders/${id}/ship`)
}
