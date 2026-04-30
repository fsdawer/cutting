import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
})

// 동시에 여러 요청이 401 날 때 refresh를 한 번만 호출하기 위한 큐
let isRefreshing = false
let waitQueue    = []

function processQueue(error, token) {
  waitQueue.forEach(({ resolve, reject }) => error ? reject(error) : resolve(token))
  waitQueue = []
}

// 요청 인터셉터: Access Token 자동 주입
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 응답 인터셉터: 401 → Refresh Token으로 재발급 후 원래 요청 재시도
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config

    // 401이 아니거나 이미 retry한 요청이면 그냥 reject
    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }

    const storedRefreshToken = localStorage.getItem('refreshToken')
    if (!storedRefreshToken) {
      // Refresh Token도 없으면 로그아웃
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      window.location.href = '/login'
      return Promise.reject(error)
    }

    // 이미 갱신 중이면 큐에 넣고 대기
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        waitQueue.push({ resolve, reject })
      }).then((newToken) => {
        original.headers.Authorization = `Bearer ${newToken}`
        return api(original)
      })
    }

    original._retry = true
    isRefreshing    = true

    try {
      const res = await axios.post(
        'http://localhost:8080/api/auth/refresh',
        null,
        { headers: { 'Refresh-Token': storedRefreshToken } },
      )
      const newAccessToken  = res.data.accessToken
      const newRefreshToken = res.data.refreshToken

      // localStorage 업데이트
      localStorage.setItem('token',        newAccessToken)
      localStorage.setItem('refreshToken', newRefreshToken)

      // Pinia 스토어 업데이트 (동적 임포트로 순환 의존 방지)
      const { useAuthStore } = await import('@/stores/authStore')
      useAuthStore().updateTokens(newAccessToken, newRefreshToken)

      processQueue(null, newAccessToken)
      original.headers.Authorization = `Bearer ${newAccessToken}`
      return api(original)
    } catch (refreshError) {
      processQueue(refreshError, null)
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      window.location.href = '/login'
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)

export default api
