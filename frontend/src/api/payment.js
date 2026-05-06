import api from './index'

export const paymentApi = {
  /** POST /api/payments/prepare — 결제 준비 (orderId 생성 + Payment PENDING 저장) */
  prepare: (data) => api.post('/api/payments/prepare', data),

  /** POST /api/payments/confirm — 결제 승인 (토스페이먼츠 서버 검증 후 PAID 처리) */
  confirm: (data) => api.post('/api/payments/confirm', data),

  /** POST /api/payments/{id}/refund — 환불 요청 */
  refund: (paymentId, cancelReason) =>
    api.post(`/api/payments/${paymentId}/refund`, { cancelReason }),

  /** GET /api/payments/my — 내 결제 내역 조회 */
  getMyPayments: () => api.get('/api/payments/my'),

  /** POST /api/payments/cancel-pending — 결제 실패 시 orderId로 PENDING 즉시 취소 */
  cancelPending: (orderId) => api.post(`/api/payments/cancel-pending?orderId=${orderId}`),

  /** GET /api/payments/by-order — orderId로 단건 결제 조회 (결제 성공 화면용) */
  getByOrderId: (orderId) => api.get(`/api/payments/by-order?orderId=${orderId}`),
}
