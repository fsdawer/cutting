import api from './index'

export const authApi = {
  register: (data)         => api.post('/api/auth/register', data),
  login:    (data)         => api.post('/api/auth/login', data),
  refresh:  (refreshToken) => api.post('/api/auth/refresh', null, {
    headers: { 'Refresh-Token': refreshToken },
  }),
  logout:   ()             => api.post('/api/auth/logout'),
}
