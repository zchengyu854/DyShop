import { request } from '@/utils/request'

// ============ 后台售后管理（ch12） ============

// 售后分页（status/keyword=订单号/用户名/商品名）
export function fetchAdminAfterSales(params) {
  return request.get('/admin/after-sales', { params })
}

// 同意：模拟退款
export function approveAfterSale(id) {
  return request.post(`/admin/after-sales/${id}/approve`)
}

// 拒绝：必填理由
export function rejectAfterSale(id, reason) {
  return request.post(`/admin/after-sales/${id}/reject`, { reason })
}
