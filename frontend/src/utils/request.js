import axios from 'axios'
import { getTokenForPath, removeAdminToken, removeToken } from './auth'

// 统一请求封装：按路径自动附加对应 Bearer Token（/admin/** 后台 token，其余 C 端 token）、
// 统一解包后端 Result<T>、401 时清除对应 token 并跳对应登录页
const createService = (baseURL) => {
  const service = axios.create({ baseURL, timeout: 10000 })

  service.interceptors.request.use((config) => {
    const token = getTokenForPath(config.url)
    if (token) config.headers.Authorization = `Bearer ${token}`
    // 订单等交易接口禁止依赖浏览器/代理 HTTP 缓存，前端显式要求 no-store，
    // 避免下单成功回调后 GET 列表仍命中缓存返回旧数据（后端 CacheControlFilter 双保险）。
    if (config.url && isOrderUrl(config.url)) {
      config.headers['Cache-Control'] = 'no-store, max-age=0'
      config.headers['Pragma'] = 'no-cache'
    }
    return config
  })

  service.interceptors.response.use(
    (response) => {
      // 后端统一返回 { code, message, data }，code=0 表示成功
      const res = response.data
      if (res.code !== 0) {
        if (res.code === 401) handleUnauthorized(response.config)
        const err = new Error(res.message || '请求失败')
        err.code = res.code
        return Promise.reject(err)
      }
      return res.data
    },
    (error) => {
      // HTTP 错误（4xx/5xx）：透出后端 Result 的 message，并附加 status 供页面判断
      const status = error.response?.status
      const res = error.response?.data
      const err = new Error(res?.message || error.message || '网络异常，请稍后重试')
      err.status = status
      if (status === 401) handleUnauthorized(error.config)
      return Promise.reject(err)
    }
  )

  return service
}

// 判定订单相关 URL（C 端 /orders、后台 /admin/orders，含明细路径）
function isOrderUrl(url) {
  return (
    url.startsWith('/orders/') ||
    url.startsWith('/orders') ||
    url.startsWith('/admin/orders/') ||
    url.startsWith('/admin/orders')
  )
}

// 401：清除对应域 token（后台 /admin/** 清后台 token，否则清 C 端 token）；
// 登录接口自身失败不跳转，避免登录页死循环
function handleUnauthorized(config) {
  const isAdmin = config?.url?.startsWith('/admin/')
  if (isAdmin) removeAdminToken()
  else removeToken()
  const isLoginRequest = config?.url?.includes('/auth/login')
  const isAuthPage = window.location.pathname === '/login' || window.location.pathname === '/register'
  if (!isLoginRequest && !isAuthPage) {
    window.location.href = isAdmin ? '/admin/login' : '/login'
  }
}

// C 端与后台接口统一经 /api（后台路径带 /admin/ 前缀，Vite 代理 → 8081；
// getTokenForPath 按路径前缀自动挂对应 token，见 utils/auth.js）
export const request = createService('/api')

export default request
