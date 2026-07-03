import { defineStore } from 'pinia'
import { getMe, login, logoutApi, register } from '../api/auth'

const tokenKey = 'ticket-market-token'
const userKey = 'ticket-market-user'

const readStoredUser = () => {
  try {
    return JSON.parse(localStorage.getItem(userKey) || 'null')
  } catch {
    return null
  }
}

const isLocalAdminHost = () => ['localhost', '127.0.0.1', '::1', '[::1]'].includes(window.location.hostname)

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(tokenKey) || '',
    userInfo: readStoredUser(),
    initialized: false
  }),
  getters: {
    profile: (state) => state.userInfo,
    role: (state) => state.userInfo?.roleCode || '',
    isLoggedIn: (state) => Boolean(state.token && state.userInfo),
    canEnterAdmin: (state) => isLocalAdminHost() && ['ADMIN', 'MANAGER', 'CHECKER'].includes(state.userInfo?.roleCode),
    canUseAdminApi: (state) => isLocalAdminHost() && ['ADMIN', 'MANAGER'].includes(state.userInfo?.roleCode),
    canUseChecker: (state) => isLocalAdminHost() && ['ADMIN', 'MANAGER', 'CHECKER'].includes(state.userInfo?.roleCode)
  },
  actions: {
    async login(payload) {
      const data = await login(payload)
      this.setAuth(data)
      await this.fetchMe(true)
      return this.userInfo
    },
    async register(payload) {
      const data = await register(payload)
      this.setAuth(data)
      await this.fetchMe(true)
      return this.userInfo
    },
    async fetchMe(force = false) {
      if (!this.token) {
        this.clearAuth()
        this.initialized = true
        return null
      }
      if (this.initialized && this.userInfo && !force) {
        return this.userInfo
      }
      try {
        const data = await getMe()
        this.userInfo = data
        localStorage.setItem(userKey, JSON.stringify(data))
        this.initialized = true
        return data
      } catch (error) {
        this.clearAuth()
        this.initialized = true
        throw error
      }
    },
    setAuth(data) {
      this.token = data.token
      this.userInfo = {
        userId: data.userId,
        username: data.username,
        nickname: data.nickname,
        roleCode: data.roleCode,
        realNameVerified: data.realNameVerified
      }
      localStorage.setItem(tokenKey, data.token)
      localStorage.setItem(userKey, JSON.stringify(this.userInfo))
    },
    clearAuth() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(tokenKey)
      localStorage.removeItem(userKey)
    },
    async logout() {
      try {
        if (this.token) await logoutApi()
      } catch {
        // Local logout should still succeed when the token is already invalid.
      }
      this.clearAuth()
    }
  }
})
