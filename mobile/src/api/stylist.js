import api from './index'

export const stylistApi = {
  getStylists: (params) => api.get('/api/stylists', { params }),
  getStylist: (id) => api.get(`/api/stylists/${id}`),
  getMyProfile: () => api.get('/api/stylists/me'),
  updateProfile: (data) => api.put('/api/stylists/me', data),
  getNearbyStylists: (lat, lng, radius = 3000) =>
    api.get('/api/stylists/nearby', { params: { lat, lng, radius } }),
  addService: (data) => api.post('/api/stylists/me/services', data),
  updateService: (id, data) => api.put(`/api/stylists/me/services/${id}`, data),
  deleteService: (id) => api.delete(`/api/stylists/me/services/${id}`),
  updateHours: (data) => api.put('/api/stylists/me/hours', data),
}
