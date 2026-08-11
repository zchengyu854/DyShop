import { defineStore } from 'pinia'
import { login as apiLogin } from '@/api/auth'
import { fetchAdminMe } from '@/api/admin/user'
import { getAdminToken, removeAdminToken, setAdminToken } from '@/utils/auth'

// 后台独立登录态（ch08.3）：token 存独立 key dyshop_admin_token，
// 与 C 端用户会话完全隔离——管理员登录后台不影响 C 端登录状态。
export const useAdminStore = defineStore('admin', {
  state: () => ({
    token: getAdminToken() || '',
    adminInfo: null,
  }),
  getters: {
    isLogin: (state) => !!state.token,
  },
  actions: {
    // 仅管理员放行：非管理员抛错（不写入任何 token）
    async login(credentials) {
      const data = await apiLogin(credentials)
      if (data.user?.role !== 1) {
        throw new Error('该账号无后台权限，请使用管理员账号登录')
      }
      this.token = data.token
      this.adminInfo = data.user
      setAdminToken(data.token)
    },
    async fetchAdminInfo() {
      if (!this.token) return
      this.adminInfo = await fetchAdminMe()
    },
    // C 端登录页发现管理员账号时，直接把会话归属后台（不重复请求登录接口）
    adopt(token, info) {
      this.token = token
      this.adminInfo = info
      setAdminToken(token)
    },
    logout() {
      this.token = ''
      this.adminInfo = null
      removeAdminToken()
    },
  },
})
