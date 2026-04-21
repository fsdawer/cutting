<template>
  <main class="page">
    <div class="container">
      <h1 class="section-title">마이페이지</h1>
      <p class="section-subtitle">내 예약 내역과 프로필을 관리하세요</p>

      <div class="mypage-layout">
        <!-- Sidebar Profile -->
        <aside class="mp-sidebar">
          <div class="card profile-box">
            <img
              :src="authStore.user?.profileImg || `https://i.pravatar.cc/100?u=${authStore.user?.id}`"
              class="mp-avatar"
            />
            <h2 class="mp-name">{{ authStore.user?.name || '사용자' }}</h2>
            <p class="mp-email">{{ authStore.user?.email || '' }}</p>
            <span class="badge" :class="authStore.isStylist ? 'badge-gold' : 'badge-green'">
              {{ authStore.isStylist ? '미용사' : '일반 사용자' }}
            </span>

            <div class="sidebar-actions">
              <button v-if="!authStore.isStylist" class="btn btn-outline btn-full" @click="showUpgradeModal = true">
                미용사로 전환
              </button>
              <RouterLink v-if="authStore.isStylist" to="/stylist/manage" class="btn btn-outline btn-full">
                내 프로필 관리
              </RouterLink>
              <RouterLink v-if="authStore.isStylist" to="/stylist/reservations" class="btn btn-ghost btn-full">
                예약 관리
              </RouterLink>
            </div>
          </div>
        </aside>

        <!-- Content -->
        <section class="mp-content">
          <!-- Tab -->
          <div class="tab-bar">
            <button
              v-for="tab in tabs"
              :key="tab.value"
              class="tab-btn"
              :class="{ active: activeTab === tab.value }"
              @click="activeTab = tab.value"
            >{{ tab.label }}</button>
          </div>

          <!-- 예약 내역 탭 -->
          <div v-if="activeTab === 'reservations'">
            <div class="status-filter">
              <button
                v-for="s in statusFilters"
                :key="s.value"
                class="filter-chip"
                :class="{ active: statusFilter === s.value }"
                @click="statusFilter = s.value"
              >{{ s.label }}</button>
            </div>

            <div v-if="resLoading" style="text-align:center;padding:40px 0">
              <div class="spinner"></div>
            </div>

            <div v-else class="reservation-list">
              <div
                v-for="r in filteredReservations"
                :key="r.id"
                class="card reservation-card"
              >
                <div class="rc-left">
                  <img
                    :src="r.stylistProfileImg || `https://i.pravatar.cc/60?u=${r.stylistId}`"
                    class="rc-avatar"
                  />
                  <div class="rc-info">
                    <p class="rc-stylist">{{ r.stylistName }} · {{ r.salonName }}</p>
                    <p class="rc-service">{{ r.serviceName }}</p>
                    <p class="rc-date">📅 {{ formatDate(r.reservedAt) }}</p>
                  </div>
                </div>
                <div class="rc-right">
                  <span class="badge" :class="statusBadge(r.status)">{{ statusLabel(r.status) }}</span>
                  <div class="rc-actions">
                    <RouterLink
                      v-if="r.chatRoomId"
                      :to="`/chat/${r.chatRoomId}`"
                      class="btn btn-primary btn-sm"
                    >채팅</RouterLink>
                    <button
                      class="btn btn-ghost btn-sm"
                      @click="openResModal(r)"
                    >상세보기</button>
                    <button
                      v-if="r.status === 'CONFIRMED' || r.status === 'PENDING'"
                      class="btn btn-ghost btn-sm"
                      style="color:var(--color-danger)"
                      @click="cancelReservation(r.id)"
                    >취소</button>
                  </div>
                </div>
              </div>

              <div v-if="filteredReservations.length === 0" class="empty-state">
                <div class="empty-icon">📋</div>
                <p class="empty-text">예약 내역이 없습니다</p>
                <RouterLink to="/" class="btn btn-primary">미용사 찾기</RouterLink>
              </div>
            </div>
          </div>

          <!-- 결제 내역 탭 -->
          <div v-if="activeTab === 'payments'">
            <div v-if="payLoading" style="text-align:center;padding:40px 0">
              <div class="spinner"></div>
            </div>
            <div v-else class="payment-list">
              <div v-for="p in payments" :key="p.id" class="card payment-card">
                <div class="pay-row">
                  <div>
                    <p class="pay-order">주문번호 {{ p.orderId }}</p>
                    <p class="pay-date">{{ formatDate(p.paidAt || p.createdAt) }}</p>
                  </div>
                  <div style="text-align:right">
                    <p class="pay-amount">{{ p.amount?.toLocaleString() }}원</p>
                    <span class="badge" :class="payStatusBadge(p.status)">{{ payStatusLabel(p.status) }}</span>
                  </div>
                </div>
              </div>
              <div v-if="payments.length === 0" class="empty-state">
                <div class="empty-icon">💳</div>
                <p class="empty-text">결제 내역이 없습니다</p>
              </div>
            </div>
          </div>

          <!-- 내 정보 탭 -->
          <div v-if="activeTab === 'profile'" class="card profile-edit">
            <h2 class="profile-edit-title">내 정보 수정</h2>
            <form @submit.prevent="saveProfile" class="profile-form">
              <div class="form-group">
                <label class="form-label">이름</label>
                <input v-model="profileForm.name" type="text" class="form-input" />
              </div>
              <div class="form-group">
                <label class="form-label">전화번호</label>
                <input v-model="profileForm.phone" type="tel" class="form-input" placeholder="010-0000-0000" />
              </div>
              <p v-if="profileMsg" :class="profileSuccess ? 'success-msg' : 'error-msg'">{{ profileMsg }}</p>
              <div style="display:flex;gap:12px;margin-top:8px">
                <button type="submit" class="btn btn-primary" :disabled="profileSaving">저장하기</button>
              </div>
            </form>

            <hr style="border-color:var(--color-border);margin:28px 0" />

            <h2 class="profile-edit-title">비밀번호 변경</h2>
            <form @submit.prevent="savePassword" class="profile-form">
              <div class="form-group">
                <label class="form-label">현재 비밀번호</label>
                <input v-model="pwForm.currentPassword" type="password" class="form-input" required />
              </div>
              <div class="form-group">
                <label class="form-label">새 비밀번호</label>
                <input v-model="pwForm.newPassword" type="password" class="form-input" required minlength="8" />
              </div>
              <div class="form-group">
                <label class="form-label">새 비밀번호 확인</label>
                <input v-model="pwForm.confirmNewPassword" type="password" class="form-input" required minlength="8" />
              </div>
              <p v-if="pwMsg" :class="pwSuccess ? 'success-msg' : 'error-msg'">{{ pwMsg }}</p>
              <button type="submit" class="btn btn-primary" :disabled="pwSaving">변경하기</button>
            </form>

            <hr style="border-color:var(--color-border);margin:28px 0" />

            <h2 class="profile-edit-title" style="color:var(--color-danger)">계정 탈퇴</h2>
            <p style="font-size:14px;color:var(--color-text-secondary);margin-bottom:16px">
              탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.
            </p>
            <button class="btn btn-ghost" style="color:var(--color-danger);border-color:var(--color-danger)" @click="deleteAccount">
              회원 탈퇴
            </button>
          </div>
        </section>
      </div>
    </div>

    <!-- Upgrade to Stylist Modal -->
    <div v-if="showUpgradeModal" class="modal-overlay" @click.self="showUpgradeModal = false">
      <div class="modal">
        <h2 class="modal-title">미용사로 전환하기</h2>
        <p class="modal-sub">추가 정보를 입력하시면 미용사 계정으로 승급됩니다.</p>

        <form @submit.prevent="handleUpgrade" class="upgrade-form">
          <div class="form-group">
            <label class="form-label">미용실 이름</label>
            <input v-model="upgradeForm.salonName" type="text" class="form-input" placeholder="예: 뷰티북 스튜디오" required>
          </div>
          <div class="form-group">
            <label class="form-label">근무 지역</label>
            <input v-model="upgradeForm.location" type="text" class="form-input" placeholder="예: 강남구 청담동" required>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" @click="showUpgradeModal = false">취소</button>
            <button type="submit" class="btn btn-primary" :disabled="upgradeLoading">전환하기</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Reservation Detail Modal -->
    <div v-if="showResModal" class="modal-overlay" @click.self="showResModal = false">
      <div class="modal">
        <h2 class="modal-title" style="margin-bottom:20px;">예약 상세 정보</h2>

        <div class="res-detail-info" v-if="selectedRes">
          <div class="confirm-row">
            <span class="cr-label">예약 상태</span>
            <span class="badge" :class="statusBadge(selectedRes.status)">{{ statusLabel(selectedRes.status) }}</span>
          </div>
          <div class="confirm-row">
            <span class="cr-label">미용사</span>
            <span class="cr-value">{{ selectedRes.stylistName }} · {{ selectedRes.salonName }}</span>
          </div>
          <div class="confirm-row">
            <span class="cr-label">위치</span>
            <span class="cr-value">{{ selectedRes.location || '위치 미상' }}</span>
          </div>
          <div class="confirm-row">
            <span class="cr-label">예약 일시</span>
            <span class="cr-value">{{ formatDate(selectedRes.reservedAt) }}</span>
          </div>
          <div class="confirm-row">
            <span class="cr-label">서비스</span>
            <span class="cr-value">{{ selectedRes.serviceName }}</span>
          </div>
          
          <div class="confirm-row total">
            <span class="cr-label">결제 금액</span>
            <span class="cr-value gold">{{ selectedRes.totalPrice?.toLocaleString() }}원</span>
          </div>
          <p v-if="selectedRes.status === 'PENDING'" style="text-align:right; font-size:12px; color:var(--color-danger); margin-top:4px;">
            결제가 아직 완료되지 않았습니다.
          </p>
        </div>

        <div class="modal-actions" style="margin-top:24px;">
          <RouterLink
            :to="`/stylist/${selectedRes.stylistId}`"
            class="btn btn-outline"
            style="margin-right:auto;"
          >미용사 프로필 보기</RouterLink>
          <button class="btn btn-primary" @click="showResModal = false">닫기</button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/authStore'
import { useRouter } from 'vue-router'
import { reservationApi } from '@/api/reservation'
import { paymentApi } from '@/api/payment'
import { userApi } from '@/api/user'

const authStore = useAuthStore()
const router = useRouter()

const activeTab = ref('reservations')
const tabs = [
  { label: '예약 내역', value: 'reservations' },
  { label: '결제 내역', value: 'payments' },
  { label: '내 정보', value: 'profile' },
]

// ── 예약 내역 ────────────────────────────────────────────────
const resLoading = ref(false)
const reservations = ref([])
const statusFilter = ref('all')
const statusFilters = [
  { label: '전체', value: 'all' },
  { label: '확정', value: 'CONFIRMED' },
  { label: '완료', value: 'DONE' },
  { label: '취소', value: 'CANCELLED' },
]

const filteredReservations = computed(() => {
  if (statusFilter.value === 'all') return reservations.value
  return reservations.value.filter(r => r.status === statusFilter.value)
})

function statusLabel(s) {
  return { PENDING: '대기중(미결제)', CONFIRMED: '예약확정', DONE: '완료', CANCELLED: '취소됨' }[s] || s
}
function statusBadge(s) {
  return { PENDING: 'badge-gold', CONFIRMED: 'badge-green', DONE: 'badge-gray', CANCELLED: 'badge-red' }[s] || ''
}

const showResModal = ref(false)
const selectedRes = ref(null)

function openResModal(r) {
  selectedRes.value = r
  showResModal.value = true
}

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} · ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function loadReservations() {
  resLoading.value = true
  try {
    const res = await reservationApi.getMyReservations()
    reservations.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    resLoading.value = false
  }
}

async function cancelReservation(id) {
  if (!confirm('예약을 취소하시겠습니까?')) return
  try {
    await reservationApi.cancel(id)
    await loadReservations()
    alert('예약이 취소되었습니다.')
  } catch (e) {
    alert(e.response?.data?.message || '취소 중 오류가 발생했습니다.')
  }
}

// ── 결제 내역 ────────────────────────────────────────────────
const payLoading = ref(false)
const payments = ref([])


function payStatusLabel(s) {
  return { PENDING: '결제대기', PAID: '결제완료', REFUNDED: '환불완료', FAILED: '실패' }[s] || s
}
function payStatusBadge(s) {
  return { PENDING: 'badge-gold', PAID: 'badge-green', REFUNDED: 'badge-gray', FAILED: 'badge-red' }[s] || ''
}

async function loadPayments() {
  payLoading.value = true
  try {
    const res = await paymentApi.getMyPayments()
    payments.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    payLoading.value = false
  }
}

// ── 내 정보 수정 ─────────────────────────────────────────────
const profileForm = ref({
  name: authStore.user?.name || '',
  phone: authStore.user?.phone || '',
})
const profileMsg = ref('')
const profileSuccess = ref(false)
const profileSaving = ref(false)

async function saveProfile() {
  profileSaving.value = true
  profileMsg.value = ''
  try {
    const res = await userApi.updateMe({ name: profileForm.value.name, phone: profileForm.value.phone })
    authStore.setAuth(res.data, authStore.token)
    profileMsg.value = '정보가 저장되었습니다.'
    profileSuccess.value = true
  } catch (e) {
    profileMsg.value = e.response?.data?.message || '저장 중 오류가 발생했습니다.'
    profileSuccess.value = false
  } finally {
    profileSaving.value = false
  }
}

const pwForm = ref({ currentPassword: '', newPassword: '', confirmNewPassword: '' })
const pwMsg = ref('')
const pwSuccess = ref(false)
const pwSaving = ref(false)

async function savePassword() {
  pwSuccess.value = false
  pwMsg.value = ''
  
  if (pwForm.value.currentPassword === pwForm.value.newPassword) {
    pwMsg.value = '새 비밀번호는 기존 비밀번호와 달라야 합니다.'
    return
  }
  if (pwForm.value.newPassword !== pwForm.value.confirmNewPassword) {
    pwMsg.value = '새 비밀번호와 확인용 비밀번호가 일치하지 않습니다.'
    return
  }

  pwSaving.value = true
  try {
    await userApi.changePassword({
      currentPassword: pwForm.value.currentPassword,
      newPassword: pwForm.value.newPassword
    })
    pwMsg.value = '비밀번호가 변경되었습니다.'
    pwSuccess.value = true
    pwForm.value = { currentPassword: '', newPassword: '', confirmNewPassword: '' }
  } catch (e) {
    pwMsg.value = e.response?.data?.message || '변경 중 오류가 발생했습니다.'
  } finally {
    pwSaving.value = false
  }
}

async function deleteAccount() {
  if (!confirm('정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다.')) return
  try {
    await userApi.deleteMe()
    authStore.logout()
    router.push('/')
  } catch (e) {
    alert(e.response?.data?.message || '탈퇴 중 오류가 발생했습니다.')
  }
}

// ── 미용사 전환 ──────────────────────────────────────────────
const showUpgradeModal = ref(false)
const upgradeLoading = ref(false)
const upgradeForm = ref({ salonName: '', location: '' })

async function handleUpgrade() {
  upgradeLoading.value = true
  try {
    await userApi.upgradeToStylist(upgradeForm.value)
    alert('미용사로 전환되었습니다! 다시 로그인해주세요.')
    authStore.logout()
    router.push('/login')
  } catch (error) {
    alert(error.response?.data?.message || '권한 변경 중 오류가 발생했습니다.')
  } finally {
    upgradeLoading.value = false
    showUpgradeModal.value = false
  }
}

onMounted(() => {
  loadReservations()
  loadPayments()
})
</script>

<style scoped>
.mypage-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
  align-items: start;
}

.mp-sidebar { position: sticky; top: 88px; }
.profile-box { text-align: center; }
.mp-avatar { width: 80px; height: 80px; border-radius: 50%; object-fit: cover; margin: 0 auto 12px; border: 3px solid var(--color-border); display: block; }
.mp-name { font-size: 17px; font-weight: 700; margin-bottom: 4px; }
.mp-email { font-size: 13px; color: var(--color-text-muted); margin-bottom: 12px; }
.sidebar-actions { display: flex; flex-direction: column; gap: 8px; margin-top: 12px; }

.tab-bar { display: flex; gap: 4px; margin-bottom: 20px; border-bottom: 1px solid var(--color-border); }
.tab-btn {
  padding: 10px 18px; background: none; border: none;
  font-size: 14px; font-weight: 500; color: var(--color-text-muted);
  cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px;
  transition: var(--transition);
}
.tab-btn:hover { color: var(--color-text-primary); }
.tab-btn.active { color: var(--color-gold); border-bottom-color: var(--color-gold); }

.status-filter { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.filter-chip {
  padding: 6px 14px; border-radius: var(--radius-full);
  background: transparent; border: 1px solid var(--color-border);
  color: var(--color-text-secondary); font-size: 13px; cursor: pointer; transition: var(--transition);
}
.filter-chip:hover { border-color: var(--color-gold); color: var(--color-gold); }
.filter-chip.active { background: var(--color-gold); border-color: var(--color-gold); color: #1a1206; font-weight: 600; }

.reservation-list, .payment-list { display: flex; flex-direction: column; gap: 14px; }
.reservation-card {
  display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 20px;
}
.rc-left { display: flex; gap: 14px; align-items: center; flex: 1; min-width: 0; }
.rc-avatar { width: 52px; height: 52px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.rc-stylist { font-size: 15px; font-weight: 600; }
.rc-service { font-size: 13px; color: var(--color-text-secondary); margin: 2px 0; }
.rc-date { font-size: 13px; color: var(--color-text-muted); }
.rc-right { display: flex; flex-direction: column; align-items: flex-end; gap: 10px; flex-shrink: 0; }
.rc-actions { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }

.payment-card { padding: 18px 20px; }
.pay-row { display: flex; justify-content: space-between; align-items: center; }
.pay-order { font-size: 13px; color: var(--color-text-muted); margin-bottom: 4px; }
.pay-date { font-size: 12px; color: var(--color-text-muted); }
.pay-amount { font-size: 16px; font-weight: 600; color: var(--color-gold); margin-bottom: 4px; }

.profile-edit { padding: 28px; }
.profile-edit-title { font-size: 16px; font-weight: 700; margin-bottom: 20px; }
.profile-form { display: flex; flex-direction: column; gap: 16px; }
.success-msg { color: var(--color-success); font-size: 13px; }
.error-msg { color: var(--color-danger); font-size: 13px; }

.empty-state { text-align: center; padding: 60px 0; }
.empty-icon { font-size: 40px; margin-bottom: 12px; }
.empty-text { color: var(--color-text-secondary); margin-bottom: 16px; }

/* Modal */
.modal-overlay {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center;
  z-index: 1000;
}
.modal {
  background: var(--color-bg-card); padding: 32px; border-radius: var(--radius-lg);
  width: 90%; max-width: 400px;
}
.modal-title { font-size: 20px; font-weight: 700; margin-bottom: 8px; }
.modal-sub { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 24px; }
.upgrade-form { display: flex; flex-direction: column; gap: 16px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 24px; }

.res-detail-info { display: flex; flex-direction: column; gap: 12px; }
.confirm-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.cr-label { font-size: 13px; color: var(--color-text-muted); flex-shrink: 0; }
.cr-value { font-size: 14px; color: var(--color-text-primary); text-align: right; }
.confirm-row.total { padding-top: 14px; border-top: 1px solid var(--color-border); }
.cr-value.gold { font-size: 18px; font-weight: 700; color: var(--color-gold); }

@media (max-width: 768px) {
  .mypage-layout { grid-template-columns: 1fr; }
  .mp-sidebar { position: static; }
  .reservation-card { flex-direction: column; align-items: flex-start; }
  .rc-right { align-items: flex-start; }
}
</style>
