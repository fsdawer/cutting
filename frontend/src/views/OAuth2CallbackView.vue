<template>
  <main class="callback-page">
    <div class="spinner-wrap">
      <span class="spinner"></span>
      <p>카카오 로그인 처리 중...</p>
    </div>
  </main>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import axios from 'axios'

const router = useRouter()
const route  = useRoute()
const auth   = useAuthStore()

onMounted(async () => {
  // 백엔드가 리다이렉트한 URL에서 token 추출
  // 예: /oauth2/callback?token=eyJhbGci...
  const token        = route.query.token
  const refreshToken = route.query.refreshToken

  if (!token) {
    router.push('/login')
    return
  }

  try {
    const res = await axios.get('http://localhost:8080/api/users/me', {
      headers: { Authorization: `Bearer ${token}` }
    })
    auth.setAuth(res.data, token, refreshToken)

    // 홈으로 이동
    router.push('/')
  } catch (e) {
    console.error('OAuth2 로그인 실패', e)
    router.push('/login')
  }
})
</script>

<style scoped>
.callback-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
.spinner-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: var(--color-text-secondary);
}
</style>
