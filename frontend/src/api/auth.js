import { http } from './http'

export const login = (payload) => http.post('/api/auth/login', payload)
export const register = (payload) => http.post('/api/auth/register', payload)
export const getMe = () => http.get('/api/auth/me')
export const logoutApi = () => http.post('/api/auth/logout')
export const getViewers = () => http.get('/api/user/viewers')
export const addViewer = (payload) => http.post('/api/user/viewers', payload)
export const updateViewer = (id, payload) => http.put(`/api/user/viewers/${id}`, payload)
export const deleteViewer = (id) => http.delete(`/api/user/viewers/${id}`)
export const setDefaultViewer = (id) => http.put(`/api/user/viewers/${id}/default`)
export const submitRealName = (payload) => http.post('/api/user/real-name', payload)
