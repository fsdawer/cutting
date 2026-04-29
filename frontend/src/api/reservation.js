import api from './index'

export const reservationApi = {
  /** POST /api/reservations — 예약 생성 */
  create: (data) => api.post('/api/reservations', data),

  /** GET /api/reservations/my — 내 예약 목록 */
  getMyReservations: () => api.get('/api/reservations/my'),

  /** GET /api/reservations/{id} — 예약 상세 */
  getById: (id) => api.get(`/api/reservations/${id}`),

  /** DELETE /api/reservations/{id} — 예약 취소 */
  cancel: (id) => api.delete(`/api/reservations/${id}`),

  // ── 미용사 전용 ──────────────────────────────────────────────

  /** GET /api/reservations/stylist — 내 미용실 예약 목록 (미용사) */
  getStylistReservations: () => api.get('/api/reservations/stylist'),

  /** PUT /api/reservations/{id}/status — 예약 상태 변경 */
  updateStatus: (id, status) =>
    api.put(`/api/reservations/${id}/status`, null, { params: { status } }),

  /** GET /api/reservations/stylists/{stylistId}/booked-times — 특정 미용사의 특정 날짜 예약된 시간 리스트 조회 (고객용/예약화면용) */
  getBookedTimes: (stylistId, date) =>
    api.get(`/api/reservations/stylists/${stylistId}/booked-times`, { params: { date } }),

  /** POST /api/reservations/{id}/images — 예약 이미지 업로드 */
  uploadImages: (id, formData) =>
    api.post(`/api/reservations/${id}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
}
