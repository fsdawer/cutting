<template>
  <main class="page">
    <div class="container">
      <div v-if="loading" class="state-center">
        <div class="spinner"></div>
        <p class="state-text">결제 정보를 불러오는 중...</p>
      </div>

      <div v-else class="pay-layout">
        <!-- 좌측: 예약 정보 + 결제 수단 -->
        <div class="pay-main">
          <div class="page-header">
            <button class="back-btn" @click="$router.back()">← 뒤로가기</button>
            <h1 class="section-title">결제</h1>
          </div>

          <!-- 예약 정보 -->
          <div class="card info-card">
            <h2 class="card-title">예약 내역</h2>
            <div class="res-info">
              <img
                :src="reservation.stylistProfileImg || `https://i.pravatar.cc/80?u=${reservation.stylistId}`"
                class="res-avatar"
              />
              <div class="res-detail">
                <p class="res-name">{{ reservation.stylistName }} · {{ reservation.salonName }}</p>
                <p class="res-meta">📅 {{ formatDate(reservation.reservedAt) }}</p>
                <p class="res-meta">💇 {{ reservation.serviceName }}</p>
              </div>
            </div>
          </div>

          <!-- 결제 수단 -->
          <div class="card method-card">
            <h2 class="card-title">결제 수단</h2>
            <div class="method-grid">
              <button
                v-for="m in payMethods"
                :key="m.value"
                class="method-btn"
                :class="{ selected: payMethod === m.value }"
                @click="payMethod = m.value"
              >
                <span class="method-icon">{{ m.icon }}</span>
                <span>{{ m.label }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- 우측: 결제 요약 -->
        <aside class="pay-sidebar">
          <div class="card summary-card">
            <h2 class="card-title">결제 요약</h2>
            <div class="summary-rows">
              <div class="sr-row">
                <span>서비스</span>
                <span>{{ reservation.serviceName }}</span>
              </div>
              <div class="sr-row">
                <span>기본 요금</span>
                <span>{{ reservation.totalPrice?.toLocaleString() }}원</span>
              </div>
              <div class="sr-row total">
                <span>최종 결제액</span>
                <span class="total-price">{{ reservation.totalPrice?.toLocaleString() }}원</span>
              </div>
            </div>

            <p v-if="payError" class="msg-error">{{ payError }}</p>

            <button
              class="btn btn-primary btn-full btn-lg"
              @click="handlePayment"
              :disabled="paying"
              style="margin-top: 16px;"
            >
              <span v-if="paying" class="spinner" style="width:18px;height:18px;border-width:2px;"></span>
              <span v-else>{{ reservation.totalPrice?.toLocaleString() }}원 결제하기</span>
            </button>

            <div class="secure-badge">
              <span>🔒</span>
              <span>토스페이먼츠 보안 결제</span>
            </div>
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
import { useAuthStore } from '@/stores/authStore'

const route         = useRoute()
const router        = useRouter()
const authStore     = useAuthStore()
const reservationId = route.params.reservationId

const loading   = ref(true)
const reservation = ref({})
const paying    = ref(false)
const payError  = ref('')
const orderId   = ref('')

const payMethod  = ref('카드')
const payMethods = [
  { value: '카드',    label: '신용/체크카드', icon: '💳' },
  { value: '토스페이', label: '토스페이',     icon: '💸' },
  { value: '계좌이체', label: '계좌이체',     icon: '🏦' },
]

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} · ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function loadTossScript() {
  return new Promise((resolve) => {
    if (window.TossPayments) { resolve(true); return }
    const script = document.createElement('script')
    script.src = 'https://js.tosspayments.com/v1/payment'
    script.onload  = () => resolve(true)
    script.onerror = () => resolve(false)
    document.head.appendChild(script)
  })
}

onMounted(async () => {
  try {
    const res = await reservationApi.getById(reservationId)
    reservation.value = res.data

    const prepareRes = await paymentApi.prepare(Number(reservationId))
    orderId.value = prepareRes.data.orderId
  } catch (e) {
    console.error('결제 준비 실패', e)
    payError.value = '결제 초기화에 실패했습니다.'
  } finally {
    loading.value = false
  }
  await loadTossScript()
})

async function handlePayment() {
  if (!orderId.value) { payError.value = '주문 정보가 없습니다.'; return }
  paying.value  = true
  payError.value = ''
  try {
    if (!window.TossPayments) {
      payError.value = '결제 모듈을 불러오지 못했습니다.'
      return
    }
    const toss = window.TossPayments(import.meta.env.VITE_TOSS_CLIENT_KEY || 'test_ck_placeholder')
    await toss.requestPayment(payMethod.value, {
      amount:       reservation.value.totalPrice,
      orderId:      orderId.value,
      orderName:    reservation.value.serviceName,
      customerName: authStore.user?.name || '고객',
      successUrl:   `${window.location.origin}/payment/success`,
      failUrl:      `${window.location.origin}/payment/fail`,
    })
    // 리다이렉트 발생
  } catch (e) {
    if (e.code === 'USER_CANCEL') {
      payError.value = '결제가 취소되었습니다.'
    } else {
      payError.value = e.response?.data?.message || e.message || '결제 중 오류가 발생했습니다.'
    }
  } finally {
    paying.value = false
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.back-btn {
  background: none; border: none;
  color: var(--text-sub); font-size: 14px;
  cursor: pointer; padding: 6px 0;
  margin-bottom: 10px; display: block;
  transition: var(--transition);
}
.back-btn:hover { color: var(--primary); }

.pay-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
  align-items: start;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.info-card, .method-card { margin-bottom: 16px; }

.res-info { display: flex; gap: 14px; align-items: center; }
.res-avatar { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; flex-shrink: 0; border: 2px solid var(--border); }
.res-detail { display: flex; flex-direction: column; gap: 4px; }
.res-name { font-size: 15px; font-weight: 600; }
.res-meta { font-size: 13px; color: var(--text-sub); }

.method-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.method-btn {
  display: flex; flex-direction: column;
  align-items: center; gap: 6px;
  padding: 14px 8px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-md);
  background: #fff;
  font-size: 12px; font-weight: 500; color: var(--text-sub);
  cursor: pointer; transition: var(--transition);
}
.method-btn:hover { border-color: var(--primary); color: var(--primary); }
.method-btn.selected { border-color: var(--primary); background: var(--primary-light); color: var(--primary); font-weight: 600; }
.method-icon { font-size: 22px; }

/* Summary */
.summary-card { position: sticky; top: calc(var(--navbar-h) + 16px); }
.summary-rows { display: flex; flex-direction: column; gap: 12px; margin-bottom: 16px; }
.sr-row { display: flex; justify-content: space-between; font-size: 14px; color: var(--text-sub); }
.sr-row.total { padding-top: 12px; border-top: 1px solid var(--border); color: var(--text); font-weight: 600; }
.total-price { font-size: 20px; font-weight: 700; color: var(--primary); }

.msg-error { color: var(--red); font-size: 13px; margin-top: 8px; }

.secure-badge {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  margin-top: 14px; padding-top: 12px;
  border-top: 1px solid var(--border);
  font-size: 12px; color: var(--text-muted);
}

.state-center { text-align: center; padding: 80px 0; display: flex; flex-direction: column; align-items: center; gap: 12px; }
.state-text { color: var(--text-sub); }

@media (max-width: 768px) {
  .pay-layout { grid-template-columns: 1fr; }
  .method-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
