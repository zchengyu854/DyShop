import { request } from '@/utils/request'

// ============ 后台优惠券管理（ch11） ============

// 模板分页/搜索
export function fetchCouponTemplates(params) {
  return request.get('/admin/coupon/templates', { params })
}

// 新建模板
export function createCouponTemplate(data) {
  return request.post('/admin/coupon/templates', data)
}

// 编辑模板（已发放模板仅允许改名称）
export function updateCouponTemplate(id, data) {
  return request.put(`/admin/coupon/templates/${id}`, data)
}

// 启用/停用（status: 1/0）
export function changeCouponTemplateStatus(id, status) {
  return request.patch(`/admin/coupon/templates/${id}/status`, null, { params: { status } })
}

// 删除模板（仅停用状态）
export function deleteCouponTemplate(id) {
  return request.delete(`/admin/coupon/templates/${id}`)
}

// 发放：{ templateId, target: 'all'|'manual', userIds? }
export function grantCoupon(data) {
  return request.post('/admin/coupon/grants', data)
}

// 用户券分页/搜索（keyword 用户名/手机号、templateId、status、source）
export function fetchUserCoupons(params) {
  return request.get('/admin/coupon/user-coupons', { params })
}

// 作废用户券（仅未使用）
export function voidUserCoupon(id) {
  return request.patch(`/admin/coupon/user-coupons/${id}/void`)
}
