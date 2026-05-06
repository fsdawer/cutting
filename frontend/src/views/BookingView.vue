<template>
  <main class="page">
    <div class="container">
      <button class="back-link" @click="$router.back()">← 뒤로가기</button>

      <div class="booking-grid">

        <!-- ──────────── 메인 콘텐츠 ──────────── -->
        <div class="booking-main">

          <!-- 섹션 1: 날짜 & 시간 -->
          <section class="card booking-section" id="sec-datetime">
            <div class="section-badge">1</div>
            <h2 class="section-title">날짜 &amp; 시간 선택</h2>

            <div class="calendar-section">
              <div class="cal-header">
                <button class="cal-nav" @click="prevMonth">←</button>
                <span class="cal-month">{{ calYear }}년 {{ calMonth + 1 }}월</span>
                <button class="cal-nav" @click="nextMonth">→</button>
              </div>
              <div class="cal-grid">
                <span v-for="d in ['일','월','화','수','목','금','토']" :key="d" class="cal-dow">{{ d }}</span>
                <span v-for="blank in startBlank" :key="'b'+blank" class="cal-day empty"></span>
                <button
                  v-for="day in daysInMonth"
                  :key="day"
                  class="cal-day"
                  :class="{ selected: isSelectedDate(day), today: isTodayDate(day), disabled: isPastDate(day) }"
                  :disabled="isPastDate(day)"
                  @click="selectDate(day)"
                >{{ day }}</button>
              </div>
            </div>

            <div v-if="selectedDate" class="time-section">
              <h3 class="sub-title">시간 선택</h3>
              <div v-if="slotsLoading" class="slots-loading"><div class="spinner" style="width:22px;height:22px;border-width:2px"></div></div>
              <div v-else class="time-grid">
                <button
                  v-for="slot in timeSlots"
                  :key="slot.time"
                  class="time-slot"
                  :class="{ selected: selectedTime === slot.time, booked: slot.booked }"
                  @click="slot.booked ? openWaitingModal(slot.time) : (selectedTime = slot.time)"
                >
                  {{ slot.time }}
                  <span v-if="slot.booked" class="slot-booked-label">마감</span>
                </button>
              </div>
              <p class="slot-hint">마감된 시간을 클릭하면 빈자리 알림을 신청할 수 있습니다</p>
            </div>

            <Transition name="toast">
              <div v-if="waitingSuccess" class="waiting-toast">{{ waitingSuccess }}</div>
            </Transition>

            <div v-if="selectedDate && selectedTime" class="done-badge">
              ✓ {{ selectedDate }} {{ selectedTime }} 선택됨
            </div>
          </section>

          <!-- 섹션 2: 서비스 선택 -->
          <section class="card booking-section" id="sec-service">
            <div class="section-badge">2</div>
            <h2 class="section-title">서비스 선택</h2>

            <div v-if="services.length === 0" class="state-center">
              <p class="state-text">등록된 서비스가 없습니다.</p>
            </div>
            <div v-else class="service-list">
              <label
                v-for="svc in services"
                :key="svc.id"
                class="svc-option"
                :class="{ selected: selectedServiceId === svc.id }"
              >
                <input type="radio" :value="svc.id" v-model="selectedServiceId" class="svc-radio" />
                <div class="svc-body">
                  <div class="svc-top">
                    <span class="svc-badge">{{ svc.category }}</span>
                    <span class="svc-name">{{ svc.name }}</span>
                  </div>
                  <p class="svc-desc">{{ svc.description }}</p>
                  <div class="svc-meta">
                    <span class="svc-duration">{{ svc.durationMinutes }}분</span>
                    <span class="svc-price">{{ svc.price?.toLocaleString() }}원</span>
                  </div>
                </div>
              </label>
            </div>

            <div class="form-group" style="margin-top:20px">
              <label class="form-label">요구사항 (선택)</label>
              <textarea v-model="requestMemo" class="form-input" rows="3" placeholder="원하는 스타일, 특이사항 등을 입력해주세요"></textarea>
            </div>
          </section>

          <!-- 섹션 3: 결제 -->
          <section class="card booking-section" id="sec-payment">
            <div class="section-badge">3</div>
            <h2 class="section-title">결제</h2>

            <div v-if="!selectedDate || !selectedTime || !selectedServiceId" class="payment-placeholder">
              <p>날짜, 시간, 서비스를 모두 선택하면<br>결제 수단이 표시됩니다</p>
            </div>

            <template v-else>
              <!-- 예약 요약 -->
              <div class="confirm-rows">
                <div class="confirm-row">
                  <span class="confirm-label">스타일리스트</span>
                  <span class="confirm-val">{{ stylist?.name }}</span>
                </div>
                <div class="confirm-row">
                  <span class="confirm-label">날짜 / 시간</span>
                  <span class="confirm-val">{{ selectedDate }} {{ selectedTime }}</span>
                </div>
                <div class="confirm-row">
                  <span class="confirm-label">서비스</span>
                  <span class="confirm-val">{{ selectedService?.name }}</span>
                </div>
                <div class="confirm-row total">
                  <span class="confirm-label">결제 금액</span>
                  <span class="confirm-val price">{{ selectedService?.price?.toLocaleString() }}원</span>
                </div>
              </div>

              <!-- 토스 결제 위젯 -->
              <div class="widget-wrap">
                <div id="payment-widget"></div>
                <div id="payment-agreement"></div>
              </div>

              <p v-if="widgetError" class="err-msg">{{ widgetError }}</p>
              <p v-if="payError" class="err-msg">{{ payError }}</p>

              <button
                class="btn btn-primary btn-full pay-btn"
                :disabled="paying || !widgetReady"
                @click="handlePay"
              >
                <span v-if="paying" class="spinner" style="width:16px;height:16px;border-width:2px"></span>
                <span v-else>{{ selectedService?.price?.toLocaleString() }}원 결제하기</span>
              </button>
            </template>
          </section>

        </div><!-- /booking-main -->

        <!-- ──────────── 사이드바 ──────────── -->
        <aside class="booking-sidebar">
          <div class="card stylist-card" v-if="stylist">
            <img :src="stylist.profileImg || `https://i.pravatar.cc/80?u=${stylist.id}`" class="sidebar-img" />
            <div class="sidebar-info">
              <p class="sidebar-name">{{ stylist.name }}</p>
              <p class="sidebar-salon">{{ stylist.salonName }}</p>
              <div class="sidebar-rating"><span class="star-icon">★</span>{{ stylist.rating?.toFixed(1) }}</div>
            </div>
          </div>

          <div class="card selection-summary" v-if="selectedDate || selectedServiceId">
            <h3 class="summary-title">선택 내역</h3>
            <div v-if="selectedDate && selectedTime" class="summary-row">
              <span class="summary-label">일시</span>
              <span class="summary-val">{{ selectedDate }}<br>{{ selectedTime }}</span>
            </div>
            <div v-if="selectedService" class="summary-row">
              <span class="summary-label">서비스</span>
              <span class="summary-val">{{ selectedService.name }}</span>
            </div>
            <div v-if="selectedService" class="summary-row price-row">
              <span class="summary-label">금액</span>
              <span class="summary-val price">{{ selectedService.price?.toLocaleString() }}원</span>
            </div>
          </div>
        </aside>

      </div><!-- /booking-grid -->
    </div>
  </main>

  <!-- 빈자리 알림 모달 -->
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="waitingModal.show" class="modal-overlay" @click.self="waitingModal.show = false">
        <div class="modal-box">
          <div class="modal-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
          </div>
          <h3 class="modal-title">빈자리 알림 신청</h3>
          <p class="modal-desc">
            <strong>{{ selectedDate }} {{ waitingModal.time }}</strong> 시간대는 이미 예약이 차있습니다.<br>
            해당 예약이 취소되면 알림을 보내드릴까요?
          </p>
          <p v-if="waitingModal.error" class="modal-error">{{ waitingModal.error }}</p>
          <div class="modal-actions">
            <button class="btn btn-ghost" @click="waitingModal.show = false">취소</button>
            <button class="btn btn-primary" :disabled="waitingModal.loading" @click="registerWaiting">
              <span v-if="waitingModal.loading" class="spinner" style="width:14px;height:14px;border-width:2px;margin-right:6px"></span>
              알림 신청
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { stylistApi } from '@/api/stylist'
import { reservationApi, waitingApi } from '@/api/reservation'
import { paymentApi } from '@/api/payment'
import { useAuthStore } from '@/stores/authStore'

const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()

const stylist  = ref(null)
const services = ref([])

const calYear  = ref(new Date().getFullYear())
const calMonth = ref(new Date().getMonth())

const selectedDate      = ref('')
const selectedTime      = ref('')
const selectedServiceId = ref(route.query.serviceId ? Number(route.query.serviceId) : null)
const requestMemo       = ref('')
const slotsLoading      = ref(false)
const bookedTimes       = ref([])
const paying            = ref(false)
const payError          = ref('')
const widgetError       = ref('')
const widgetReady       = ref(false)
const waitingModal      = ref({ show: false, time: '', loading: false, error: '' })
const waitingSuccess    = ref('')

let paymentWidget        = null
let paymentMethodsWidget = null

const selectedService = computed(() => services.value.find(s => s.id === selectedServiceId.value))

const daysInMonth = computed(() => new Date(calYear.value, calMonth.value + 1, 0).getDate())
const startBlank  = computed(() => new Date(calYear.value, calMonth.value, 1).getDay())

const timeSlots = computed(() => {
  const slots = []
  for (let h = 9; h <= 19; h++) {
    const time = `${String(h).padStart(2,'0')}:00`
    slots.push({ time, booked: bookedTimes.value.includes(time) })
  }
  return slots
})

function prevMonth() {
  if (calMonth.value === 0) { calYear.value--; calMonth.value = 11 } else calMonth.value--
}
function nextMonth() {
  if (calMonth.value === 11) { calYear.value++; calMonth.value = 0 } else calMonth.value++
}
function isSelectedDate(day) {
  return selectedDate.value === `${calYear.value}-${String(calMonth.value+1).padStart(2,'0')}-${String(day).padStart(2,'0')}`
}
function isTodayDate(day) {
  const t = new Date()
  return calYear.value === t.getFullYear() && calMonth.value === t.getMonth() && day === t.getDate()
}
function isPastDate(day) {
  const d = new Date(calYear.value, calMonth.value, day)
  const t = new Date(); t.setHours(0,0,0,0)
  return d < t
}

async function selectDate(day) {
  if (isPastDate(day)) return
  selectedDate.value = `${calYear.value}-${String(calMonth.value+1).padStart(2,'0')}-${String(day).padStart(2,'0')}`
  selectedTime.value = ''
  slotsLoading.value = true
  try {
    const res = await reservationApi.getBookedTimes(route.params.stylistId, selectedDate.value)
    bookedTimes.value = res.data || []
  } catch { bookedTimes.value = [] }
  finally { slotsLoading.value = false }
}

function openWaitingModal(time) {
  if (!auth.isLoggedIn) { router.push('/login'); return }
  waitingModal.value = { show: true, time, loading: false, error: '' }
}

async function registerWaiting() {
  waitingModal.value.loading = true
  waitingModal.value.error = ''
  try {
    await waitingApi.register(route.params.stylistId, selectedDate.value, waitingModal.value.time)
    const t = waitingModal.value.time
    waitingModal.value.show = false
    waitingSuccess.value = `${selectedDate.value} ${t} 빈자리 알림이 신청되었습니다.`
    setTimeout(() => { waitingSuccess.value = '' }, 4000)
  } catch (e) {
    waitingModal.value.error = e.response?.data?.message || '신청 중 오류가 발생했습니다.'
  } finally {
    waitingModal.value.loading = false
  }
}

// ── Toss 위젯 ──────────────────────────────────────────────────────────────

async function initWidget(price) {
  widgetError.value = ''
  widgetReady.value = false

  if (!window.PaymentWidget) {
    widgetError.value = '결제 모듈을 불러올 수 없습니다. 페이지를 새로고침해주세요.'
    return
  }
  if (!price || price <= 0) return

  try {
    // 위젯이 이미 있으면 컨테이너를 비우고 재생성
    const container = document.getElementById('payment-widget')
    if (container) container.innerHTML = ''
    const agreeContainer = document.getElementById('payment-agreement')
    if (agreeContainer) agreeContainer.innerHTML = ''

    paymentWidget = window.PaymentWidget(
      import.meta.env.VITE_TOSS_CLIENT_KEY,
      window.PaymentWidget.ANONYMOUS,
    )
    paymentMethodsWidget = await paymentWidget.renderPaymentMethods(
      '#payment-widget',
      { value: price },
    )
    try {
      await paymentWidget.renderAgreement('#payment-agreement')
    } catch { /* 약관 위젯 미지원 무시 */ }
    widgetReady.value = true
  } catch (e) {
    console.error('[Toss Widget]', e)
    widgetError.value = `결제 위젯 오류: ${e.message}`
  }
}

// 날짜·시간·서비스 중 하나라도 바뀌면 결제 위젯 초기화/갱신
watch([selectedDate, selectedTime, selectedServiceId], async ([date, time, serviceId]) => {
  if (!date || !time || !serviceId) return
  const svc = services.value.find(s => s.id === serviceId)
  if (!svc) return

  await nextTick() // #payment-widget 이 DOM에 렌더링될 때까지 대기

  if (paymentMethodsWidget) {
    try { await paymentMethodsWidget.updateAmount(svc.price); return } catch {}
  }
  await initWidget(svc.price)
})

// ── 결제 처리 ───────────────────────────────────────────────────────────────

async function handlePay() {
  payError.value = ''
  if (!selectedDate.value || !selectedTime.value) { payError.value = '날짜와 시간을 선택해주세요.'; return }
  if (!selectedServiceId.value) { payError.value = '서비스를 선택해주세요.'; return }
  if (!paymentWidget) { payError.value = '결제 위젯이 준비되지 않았습니다. 잠시 후 다시 시도해주세요.'; return }

  paying.value = true
  try {
    // 1. 예약 생성 (PENDING)
    const reservedAt = `${selectedDate.value}T${selectedTime.value}:00`
    const resRes = await reservationApi.create({
      stylistId: Number(route.params.stylistId),
      serviceId: selectedServiceId.value,
      reservedAt,
      requestMemo: requestMemo.value,
    })
    const reservationId = resRes.data.id

    // 2. 결제 준비 (orderId 발급)
    const prepRes = await paymentApi.prepare({ reservationId, method: 'TOSS' })
    const { orderId } = prepRes.data

    // 3. 토스 결제창 (리다이렉트)
    await paymentWidget.requestPayment({
      orderId,
      orderName: selectedService.value?.name,
      successUrl: `${window.location.origin}/payment/success`,
      failUrl:    `${window.location.origin}/payment/fail`,
    })
    // requestPayment 후 리다이렉트되므로 아래 코드는 실행되지 않음
  } catch (e) {
    if (e?.code === 'PAY_PROCESS_CANCELED' || e?.code === 'USER_CANCEL') {
      // 사용자가 결제창을 닫음 → PENDING 예약은 10분 후 스케줄러가 정리
      paying.value = false
      return
    }
    payError.value = e.response?.data?.message || e.message || '결제 처리 중 오류가 발생했습니다.'
    paying.value = false
  }
}

// ── 초기화 ──────────────────────────────────────────────────────────────────

onMounted(async () => {
  const id = route.params.stylistId
  try {
    const res = await stylistApi.getStylist(id)
    stylist.value  = res.data
    services.value = res.data.services || []
    if (route.query.time) selectedTime.value = route.query.time

    // 쿼리로 서비스가 미리 선택된 경우 위젯 즉시 초기화
    if (selectedServiceId.value) {
      await nextTick()
      const svc = services.value.find(s => s.id === selectedServiceId.value)
      if (svc) await initWidget(svc.price)
    }
  } catch {}
})
</script>

<style scoped>
.back-link {
  display: inline-block; margin-bottom: 24px;
  font-size: 13px; color: var(--text-muted); background: none; border: none; cursor: pointer; padding: 0;
}
.back-link:hover { color: var(--text); }

/* Grid */
.booking-grid {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
  align-items: start;
}
.booking-main { display: flex; flex-direction: column; gap: 20px; min-width: 0; }

/* Section cards */
.booking-section { position: relative; padding-top: 28px; }
.section-badge {
  position: absolute; top: -14px; left: 20px;
  width: 28px; height: 28px; border-radius: 50%;
  background: var(--primary); color: #fff;
  font-size: 12px; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
}
.section-title { font-size: 18px; font-weight: 700; margin-bottom: 20px; letter-spacing: -0.02em; }
.sub-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: var(--text-sub); }

.done-badge {
  margin-top: 16px; padding: 10px 14px;
  background: var(--success-light); border-radius: var(--radius-sm);
  font-size: 13px; font-weight: 600; color: var(--success);
}

/* Calendar */
.cal-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.cal-month { font-size: 15px; font-weight: 700; }
.cal-nav { background: none; border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 5px 11px; font-size: 13px; cursor: pointer; transition: var(--transition); }
.cal-nav:hover { background: var(--bg-surface); }
.cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
.cal-dow { text-align: center; font-size: 12px; font-weight: 600; color: var(--text-muted); padding: 6px 0; }
.cal-day {
  aspect-ratio: 1; display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-sm); font-size: 13px; font-weight: 500;
  background: transparent; border: 1px solid transparent; cursor: pointer; transition: var(--transition);
}
.cal-day:hover:not(.disabled):not(.empty) { background: var(--bg-surface); border-color: var(--border); }
.cal-day.today { font-weight: 800; color: var(--primary); }
.cal-day.selected { background: var(--primary); color: #fff; border-color: var(--primary); }
.cal-day.disabled { color: var(--text-muted); cursor: not-allowed; opacity: 0.4; }
.cal-day.empty { pointer-events: none; }

/* Time slots */
.time-section { margin-top: 20px; border-top: 1px solid var(--border); padding-top: 18px; }
.slots-loading { padding: 16px 0; display: flex; justify-content: center; }
.time-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.time-slot {
  padding: 9px 0; text-align: center; border-radius: var(--radius-sm);
  border: 1.5px solid var(--border); background: var(--bg-card);
  font-size: 13px; font-weight: 600; cursor: pointer; transition: var(--transition);
  position: relative;
}
.time-slot:hover:not(.booked) { border-color: var(--primary); color: var(--primary); }
.time-slot.selected { background: var(--primary); color: #fff; border-color: var(--primary); }
.time-slot.booked { background: var(--bg-surface); color: var(--text-muted); cursor: pointer; }
.time-slot.booked:hover { border-color: var(--accent); color: var(--accent); }
.slot-booked-label { display: block; font-size: 9px; font-weight: 700; color: var(--danger); margin-top: 1px; }
.slot-hint { font-size: 12px; color: var(--text-muted); margin-top: 10px; padding: 8px 12px; background: var(--bg-surface); border-radius: var(--radius-sm); }

/* Service list */
.service-list { display: flex; flex-direction: column; gap: 8px; }
.svc-option { display: flex; gap: 14px; align-items: flex-start; padding: 14px 16px; border-radius: var(--radius-md); border: 1.5px solid var(--border); cursor: pointer; transition: var(--transition); }
.svc-option:hover { border-color: var(--border-strong); }
.svc-option.selected { border-color: var(--primary); background: rgba(24,24,27,0.02); }
.svc-radio { margin-top: 3px; accent-color: var(--primary); flex-shrink: 0; }
.svc-body { flex: 1; }
.svc-top { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.svc-badge { font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: var(--radius-full); background: var(--accent-light); color: var(--accent); }
.svc-name { font-size: 15px; font-weight: 700; }
.svc-desc { font-size: 13px; color: var(--text-muted); margin-bottom: 8px; }
.svc-meta { display: flex; gap: 12px; font-size: 13px; }
.svc-duration { color: var(--text-muted); }
.svc-price { font-weight: 700; color: var(--text); }

/* Form */
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; font-weight: 600; color: var(--text-sub); }
.form-input { padding: 10px 14px; border: 1.5px solid var(--border); border-radius: var(--radius-md); font-size: 14px; background: var(--bg); color: var(--text); resize: vertical; transition: var(--transition); }
.form-input:focus { border-color: var(--primary); background: #fff; }

/* Payment section */
.payment-placeholder {
  padding: 48px 0; text-align: center;
  color: var(--text-muted); font-size: 14px; line-height: 1.8;
  background: var(--bg-surface); border-radius: var(--radius-md);
}

.confirm-rows { display: flex; flex-direction: column; gap: 2px; margin-bottom: 20px; }
.confirm-row { display: flex; justify-content: space-between; align-items: center; padding: 11px 14px; border-radius: var(--radius-sm); background: var(--bg-surface); }
.confirm-row.total { background: var(--primary); border-radius: var(--radius-md); margin-top: 4px; }
.confirm-label { font-size: 13px; color: var(--text-muted); font-weight: 500; }
.confirm-val { font-size: 14px; font-weight: 600; color: var(--text); }
.confirm-row.total .confirm-label { color: rgba(255,255,255,0.75); }
.confirm-row.total .confirm-val { color: #fff; }
.confirm-val.price { font-size: 17px; font-weight: 800; }

.widget-wrap { margin: 16px -24px 0; }
#payment-widget { min-height: 50px; }
#payment-agreement { }

.err-msg { color: var(--danger); font-size: 13px; margin-top: 8px; padding: 10px 14px; background: var(--danger-light); border-radius: var(--radius-sm); }
.pay-btn { margin-top: 16px; padding: 15px; font-size: 15px; }
.pay-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* Sidebar */
.booking-sidebar { position: sticky; top: 76px; display: flex; flex-direction: column; gap: 16px; }
.stylist-card { display: flex; gap: 14px; align-items: center; }
.sidebar-img { width: 52px; height: 52px; border-radius: var(--radius-md); object-fit: cover; flex-shrink: 0; }
.sidebar-name { font-size: 15px; font-weight: 700; }
.sidebar-salon { font-size: 12px; color: var(--text-muted); margin: 2px 0 6px; }
.sidebar-rating { display: flex; align-items: center; gap: 4px; font-size: 13px; font-weight: 600; }
.star-icon { color: var(--gold); }

.selection-summary { display: flex; flex-direction: column; gap: 0; }
.summary-title { font-size: 13px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.06em; margin-bottom: 12px; }
.summary-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; padding: 8px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.summary-row:last-child { border-bottom: none; }
.summary-label { color: var(--text-muted); flex-shrink: 0; }
.summary-val { font-weight: 600; color: var(--text); text-align: right; line-height: 1.5; }
.price-row .summary-val.price { font-size: 16px; font-weight: 800; color: var(--primary); }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 20px; }
.modal-box { background: #fff; border-radius: var(--radius-lg); padding: 32px 28px; max-width: 400px; width: 100%; box-shadow: 0 20px 60px rgba(0,0,0,0.2); text-align: center; }
.modal-icon { width: 52px; height: 52px; border-radius: 50%; background: rgba(99,102,241,0.1); color: var(--accent); display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; }
.modal-title { font-size: 18px; font-weight: 800; margin-bottom: 10px; }
.modal-desc { font-size: 14px; color: var(--text-sub); line-height: 1.7; margin-bottom: 24px; }
.modal-desc strong { color: var(--text); }
.modal-error { font-size: 13px; color: var(--danger); margin-bottom: 12px; }
.modal-actions { display: flex; gap: 8px; justify-content: center; }
.modal-actions .btn { min-width: 100px; }

/* Toast */
.waiting-toast { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); background: var(--primary); color: #fff; padding: 12px 24px; border-radius: var(--radius-full); font-size: 13px; font-weight: 600; box-shadow: 0 4px 16px rgba(0,0,0,0.2); z-index: 2000; white-space: nowrap; }
.toast-enter-active, .toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(12px); }

/* Modal animation */
.modal-enter-active, .modal-leave-active { transition: all 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal-box, .modal-leave-to .modal-box { transform: scale(0.95); }

@media (max-width: 768px) {
  .booking-grid { grid-template-columns: 1fr; }
  .booking-sidebar { position: static; order: -1; }
  .time-grid { grid-template-columns: repeat(4, 1fr); }
  .widget-wrap { margin: 0 -16px; }
}
</style>
