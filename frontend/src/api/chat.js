import api from './index'

export const chatApi = {
  getMyRooms:  ()       => api.get('/api/chat/rooms'),
  getRoom:     (roomId) => api.get(`/api/chat/rooms/${roomId}`),
  getMessages: (roomId) => api.get(`/api/chat/rooms/${roomId}/messages`),
  markAsRead:  (roomId) => api.post(`/api/chat/rooms/${roomId}/read`),
}
