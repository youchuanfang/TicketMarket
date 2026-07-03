import axios from 'axios'

const fallbackBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080/api`
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || fallbackBaseUrl
const baseUrlIncludesApi = apiBaseUrl.replace(/\/$/, '').endsWith('/api')
const tokenKey = 'ticket-market-token'
const userKey = 'ticket-market-user'

export const http = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000
})

http.interceptors.request.use((config) => {
  if (baseUrlIncludesApi && typeof config.url === 'string' && config.url.startsWith('/api/')) {
    config.url = config.url.slice('/api'.length)
  }
  const token = localStorage.getItem(tokenKey)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const normalizeError = (response) => {
  const body = response?.data || {}
  const error = new Error(body.message || '请求失败，请稍后重试')
  error.code = body.code || response?.status
  error.status = response?.status
  return error
}

const redirectToLogin = () => {
  localStorage.removeItem(tokenKey)
  localStorage.removeItem(userKey)
  const current = `${window.location.pathname}${window.location.search}`
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = `/login?redirect=${encodeURIComponent(current)}`
  }
}

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code !== 0) {
      const error = normalizeError(response)
      if (error.code === 401) redirectToLogin()
      if (error.code === 403) window.location.href = '/403'
      return Promise.reject(error)
    }
    return body.data
  },
  (error) => {
    const normalized = normalizeError(error.response)
    if (normalized.code === 401 || error.response?.status === 401) {
      redirectToLogin()
    } else if (normalized.code === 403 || error.response?.status === 403) {
      window.location.href = '/403'
    }
    return Promise.reject(normalized)
  }
)
