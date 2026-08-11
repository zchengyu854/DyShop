import { request } from '@/utils/request'

// 后台分类列表（含停用）
export function fetchAdminCategories() {
  return request.get('/admin/categories')
}

export function createAdminCategory(data) {
  return request.post('/admin/categories', data)
}

export function updateAdminCategory(id, data) {
  return request.put(`/admin/categories/${id}`, data)
}

export function changeCategoryStatus(id, status) {
  return request.put(`/admin/categories/${id}/status`, null, { params: { status } })
}

export function deleteAdminCategory(id) {
  return request.delete(`/admin/categories/${id}`)
}
