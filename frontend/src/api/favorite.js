import api from './index'

export const favoriteApi = {
  /** GET /api/favorites/stylists — 내 관심 디자이너 목록 */
  getMyFavorites: () => api.get('/api/favorites/stylists'),

  /** POST /api/favorites/stylists/{id} — 찜 토글 */
  toggle: (stylistProfileId) => api.post(`/api/favorites/stylists/${stylistProfileId}`),
}
