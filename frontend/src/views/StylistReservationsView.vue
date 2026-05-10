<template>
  <main class="page">
    <div class="container">
      <div class="page-header">
        <div>
          <h1 class="section-title">예약 관리</h1>
          <p class="section-subtitle">고객 예약을 확인하고 상태를 업데이트하세요</p>
        </div>
        <RouterLink to="/stylist/manage" class="btn btn-ghost btn-sm">← 프로필 관리</RouterLink>
      </div>

      <!-- 통계 카드 -->
      <div class="stat-row">
        <div class="stat-card">
          <p class="stat-label">전체 예약</p>
          <p class="stat-val">{{ reservations.length }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">확정</p>
          <p class="stat-val success">{{ countByStatus('CONFIRMED') }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">완료</p>
          <p class="stat-val">{{ countByStatus('DONE') }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">취소</p>
          <p class="stat-val muted">{{ countByStatus('CANCELLED') }}</p>
        </div>
      </div>

      <!-- 매출 통계 -->
      <div class="card dash-card">
        <h2 class="dash-title">매출 통계</h2>
        <div class="dash-filter">
          <input v-model="dashStart" type="date" class="form-input date-input" />
          <span class="dash-sep">~</span>
          <input v-model="dashEnd" type="date" class="form-input date-input" />
          <button class="btn btn-primary btn-sm" @click="loadDashboard">조회</button>
        </div>
        <div v-if="dashStats.length" class="dash-summary">
          <div class="summary-card">
            <span class="summary-label">총 매출</span>
            <span class="summary-value">{{ totalRevenue.toLocaleString() }}원</span>
          </div>
          <div class="summary-card">
            <span class="summary-label">예약 건수</span>
            <span class="summary-value">{{ totalReservations }}건</span>
          </div>
          <div class="summary-card">
            <span class="summary-label">취소 건수</span>
            <span class="summary-value cancel">{{ totalCancels }}건</span>
          </div>
        </div>
        <table v-if="dashStats.length" class="dash-table">
          <thead>
            <tr><th>날짜</th><th>예약</th><th>취소</th><th>매출</th></tr>
          </thead>
          <tbody>
            <tr v-for="s in dashStats" :key="s.statDate">
              <td>{{ s.statDate }}</td>
              <td>{{ s.reservationCount }}건</td>
              <td>{{ s.cancelCount }}건</td>
              <td>{{ s.totalRevenue.toLocaleString() }}원</td>
            </tr>
          </tbody>
        </table>
        <p v-else-if="dashLoaded" class="empty-msg">해당 기간의 데이터가 없습니다.</p>
        <p v-if="dashError" class="msg-error">{{ dashError }}</p>
      </div>

      <!-- 탭 -->
      <div class="tab-bar">
        <button v-for="tab in tabs" :key="tab.value" class="tab-btn" :class="{ active: activeTab === tab.value }" @click="activeTab = tab.value">
          {{ tab.label }}
          <span v-if="tab.value !== 'all' && countByStatus(tab.value)" class="tab-count">{{ countByStatus(tab.value) }}</span>
        </button>
      </div>

      <div v-if="loading" class="state-center"><div class="spinner"></div></div>

      <div v-else-if="filteredReservations.length === 0" class="state-center">
        <p class="state-text">해당 상태의 예약이 없습니다.</p>
      </div>

      <div v-else class="res-list">
        <div v-for="r in filteredReservations" :key="r.id" class="res-card card">
          <!-- 헤더 -->
          <div class="rc-header">
            <div class="rc-header-left">
              <span class="badge" :class="statusBadge(r.status)">{{ statusLabel(r.status) }}</span>
              <span class="rc-id">#{{ r.id }}</span>
            </div>
            <div class="rc-actions">
              <button v-if="r.status === 'CONFIRMED'" class="btn btn-primary btn-sm" @click="updateStatus(r.id, 'DONE')">완료 처리</button>
              <button v-if="r.status === 'CONFIRMED'" class="btn btn-danger-ghost btn-sm" @click="updateStatus(r.id, 'CANCELLED')">취소</button>
              <RouterLink v-if="r.chatRoomId" :to="`/chat/${r.chatRoomId}`" class="btn btn-ghost btn-sm">채팅</RouterLink>
            </div>
          </div>

          <!-- 서비스/날짜 -->
          <div class="rc-body">
            <div class="rc-main">
              <p class="rc-service">{{ r.serviceName }}</p>
              <div class="rc-meta-row">
                <span class="rc-meta-item">{{ formatDate(r.reservedAt) }}</span>
                <span class="rc-meta-dot">·</span>
                <span class="rc-meta-item price">{{ r.totalPrice?.toLocaleString() }}원</span>
              </div>
            </div>
            <!-- 고객 정보 -->
            <div class="rc-customer">
              <div class="customer-avatar">{{ r.userName?.[0] ?? 'U' }}</div>
              <div>
                <p class="customer-name">{{ r.userName }}</p>
                <p v-if="r.userPhone" class="customer-phone">{{ r.userPhone }}</p>
              </div>
            </div>
          </div>

          <!-- 요구사항 -->
          <div v-if="r.requestMemo" class="rc-memo">
            <span class="memo-label">요구사항</span>
            <p class="memo-text">{{ r.requestMemo }}</p>
          </div>

          <!-- 첨부 이미지 -->
          <div v-if="r.imageUrls?.length" class="rc-images">
            <span class="images-label">첨부 이미지</span>
            <div class="images-grid">
              <a v-for="(url, idx) in r.imageUrls" :key="idx" :href="url" target="_blank" class="img-link">
                <img :src="url" class="img-thumb" :alt="`이미지 ${idx+1}`" />
              </a>
            </div>
          </div>
        </div>
      </div>

      <div v-if="hasMore" style="display:flex;justify-content:center;padding:16px 0">
        <button class="btn btn-ghost" :disabled="loadingMore" @click="loadMore">
          <span v-if="loadingMore" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
          <span v-else>더 보기</span>
        </button>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { reservationApi } from '@/api/reservation'
import { stylistApi } from '@/api/stylist'

const loading = ref(false)
const loadingMore = ref(false)
const reservations = ref([])
const hasMore = ref(false)
const nextCursorId = ref(null)
const activeTab = ref('all')

const tabs = [
  { label: '전체', value: 'all' },
  { label: '예약확정', value: 'CONFIRMED' },
  { label: '완료', value: 'DONE' },
  { label: '취소', value: 'CANCELLED' },
]

const filteredReservations = computed(() => {
  if (activeTab.value === 'all') return reservations.value
  return reservations.value.filter(r => r.status === activeTab.value)
})

function countByStatus(s) { return reservations.value.filter(r => r.status === s).length }
function statusLabel(s) { return { CONFIRMED: '예약확정', DONE: '완료', CANCELLED: '취소됨' }[s] || s }
function statusBadge(s) { return { CONFIRMED: 'badge-green', DONE: 'badge-gray', CANCELLED: 'badge-red' }[s] || 'badge-gray' }
function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function loadReservations(cursorId = null, append = false) {
  if (!append) loading.value = true
  else loadingMore.value = true
  try {
    const res = await reservationApi.getStylistReservations(cursorId)
    const content = res.data.content || []
    reservations.value = append ? [...reservations.value, ...content] : content
    hasMore.value = res.data.hasMore
    nextCursorId.value = res.data.nextCursorId
  } catch { if (!append) reservations.value = [] }
  finally { loading.value = false; loadingMore.value = false }
}

async function loadMore() {
  await loadReservations(nextCursorId.value, true)
}

async function updateStatus(id, status) {
  if (!confirm(`예약을 ${status === 'DONE' ? '완료 처리' : '취소'}하시겠습니까?`)) return
  try { await reservationApi.updateStatus(id, status); await loadReservations() }
  catch (e) { alert(e.response?.data?.message || '상태 변경 오류') }
}

// ── 매출 통계 ─────────────────────────────────────────────────────
const today = new Date().toISOString().slice(0, 10)
const firstOfMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10)
const dashStart = ref(firstOfMonth)
const dashEnd = ref(today)
const dashStats = ref([])
const dashLoaded = ref(false)
const dashError = ref('')

const totalRevenue = computed(() => dashStats.value.reduce((s, r) => s + r.totalRevenue, 0))
const totalReservations = computed(() => dashStats.value.reduce((s, r) => s + r.reservationCount, 0))
const totalCancels = computed(() => dashStats.value.reduce((s, r) => s + r.cancelCount, 0))

async function loadDashboard() {
  dashError.value = ''
  try {
    const res = await stylistApi.getDashboardStats(dashStart.value, dashEnd.value)
    dashStats.value = res.data
    dashLoaded.value = true
  } catch (e) {
    dashError.value = e.response?.data?.message || '조회 실패'
  }
}

onMounted(() => { loadReservations(); loadDashboard() })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; gap: 16px; }

/* Stats */
.stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 24px; }
.stat-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-md); padding: 16px 20px; box-shadow: var(--shadow-sm); }
.stat-label { font-size: 12px; color: var(--text-muted); font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 6px; }
.stat-val { font-size: 28px; font-weight: 800; letter-spacing: -0.03em; color: var(--text); }
.stat-val.success { color: var(--success); }
.stat-val.muted { color: var(--text-muted); }

/* Cards */
.res-list { display: flex; flex-direction: column; gap: 10px; }
.res-card { padding: 0; overflow: hidden; }

.rc-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 20px; background: var(--bg-surface);
  border-bottom: 1px solid var(--border);
}
.rc-header-left { display: flex; align-items: center; gap: 10px; }
.rc-id { font-size: 12px; color: var(--text-muted); font-family: monospace; }
.rc-actions { display: flex; gap: 8px; flex-wrap: wrap; }

.rc-body { display: flex; justify-content: space-between; align-items: flex-start; padding: 16px 20px; gap: 16px; }
.rc-main { flex: 1; }
.rc-service { font-size: 17px; font-weight: 700; margin-bottom: 6px; }
.rc-meta-row { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-muted); }
.rc-meta-dot { color: var(--border-strong); }
.rc-meta-item.price { font-weight: 700; color: var(--text); }

.rc-customer { display: flex; gap: 10px; align-items: center; flex-shrink: 0; }
.customer-avatar {
  width: 38px; height: 38px; border-radius: 50%;
  background: var(--bg-surface); border: 1px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  font-size: 15px; font-weight: 700; color: var(--text-sub); flex-shrink: 0;
}
.customer-name { font-size: 14px; font-weight: 600; }
.customer-phone { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.rc-memo { padding: 12px 20px; background: var(--bg-surface); border-top: 1px solid var(--border); }
.memo-label { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; display: block; margin-bottom: 4px; }
.memo-text { font-size: 13px; color: var(--text-sub); line-height: 1.6; }

.rc-images { padding: 12px 20px; border-top: 1px solid var(--border); }
.images-label { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; display: block; margin-bottom: 8px; }
.images-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.img-link { display: block; border-radius: var(--radius-sm); overflow: hidden; border: 1px solid var(--border); }
.img-thumb { width: 80px; height: 80px; object-fit: cover; display: block; transition: var(--transition); }
.img-thumb:hover { opacity: 0.8; }

/* Dashboard */
.dash-card { padding: 20px; margin-bottom: 24px; }
.dash-title { font-size: 13px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.06em; margin-bottom: 16px; }
.dash-filter { display: flex; align-items: center; gap: 8px; margin-bottom: 20px; }
.dash-sep { color: var(--text-muted); }
.date-input { width: 140px; }
.dash-summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 20px; }
.summary-card { background: var(--bg-surface); border: 1px solid var(--border); border-radius: var(--radius-md); padding: 16px; text-align: center; }
.summary-label { display: block; font-size: 12px; color: var(--text-muted); margin-bottom: 6px; }
.summary-value { font-size: 20px; font-weight: 700; color: var(--text-primary); }
.summary-value.cancel { color: var(--danger); }
.dash-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.dash-table th { text-align: left; padding: 8px 12px; font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 2px solid var(--border); }
.dash-table td { padding: 10px 12px; border-bottom: 1px solid var(--border); }
.dash-table tr:last-child td { border-bottom: none; }
.empty-msg { text-align: center; color: var(--text-muted); padding: 24px 0; font-size: 14px; }
.msg-error { color: var(--danger); font-size: 13px; margin-top: 8px; }

@media (max-width: 768px) {
  .stat-row { grid-template-columns: repeat(2, 1fr); }
  .rc-body { flex-direction: column; }
  .rc-header { flex-direction: column; align-items: flex-start; gap: 10px; }
  .dash-summary { grid-template-columns: 1fr 1fr; }
}
</style>
