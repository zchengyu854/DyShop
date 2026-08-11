// JWT 令牌存取（localStorage）
// 双轨隔离（ch08.3）：C 端 dyshop_token 与后台 dyshop_admin_token 互不覆盖，
// 管理员在后台登录不影响 C 端会话，反之亦然。
const TOKEN_KEY = 'dyshop_token'
const ADMIN_TOKEN_KEY = 'dyshop_admin_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getAdminToken() {
  return localStorage.getItem(ADMIN_TOKEN_KEY)
}

export function setAdminToken(token) {
  localStorage.setItem(ADMIN_TOKEN_KEY, token)
}

export function removeAdminToken() {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
}

// 按请求路径选择令牌：/admin/** 走后台 token，其余走 C 端 token
export function getTokenForPath(url = '') {
  return url.startsWith('/admin/') ? getAdminToken() : getToken()
}
