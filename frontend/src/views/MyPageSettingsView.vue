<template>
  <main class="page">
    <div class="container">
      <div class="page-header">
        <button class="back-btn" @click="$router.back()">‹</button>
        <h1 class="page-title">프로필 관리</h1>
      </div>

      <div class="settings-list">
        <!-- 기본 정보 -->
        <div class="card section-card">
          <h2 class="section-title">기본 정보</h2>
          <div class="form-col">
            <div class="form-group">
              <label class="form-label">이름</label>
              <input v-model="editName" class="form-input" placeholder="이름" />
            </div>
            <div class="form-group">
              <label class="form-label">전화번호</label>
              <input v-model="editPhone" class="form-input" placeholder="010-0000-0000" />
            </div>
            <p v-if="profileMsg" :class="profileSuccess ? 'msg-success' : 'msg-error'">{{ profileMsg }}</p>
            <button class="btn btn-primary" :disabled="profileSaving" @click="saveProfile">
              <span v-if="profileSaving" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
              <span v-else>저장</span>
            </button>
          </div>
        </div>

        <!-- 비밀번호 변경 -->
        <div class="card section-card">
          <h2 class="section-title">비밀번호 변경</h2>
          <div class="form-col">
            <div class="form-group">
              <label class="form-label">현재 비밀번호</label>
              <input v-model="pw.current" type="password" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">새 비밀번호</label>
              <input v-model="pw.next" type="password" class="form-input" placeholder="8자 이상" />
            </div>
            <div class="form-group">
              <label class="form-label">새 비밀번호 확인</label>
              <input v-model="pw.confirm" type="password" class="form-input" />
            </div>
            <p v-if="pwMsg" :class="pwSuccess ? 'msg-success' : 'msg-error'">{{ pwMsg }}</p>
            <button class="btn btn-primary" :disabled="pwSaving" @click="changePassword">
              <span v-if="pwSaving" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
              <span v-else>변경</span>
            </button>
          </div>
        </div>

        <!-- 스타일리스트 전환 -->
        <div v-if="user?.role === 'USER'" class="card section-card">
          <h2 class="section-title">스타일리스트 전환</h2>
          <p class="section-desc">스타일리스트로 전환하면 프로필을 등록하고 예약을 받을 수 있습니다.</p>
          <div class="form-col">
            <div class="form-group">
              <label class="form-label">미용사 자격증 번호</label>
              <input v-model="licenseNo" class="form-input" placeholder="자격증 번호 입력" />
            </div>
            <p v-if="upgradeMsg" :class="upgradeSuccess ? 'msg-success' : 'msg-error'">{{ upgradeMsg }}</p>
            <button class="btn btn-accent" :disabled="upgradeSaving" @click="upgradeToStylist">
              <span v-if="upgradeSaving" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
              <span v-else>스타일리스트 전환 신청</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { userApi } from '@/api/user'

const auth = useAuthStore()
const user = computed(() => auth.user)

const editName  = ref(user.value?.name || '')
const editPhone = ref(user.value?.phone || '')
const profileMsg = ref(''); const profileSuccess = ref(false); const profileSaving = ref(false)

const pw = ref({ current: '', next: '', confirm: '' })
const pwMsg = ref(''); const pwSuccess = ref(false); const pwSaving = ref(false)

const licenseNo = ref('')
const upgradeMsg = ref(''); const upgradeSuccess = ref(false); const upgradeSaving = ref(false)

async function saveProfile() {
  profileSaving.value = true; profileMsg.value = ''
  try {
    await userApi.updateMe({ name: editName.value, phone: editPhone.value })
    auth.setAuth({ ...auth.user, name: editName.value, phone: editPhone.value }, auth.token)
    profileMsg.value = '저장되었습니다.'; profileSuccess.value = true
  } catch { profileMsg.value = '저장 실패'; profileSuccess.value = false }
  finally { profileSaving.value = false }
}

async function changePassword() {
  if (pw.value.next !== pw.value.confirm) { pwMsg.value = '새 비밀번호가 일치하지 않습니다.'; pwSuccess.value = false; return }
  pwSaving.value = true; pwMsg.value = ''
  try {
    await userApi.changePassword({ currentPassword: pw.value.current, newPassword: pw.value.next })
    pwMsg.value = '비밀번호가 변경되었습니다.'; pwSuccess.value = true
    pw.value = { current: '', next: '', confirm: '' }
  } catch (e) { pwMsg.value = e.response?.data?.message || '변경 실패'; pwSuccess.value = false }
  finally { pwSaving.value = false }
}

async function upgradeToStylist() {
  upgradeSaving.value = true; upgradeMsg.value = ''
  try {
    await userApi.upgradeToStylist({ licenseNumber: licenseNo.value })
    auth.setAuth({ ...auth.user, role: 'STYLIST' }, auth.token)
    upgradeMsg.value = '스타일리스트로 전환되었습니다.'; upgradeSuccess.value = true
  } catch (e) { upgradeMsg.value = e.response?.data?.message || '전환 실패'; upgradeSuccess.value = false }
  finally { upgradeSaving.value = false }
}
</script>

<style scoped>
.container { max-width: 480px; padding: 16px; }
.page-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.back-btn { font-size: 24px; background: none; border: none; cursor: pointer; color: var(--text); padding: 0 4px; line-height: 1; }
.page-title { font-size: 20px; font-weight: 800; }

.settings-list { display: flex; flex-direction: column; gap: 12px; }
.section-card { padding: 20px; }
.section-title { font-size: 15px; font-weight: 700; margin-bottom: 14px; }
.section-desc { font-size: 13px; color: var(--text-muted); margin-bottom: 14px; line-height: 1.6; }
.form-col { display: flex; flex-direction: column; gap: 12px; }
.msg-success { color: var(--success); font-size: 13px; }
.msg-error   { color: var(--danger); font-size: 13px; }
</style>
