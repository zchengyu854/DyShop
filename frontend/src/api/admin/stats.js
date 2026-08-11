import { request } from '@/utils/request'

// 后台仪表盘统计
export function fetchOverview() {
  return request.get('/admin/stats/overview')
}

export function fetchTrend(days) {
  return request.get('/admin/stats/trend', { params: { days } })
}
