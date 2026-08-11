import { request } from '@/utils/request'

// ============ C 端优惠券（ch11） ============

// 领券中心模板列表（含每人领取状态与剩余量）
export function fetchCouponCenter() {
  return request.get('/user/coupon/center')
}

// 领取（幂等：重复领取 409）
export function claimCoupon(templateId) {
  return request.post('/user/coupon/center/claim', null, { params: { templateId } })
}

// 我的优惠券（status=0/1/2 分态，page 分页）
export function fetchMyCoupons(params) {
  return request.get('/user/coupon/mine', { params })
}
