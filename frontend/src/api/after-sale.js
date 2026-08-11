import { request } from '@/utils/request'

// ============ C 端售后（ch12） ============

// 我的售后列表（status 筛选 + 分页）
export function fetchAfterSales(params) {
  return request.get('/user/after-sales', { params })
}

// 申请售后：{ orderItemId, reason }
export function createAfterSale(data) {
  return request.post('/user/after-sales', data)
}

// 取消申请（仅待处理）
export function cancelAfterSale(id) {
  return request.post(`/user/after-sales/${id}/cancel`)
}
