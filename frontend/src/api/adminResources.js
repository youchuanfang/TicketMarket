import { http } from './http'

export const adminApi = {
  dashboard: () => http.get('/api/admin/dashboard'),
  venues: () => http.get('/api/admin/venues'),
  createVenue: (payload) => http.post('/api/admin/venues', payload),
  updateVenue: (id, payload) => http.put(`/api/admin/venues/${id}`, payload),
  deleteVenue: (id) => http.delete(`/api/admin/venues/${id}`),
  areas: (venueId) => http.get(`/api/admin/venues/${venueId}/areas`),
  createArea: (venueId, payload) => http.post(`/api/admin/venues/${venueId}/areas`, payload),
  updateArea: (id, payload) => http.put(`/api/admin/venue-areas/${id}`, payload),
  seats: (venueId) => http.get(`/api/admin/venues/${venueId}/seats`),
  generateSeats: (venueId, payload) => http.post(`/api/admin/venues/${venueId}/seats/generate`, payload),
  updateSeat: (id, payload) => http.put(`/api/admin/seats/${id}`, payload),
  sessions: () => http.get('/api/admin/sessions'),
  createSession: (payload) => http.post('/api/admin/sessions', payload),
  initSessionSeats: (sessionId) => http.post(`/api/admin/sessions/${sessionId}/init-seats`),
  sessionSeats: (sessionId) => http.get(`/api/admin/sessions/${sessionId}/seats`),
  ticketLevels: (sessionId) => http.get(`/api/admin/sessions/${sessionId}/ticket-levels`),
  createTicketLevel: (payload) => http.post('/api/admin/ticket-levels', payload),
  saleBatches: () => http.get('/api/admin/sale-batches'),
  createSaleBatch: (payload) => http.post('/api/admin/sale-batches', payload),
  startBatch: (id) => http.post(`/api/admin/sale-batches/${id}/start`),
  lockBatch: (id) => http.post(`/api/admin/sale-batches/${id}/lock`),
  initRedisStock: (id) => http.post(`/api/admin/sale-batches/${id}/init-redis-stock`),
  batchSummary: (id) => http.get(`/api/admin/sale-batches/${id}/stock-summary`),
  stockPool: () => http.get('/api/admin/stock-pool')
}
