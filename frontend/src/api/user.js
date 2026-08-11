import { request } from '@/utils/request'

// 个人资料
export function updateProfile(data) {
  return request.put('/user/profile', data)
}

// 修改密码
export function updatePassword(data) {
  return request.put('/user/password', data)
}

// 我的收藏
export function fetchFavorites(params) {
  return request.get('/user/favorites', { params })
}

export function addFavorite(productId) {
  return request.post(`/user/favorites/${productId}`)
}

export function removeFavorite(productId) {
  return request.delete(`/user/favorites/${productId}`)
}

// 收藏状态：{ favorited }
export function fetchFavoriteStatus(productId) {
  return request.get(`/user/favorites/status/${productId}`)
}

// 个人中心首页聚合统计（一次请求，合并订单概览 + 会员全景）：
// { totalSpent, totalOrders, pendingShipment, pendingReceive, points,
//   levelCode, levelName, nextLevelThreshold, needAmount, progressPct }
export function fetchDashboardStats() {
  return request.get('/user/dashboard-stats')
}

// 会员全景：{ level, totalConsumption, annualConsumption, nextLevel, needAmount, progressPct, points }
export function fetchMemberOverview() {
  return request.get('/user/member/overview')
}

// 积分流水
export function fetchMemberPoints(params) {
  return request.get('/user/member/points', { params })
}

// ============ 积分商城（ch13） ============

// 积分商城：在售商品 + 我的可用积分余额 → { myPoints, goods:[PointsGoodsVO] }
export function fetchPointsMall() {
  return request.get('/user/points/goods')
}

// 兑换虚拟商品 { goodsId } → { exchangeNo, goodsType, code?, couponId?, pointCost }
export function exchangePoints(goodsId) {
  return request.post('/user/points/exchange', null, { params: { goodsId } })
}

// 我的兑换记录分页
export function fetchPointsExchanges(params) {
  return request.get('/user/points/exchanges', { params })
}
