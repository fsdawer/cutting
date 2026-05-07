import api from './index'

export const notificationApi = {
  getPending: () => api.get('/notifications/pending'),
}
