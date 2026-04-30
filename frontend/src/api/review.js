import api from './index'

export const reviewApi = {
  /** POST /api/reviews — 리뷰 작성 */
  create: (data) => api.post('/api/reviews', data),

  /** GET /api/reviews/stylist/{stylistId} — 미용사별 리뷰 목록 */
  getByStylist: (stylistId) => api.get(`/api/reviews/stylist/${stylistId}`),

  /** GET /api/reviews/salon/{salonId} — 미용실별 미용사 묶음 조회 */
  getBySalon: (salonId) => api.get(`/api/reviews/salon/${salonId}`),

  /** PUT /api/reviews/{reviewId} — 리뷰 수정 */
  update: (reviewId, data) => api.put(`/api/reviews/${reviewId}`, data),

  /** DELETE /api/reviews/{reviewId} — 리뷰 삭제 */
  remove: (reviewId) => api.delete(`/api/reviews/${reviewId}`),
}
