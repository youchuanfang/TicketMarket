import { http } from './http'

export const applyRefund = (orderId) => http.post(`/api/user/orders/${orderId}/refund`)
export const getUserRefunds = () => http.get('/api/user/refunds')
export const getMessages = () => http.get('/api/user/messages')
export const readMessage = (id) => http.post(`/api/user/messages/${id}/read`)
export const getAdminRefunds = () => http.get('/api/admin/refunds')
export const approveRefund = (id) => http.post(`/api/admin/refunds/${id}/approve`)
export const rejectRefund = (id) => http.post(`/api/admin/refunds/${id}/reject`)
export const verifyTicket = (payload) => http.post('/api/checker/tickets/verify', payload)
export const getCheckins = () => http.get('/api/checker/checkins')
export const getStatisticsOverview = () => http.get('/api/admin/statistics/overview')
export const getOperationLogs = () => http.get('/api/admin/operation-logs')
export const getRiskLogs = () => http.get('/api/admin/risk-logs')
