<template>
  <main class="auth-page">
    <div class="auth-box">
      <RouterLink to="/" class="auth-logo">
        <span class="logo-mark">✦</span>
        <span class="logo-text">CutIng</span>
      </RouterLink>

      <h1 class="auth-title">로그인</h1>
      <p class="auth-sub">계정에 로그인하세요</p>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="form-group">
          <label class="form-label">아이디 또는 이메일</label>
          <input v-model="form.username" type="text" class="form-input" placeholder="아이디 또는 이메일" required autofocus />
        </div>
        <div class="form-group">
          <label class="form-label">비밀번호</label>
          <div class="pw-wrap">
            <input v-model="form.password" :type="showPw ? 'text' : 'password'" class="form-input" placeholder="비밀번호" required />
            <button type="button" class="pw-toggle" @click="showPw = !showPw">{{ showPw ? '숨기기' : '보기' }}</button>
          </div>
        </div>
        <p v-if="errorMsg" class="err-msg">{{ errorMsg }}</p>
        <button type="submit" class="btn btn-primary btn-full" style="padding:13px" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:16px;height:16px;border-width:2px"></span>
          <span v-else>로그인</span>
        </button>
      </form>

      <div class="divider"><span>또는</span></div>

      <a href="http://localhost:8080/oauth2/authorization/kakao" class="kakao-btn">
        카카오로 계속하기
      </a>

      <p class="auth-switch">
        계정이 없으신가요?
        <RouterLink to="/register" class="link-primary">회원가입</RouterLink>
      </p>
    </div>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import axios from 'axios'

const router = useRouter()
const route  = useRoute()
const auth   = useAuthStore()

const form    = ref({ username: '', password: '' })
const showPw  = ref(false)
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  loading.value = true; errorMsg.value = ''
  try {
    const res = await axios.post('http://localhost:8080/api/auth/login', { username: form.value.username, password: form.value.password })
    const { accessToken, refreshToken } = res.data
    const userRes = await axios.get('http://localhost:8080/api/users/me', { headers: { Authorization: `Bearer ${accessToken}` } })
    auth.setAuth(userRes.data, accessToken, refreshToken)
    router.push(route.query.redirect || '/')
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '아이디 또는 비밀번호가 올바르지 않습니다.'
  } finally { loading.value = false }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - var(--navbar-h));
  display: flex; align-items: center; justify-content: center;
  padding: 40px 16px; background: var(--bg);
}
.auth-box {
  width: 100%; max-width: 400px;
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-xl); padding: 40px 36px; box-shadow: var(--shadow-md);
}
.auth-logo { display: flex; align-items: center; gap: 8px; margin-bottom: 32px; text-decoration: none; }
.logo-mark { font-size: 12px; color: var(--primary); }
.logo-text { font-size: 18px; font-weight: 700; color: var(--text); letter-spacing: -0.5px; }
.auth-title { font-size: 24px; font-weight: 800; letter-spacing: -0.03em; margin-bottom: 4px; }
.auth-sub { font-size: 14px; color: var(--text-muted); margin-bottom: 28px; }
.auth-form { display: flex; flex-direction: column; gap: 14px; margin-bottom: 20px; }
.pw-wrap { position: relative; }
.pw-wrap .form-input { padding-right: 60px; }
.pw-toggle { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); background: none; border: none; font-size: 12px; color: var(--text-muted); cursor: pointer; font-weight: 500; }
.pw-toggle:hover { color: var(--primary); }
.err-msg { color: var(--danger); font-size: 13px; padding: 10px 12px; background: var(--danger-light); border-radius: var(--radius-sm); }
.kakao-btn {
  display: flex; align-items: center; justify-content: center;
  width: 100%; padding: 13px; background: #FEE500;
  color: #191919; font-weight: 700; font-size: 14px;
  border-radius: var(--radius-md); text-decoration: none; transition: opacity 0.2s; margin-bottom: 20px;
}
.kakao-btn:hover { opacity: 0.85; }
.auth-switch { text-align: center; font-size: 14px; color: var(--text-muted); }
.link-primary { color: var(--primary); font-weight: 700; margin-left: 4px; }
.link-primary:hover { text-decoration: underline; }
</style>
