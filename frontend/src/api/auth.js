import { request } from '@/utils/request'

// 注册（成功后返回 token + 用户信息）
export function register(data) {
  return request.post('/auth/register', data)
}

// 登录
export function login(data) {
  return request.post('/auth/login', data)
}

// 当前登录用户信息（需认证）
export function fetchMe() {
  return request.get('/auth/me')
}
