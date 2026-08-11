import { request } from '@/utils/request'

// 等级配置列表
export function fetchMemberLevels() {
  return request.get('/admin/member/levels')
}

// 更新等级配置 { threshold?, discountRate?, pointRate? }
export function updateMemberLevel(id, data) {
  return request.put(`/admin/member/levels/${id}`, data)
}

// 会员分页列表
export function fetchMemberUsers(params) {
  return request.get('/admin/member/users', { params })
}