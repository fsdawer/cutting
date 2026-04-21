<template>
  <main class="page">
    <div class="container">
      <div v-if="loading" style="text-align:center;padding:80px 0">
        <div class="spinner"></div>
        <p style="color:var(--color-text-muted);margin-top:16px">결제 정보를 불러오는 중...</p>
      </div>

      <div v-else class="pay-layout">
        <!-- 좌측: 예약 정보 + 토스 위젯 -->
        <div class="pay-main">
          <h1 class="section-title">결제</h1>
          <p class="section-subtitle">예약 정보를 확인하고 결제를 진행하세요</p>

          <!-- 예약 내역 카드 -->
          <div class="card pay-card">
            <h2 class="pay-section-title">📋 예약 내역</h2>
            <div class="pay-stylist">
              <img
                :src="reservation.stylistProfileImg || `https://i.pravatar.cc/60?u=${reservation.stylistId}`"
                class="pay-avatar"
              />
              <div class="pay-info">
                <p class="pay-name">{{ reservation.stylistName }} · {{ reservation.salonName }}</p>
                <p class="pay-detail">📅 {{ formatDate(reservation.reservedAt) }}</p>
                <p class="pay-detail">💇 {{ reservation.serviceName }}</p>
              </div>
            </div>
          </div>

          <!-- 토스 결제 위젯 영역 -->
          <div class="card pay-card" v-if="orderId">
            <h2 class="pay-section-title">💳 결제 수단</h2>
            <div id="payment-widget" class="toss-widget-area"></div>
            <div id="agreement-widget" class="toss-agreement-area"></div>
          </div>

          <!-- SDK 없을 때 개발용 결제 수단 선택 -->
          <div class="card pay-card" v-if="!tossReady && !orderId">
            <h2 class="pay-section-title">💳 결제 수단 선택</h2>
            <div class="pay-methods">
              <button
                v-for="m in methods"
                :key="m.id"
                class="pay-method"
                :class="{ selected: selectedMethod === m.id }"
                @click="selectedMethod = m.id"
              >
                <span class="pm-icon">{{ m.icon }}</span>
                <span class="pm-name">{{ m.name }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- 우측 사이드바: 요약 + 결제하기 버튼 -->
        <aside class="pay-sidebar">
          <div class="card" style="position:sticky;top:88px;">
            <h2 class="pay-section-title">🧾 결제 요약</h2>
            <div class="pay-summary-rows">
              <div class="ps-row">
                <span>서비스</span>
                <span>{{ reservation.serviceName }}</span>
              </div>
              <div class="ps-row">
                <span>기본 요금</span>
                <span>{{ reservation.totalPrice?.toLocaleString() }}원</span>
              </div>
              <div class="ps-row ps-total">
                <span>최종 결제액</span>
                <span class="ps-amount">{{ reservation.totalPrice?.toLocaleString() }}원</span>
              </div>
            </div>

            <!-- 결제하기 버튼 -->
            <button
              class="btn btn-primary btn-full btn-lg"
              @click="handlePayment"
              :disabled="paying || (!tossReady && loading)"
              id="pay-btn"
            >
              <span v-if="paying" class="spinner" style="width:18px;height:18px;border-width:2px;"></span>
              <span v-else>결제하기</span>
            </button>

            <p v-if="payError" class="pay-error">{{ payError }}</p>
            <p class="pay-notice">결제 완료 후 마이페이지에서 예약을 확인하세요</p>

            <!-- 보안 배지 -->
            <div class="pay-secure">
              <span>🔒</span>
              <span>토스페이먼츠 보안 결제</span>
            </div>
          </div>
        </aside>
      </div>

      <!-- 성공 모달 -->
      <Transition name="fade">
        <div v-if="showSuccess" class="modal-overlay" @click.self="goToMyPage">
          <div class="modal-box card">
            <div class="modal-icon">🎉</div>
            <h2 class="modal-title">결제가 완료되었습니다!</h2>
            <p class="modal-desc">마이페이지에서 예약 내역을 확인하세요.</p>
            <button class="btn btn-primary btn-lg" @click="goToMyPage">마이페이지로 이동</button>
          </div>
        </div>
      </Transition>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { reservationApi } from '@/api/reservation'
import { paymentApi } from '@/api/payment'

const route = useRoute()
const router = useRouter()
const reservationId = route.params.reservationId

const loading = ref(true)
const reservation = ref({})
const selectedMethod = ref('card')
const paying = ref(false)
const payError = ref('')
const showSuccess = ref(false)
const orderId = ref('')       // prepare 후 발급된 orderId
const tossReady = ref(false)  // 토스 SDK 로드 여부
let tossPaymentsInstance = null
let paymentWidgetInstance = null

const methods = [
  { id: 'card', icon: '💳', name: '신용/체크카드' },
  { id: 'kakao', icon: '💬', name: '카카오페이' },
  { id: 'naver', icon: '🅝', name: '네이버페이' },
  { id: 'toss', icon: '💸', name: '토스페이' },
]

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const yy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${yy}.${mm}.${dd} · ${hh}:${min}`
}

/** 토스 SDK 스크립트 동적 로드 */
function loadTossScript() {
  return new Promise((resolve) => {
    if (window.TossPayments) { resolve(true); return }
    const script = document.createElement('script')
    script.src = 'https://js.tosspayments.com/v2/standard'
    script.onload = () => resolve(true)
    script.onerror = () => resolve(false)
    document.head.appendChild(script)
  })
}

onMounted(async () => {
  try {
    const res = await reservationApi.getById(reservationId)
    reservation.value = res.data
  } catch (e) {
    console.error('예약 정보 로드 실패', e)
  } finally {
    loading.value = false
  }

  // 토스 SDK 로드 시도
  const loaded = await loadTossScript()
  if (loaded && window.TossPayments) {
    tossReady.value = true
  }

  // 바로 결제 준비 (prepare) 후 위젯 렌더링
  if (reservation.value && reservation.value.id) {
    try {
      const prepareRes = await paymentApi.prepare(Number(reservationId))
      orderId.value = prepareRes.data.orderId
      const amount = prepareRes.data.amount

      if (tossReady.value && window.TossPayments) {
        await nextTick()
        await initTossWidget(orderId.value, amount)
      }
    } catch (e) {
      console.error('결제 준비 실패', e)
      payError.value = '결제 초기화에 실패했습니다.'
    }
  }
})

/** 결제하기 버튼 클릭 */
async function handlePayment() {
  paying.value = true
  payError.value = ''

  try {
    // 위젯이 정상 로드 되어 있을 때 SDK의 requestPayment() 호출
    if (paymentWidgetInstance) {
      await paymentWidgetInstance.requestPayment({
        orderId: orderId.value,
        orderName: reservation.value.serviceName,
        successUrl: `${window.location.origin}/payment/success`,
        failUrl: `${window.location.origin}/payment/fail`,
      })
      // 리다이렉트 발생
      return
    }

    // SDK 없을 때 mock confirm (개발/테스트용)
    if (orderId.value) {
      await paymentApi.confirm({
        paymentKey: `mock_${Date.now()}`,
        orderId: orderId.value,
        amount: reservation.value.totalPrice,
      })
      showSuccess.value = true
    }
  } catch (e) {
    payError.value = e.response?.data?.message || e.message || '결제 중 오류가 발생했습니다.'
  } finally {
    paying.value = false
  }
}

/** 토스 위젯 초기화 및 DOM에 렌더링 */
async function initTossWidget(orderIdVal, amount) {
  const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY || 'test_ck_placeholder'
  tossPaymentsInstance = window.TossPayments(clientKey)
  paymentWidgetInstance = tossPaymentsInstance.widgets({ customerKey: 'ANONYMOUS' })

  await paymentWidgetInstance.setAmount({
    currency: 'KRW',
    value: amount,
  })

  await Promise.all([
    paymentWidgetInstance.renderPaymentMethods({
      selector: '#payment-widget',
      variantKey: 'DEFAULT',
    }),
    paymentWidgetInstance.renderAgreement({
      selector: '#agreement-widget',
      variantKey: 'AGREEMENT',
    }),
  ])
}

function goToMyPage() {
  router.push('/mypage')
}
</script>

<style scoped>
.pay-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;
  align-items: start;
}
.pay-card { margin-bottom: 20px; }
.pay-section-title {
  font-size: 16px; font-weight: 600; margin-bottom: 16px;
  display: flex; align-items: center; gap: 8px;
}

/* 예약 정보 */
.pay-stylist { display: flex; gap: 14px; align-items: center; }
.pay-avatar { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; border: 2px solid rgba(201,169,110,0.3); }
.pay-info { display: flex; flex-direction: column; gap: 4px; }
.pay-name { font-size: 15px; font-weight: 600; }
.pay-detail { font-size: 13px; color: var(--color-text-secondary); }

/* 토스 위젯 영역 */
.toss-widget-area { min-height: 200px; }
.toss-agreement-area { margin-top: 12px; }

/* 결제 수단 선택 (SDK 없을 때) */
.pay-methods { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.pay-method {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 16px; border-radius: var(--radius-md);
  border: 1.5px solid var(--color-border); background: transparent;
  cursor: pointer; transition: var(--transition);
}
.pay-method:hover { border-color: rgba(201,169,110,0.4); }
.pay-method.selected { border-color: var(--color-gold); background: rgba(201,169,110,0.08); }
.pm-icon { font-size: 20px; }
.pm-name { font-size: 14px; font-weight: 500; color: var(--color-text-primary); }

/* 사이드바 요약 */
.pay-summary-rows { display: flex; flex-direction: column; gap: 12px; margin-bottom: 20px; }
.ps-row { display: flex; justify-content: space-between; font-size: 14px; color: var(--color-text-secondary); }
.ps-total { padding-top: 12px; border-top: 1px solid var(--color-border); color: var(--color-text-primary); font-weight: 600; }
.ps-amount { color: var(--color-gold); font-size: 18px; }

.pay-notice { text-align: center; font-size: 12px; color: var(--color-text-muted); margin-top: 12px; }
.pay-error { color: var(--color-danger, #e74c3c); font-size: 13px; text-align: center; margin-top: 10px; }

.pay-secure {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  margin-top: 16px; padding-top: 12px; border-top: 1px solid var(--color-border);
  font-size: 12px; color: var(--color-text-muted);
}

/* 성공 모달 */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.75);
  display: flex; align-items: center; justify-content: center; z-index: 999;
  backdrop-filter: blur(6px);
}
.modal-box { text-align: center; padding: 48px 40px; max-width: 380px; width: 90%; }
.modal-icon { font-size: 52px; margin-bottom: 16px; }
.modal-title { font-size: 22px; font-weight: 700; margin-bottom: 10px; }
.modal-desc { color: var(--color-text-secondary); font-size: 14px; margin-bottom: 24px; line-height: 1.7; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .pay-layout { grid-template-columns: 1fr; }
  .pay-methods { grid-template-columns: 1fr 1fr; }
}
</style>
