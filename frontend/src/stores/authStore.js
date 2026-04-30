import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user         = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const token        = ref(localStorage.getItem('token') || null)
  const refreshToken = ref(localStorage.getItem('refreshToken') || null)

  const isLoggedIn = computed(() => !!token.value)
  const isStylist  = computed(() => user.value?.role === 'STYLIST')

  function setAuth(userData, accessToken, newRefreshToken = null) {
    user.value  = userData
    token.value = accessToken
    localStorage.setItem('user',  JSON.stringify(userData))
    localStorage.setItem('token', accessToken)

    if (newRefreshToken) {
      refreshToken.value = newRefreshToken
      localStorage.setItem('refreshToken', newRefreshToken)
    }
  }

  function updateTokens(newAccessToken, newRefreshToken) {
    token.value        = newAccessToken
    refreshToken.value = newRefreshToken
    localStorage.setItem('token',        newAccessToken)
    localStorage.setItem('refreshToken', newRefreshToken)
  }

  async function logout() {
    try {
      if (token.value) await authApi.logout()
    } catch (_) {}
    finally {
      user.value         = null
      token.value        = null
      refreshToken.value = null
      localStorage.removeItem('user')
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
    }
  }

  return { user, token, refreshToken, isLoggedIn, isStylist, setAuth, updateTokens, logout }
})
