<template>
  <main class="auth-page">
    <div class="auth-box">
      <div class="auth-logo">
        <RouterLink to="/">
          <span class="logo-mark">✂</span>
          <span class="logo-text">CutIng</span>
        </RouterLink>
      </div>

      <h1 class="auth-title">로그인</h1>
      <p class="auth-sub">서비스를 이용하려면 로그인하세요</p>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="form-group">
          <label class="form-label">아이디 또는 이메일</label>
          <input
            v-model="form.username"
            type="text"
            class="form-input"
            placeholder="아이디 또는 이메일"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label">비밀번호</label>
          <div class="pw-wrap">
            <input
              v-model="form.password"
              :type="showPw ? 'text' : 'password'"
              class="form-input"
              placeholder="비밀번호"
              required
            />
            <button type="button" class="pw-toggle" @click="showPw = !showPw">
              {{ showPw ? '숨기기' : '보기' }}
            </button>
          </div>
        </div>

        <p v-if="errorMsg" class="msg-error">{{ errorMsg }}</p>

        <button type="submit" class="btn btn-primary btn-full btn-lg submit-btn" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:18px;height:18px;border-width:2px;"></span>
          <span v-else>로그인</span>
        </button>
      </form>

      <div class="divider"><span>또는</span></div>

      <div class="social-area">
        <a href="http://localhost:8080/oauth2/authorization/kakao" class="btn-kakao">
          <img src="https://developers.kakao.com/assets/img/about/logos/kakaolink/kakaolink_btn_medium.png" alt="카카오" width="20" />
          카카오로 계속하기
        </a>
      </div>

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
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await axios.post('http://localhost:8080/api/auth/login', {
      username: form.value.username,
      password: form.value.password,
    })
    const { accessToken, refreshToken } = res.data
    const userRes = await axios.get('http://localhost:8080/api/users/me', {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    auth.setAuth(userRes.data, accessToken, refreshToken)
    router.push(route.query.redirect || '/')
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '아이디 또는 비밀번호가 올바르지 않습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - var(--navbar-h));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  background: var(--bg);
}

.auth-box {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 40px 36px;
  box-shadow: var(--shadow);
}

.auth-logo {
  text-align: center;
  margin-bottom: 28px;
}
.auth-logo a {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.logo-mark { font-size: 20px; color: var(--primary); }
.logo-text  { font-size: 18px; font-weight: 700; color: var(--text); }

.auth-title { font-size: 22px; font-weight: 700; margin-bottom: 6px; }
.auth-sub   { font-size: 13px; color: var(--text-sub); margin-bottom: 28px; }

.auth-form { display: flex; flex-direction: column; gap: 16px; margin-bottom: 24px; }

.pw-wrap { position: relative; }
.pw-wrap .form-input { padding-right: 60px; }
.pw-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-size: 12px;
  color: var(--text-muted);
  cursor: pointer;
  font-weight: 500;
}
.pw-toggle:hover { color: var(--primary); }

.msg-error { color: var(--red); font-size: 13px; }

.submit-btn { margin-top: 4px; }

.social-area { margin-bottom: 20px; }
.btn-kakao {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 13px;
  background: #FEE500;
  color: #191919;
  font-weight: 600;
  font-size: 15px;
  border-radius: var(--radius-sm);
  text-decoration: none;
  transition: opacity 0.2s;
}
.btn-kakao:hover { opacity: 0.85; }

.auth-switch {
  text-align: center;
  font-size: 14px;
  color: var(--text-sub);
}
.link-primary {
  color: var(--primary);
  font-weight: 600;
  margin-left: 4px;
}
.link-primary:hover { text-decoration: underline; }
</style>
