<template>
  <main class="page">
    <div class="container">
      <!-- 프로필 헤더 -->
      <div class="profile-header card">
        <div class="profile-left">
          <div class="avatar">{{ user?.name?.[0] ?? 'U' }}</div>
          <div>
            <p class="user-name">{{ user?.name }}</p>
            <p class="user-email">{{ user?.email }}</p>
            <span class="role-badge" :class="user?.role === 'STYLIST' ? 'badge-accent' : 'badge-gray'">
              {{ user?.role === 'STYLIST' ? '스타일리스트' : '일반 회원' }}
            </span>
          </div>
        </div>
        <div class="profile-actions">
          <RouterLink v-if="user?.role === 'STYLIST'" to="/stylist/manage" class="btn btn-ghost btn-sm">프로필 관리</RouterLink>
          <RouterLink v-if="user?.role === 'STYLIST'" to="/stylist/reservations" class="btn btn-ghost btn-sm">예약 관리</RouterLink>
        </div>
      </div>

      <!-- 탭 -->
      <div class="tab-bar">
        <button v-for="t in tabs" :key="t.id" class="tab-btn" :class="{ active: activeTab === t.id }" @click="activeTab = t.id">
          {{ t.label }}
          <span v-if="t.id === 'reservations' && pendingCount" class="tab-count">{{ pendingCount }}</span>
        </button>
      </div>

      <!-- 내 예약 -->
      <div v-if="activeTab === 'reservations'">
        <div v-if="resLoading" class="state-center"><div class="spinner"></div></div>
        <div v-else-if="reservations.length === 0" class="state-center">
          <p class="state-text">예약 내역이 없습니다.</p>
          <RouterLink to="/" class="btn btn-primary btn-sm">미용사 찾기</RouterLink>
        </div>
        <div v-else class="res-list">
          <div v-for="r in reservations" :key="r.id" class="res-card card">
            <div class="res-top">
              <div class="res-left">
                <div class="res-meta-row">
                  <span class="badge" :class="statusBadge(r.status)">{{ statusLabel(r.status) }}</span>
                  <span class="res-id">#{{ r.id }}</span>
                </div>
                <p class="res-service">{{ r.serviceName }}</p>
                <p class="res-info">{{ r.stylistName }} · {{ r.salonName }}</p>
                <p class="res-date">{{ formatDate(r.reservedAt) }}</p>
              </div>
              <div class="res-right">
                <p class="res-price">{{ r.totalPrice?.toLocaleString() }}원</p>
                <div class="res-actions">
                  <RouterLink v-if="r.chatRoomId" :to="`/chat/${r.chatRoomId}`" class="btn btn-ghost btn-sm">채팅</RouterLink>
                  <button v-if="r.status === 'CONFIRMED'" class="btn btn-danger-ghost btn-sm" @click="cancelReservation(r.id)">취소</button>
                  <button v-if="r.status === 'DONE' && !r.reviewed" class="btn btn-ghost btn-sm" @click="openReview(r)">리뷰 작성</button>
                </div>
              </div>
            </div>
            <div v-if="r.requestMemo" class="res-memo">{{ r.requestMemo }}</div>
          </div>
        </div>
      </div>

      <!-- 결제 내역 -->
      <div v-if="activeTab === 'payments'">
        <div v-if="payLoading" class="state-center"><div class="spinner"></div></div>
        <div v-else-if="payments.length === 0" class="state-center">
          <p class="state-text">결제 내역이 없습니다.</p>
        </div>
        <div v-else class="pay-list">
          <div v-for="p in payments" :key="p.id" class="pay-card card">
            <div class="pay-top">
              <div>
                <span class="badge" :class="payStatusBadge(p.status)">{{ payStatusLabel(p.status) }}</span>
                <p class="pay-service">{{ p.serviceName }}</p>
                <p class="pay-meta">{{ methodLabel(p.method) }} · {{ formatDate(p.createdAt) }}</p>
                <p class="pay-order mono">{{ p.orderId }}</p>
              </div>
              <div class="pay-amount-wrap">
                <p class="pay-amount">{{ p.amount?.toLocaleString() }}원</p>
                <button v-if="p.status === 'PAID'" class="btn btn-ghost btn-sm" @click="handleRefund(p.id)">환불 요청</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 프로필 설정 -->
      <div v-if="activeTab === 'settings'" class="settings-grid">
        <div class="card">
          <h2 class="settings-title">기본 정보 수정</h2>
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

        <div class="card">
          <h2 class="settings-title">비밀번호 변경</h2>
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

        <div v-if="user?.role === 'USER'" class="card upgrade-card">
          <h2 class="settings-title">스타일리스트 전환</h2>
          <p class="settings-desc">스타일리스트로 전환하면 프로필을 등록하고 예약을 받을 수 있습니다.</p>
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

    <!-- 리뷰 모달 -->
    <Teleport to="body">
      <div v-if="reviewModal.open" class="modal-backdrop" @click.self="reviewModal.open = false">
        <div class="modal-box">
          <div class="modal-header">
            <h3>리뷰 작성</h3>
            <button class="modal-close" @click="reviewModal.open = false">×</button>
          </div>
          <div class="modal-body">
            <p class="review-target">{{ reviewModal.reservation?.serviceName }} · {{ reviewModal.reservation?.stylistName }}</p>
            <div class="form-group">
              <label class="form-label">별점</label>
              <div class="star-picker">
                <button v-for="i in 5" :key="i" class="star-pick-btn" :class="{ filled: i <= reviewModal.rating }" @click="reviewModal.rating = i">★</button>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">리뷰 내용</label>
              <textarea v-model="reviewModal.content" class="form-input" rows="4" placeholder="솔직한 리뷰를 남겨주세요"></textarea>
            </div>
            <p v-if="reviewModal.error" class="msg-error">{{ reviewModal.error }}</p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-ghost" @click="reviewModal.open = false">취소</button>
            <button class="btn btn-primary" :disabled="reviewModal.saving" @click="submitReview">
              <span v-if="reviewModal.saving" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
              <span v-else>등록</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { userApi } from '@/api/user'
import { reservationApi } from '@/api/reservation'
import { paymentApi } from '@/api/payment'
import { reviewApi } from '@/api/review'

const auth = useAuthStore()
const user = computed(() => auth.user)

const activeTab = ref('reservations')
const tabs = [
  { id: 'reservations', label: '예약 내역' },
  { id: 'payments', label: '결제 내역' },
  { id: 'settings', label: '설정' },
]

const reservations = ref([])
const payments = ref([])
const resLoading = ref(false)
const payLoading = ref(false)

const pendingCount = computed(() => reservations.value.filter(r => r.status === 'CONFIRMED').length)

const editName  = ref(user.value?.name || '')
const editPhone = ref(user.value?.phone || '')
const profileMsg = ref(''); const profileSuccess = ref(false); const profileSaving = ref(false)

const pw = ref({ current: '', next: '', confirm: '' })
const pwMsg = ref(''); const pwSuccess = ref(false); const pwSaving = ref(false)

const licenseNo = ref('')
const upgradeMsg = ref(''); const upgradeSuccess = ref(false); const upgradeSaving = ref(false)

const reviewModal = ref({ open: false, reservation: null, rating: 5, content: '', saving: false, error: '' })

function statusLabel(s) { return { CONFIRMED: '예약확정', DONE: '완료', CANCELLED: '취소됨', PENDING: '대기중' }[s] || s }
function statusBadge(s) { return { CONFIRMED: 'badge-green', DONE: 'badge-gray', CANCELLED: 'badge-red', PENDING: 'badge-orange' }[s] || 'badge-gray' }
function payStatusLabel(s) { return { PAID: '결제완료', PENDING: '대기', REFUNDED: '환불' }[s] || s }
function payStatusBadge(s) { return { PAID: 'badge-green', PENDING: 'badge-orange', REFUNDED: 'badge-blue' }[s] || 'badge-gray' }
function methodLabel(m) { return { TOSS: '토스페이', KAKAO_PAY: '카카오페이', NAVER_PAY: '네이버페이' }[m] || m }
function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function cancelReservation(id) {
  if (!confirm('예약을 취소하시겠습니까?')) return
  try { await reservationApi.cancel(id); await loadReservations() }
  catch (e) { alert(e.response?.data?.message || '취소 실패') }
}

async function handleRefund(payId) {
  if (!confirm('환불 요청을 진행하시겠습니까?')) return
  try { await paymentApi.refund(payId, { reason: '사용자 요청' }); await loadPayments() }
  catch (e) { alert(e.response?.data?.message || '환불 요청 실패') }
}

function openReview(r) {
  reviewModal.value = { open: true, reservation: r, rating: 5, content: '', saving: false, error: '' }
}
async function submitReview() {
  if (!reviewModal.value.content.trim()) { reviewModal.value.error = '내용을 입력해주세요.'; return }
  reviewModal.value.saving = true; reviewModal.value.error = ''
  try {
    await reviewApi.create({
      reservationId: reviewModal.value.reservation.id,
      rating: reviewModal.value.rating,
      content: reviewModal.value.content,
    })
    reviewModal.value.open = false
    await loadReservations()
  } catch (e) { reviewModal.value.error = e.response?.data?.message || '등록 실패' }
  finally { reviewModal.value.saving = false }
}

async function saveProfile() {
  profileSaving.value = true; profileMsg.value = ''
  try {
    const updated = await userApi.updateMe({ name: editName.value, phone: editPhone.value })
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

async function loadReservations() {
  resLoading.value = true
  try { const res = await reservationApi.getMyReservations(); reservations.value = res.data || [] }
  catch { reservations.value = [] }
  finally { resLoading.value = false }
}

async function loadPayments() {
  payLoading.value = true
  try { const res = await paymentApi.getMyPayments(); payments.value = res.data || [] }
  catch { payments.value = [] }
  finally { payLoading.value = false }
}

onMounted(() => { loadReservations(); loadPayments() })
</script>

<style scoped>
/* Profile header */
.profile-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 16px; flex-wrap: wrap; }
.profile-left { display: flex; align-items: center; gap: 16px; }
.avatar {
  width: 52px; height: 52px; border-radius: 50%;
  background: var(--primary); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 800; flex-shrink: 0;
}
.user-name { font-size: 18px; font-weight: 700; }
.user-email { font-size: 13px; color: var(--text-muted); margin: 2px 0 6px; }
.role-badge { font-size: 11px; }
.profile-actions { display: flex; gap: 8px; flex-wrap: wrap; }

/* Reservation list */
.res-list { display: flex; flex-direction: column; gap: 10px; }
.res-card { padding: 18px 20px; }
.res-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.res-left { flex: 1; }
.res-meta-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.res-id { font-size: 12px; color: var(--text-muted); }
.res-service { font-size: 16px; font-weight: 700; margin-bottom: 3px; }
.res-info { font-size: 13px; color: var(--text-muted); margin-bottom: 3px; }
.res-date { font-size: 13px; color: var(--text-sub); }
.res-right { text-align: right; flex-shrink: 0; }
.res-price { font-size: 18px; font-weight: 800; margin-bottom: 8px; }
.res-actions { display: flex; gap: 6px; justify-content: flex-end; flex-wrap: wrap; }
.res-memo { margin-top: 12px; padding: 10px 14px; background: var(--bg-surface); border-radius: var(--radius-sm); font-size: 13px; color: var(--text-sub); border-left: 3px solid var(--border-strong); }

/* Payment list */
.pay-list { display: flex; flex-direction: column; gap: 10px; }
.pay-card { padding: 18px 20px; }
.pay-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.pay-top .badge { margin-bottom: 8px; }
.pay-service { font-size: 16px; font-weight: 700; margin-bottom: 3px; }
.pay-meta { font-size: 13px; color: var(--text-muted); margin-bottom: 3px; }
.pay-order { font-size: 11px; color: var(--text-muted); font-family: 'SF Mono', monospace; }
.pay-amount-wrap { text-align: right; flex-shrink: 0; }
.pay-amount { font-size: 18px; font-weight: 800; margin-bottom: 8px; }
.mono { font-family: 'SF Mono', 'Fira Code', monospace; }

/* Settings */
.settings-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.settings-title { font-size: 16px; font-weight: 700; margin-bottom: 16px; }
.settings-desc { font-size: 13px; color: var(--text-muted); margin-bottom: 16px; line-height: 1.6; }
.form-col { display: flex; flex-direction: column; gap: 14px; }
.msg-success { color: var(--success); font-size: 13px; }
.msg-error   { color: var(--danger); font-size: 13px; }
.upgrade-card { grid-column: 1 / -1; }

/* Modal */
.modal-backdrop {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center; z-index: 500; padding: 16px;
}
.modal-box {
  background: var(--bg-card); border-radius: var(--radius-xl);
  width: 100%; max-width: 480px; box-shadow: var(--shadow-lg); overflow: hidden;
}
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px 0; }
.modal-header h3 { font-size: 18px; font-weight: 700; }
.modal-close { background: none; border: none; font-size: 22px; color: var(--text-muted); cursor: pointer; line-height: 1; }
.modal-body { padding: 20px 24px; display: flex; flex-direction: column; gap: 16px; }
.modal-footer { display: flex; gap: 10px; justify-content: flex-end; padding: 0 24px 24px; }
.review-target { font-size: 13px; color: var(--text-muted); padding: 10px 14px; background: var(--bg-surface); border-radius: var(--radius-sm); }

.star-picker { display: flex; gap: 4px; }
.star-pick-btn { font-size: 28px; background: none; border: none; color: var(--border-strong); cursor: pointer; transition: var(--transition); line-height: 1; }
.star-pick-btn.filled { color: var(--gold); }
.star-pick-btn:hover { transform: scale(1.15); }

@media (max-width: 768px) {
  .profile-header { flex-direction: column; align-items: flex-start; }
  .settings-grid { grid-template-columns: 1fr; }
  .upgrade-card { grid-column: 1; }
  .res-top { flex-direction: column; }
  .res-right { text-align: left; }
  .res-actions { justify-content: flex-start; }
}
</style>
