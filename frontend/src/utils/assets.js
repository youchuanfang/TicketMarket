const fallbackApiBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080/api`
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || fallbackApiBaseUrl

const apiOrigin = (() => {
  try {
    return new URL(apiBaseUrl, window.location.origin).origin
  } catch {
    return window.location.origin
  }
})()

export const isLocalFilePath = (value) => {
  const path = String(value || '').trim()
  return /^[a-zA-Z]:[\\/]/.test(path) || path.startsWith('\\\\')
}

export const assetUrl = (value) => {
  const path = String(value || '').trim()
  if (!path) return ''
  if (/^(https?:|data:|blob:)/i.test(path)) return path
  if (isLocalFilePath(path)) return ''
  if (path.startsWith('/uploads/')) return `${apiOrigin}${path}`
  return path
}
