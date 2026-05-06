import api from './index'

export const reservationApi = {
  create: (data) => api.post('/api/reservations', data),
  getMyReservations: () => api.get('/api/reservations/my'),
  getStylistReservations: () => api.get('/api/reservations/stylist'),
  getBookedTimes: (stylistId, date) =>
    api.get(`/api/reservations/booked-times/${stylistId}`, { params: { date } }),
  cancel: (id) => api.patch(`/api/reservations/${id}/cancel`),
  updateStatus: (id, status) => api.patch(`/api/reservations/${id}/status`, { status }),
}
