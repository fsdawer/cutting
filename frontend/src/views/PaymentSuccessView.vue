<template>
  <main class="page">
    <div class="container state-center">
      <div v-if="loading" class="loading-box">
        <div class="spinner"></div>
        <p class="state-text">결제를 확인하고 있습니다...</p>
        <p class="state-hint">창을 닫지 마세요.</p>
      </div>

      <div v-else-if="error" class="result-box">
        <div class="result-icon fail">✗</div>
        <h2 class="result-title">결제 승인 실패</h2>
        <p class="result-desc">{{ error }}</p>
        <button class="btn btn-primary" @click="$router.push('/')">홈으로 이동</button>
      </div>

      <div v-else class="result-box">
        <div class="result-icon success">✓</div>
        <h2 class="result-title">결제가 완료되었습니다</h2>
        <p class="result-desc">예약이 확정되었습니다.</p>

        <div v-if="reservation" class="res-detail-card">
          <div class="res-row">
            <span class="res-label">미용사</span>
            <span class="res-value">{{ reservation.stylistName }} · {{ reservation.salonName }}</span>
          </div>
          <div class="res-row">
            <span class="res-label">서비스</span>
            <span class="res-value">{{ reservation.serviceName }}</span>
          </div>
          <div class="res-row">
            <span class="res-label">일시</span>
            <span class="res-value">{{ formatDate(reservation.reservedAt) }}</span>
          </div>
          <div class="res-row">
            <span class="res-label">결제 금액</span>
            <span class="res-value price">{{ reservation.totalPrice?.toLocaleString() }}원</span>
          </div>
        </div>

        <div class="chat-notice">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          <span>채팅방이 생성되었습니다</span>
        </div>

        <button class="btn btn-primary btn-lg btn-full-mob" @click="$router.push('/mypage')">확인</button>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { paymentApi } from '@/api/payment'
import { reservationApi } from '@/api/reservation'

const route       = useRoute()
const loading     = ref(true)
const error       = ref('')
const reservation = ref(null)

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} · ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

onMounted(async () => {
  const { paymentKey, orderId, amount } = route.query
  if (!paymentKey || !orderId || !amount) {
    error.value = '결제 정보가 유효하지 않습니다.'
    loading.value = false
    return
  }
  try {
    const payRes = await paymentApi.confirm({ paymentKey, orderId, amount: Number(amount) })
    const reservationId = payRes.data?.reservationId
    if (reservationId) {
      const resRes = await reservationApi.getById(reservationId)
      reservation.value = resRes.data
    }
  } catch (e) {
    error.value = e.response?.data?.message || '결제 승인 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.state-center {
  text-align: center;
  padding: 60px 0 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.state-text { font-size: 16px; font-weight: 600; color: var(--text); }
.state-hint { font-size: 13px; color: var(--text-muted); }

.result-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  width: 100%;
  max-width: 480px;
}

.result-icon {
  width: 72px; height: 72px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 32px; font-weight: 700;
}
.result-icon.success { background: var(--primary-light); color: var(--primary); }
.result-icon.fail    { background: #FFF0EE; color: var(--red); }

.result-title { font-size: 22px; font-weight: 700; margin: 0; }
.result-desc  { font-size: 14px; color: var(--text-sub); margin: 0; }

/* Reservation detail card */
.res-detail-card {
  width: 100%;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 4px;
}
.res-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}
.res-label { color: var(--text-sub); font-weight: 500; flex-shrink: 0; }
.res-value { font-weight: 600; color: var(--text); text-align: right; }
.res-value.price { color: var(--primary); font-size: 16px; }

/* Chat notice badge */
.chat-notice {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background: var(--primary-light);
  color: var(--primary);
  font-size: 14px;
  font-weight: 600;
  padding: 10px 20px;
  border-radius: var(--radius-full);
}

.btn-full-mob { width: 100%; }

@media (max-width: 480px) {
  .res-detail-card { padding: 16px 18px; }
}
</style>
