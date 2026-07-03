import { http } from './http'

export const getHome = () => http.get('/api/portal/home')
export const getCategories = () => http.get('/api/portal/categories')
export const searchPerformances = (params) => http.get('/api/portal/search', { params })
export const getPerformance = (id) => http.get(`/api/portal/performances/${id}`)
export const getMovie = (id) => http.get(`/api/portal/movies/${id}`)
export const getPerformanceSessions = (performanceId) => http.get(`/api/portal/performances/${performanceId}/sessions`)
export const getSessionTicketLevels = (sessionId) => http.get(`/api/portal/sessions/${sessionId}/ticket-levels`)
export const getSessionSeats = (sessionId) => http.get(`/api/portal/sessions/${sessionId}/seats`)
export const getActiveBatch = (sessionId) => http.get(`/api/portal/sessions/${sessionId}/active-batch`)
export const getSessionSaleStatus = (sessionId) => http.get(`/api/portal/sessions/${sessionId}/sale-status`)
