import api from './index'

export const rankingApi = {
  getRanking: (district) => api.get('/api/ranking', { params: { district } }),
}
