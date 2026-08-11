import { defineStore } from 'pinia'
import { fetchMe, login as apiLogin, register as apiRegister } from '@/api/auth'
import { updateProfile as apiUpdateProfile } from '@/api/user'
import { getToken, removeToken, setToken } from '@/utils/auth'

// 用户态：token + 用户信息。token 持久化 localStorage，刷新后保持登录。
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: null,
  }),
  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.userInfo?.role === 1,
  },
  actions: {
    async login(credentials) {
      const data = await apiLogin(credentials)
      this.token = data.token
      this.userInfo = data.user
      setToken(data.token)
    },
    async register(form) {
      const data = await apiRegister(form)
      this.token = data.token
      this.userInfo = data.user
      setToken(data.token)
    },
    async fetchUserInfo() {
      if (!this.token) return
      this.userInfo = await fetchMe()
    },
    async updateProfile(form) {
      this.userInfo = await apiUpdateProfile(form)
    },
    logout() {
      this.token = ''
      this.userInfo = null
      removeToken()
    },
  },
})
