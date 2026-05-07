<template>
  <main class="page">
    <div class="container">
      <button class="back-link" @click="router.push('/mypage')">← 뒤로가기</button>

      <div v-if="loading" class="state-center">
        <div class="spinner"></div>
        <p class="state-text">결제 정보를 불러오는 중...</p>
      </div>

      <div v-else class="pay-layout">
        <div class="pay-main">
          <h1 class="pay-title">결제</h1>

          <div class="card info-card">
            <h2 class="card-section-title">예약 내역</h2>
            <div class="res-summary">
              <img :src="`https://i.pravatar.cc/64?u=${reservation.stylistId}`" class="res-avatar" />
              <div class="res-detail">
                <p class="res-stylist">{{ reservation.stylistName }}</p>
                <p class="res-salon">{{ reservation.salonName }}</p>
                <div class="res-meta-row">
                  <span class="res-meta-item">{{ formatDate(reservation.reservedAt) }}</span>
                  <span class="meta-dot">·</span>
                  <span class="res-meta-item">{{ reservation.serviceName }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <aside class="pay-sidebar">
          <div class="card summary-card">
            <h2 class="card-section-title">결제 요약</h2>
            <div class="summary-rows">
              <div class="summary-row">
                <span>서비스</span>
                <span>{{ reservation.serviceName }}</span>
              </div>
              <div class="summary-row">
                <span>소요 시간</span>
                <span>{{ reservation.serviceDuration }}분</span>
              </div>
              <div class="summary-divider"></div>
              <div class="summary-row total">
                <span>총 결제 금액</span>
                <span class="total-price">{{ reservation.totalPrice?.toLocaleString() }}원</span>
              </div>
            </div>

            <p v-if="payError" class="err-msg">{{ payError }}</p>

            <button
              class="btn btn-primary btn-full pay-btn"
              :disabled="paying"
              @click="handlePay"
            >
              <span v-if="paying" class="spinner" style="width:16px;height:16px;border-width:2px"></span>
              <span v-else>{{ reservation.totalPrice?.toLocaleString() }}원 결제하기</span>
            </button>
            <p class="pay-note">결제 후 예약이 확정됩니다</p>
          </div>
        </aside>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { reservationApi } from '@/api/reservation'
import { paymentApi } from '@/api/payment'

const route  = useRoute()
const router = useRouter()

const reservation = ref({})
const loading     = ref(true)
const paying      = ref(false)
const payError    = ref('')

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function handlePay() {
  paying.value = true
  payError.value = ''
  try {
    const prepRes = await paymentApi.prepare({
      reservationId: reservation.value.id,
      method: 'TOSS',
    })
    const { orderId } = prepRes.data

    const tossPayments = window.TossPayments(import.meta.env.VITE_TOSS_CLIENT_KEY)
    const payment = tossPayments.payment({ customerKey: window.TossPayments.ANONYMOUS })
    await payment.requestPayment({
      method: 'CARD',
      amount: { currency: 'KRW', value: reservation.value.totalPrice },
      orderId,
      orderName: reservation.value.serviceName,
      successUrl: `${window.location.origin}/payment/success`,
      failUrl:    `${window.location.origin}/payment/fail`,
    })
  } catch (e) {
    if (e?.code === 'PAY_PROCESS_CANCELED' || e?.code === 'USER_CANCEL') {
      paying.value = false
      return
    }
    payError.value = e.response?.data?.message || e.message || '결제 처리 중 오류가 발생했습니다.'
    paying.value = false
  }
}

onMounted(async () => {
  try {
    const res = await reservationApi.getById(route.params.reservationId)
    reservation.value = res.data
  } catch {
    router.push('/')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.back-link {
  display: inline-block; margin-bottom: 20px;
  font-size: 13px; color: var(--text-muted); background: none; border: none; cursor: pointer; padding: 0;
}
.back-link:hover { color: var(--text); }

.pay-title { font-size: 24px; font-weight: 800; letter-spacing: -0.03em; margin-bottom: 20px; }
.pay-layout { display: grid; grid-template-columns: 1fr 320px; gap: 20px; align-items: start; }
.pay-main { display: flex; flex-direction: column; gap: 16px; }

.card-section-title { font-size: 14px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.06em; margin-bottom: 16px; }

.res-summary { display: flex; gap: 14px; align-items: center; }
.res-avatar { width: 56px; height: 56px; border-radius: var(--radius-md); object-fit: cover; flex-shrink: 0; border: 1px solid var(--border); }
.res-stylist { font-size: 16px; font-weight: 700; }
.res-salon { font-size: 13px; color: var(--text-muted); margin: 2px 0 6px; }
.res-meta-row { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-sub); }
.meta-dot { color: var(--border-strong); }

.summary-rows { display: flex; flex-direction: column; gap: 10px; margin-bottom: 20px; }
.summary-row { display: flex; justify-content: space-between; font-size: 14px; color: var(--text-sub); }
.summary-divider { height: 1px; background: var(--border); margin: 4px 0; }
.summary-row.total { font-size: 15px; font-weight: 700; color: var(--text); margin-top: 4px; }
.total-price { font-size: 20px; font-weight: 800; color: var(--primary); }

.err-msg { color: var(--danger); font-size: 13px; margin-bottom: 12px; padding: 10px 14px; background: var(--danger-light); border-radius: var(--radius-sm); }
.pay-btn { margin-bottom: 10px; padding: 14px; font-size: 16px; }
.pay-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.pay-note { text-align: center; font-size: 12px; color: var(--text-muted); }

.pay-sidebar { position: sticky; top: 76px; }

@media (max-width: 768px) {
  .pay-layout { grid-template-columns: 1fr; }
  .pay-sidebar { position: static; }
}
</style>
