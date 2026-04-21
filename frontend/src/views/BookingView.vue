<template>
  <main class="page">
    <div class="container">
      <div class="booking-header">
        <button class="back-btn" @click="$router.back()">← 뒤로</button>
        <h1 class="section-title">예약하기</h1>
        <p class="section-subtitle" v-if="stylist.name">{{ stylist.name }} · {{ stylist.salonName }}</p>
      </div>

      <!-- Step Indicator -->
      <div class="steps">
        <div v-for="(step, i) in steps" :key="i" class="step" :class="{ active: currentStep === i, done: currentStep > i }">
          <div class="step-dot">{{ currentStep > i ? '✓' : i + 1 }}</div>
          <span>{{ step }}</span>
        </div>
      </div>

      <div class="booking-body">
        <!-- Step 1: 날짜/시간 선택 -->
        <div v-if="currentStep === 0" class="card step-card">
          <h2 class="step-title">날짜 &amp; 시간 선택</h2>

          <!-- Calendar -->
          <div class="calendar">
            <div class="cal-header">
              <button @click="prevMonth">‹</button>
              <span>{{ currentYear }}년 {{ currentMonth + 1 }}월</span>
              <button @click="nextMonth">›</button>
            </div>
            <div class="cal-weekdays">
              <span v-for="d in ['일','월','화','수','목','금','토']" :key="d">{{ d }}</span>
            </div>
            <div class="cal-days">
              <span v-for="blank in startDay" :key="'b'+blank" class="cal-day empty"></span>
              <button
                v-for="day in daysInMonth"
                :key="day"
                class="cal-day"
                :class="{
                  selected: selectedDate === day,
                  today: isToday(day),
                  past: isPast(day)
                }"
                :disabled="isPast(day)"
                @click="selectedDate = day"
              >{{ day }}</button>
            </div>
          </div>

          <!-- Time Slots -->
          <div v-if="selectedDate" class="time-section">
            <h3 class="time-title">시간 선택</h3>
            <div class="time-slots">
              <button
                v-for="slot in timeSlots"
                :key="slot.time"
                class="time-slot"
                :class="{ selected: selectedTime === slot.time, booked: slot.booked }"
                :disabled="slot.booked"
                @click="!slot.booked && (selectedTime = slot.time)"
              >{{ slot.time }}</button>
            </div>
          </div>

          <div class="step-actions">
            <button
              class="btn btn-primary btn-lg"
              :disabled="!selectedDate || !selectedTime"
              @click="currentStep++"
            >다음 →</button>
          </div>
        </div>

        <!-- Step 2: 서비스 선택 -->
        <div v-if="currentStep === 1" class="card step-card">
          <h2 class="step-title">서비스 &amp; 요구사항</h2>

          <div v-if="servicesLoading" style="text-align:center;padding:40px 0">
            <div class="spinner"></div>
          </div>
          <div v-else>
            <!-- Service Select -->
            <div class="form-group" style="margin-bottom:24px">
              <label class="form-label">서비스 선택</label>
              
              <div v-for="(list, cat) in groupedServices" :key="cat" class="service-category-group">
                <h4 class="service-cat-title">{{ cat }}</h4>
                <div class="service-options">
                  <button
                    v-for="s in list"
                    :key="s.id"
                    class="service-opt"
                    :class="{ selected: selectedService?.id === s.id }"
                    @click="selectedService = s"
                  >
                    <div style="text-align:left">
                      <span class="so-name">{{ s.name }}</span>
                      <p class="so-desc" v-if="s.description">{{ s.description }}</p>
                    </div>
                    <span class="so-price">{{ s.price?.toLocaleString() }}원</span>
                  </button>
                </div>
              </div>
              
              <p v-if="!services.length" style="color:var(--color-text-muted);font-size:14px;margin-top:12px">
                등록된 서비스가 없습니다.
              </p>
            </div>

            <!-- Request Text -->
            <div class="form-group" style="margin-bottom:20px">
              <label class="form-label">요구사항 / 건의사항</label>
              <textarea
                v-model="requestText"
                class="form-input"
                rows="4"
                placeholder="원하시는 스타일, 참고사항 등을 자유롭게 작성해 주세요..."
              ></textarea>
            </div>
          </div>

          <div class="step-actions">
            <button class="btn btn-ghost" @click="currentStep--">← 이전</button>
            <button
              class="btn btn-primary btn-lg"
              :disabled="!selectedService"
              @click="currentStep++"
            >다음 →</button>
          </div>
        </div>

        <!-- Step 3: 확인 및 결제 -->
        <div v-if="currentStep === 2" class="card step-card">
          <h2 class="step-title">예약 확인 &amp; 결제</h2>

          <div class="confirm-info">
            <div class="confirm-row">
              <span class="cr-label">미용사</span>
              <span class="cr-value">{{ stylist.name }} · {{ stylist.salonName }}</span>
            </div>
            <div class="confirm-row">
              <span class="cr-label">날짜 &amp; 시간</span>
              <span class="cr-value">{{ reservedAtDisplay }}</span>
            </div>
            <div class="confirm-row">
              <span class="cr-label">서비스</span>
              <span class="cr-value">{{ selectedService?.name }}</span>
            </div>
            <div class="confirm-row">
              <span class="cr-label">요구사항</span>
              <span class="cr-value">{{ requestText || '없음' }}</span>
            </div>
            <div class="confirm-row total">
              <span class="cr-label">결제 금액</span>
              <span class="cr-value gold">{{ selectedService?.price?.toLocaleString() }}원</span>
            </div>
          </div>

          <!-- 토스 결제 위젯 영역 -->
          <div v-show="tossReady" class="toss-area" style="margin-top:24px;">
            <div id="payment-widget"></div>
            <div id="agreement-widget"></div>
          </div>

          <p v-if="bookingError" class="error-msg">{{ bookingError }}</p>

          <div class="step-actions">
            <button class="btn btn-ghost" @click="currentStep--">← 이전</button>
            <button
              class="btn btn-primary btn-lg"
              :disabled="booking"
              @click="submitReservation"
            >
              <span v-if="booking" class="spinner" style="width:18px;height:18px;border-width:2px;"></span>
              <span v-else>결제하기</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { stylistApi } from '@/api/stylist'
import { reservationApi } from '@/api/reservation'
import { paymentApi } from '@/api/payment'

const route = useRoute()
const router = useRouter()
const stylistId = route.params.stylistId

const currentStep = ref(0)
const steps = ['날짜/시간', '서비스/요구사항', '예약 확인']

const stylist = ref({})
const services = ref([])
const servicesLoading = ref(false)

const groupedServices = computed(() => {
  const groups = {}
  services.value.forEach(s => {
    const cat = s.category || '기타'
    if (!groups[cat]) groups[cat] = []
    groups[cat].push(s)
  })
  return groups
})

const selectedService = ref(null)
const selectedDate = ref(null)
const selectedTime = ref(null)
const requestText = ref('')
const booking = ref(false)
const bookingError = ref('')
const bookedTimes = ref([])

const tossReady = ref(false)
let tossPaymentsInstance = null
let paymentWidgetInstance = null

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

watch(selectedDate, async (newVal) => {
  if (!newVal) {
    bookedTimes.value = []
    selectedTime.value = null
    return
  }
  selectedTime.value = null
  const mm = String(currentMonth.value + 1).padStart(2, '0')
  const dd = String(newVal).padStart(2, '0')
  const dateStr = `${currentYear.value}-${mm}-${dd}`
  try {
    const res = await reservationApi.getBookedTimes(stylistId, dateStr)
    bookedTimes.value = res.data || []
  } catch (e) {
    console.error('예약된 시간 조회 실패', e)
    bookedTimes.value = []
  }
})

watch([currentStep, tossReady], async ([newStep, isReady]) => {
  if (newStep === 2 && isReady && window.TossPayments && selectedService.value) {
    await nextTick()
    await initTossWidget(selectedService.value.price || 0)
  }
})

async function initTossWidget(amount) {
  try {
    const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY || 'test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm'
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
  } catch (error) {
    console.error('토스 위젯 초기화 에러:', error)
    bookingError.value = '토스페이먼츠(결제모듈) 불러오기에 실패했습니다: ' + (error.message || '알 수 없는 에러')
    paymentWidgetInstance = null
  }
}

// Calendar
const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth())

const daysInMonth = computed(() =>
  new Date(currentYear.value, currentMonth.value + 1, 0).getDate()
)
const startDay = computed(() =>
  new Date(currentYear.value, currentMonth.value, 1).getDay()
)

function prevMonth() {
  if (currentMonth.value === 0) { currentYear.value--; currentMonth.value = 11 }
  else currentMonth.value--
}
function nextMonth() {
  if (currentMonth.value === 11) { currentYear.value++; currentMonth.value = 0 }
  else currentMonth.value++
}
function isToday(day) {
  const d = new Date()
  return d.getFullYear() === currentYear.value && d.getMonth() === currentMonth.value && d.getDate() === day
}
function isPast(day) {
  const d = new Date(currentYear.value, currentMonth.value, day)
  const t = new Date(); t.setHours(0, 0, 0, 0)
  return d < t
}

// 시간 슬롯 (스타일리스트 영업시간 기반 30분 단위)
const timeSlots = computed(() => {
  if (!selectedDate.value || !stylist.value.workingHours) return []
  
  const d = new Date(currentYear.value, currentMonth.value, selectedDate.value)
  const jsDay = d.getDay()
  const backendDay = jsDay === 0 ? 6 : jsDay - 1 // 0=월, 6=일

  const dailyHours = stylist.value.workingHours.find(h => h.dayOfWeek === backendDay)
  if (!dailyHours || dailyHours.isDayOff || !dailyHours.openTime || !dailyHours.closeTime) return []

  const [openH, openM] = dailyHours.openTime.split(':').map(Number)
  const [closeH, closeM] = dailyHours.closeTime.split(':').map(Number)
  
  const slots = []
  let curH = openH
  let curM = openM

  while (curH < closeH || (curH === closeH && curM < closeM)) {
    const timeStr = `${String(curH).padStart(2, '0')}:${String(curM).padStart(2, '0')}`
    slots.push({ time: timeStr, booked: bookedTimes.value.includes(timeStr) })
    curM += 30
    if (curM >= 60) {
      curH += 1
      curM -= 60
    }
  }
  return slots
})

const reservedAtDisplay = computed(() => {
  if (!selectedDate.value || !selectedTime.value) return ''
  const mm = String(currentMonth.value + 1).padStart(2, '0')
  const dd = String(selectedDate.value).padStart(2, '0')
  return `${currentYear.value}.${mm}.${dd} ${selectedTime.value}`
})

onMounted(async () => {
  servicesLoading.value = true
  try {
    const res = await stylistApi.getStylist(stylistId)
    stylist.value = res.data
    services.value = res.data.services ?? []
  } catch (e) {
    console.error('스타일리스트 정보 로드 실패', e)
  } finally {
    servicesLoading.value = false
  }

  const loaded = await loadTossScript()
  if (loaded && window.TossPayments) {
    tossReady.value = true
  }
})

async function submitReservation() {
  booking.value = true
  bookingError.value = ''
  try {
    const mm = String(currentMonth.value + 1).padStart(2, '0')
    const dd = String(selectedDate.value).padStart(2, '0')
    const reservedAt = `${currentYear.value}-${mm}-${dd}T${selectedTime.value}:00`

    const res = await reservationApi.create({
      stylistId: Number(stylistId),
      serviceId: selectedService.value.id,
      reservedAt,
      requestMemo: requestText.value || null,
    })

    const createdReservationId = res.data.id;
    if (createdReservationId) {
      if (!paymentWidgetInstance) {
        // 예약은 이미 생성(PENDING)됐으므로 즉시 취소 요청
        try { await reservationApi.cancel(createdReservationId) } catch (_) {}
        bookingError.value = '결제 시스템이 아직 준비되지 않았습니다. 잠시 후 다시 시도해주세요.'
        booking.value = false
        return
      }

      // 2. 결제 API prepare 호출하여 orderId 가져오기
      const prepareRes = await paymentApi.prepare(createdReservationId)
      const orderId = prepareRes.data.orderId

      // 3. 토스 팝업 결제창 호출
      await paymentWidgetInstance.requestPayment({
        orderId: orderId,
        orderName: selectedService.value.name,
        successUrl: `${window.location.origin}/payment/success`,
        failUrl: `${window.location.origin}/payment/fail`,
      })
      // 결제 진행되면 여기서 리다이렉트 되므로 별도 액션 없음
      return;
    } else {
      router.push('/mypage');
    }
  } catch (e) {
    bookingError.value = e.response?.data?.message || e.message || '예약 중 오류가 발생했습니다.'
    booking.value = false
  }
}
</script>

<style scoped>
.booking-header { margin-bottom: 28px; }
.back-btn { background: none; border: none; color: var(--color-text-secondary); font-size: 14px; cursor: pointer; margin-bottom: 12px; }
.back-btn:hover { color: var(--color-gold); }

.steps {
  display: flex;
  gap: 0;
  margin-bottom: 32px;
  position: relative;
}
.steps::before {
  content: '';
  position: absolute;
  top: 16px;
  left: 0;
  right: 0;
  height: 1px;
  background: var(--color-border);
}
.step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-text-muted);
  position: relative;
}
.step-dot {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: var(--color-bg-surface);
  border: 2px solid var(--color-border);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px;
  font-weight: 600;
  z-index: 1;
}
.step.active .step-dot { border-color: var(--color-gold); color: var(--color-gold); }
.step.active { color: var(--color-gold); }
.step.done .step-dot { background: var(--color-gold); border-color: var(--color-gold); color: #1a1206; }

.booking-body { max-width: 640px; margin: 0 auto; }
.step-title { font-size: 18px; font-weight: 700; margin-bottom: 24px; }

/* Calendar */
.calendar { margin-bottom: 24px; }
.cal-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; font-weight: 600;
}
.cal-header button {
  background: none; border: 1px solid var(--color-border); border-radius: var(--radius-sm);
  width: 32px; height: 32px; color: var(--color-text-primary); font-size: 18px; cursor: pointer;
}
.cal-header button:hover { border-color: var(--color-gold); color: var(--color-gold); }
.cal-weekdays {
  display: grid; grid-template-columns: repeat(7, 1fr);
  text-align: center; font-size: 12px; color: var(--color-text-muted);
  margin-bottom: 8px;
}
.cal-days { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
.cal-day {
  aspect-ratio: 1; border-radius: var(--radius-sm);
  border: 1.5px solid transparent; background: transparent;
  color: var(--color-text-primary); font-size: 14px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: var(--transition);
}
.cal-day:hover:not(.empty):not([disabled]) { border-color: var(--color-gold); color: var(--color-gold); }
.cal-day.selected { background: var(--color-gold); color: #ffffff; font-weight: 700; }
.cal-day.today:not(.selected) { border-color: rgba(201,169,110,0.4); }
.cal-day.past, .cal-day[disabled] { color: var(--color-text-muted); cursor: not-allowed; }
.cal-day.empty { pointer-events: none; }

/* Time Slots */
.time-section { margin-top: 20px; }
.time-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.time-slots { display: flex; flex-wrap: wrap; gap: 8px; }
.time-slot {
  padding: 8px 16px; border-radius: var(--radius-sm);
  border: 1.5px solid var(--color-border); background: transparent;
  color: var(--color-text-primary); font-size: 13px; cursor: pointer;
  transition: var(--transition);
}
.time-slot:hover:not(.booked) { border-color: var(--color-gold); color: var(--color-gold); }
.time-slot.selected { background: var(--color-gold); border-color: var(--color-gold); color: #ffffff; font-weight: 600; }
.time-slot.booked { background: var(--color-bg-surface); color: var(--color-text-muted); cursor: not-allowed; text-decoration: line-through; opacity: 0.5; }

/* Service options */
.service-category-group { margin-bottom: 24px; }
.service-cat-title { font-size: 14px; font-weight: 700; color: var(--color-text-secondary); margin-bottom: 12px; border-bottom: 1px solid var(--color-border); padding-bottom: 6px; }

.service-options { display: flex; flex-direction: column; gap: 8px; }
.service-opt {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; border-radius: var(--radius-md);
  border: 1.5px solid var(--color-border); background: transparent;
  cursor: pointer; transition: var(--transition);
}
.service-opt:hover { border-color: rgba(201,169,110,0.4); }
.service-opt.selected { border-color: var(--color-gold); background: rgba(201,169,110,0.08); }
.so-name { font-size: 15px; font-weight: 600; color: var(--color-text-primary); display: block; margin-bottom: 2px; }
.so-desc { font-size: 12px; color: var(--color-text-muted); }
.so-price { font-size: 15px; font-weight: 700; color: var(--color-gold); flex-shrink: 0; }

/* Confirm */
.confirm-info {
  background: var(--color-bg-surface); border-radius: var(--radius-md);
  padding: 20px; margin-bottom: 24px;
  display: flex; flex-direction: column; gap: 14px;
}
.confirm-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.cr-label { font-size: 13px; color: var(--color-text-muted); flex-shrink: 0; }
.cr-value { font-size: 14px; color: var(--color-text-primary); text-align: right; }
.confirm-row.total { padding-top: 14px; border-top: 1px solid var(--color-border); }
.cr-value.gold { font-size: 18px; font-weight: 700; color: var(--color-gold); }

.error-msg { color: var(--color-danger); font-size: 13px; text-align: center; margin-bottom: 16px; }
.step-actions { display: flex; justify-content: space-between; margin-top: 28px; gap: 12px; }
.step-actions .btn { flex: 1; }
</style>
