import { http } from './http'

export const getHome = () => http.get('/api/portal/home')
export const getCategories = () => http.get('/api/portal/categories')
export const searchPerformances = (params) => http.get('/api/portal/search', { params })
export const getPerformance = (id) => http.get(`/api/portal/performances/${id}`)
export const getMovie = (id) => http.get(`/api/portal/movies/${id}`)
