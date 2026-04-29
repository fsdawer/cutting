<template>
  <main class="page">
    <div class="container">
      <div class="page-header">
        <div>
          <h1 class="section-title">예약 관리</h1>
          <p class="section-subtitle">고객 예약 목록을 확인하고 완료 처리하세요</p>
        </div>
        <RouterLink to="/stylist/manage" class="btn btn-ghost btn-sm">← 프로필 관리</RouterLink>
      </div>

      <!-- Tabs -->
      <div class="tab-bar">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          class="tab-btn"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
          <span v-if="countByTab(tab.value)" class="tab-count">{{ countByTab(tab.value) }}</span>
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="state-center">
        <div class="spinner"></div>
      </div>

      <!-- List -->
      <div v-else class="res-list">
        <div v-for="r in filteredReservations" :key="r.id" class="card res-card">
          <!-- 상단: 상태 + 서비스 + 날짜 + 금액 -->
          <div class="rc-top">
            <div class="rc-top-left">
              <div class="rc-meta">
                <span class="badge" :class="statusBadge(r.status)">{{ statusLabel(r.status) }}</span>
                <span class="rc-id">#{{ r.id }}</span>
              </div>
              <h3 class="rc-service">{{ r.serviceName }}</h3>
              <p class="rc-date">📅 {{ formatDate(r.reservedAt) }}</p>
              <p class="rc-price">{{ r.totalPrice?.toLocaleString() }}원</p>
            </div>
            <div class="rc-actions">
              <button
                v-if="r.status === 'PENDING' || r.status === 'CONFIRMED'"
                class="btn btn-primary btn-sm"
                @click="updateStatus(r.id, 'DONE')"
              >완료 처리</button>
              <button
                v-if="r.status === 'PENDING' || r.status === 'CONFIRMED'"
                class="btn btn-ghost btn-sm danger"
                @click="updateStatus(r.id, 'CANCELLED')"
              >취소</button>
              <RouterLink v-if="r.chatRoomId" :to="`/chat/${r.chatRoomId}`" class="btn btn-ghost btn-sm">
                채팅
              </RouterLink>
            </div>
          </div>

          <!-- 고객 정보 -->
          <div class="rc-customer">
            <span class="rc-customer-label">고객</span>
            <span class="rc-customer-name">{{ r.userName }}</span>
            <span v-if="r.userPhone" class="rc-customer-phone">{{ r.userPhone }}</span>
          </div>

          <!-- 요구사항 -->
          <div v-if="r.requestMemo" class="rc-memo">
            <span class="rc-memo-label">💬 요구사항</span>
            <p class="rc-memo-text">{{ r.requestMemo }}</p>
          </div>

          <!-- 첨부 이미지 -->
          <div v-if="r.imageUrls && r.imageUrls.length" class="rc-images">
            <span class="rc-images-label">📎 첨부 이미지</span>
            <div class="rc-image-grid">
              <a
                v-for="(url, idx) in r.imageUrls"
                :key="idx"
                :href="url"
                target="_blank"
                class="rc-image-link"
              >
                <img :src="url" class="rc-image-thumb" :alt="`첨부이미지 ${idx+1}`" />
              </a>
            </div>
          </div>
        </div>

        <div v-if="filteredReservations.length === 0" class="state-center">
          <p class="state-icon">📋</p>
          <p class="state-text">해당 상태의 예약이 없습니다</p>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { reservationApi } from '@/api/reservation'

const loading      = ref(false)
const reservations = ref([])
const activeTab    = ref('active')

const tabs = [
  { label: '전체',   value: 'all' },
  { label: '대기중', value: 'active' },
  { label: '완료',   value: 'DONE' },
  { label: '취소',   value: 'CANCELLED' },
]

const filteredReservations = computed(() => {
  if (activeTab.value === 'all')    return reservations.value
  if (activeTab.value === 'active') return reservations.value.filter(r => r.status === 'PENDING' || r.status === 'CONFIRMED')
  return reservations.value.filter(r => r.status === activeTab.value)
})

function countByTab(tabValue) {
  if (tabValue === 'all') return null
  const list = tabValue === 'active'
    ? reservations.value.filter(r => r.status === 'PENDING' || r.status === 'CONFIRMED')
    : reservations.value.filter(r => r.status === tabValue)
  return list.length > 0 ? list.length : null
}

function statusLabel(s) {
  return { PENDING: '대기중', CONFIRMED: '대기중', DONE: '완료', CANCELLED: '취소됨' }[s] || s
}
function statusBadge(s) {
  return { PENDING: 'badge-gold', CONFIRMED: 'badge-gold', DONE: 'badge-gray', CANCELLED: 'badge-red' }[s] || ''
}
function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function loadReservations() {
  loading.value = true
  try {
    const res = await reservationApi.getStylistReservations()
    reservations.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function updateStatus(id, status) {
  const msgMap = { DONE: '완료 처리', CANCELLED: '취소' }
  if (!confirm(`예약을 ${msgMap[status]}하시겠습니까?`)) return
  try {
    await reservationApi.updateStatus(id, status)
    await loadReservations()
  } catch (e) {
    alert(e.response?.data?.message || '상태 변경 중 오류가 발생했습니다.')
  }
}

onMounted(() => loadReservations())
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
}

/* Tabs */
.tab-bar {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--border);
  margin-bottom: 20px;
  flex-wrap: wrap;
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
  display: flex; align-items: center; gap: 6px;
}
.tab-btn:hover { color: var(--text); }
.tab-btn.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 700; }
.tab-count {
  background: var(--primary); color: #fff;
  font-size: 11px; font-weight: 700;
  padding: 2px 6px; border-radius: var(--radius-full);
  min-width: 18px; text-align: center;
}

/* Cards */
.res-list { display: flex; flex-direction: column; gap: 14px; }
.res-card  { padding: 20px; }

.rc-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}
.rc-top-left { flex: 1; }
.rc-meta     { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.rc-id       { font-size: 12px; color: var(--text-muted); }
.rc-service  { font-size: 16px; font-weight: 700; margin-bottom: 4px; }
.rc-date     { font-size: 13px; color: var(--text-sub); margin-bottom: 3px; }
.rc-price    { font-size: 14px; font-weight: 600; color: var(--primary); }

.rc-actions { display: flex; gap: 8px; flex-wrap: wrap; flex-shrink: 0; }
.danger { color: var(--red) !important; border-color: var(--red) !important; }

/* Customer */
.rc-customer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--bg);
  border-radius: var(--radius-sm);
  margin-bottom: 10px;
}
.rc-customer-label { font-size: 12px; color: var(--text-muted); font-weight: 600; }
.rc-customer-name  { font-size: 14px; font-weight: 600; }
.rc-customer-phone { font-size: 13px; color: var(--text-sub); }

/* Memo */
.rc-memo {
  padding: 10px 14px;
  background: var(--bg);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--primary);
  margin-bottom: 10px;
}
.rc-memo-label { font-size: 12px; font-weight: 600; color: var(--primary); display: block; margin-bottom: 4px; }
.rc-memo-text  { font-size: 13px; color: var(--text-sub); line-height: 1.6; }

/* Images */
.rc-images { margin-top: 4px; }
.rc-images-label { font-size: 12px; font-weight: 600; color: var(--text-muted); display: block; margin-bottom: 8px; }
.rc-image-grid  { display: flex; flex-wrap: wrap; gap: 8px; }
.rc-image-link  { display: block; border-radius: var(--radius-sm); overflow: hidden; border: 1px solid var(--border); }
.rc-image-thumb { width: 80px; height: 80px; object-fit: cover; display: block; transition: var(--transition); }
.rc-image-thumb:hover { opacity: 0.85; }

/* States */
.state-center { text-align: center; padding: 80px 0; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.state-icon   { font-size: 40px; }
.state-text   { color: var(--text-sub); font-size: 15px; }

@media (max-width: 768px) {
  .page-header { flex-direction: column; }
  .rc-top      { flex-direction: column; }
  .rc-actions  { flex-direction: row; }
}
</style>
