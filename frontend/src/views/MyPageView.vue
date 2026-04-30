<template>
  <main class="page">
    <div class="container">
      <div class="mypage-layout">
        <!-- Sidebar -->
        <aside class="sidebar">
          <div class="card profile-card">
            <img
              :src="authStore.user?.profileImg || `https://i.pravatar.cc/100?u=${authStore.user?.id}`"
              class="profile-avatar"
            />
            <h2 class="profile-name">{{ authStore.user?.name || '사용자' }}</h2>
            <p class="profile-email">{{ authStore.user?.email }}</p>
            <span class="badge" :class="authStore.isStylist ? 'badge-gold' : 'badge-green'">
              {{ authStore.isStylist ? '미용사' : '일반 고객' }}
            </span>
            <div class="sidebar-btns">
              <button v-if="!authStore.isStylist" class="btn btn-outline btn-full btn-sm" @click="showUpgradeModal = true">
                미용사로 전환
              </button>
              <RouterLink v-if="authStore.isStylist" to="/stylist/manage" class="btn btn-outline btn-full btn-sm">
                내 프로필 관리
              </RouterLink>
              <RouterLink v-if="authStore.isStylist" to="/stylist/reservations" class="btn btn-ghost btn-full btn-sm">
                예약 관리
              </RouterLink>
            </div>
          </div>
        </aside>

        <!-- Content -->
        <section class="content">
          <!-- Tabs -->
          <div class="tab-bar">
            <button v-for="tab in tabs" :key="tab.value"
              class="tab-btn" :class="{ active: activeTab === tab.value }"
              @click="activeTab = tab.value">
              {{ tab.label }}
            </button>
          </div>

          <!-- 예약 내역 -->
          <div v-if="activeTab === 'reservations'">
            <div class="filter-row">
              <button v-for="s in statusFilters" :key="s.value"
                class="f-chip" :class="{ active: statusFilter === s.value }"
                @click="statusFilter = s.value">{{ s.label }}</button>
            </div>

            <div v-if="resLoading" class="loading-center"><div class="spinner"></div></div>
            <div v-else class="item-list">
              <div v-for="r in filteredReservations" :key="r.id" class="res-card card">
                <div class="rc-left">
                  <img :src="r.stylistProfileImg || `https://i.pravatar.cc/60?u=${r.stylistId}`" class="rc-avatar" />
                  <div>
                    <p class="rc-name">{{ r.stylistName }} · {{ r.salonName }}</p>
                    <p class="rc-service">{{ r.serviceName }}</p>
                    <p class="rc-date">{{ formatDate(r.reservedAt) }}</p>
                  </div>
                </div>
                <div class="rc-right">
                  <span class="badge" :class="statusBadge(r.status)">{{ statusLabel(r.status) }}</span>
                  <div class="rc-actions">
                    <RouterLink v-if="r.chatRoomId" :to="`/chat/${r.chatRoomId}`" class="btn btn-primary btn-sm">채팅</RouterLink>
                    <button class="btn btn-ghost btn-sm" @click="openResModal(r)">상세</button>
                    <button
                      v-if="r.status === 'DONE' && !reviewedIds.has(r.id)"
                      class="btn btn-outline btn-sm"
                      @click="openReviewModal(r)"
                    >리뷰 작성</button>
                    <button
                      v-if="r.status === 'CONFIRMED'"
                      class="btn btn-ghost btn-sm"
                      style="color:var(--red)"
                      @click="cancelReservation(r.id)"
                    >취소</button>
                  </div>
                </div>
              </div>

              <div v-if="filteredReservations.length === 0" class="empty-state">
                <p class="empty-icon">📋</p>
                <p>예약 내역이 없습니다</p>
                <RouterLink to="/" class="btn btn-primary btn-sm">미용사 찾기</RouterLink>
              </div>
            </div>
          </div>

          <!-- 결제 내역 -->
          <div v-if="activeTab === 'payments'">
            <div v-if="payLoading" class="loading-center"><div class="spinner"></div></div>
            <div v-else class="item-list">
              <div v-for="p in payments" :key="p.id" class="card pay-card">
                <div class="pc-row">
                  <div>
                    <p class="pc-service-name">{{ p.serviceName }}</p>
                    <p class="pc-stylist">{{ p.stylistName }}<span v-if="p.salonName"> · {{ p.salonName }}</span></p>
                    <p class="pc-date">주문일자 {{ formatDate(p.createdAt) }}</p>
                    <p class="pc-order-id">주문번호 {{ p.orderId }}</p>
                  </div>
                  <div style="text-align:right;flex-shrink:0;margin-left:12px">
                    <p class="pc-amount">{{ p.amount?.toLocaleString() }}원</p>
                    <p class="pc-method">{{ payMethodLabel(p.method) }}</p>
                    <span class="badge" :class="payStatusBadge(p.status)">{{ payStatusLabel(p.status) }}</span>
                  </div>
                </div>
              </div>
              <div v-if="payments.length === 0" class="empty-state">
                <p class="empty-icon">💳</p>
                <p>결제 내역이 없습니다</p>
              </div>
            </div>
          </div>

          <!-- 내 정보 -->
          <div v-if="activeTab === 'profile'">
            <div class="card edit-card">
              <h3 class="edit-title">내 정보 수정</h3>
              <form @submit.prevent="saveProfile" class="edit-form">
                <div class="form-group">
                  <label class="form-label">이름</label>
                  <input v-model="profileForm.name" type="text" class="form-input" />
                </div>
                <div class="form-group">
                  <label class="form-label">전화번호</label>
                  <input v-model="profileForm.phone" type="tel" class="form-input" placeholder="010-0000-0000" />
                </div>
                <p v-if="profileMsg" :class="profileSuccess ? 'msg-success' : 'msg-error'">{{ profileMsg }}</p>
                <button type="submit" class="btn btn-primary" :disabled="profileSaving">저장하기</button>
              </form>
            </div>

            <div class="card edit-card">
              <h3 class="edit-title">비밀번호 변경</h3>
              <form @submit.prevent="savePassword" class="edit-form">
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
                  <input v-model="pwForm.confirmNewPassword" type="password" class="form-input" required />
                </div>
                <p v-if="pwMsg" :class="pwSuccess ? 'msg-success' : 'msg-error'">{{ pwMsg }}</p>
                <button type="submit" class="btn btn-primary" :disabled="pwSaving">변경하기</button>
              </form>
            </div>

            <div class="card edit-card danger-zone">
              <h3 class="edit-title" style="color:var(--red)">계정 탈퇴</h3>
              <p class="danger-desc">탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.</p>
              <button class="btn btn-ghost btn-sm" style="color:var(--red);border-color:var(--red)" @click="deleteAccount">
                회원 탈퇴
              </button>
            </div>
          </div>
        </section>
      </div>
    </div>

    <!-- Upgrade Modal -->
    <div v-if="showUpgradeModal" class="modal-overlay" @click.self="showUpgradeModal = false">
      <div class="modal-box card">
        <h2 class="modal-title">미용사로 전환하기</h2>
        <p class="modal-sub">추가 정보를 입력하면 미용사 계정으로 전환됩니다.</p>
        <form @submit.prevent="handleUpgrade" class="edit-form">
          <div class="form-group">
            <label class="form-label">미용실 이름</label>
            <input v-model="upgradeForm.salonName" type="text" class="form-input" placeholder="예: CutIng 스튜디오" required />
          </div>
          <div class="form-group">
            <label class="form-label">근무 지역</label>
            <input v-model="upgradeForm.location" type="text" class="form-input" placeholder="예: 강남구 청담동" required />
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" @click="showUpgradeModal = false">취소</button>
            <button type="submit" class="btn btn-primary" :disabled="upgradeLoading">전환하기</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Review Write Modal -->
    <div v-if="showReviewModal && reviewTarget" class="modal-overlay" @click.self="showReviewModal = false">
      <div class="modal-box card">
        <h2 class="modal-title">리뷰 작성</h2>
        <p class="modal-sub">{{ reviewTarget.stylistName }} · {{ reviewTarget.serviceName }}</p>
        <div class="star-row">
          <button
            v-for="n in 5" :key="n"
            class="star-btn" :class="{ filled: n <= reviewForm.rating }"
            @click="reviewForm.rating = n"
          >★</button>
        </div>
        <textarea
          v-model="reviewForm.content"
          class="review-textarea"
          placeholder="리뷰 내용을 입력하세요"
          rows="4"
        ></textarea>
        <p v-if="reviewErrMsg" class="msg-error">{{ reviewErrMsg }}</p>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="showReviewModal = false">취소</button>
          <button class="btn btn-primary" :disabled="reviewSaving" @click="submitReview">
            {{ reviewSaving ? '등록 중...' : '등록하기' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Reservation Detail Modal -->
    <div v-if="showResModal && selectedRes" class="modal-overlay" @click.self="showResModal = false">
      <div class="modal-box card">
        <h2 class="modal-title">예약 상세</h2>
        <div class="detail-rows">
          <div class="detail-row">
            <span class="dr-label">상태</span>
            <span class="badge" :class="statusBadge(selectedRes.status)">{{ statusLabel(selectedRes.status) }}</span>
          </div>
          <div class="detail-row">
            <span class="dr-label">미용사</span>
            <span>{{ selectedRes.stylistName }} · {{ selectedRes.salonName }}</span>
          </div>
          <div class="detail-row">
            <span class="dr-label">일시</span>
            <span>{{ formatDate(selectedRes.reservedAt) }}</span>
          </div>
          <div class="detail-row">
            <span class="dr-label">서비스</span>
            <span>{{ selectedRes.serviceName }}</span>
          </div>
          <div class="detail-row total">
            <span class="dr-label">결제금액</span>
            <span class="dr-price">{{ selectedRes.totalPrice?.toLocaleString() }}원</span>
          </div>
        </div>
        <div class="modal-actions">
          <RouterLink :to="`/stylist/${selectedRes.stylistId}`" class="btn btn-outline btn-sm">프로필 보기</RouterLink>
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
import { reviewApi } from '@/api/review'

const authStore = useAuthStore()
const router    = useRouter()

const activeTab = ref('reservations')
const tabs = [
  { label: '예약 내역', value: 'reservations' },
  { label: '결제 내역', value: 'payments' },
  { label: '내 정보',   value: 'profile' },
]

const resLoading   = ref(false)
const reservations = ref([])
const statusFilter = ref('all')
const statusFilters = [
  { label: '전체',     value: 'all' },
  { label: '예약확정', value: 'CONFIRMED' },
  { label: '완료',     value: 'DONE' },
  { label: '취소됨',   value: 'CANCELLED' },
]

const filteredReservations = computed(() =>
  statusFilter.value === 'all'
    ? reservations.value
    : reservations.value.filter(r => r.status === statusFilter.value)
)

function statusLabel(s) {
  return { CONFIRMED: '예약확정', DONE: '완료', CANCELLED: '취소됨' }[s] || s
}
function statusBadge(s) {
  return { CONFIRMED: 'badge-green', DONE: 'badge-gray', CANCELLED: 'badge-red' }[s] || ''
}

const showResModal = ref(false)
const selectedRes  = ref(null)
function openResModal(r) { selectedRes.value = r; showResModal.value = true }

// 리뷰
const reviewedIds     = ref(new Set())
const showReviewModal = ref(false)
const reviewTarget    = ref(null)
const reviewForm      = ref({ rating: 5, content: '' })
const reviewErrMsg    = ref('')
const reviewSaving    = ref(false)

function openReviewModal(r) {
  reviewTarget.value  = r
  reviewForm.value    = { rating: 5, content: '' }
  reviewErrMsg.value  = ''
  showReviewModal.value = true
}

async function submitReview() {
  reviewSaving.value = true; reviewErrMsg.value = ''
  try {
    await reviewApi.create({
      reservationId: reviewTarget.value.id,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content,
    })
    reviewedIds.value.add(reviewTarget.value.id)
    showReviewModal.value = false
  } catch (e) {
    reviewErrMsg.value = e.response?.data?.message || '리뷰 등록 중 오류가 발생했습니다.'
  } finally { reviewSaving.value = false }
}

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function loadReservations() {
  resLoading.value = true
  try {
    const res = await reservationApi.getMyReservations()
    reservations.value = res.data
  } catch (e) { console.error(e) }
  finally { resLoading.value = false }
}

async function cancelReservation(id) {
  if (!confirm('예약을 취소하시겠습니까?')) return
  try {
    await reservationApi.cancel(id)
    await loadReservations()
  } catch (e) { alert(e.response?.data?.message || '취소 중 오류가 발생했습니다.') }
}

// 결제 내역
const payLoading = ref(false)
const payments   = ref([])

function payStatusLabel(s) {
  return { PENDING: '결제대기', PAID: '결제완료', REFUNDED: '환불완료', FAILED: '실패' }[s] || s
}
function payStatusBadge(s) {
  return { PENDING: 'badge-gold', PAID: 'badge-green', REFUNDED: 'badge-gray', FAILED: 'badge-red' }[s] || ''
}
function payMethodLabel(m) {
  return { TOSS: '토스페이', NAVER_PAY: '네이버페이', KAKAO_PAY: '카카오페이' }[m] || m || '결제'
}

async function loadPayments() {
  payLoading.value = true
  try {
    const res = await paymentApi.getMyPayments()
    payments.value = res.data
  } catch (e) { console.error(e) }
  finally { payLoading.value = false }
}

// 내 정보
const profileForm    = ref({ name: authStore.user?.name || '', phone: authStore.user?.phone || '' })
const profileMsg     = ref('')
const profileSuccess = ref(false)
const profileSaving  = ref(false)

async function saveProfile() {
  profileSaving.value = true; profileMsg.value = ''
  try {
    const res = await userApi.updateMe({ name: profileForm.value.name, phone: profileForm.value.phone })
    authStore.setAuth(res.data, authStore.token)
    profileMsg.value = '저장되었습니다.'; profileSuccess.value = true
  } catch (e) {
    profileMsg.value = e.response?.data?.message || '저장 중 오류가 발생했습니다.'; profileSuccess.value = false
  } finally { profileSaving.value = false }
}

const pwForm    = ref({ currentPassword: '', newPassword: '', confirmNewPassword: '' })
const pwMsg     = ref('')
const pwSuccess = ref(false)
const pwSaving  = ref(false)

async function savePassword() {
  pwSuccess.value = false; pwMsg.value = ''
  if (pwForm.value.currentPassword === pwForm.value.newPassword) {
    pwMsg.value = '새 비밀번호는 기존과 달라야 합니다.'; return
  }
  if (pwForm.value.newPassword !== pwForm.value.confirmNewPassword) {
    pwMsg.value = '새 비밀번호가 일치하지 않습니다.'; return
  }
  pwSaving.value = true
  try {
    await userApi.changePassword({ currentPassword: pwForm.value.currentPassword, newPassword: pwForm.value.newPassword })
    pwMsg.value = '비밀번호가 변경되었습니다.'; pwSuccess.value = true
    pwForm.value = { currentPassword: '', newPassword: '', confirmNewPassword: '' }
  } catch (e) {
    pwMsg.value = e.response?.data?.message || '변경 중 오류가 발생했습니다.'
  } finally { pwSaving.value = false }
}

async function deleteAccount() {
  if (!confirm('정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다.')) return
  try {
    await userApi.deleteMe(); authStore.logout(); router.push('/')
  } catch (e) { alert(e.response?.data?.message || '탈퇴 중 오류가 발생했습니다.') }
}

// 미용사 전환
const showUpgradeModal = ref(false)
const upgradeLoading   = ref(false)
const upgradeForm      = ref({ salonName: '', location: '' })

async function handleUpgrade() {
  upgradeLoading.value = true
  try {
    await userApi.upgradeToStylist(upgradeForm.value)
    alert('미용사로 전환되었습니다! 다시 로그인해주세요.')
    authStore.logout(); router.push('/login')
  } catch (e) { alert(e.response?.data?.message || '권한 변경 중 오류가 발생했습니다.') }
  finally { upgradeLoading.value = false; showUpgradeModal.value = false }
}

onMounted(() => { loadReservations(); loadPayments() })
</script>

<style scoped>
.mypage-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
  align-items: start;
}

/* Sidebar */
.sidebar { position: sticky; top: calc(var(--navbar-h) + 16px); }
.profile-card { text-align: center; padding: 24px 16px; }
.profile-avatar { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; margin: 0 auto 12px; border: 2px solid var(--border); display: block; }
.profile-name  { font-size: 16px; font-weight: 700; margin-bottom: 4px; }
.profile-email { font-size: 12px; color: var(--text-muted); margin-bottom: 12px; }
.sidebar-btns  { display: flex; flex-direction: column; gap: 6px; margin-top: 14px; }

/* Tabs */
.tab-bar {
  display: flex;
  border-bottom: 2px solid var(--border);
  margin-bottom: 20px;
  gap: 0;
}
.tab-btn {
  padding: 10px 18px;
  background: none; border: none;
  font-size: 14px; font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: var(--transition);
}
.tab-btn:hover { color: var(--text); }
.tab-btn.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 700; }

/* Filter */
.filter-row { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 16px; }
.f-chip {
  padding: 5px 14px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border);
  background: #fff; color: var(--text-sub);
  font-size: 13px; cursor: pointer; transition: var(--transition);
}
.f-chip:hover { border-color: var(--primary); color: var(--primary); }
.f-chip.active { background: var(--primary); border-color: var(--primary); color: #fff; font-weight: 600; }

/* Lists */
.item-list { display: flex; flex-direction: column; gap: 12px; }
.res-card { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 16px 18px; }
.rc-left  { display: flex; gap: 12px; align-items: center; flex: 1; min-width: 0; }
.rc-avatar { width: 48px; height: 48px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.rc-name    { font-size: 14px; font-weight: 600; }
.rc-service { font-size: 13px; color: var(--text-sub); margin: 2px 0; }
.rc-date    { font-size: 12px; color: var(--text-muted); }
.rc-right   { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }
.rc-actions { display: flex; gap: 6px; flex-wrap: wrap; justify-content: flex-end; }

.pay-card { padding: 16px 18px; }
.pc-row   { display: flex; justify-content: space-between; align-items: center; }
.pc-service-name { font-size: 15px; font-weight: 600; color: var(--text); margin-bottom: 3px; }
.pc-stylist  { font-size: 13px; color: var(--text-sub); margin-bottom: 4px; }
.pc-date     { font-size: 12px; color: var(--text-muted); margin-bottom: 2px; }
.pc-order-id { font-size: 11px; color: var(--text-muted); font-family: monospace; word-break: break-all; max-width: 200px; }
.pc-amount   { font-size: 16px; font-weight: 700; color: var(--primary); margin-bottom: 4px; }
.pc-method   { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }

/* Profile edit */
.edit-card  { padding: 24px; margin-bottom: 14px; }
.edit-title { font-size: 15px; font-weight: 700; margin-bottom: 18px; }
.edit-form  { display: flex; flex-direction: column; gap: 14px; }
.msg-success { color: var(--primary); font-size: 13px; }
.msg-error   { color: var(--red);     font-size: 13px; }
.danger-zone { border-color: rgba(255,59,48,0.2); }
.danger-desc { font-size: 13px; color: var(--text-sub); margin-bottom: 14px; }

/* Empty */
.empty-state { text-align: center; padding: 60px 0; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.empty-icon  { font-size: 36px; }
.loading-center { text-align: center; padding: 40px; }

/* Modal */
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 500;
}
.modal-box   { width: 90%; max-width: 400px; padding: 28px; }
.modal-title { font-size: 18px; font-weight: 700; margin-bottom: 6px; }
.modal-sub   { font-size: 13px; color: var(--text-sub); margin-bottom: 20px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 20px; }

/* Review modal */
.star-row { display: flex; gap: 4px; margin-bottom: 10px; }
.star-btn {
  font-size: 26px; background: none; border: none; cursor: pointer;
  color: var(--border); transition: color 0.1s; padding: 0; line-height: 1;
}
.star-btn.filled { color: #FFCC00; }
.review-textarea {
  width: 100%; border: 1.5px solid var(--border); border-radius: var(--radius-sm);
  padding: 10px 12px; font-size: 14px; resize: vertical; font-family: inherit;
  margin-bottom: 8px;
}
.review-textarea:focus { outline: none; border-color: var(--primary); }

.detail-rows { display: flex; flex-direction: column; gap: 12px; margin-bottom: 4px; }
.detail-row  { display: flex; justify-content: space-between; align-items: center; gap: 12px; font-size: 14px; }
.dr-label    { color: var(--text-muted); font-size: 13px; }
.detail-row.total { padding-top: 12px; border-top: 1px solid var(--border); }
.dr-price    { font-size: 18px; font-weight: 700; color: var(--primary); }

@media (max-width: 768px) {
  .mypage-layout { grid-template-columns: 1fr; }
  .sidebar { position: static; }
  .res-card { flex-direction: column; align-items: flex-start; }
  .rc-right { align-items: flex-start; }
}
</style>
