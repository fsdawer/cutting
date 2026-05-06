import api from './index'

export const paymentApi = {
  prepare: (data) => api.post('/api/payments/prepare', data),
  confirm: (data) => api.post('/api/payments/confirm', data),
  getMyPayments: () => api.get('/api/payments/my'),
  refund: (paymentId, data) => api.post(`/api/payments/${paymentId}/refund`, data),
}
