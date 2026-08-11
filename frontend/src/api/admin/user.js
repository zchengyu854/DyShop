import { request } from '@/utils/request'

// 后台用户分页列表
export function fetchAdminUsers(params) {
  return request.get('/admin/users', { params })
}

// 禁用/启用
export function changeUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, null, { params: { status } })
}

// 授权/取消管理员
export function changeUserRole(id, role) {
  return request.put(`/admin/users/${id}/role`, null, { params: { role } })
}

// 当前管理员账号信息（后台侧独立登录态）
export function fetchAdminMe() {
  return request.get('/admin/users/me')
}
