<template>
  <main class="auth-page">
    <div class="auth-box">
      <RouterLink to="/" class="auth-logo">
        <span class="logo-mark">✦</span>
        <span class="logo-text">CutIng</span>
      </RouterLink>

      <h1 class="auth-title">회원가입</h1>
      <p class="auth-sub">계정을 만들어 예약을 시작하세요</p>

      <form @submit.prevent="handleRegister" class="auth-form">
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">아이디</label>
            <input v-model="form.username" type="text" class="form-input" placeholder="영문/숫자 3자 이상" required minlength="3" />
          </div>
          <div class="form-group">
            <label class="form-label">이름</label>
            <input v-model="form.name" type="text" class="form-input" placeholder="홍길동" required />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">이메일</label>
          <input v-model="form.email" type="email" class="form-input" placeholder="example@email.com" required />
        </div>
        <div class="form-group">
          <label class="form-label">핸드폰 번호 (선택)</label>
          <input v-model="form.phone" type="tel" class="form-input" placeholder="010-0000-0000" />
        </div>
        <div class="form-group">
          <label class="form-label">비밀번호</label>
          <input v-model="form.password" type="password" class="form-input" placeholder="8자 이상" required minlength="8" />
        </div>
        <div class="form-group">
          <label class="form-label">비밀번호 확인</label>
          <input v-model="form.passwordConfirm" type="password" class="form-input" placeholder="비밀번호 재입력" required />
        </div>

        <p v-if="errorMsg" class="err-msg">{{ errorMsg }}</p>

        <button type="submit" class="btn btn-primary btn-full" style="padding:13px;margin-top:4px" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:16px;height:16px;border-width:2px"></span>
          <span v-else>회원가입</span>
        </button>
      </form>

      <p class="auth-switch">
        이미 계정이 있으신가요?
        <RouterLink to="/login" class="link-primary">로그인</RouterLink>
      </p>
    </div>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router  = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const form = ref({ username: '', name: '', email: '', phone: '', password: '', passwordConfirm: '' })

async function handleRegister() {
  if (form.value.password !== form.value.passwordConfirm) { errorMsg.value = '비밀번호가 일치하지 않습니다.'; return }
  loading.value = true; errorMsg.value = ''
  try {
    await axios.post('http://localhost:8080/api/auth/register', { username: form.value.username, email: form.value.email, phone: form.value.phone, password: form.value.password, name: form.value.name, role: 'USER' })
    alert('회원가입이 완료되었습니다.')
    router.push('/login')
  } catch (e) { errorMsg.value = e.response?.data?.message || '회원가입 중 오류가 발생했습니다.' }
  finally { loading.value = false }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - var(--navbar-h));
  display: flex; align-items: center; justify-content: center;
  padding: 40px 16px; background: var(--bg);
}
.auth-box {
  width: 100%; max-width: 460px;
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-xl); padding: 40px 36px; box-shadow: var(--shadow-md);
}
.auth-logo { display: flex; align-items: center; gap: 8px; margin-bottom: 32px; }
.logo-mark { font-size: 12px; color: var(--primary); }
.logo-text { font-size: 18px; font-weight: 700; color: var(--text); letter-spacing: -0.5px; }
.auth-title { font-size: 24px; font-weight: 800; letter-spacing: -0.03em; margin-bottom: 4px; }
.auth-sub { font-size: 14px; color: var(--text-muted); margin-bottom: 28px; }
.auth-form { display: flex; flex-direction: column; gap: 14px; margin-bottom: 20px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.err-msg { color: var(--danger); font-size: 13px; padding: 10px 12px; background: var(--danger-light); border-radius: var(--radius-sm); }
.auth-switch { text-align: center; font-size: 14px; color: var(--text-muted); }
.link-primary { color: var(--primary); font-weight: 700; margin-left: 4px; }
.link-primary:hover { text-decoration: underline; }
</style>
