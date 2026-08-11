import { request } from '@/utils/request'

// 后台商品分页列表
export function fetchAdminProducts(params) {
  return request.get('/admin/products', { params })
}

// 后台商品详情（含 specs/skus 原文）
export function fetchAdminProduct(id) {
  return request.get(`/admin/products/${id}`)
}

// 新增商品
export function createAdminProduct(data) {
  return request.post('/admin/products', data)
}

// 编辑商品
export function updateAdminProduct(id, data) {
  return request.put(`/admin/products/${id}`, data)
}

// 上下架
export function changeProductStatus(id, status) {
  return request.put(`/admin/products/${id}/status`, null, { params: { status } })
}

// 删除商品
export function deleteAdminProduct(id) {
  return request.delete(`/admin/products/${id}`)
}
