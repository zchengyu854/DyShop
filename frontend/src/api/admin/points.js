import { request } from '@/utils/request'

// ============ 后台积分商城（ch13） ============
// 说明：与其他后台 API 一致走 request（baseURL /api）+ /admin/ 路径前缀，
// getTokenForPath 按 /admin/ 前缀自动挂后台 token。

// 商品分页/搜索（keyword 名称、status 1/0）
export function fetchPointsGoods(params) {
  return request.get('/admin/points/goods', { params })
}

// 新建商品
export function createPointsGoods(data) {
  return request.post('/admin/points/goods', data)
}

// 编辑商品
export function updatePointsGoods(id, data) {
  return request.put(`/admin/points/goods/${id}`, data)
}

// 上/下架（status: 1/0）
export function changePointsGoodsStatus(id, status) {
  return request.patch(`/admin/points/goods/${id}/status`, null, { params: { status } })
}

// 逻辑删除（仅下架商品）
export function deletePointsGoods(id) {
  return request.delete(`/admin/points/goods/${id}`)
}

// 兑换记录分页（goodsId、keyword 用户名/昵称）
export function fetchPointsExchanges(params) {
  return request.get('/admin/points/exchanges', { params })
}