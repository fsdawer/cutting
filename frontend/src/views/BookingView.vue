<template>
  <main class="page">
    <div class="container">
      <!-- Header -->
      <div class="page-header">
        <button class="back-btn" @click="$router.back()">
          <span>←</span> 뒤로가기
        </button>
        <div>
          <h1 class="section-title">예약하기</h1>
          <p class="section-subtitle" v-if="stylist.name">{{ stylist.name }} · {{ stylist.salonName }}</p>
        </div>
      </div>

      <!-- Step Indicator -->
      <div class="steps">
        <div v-for="(step, i) in steps" :key="i" class="step-item" :class="{ active: currentStep === i, done: currentStep > i }">
          <div class="step-dot">
            <span v-if="currentStep > i">✓</span>
            <span v-else>{{ i + 1 }}</span>
          </div>
          <span class="step-label">{{ step }}</span>
          <div v-if="i < steps.length - 1" class="step-line"></div>
        </div>
      </div>

      <div class="booking-body">
        <!-- Step 1: 날짜/시간 -->
        <div v-if="currentStep === 0" class="card step-card">
          <h2 class="step-title">날짜 &amp; 시간 선택</h2>

          <div v-if="!stylist.workingHours?.length" class="no-hours-notice">
            이 미용사는 아직 영업시간을 설정하지 않았습니다. 예약이 불가능합니다.
          </div>

          <div class="calendar">
            <div class="cal-nav">
              <button class="cal-nav-btn" @click="prevMonth">‹</button>
              <span class="cal-month">{{ currentYear }}년 {{ currentMonth + 1 }}월</span>
              <button class="cal-nav-btn" @click="nextMonth">›</button>
            </div>
            <div class="cal-head">
              <span v-for="d in ['일','월','화','수','목','금','토']" :key="d">{{ d }}</span>
            </div>
            <div class="cal-grid">
              <span v-for="b in startDay" :key="'b'+b" class="cal-empty"></span>
              <button
                v-for="day in daysInMonth"
                :key="day"
                class="cal-day"
                :class="{
                  selected: selectedDate === day,
                  today: isToday(day),
                  past: isPast(day),
                  closed: !isPast(day) && isClosedDay(day)
                }"
                :disabled="isPast(day) || isClosedDay(day)"
                @click="selectedDate = day"
              >{{ day }}</button>
            </div>
          </div>

          <div v-if="selectedDate" class="time-section">
            <h3 class="time-title">시간 선택</h3>
            <div v-if="timeSlots.length === 0" class="no-slots">해당 날짜는 영업하지 않습니다.</div>
            <div v-else class="time-grid">
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

          <div class="step-footer">
            <button
              class="btn btn-primary btn-lg btn-full"
              :disabled="!selectedDate || !selectedTime"
              @click="currentStep++"
            >다음 단계</button>
          </div>
        </div>

        <!-- Step 2: 서비스 -->
        <div v-if="currentStep === 1" class="card step-card">
          <h2 class="step-title">서비스 선택</h2>

          <div v-if="servicesLoading" class="loading-center">
            <div class="spinner"></div>
          </div>
          <div v-else>
            <div v-for="(list, cat) in groupedServices" :key="cat" class="service-group">
              <h4 class="service-cat">{{ cat }}</h4>
              <div class="service-list">
                <button
                  v-for="s in list"
                  :key="s.id"
                  class="service-item"
                  :class="{ selected: selectedService?.id === s.id }"
                  @click="selectedService = s"
                >
                  <div class="si-info">
                    <span class="si-name">{{ s.name }}</span>
                    <span class="si-desc" v-if="s.description">{{ s.description }}</span>
                  </div>
                  <span class="si-price">{{ s.price?.toLocaleString() }}원</span>
                </button>
              </div>
            </div>
            <p v-if="!services.length" class="no-service">등록된 서비스가 없습니다.</p>

            <div class="form-group" style="margin-top: 20px;">
              <label class="form-label">요구사항 (선택)</label>
              <textarea
                v-model="requestText"
                class="form-input"
                rows="3"
                placeholder="원하시는 스타일, 참고사항 등을 적어주세요"
              ></textarea>
            </div>

            <div class="form-group" style="margin-top: 16px;">
              <label class="form-label">참고 이미지 (선택, 최대 5장)</label>
              <label class="image-upload-label">
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  style="display:none"
                  @change="handleImageChange"
                />
                <span class="upload-btn">📎 이미지 첨부</span>
              </label>
              <div v-if="imageFiles.length" class="image-preview-list">
                <div v-for="(file, idx) in imageFiles" :key="idx" class="image-preview-item">
                  <img :src="imagePreviewUrls[idx]" class="preview-img" />
                  <button type="button" class="remove-img-btn" @click="removeImage(idx)">✕</button>
                </div>
              </div>
              <p class="upload-hint">미용사에게 전달할 스타일 참고 사진을 첨부하세요</p>
            </div>
          </div>

          <div class="step-footer two-btn">
            <button class="btn btn-ghost btn-lg" @click="currentStep--">이전</button>
            <button
              class="btn btn-primary btn-lg"
              :disabled="!selectedService"
              @click="currentStep++"
            >다음 단계</button>
          </div>
        </div>

        <!-- Step 3: 확인 & 결제 -->
        <div v-if="currentStep === 2" class="card step-card">
          <h2 class="step-title">예약 확인 &amp; 결제</h2>

          <div class="confirm-box">
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
            <div class="confirm-row" v-if="requestText">
              <span class="cr-label">요구사항</span>
              <span class="cr-value">{{ requestText }}</span>
            </div>
            <div class="confirm-row total-row">
              <span class="cr-label">결제 금액</span>
              <span class="cr-value cr-price">{{ selectedService?.price?.toLocaleString() }}원</span>
            </div>
          </div>

          <!-- 결제 수단 선택 -->
          <div class="method-section">
            <h3 class="method-title">결제 수단</h3>
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

          <p v-if="bookingError" class="msg-error">{{ bookingError }}</p>

          <div class="step-footer two-btn">
            <button class="btn btn-ghost btn-lg" @click="currentStep--">이전</button>
            <button
              class="btn btn-primary btn-lg"
              :disabled="booking"
              @click="submitReservation"
            >
              <span v-if="booking" class="spinner" style="width:18px;height:18px;border-width:2px;"></span>
              <span v-else>{{ selectedService?.price?.toLocaleString() }}원 결제하기</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { stylistApi } from '@/api/stylist'
import { reservationApi } from '@/api/reservation'
import { paymentApi } from '@/api/payment'
import { useAuthStore } from '@/stores/authStore'

const route     = useRoute()
const router    = useRouter()
const authStore = useAuthStore()
const stylistId = route.params.stylistId

const currentStep = ref(0)
const steps = ['날짜/시간', '서비스', '결제']

const stylist          = ref({})
const services         = ref([])
const servicesLoading  = ref(false)
const selectedService  = ref(null)
const selectedDate     = ref(null)
const selectedTime     = ref(null)
const requestText      = ref('')
const booking               = ref(false)
const bookingError          = ref('')
const bookedTimes           = ref([])
const pendingReservationId  = ref(null)  // 결제 도중 생성된 예약 ID (취소 시 정리용)

const imageFiles       = ref([])
const imagePreviewUrls = ref([])

function handleImageChange(e) {
  const selected = Array.from(e.target.files)
  const remaining = 5 - imageFiles.value.length
  const toAdd = selected.slice(0, remaining)
  toAdd.forEach(file => {
    imageFiles.value.push(file)
    imagePreviewUrls.value.push(URL.createObjectURL(file))
  })
  e.target.value = ''
}

function removeImage(idx) {
  URL.revokeObjectURL(imagePreviewUrls.value[idx])
  imageFiles.value.splice(idx, 1)
  imagePreviewUrls.value.splice(idx, 1)
}

const payMethod  = ref('카드')
const payMethods = [
  { value: '카드',    label: '신용/체크카드', icon: '💳' },
  { value: '토스페이', label: '토스페이',     icon: '💸' },
  { value: '계좌이체', label: '계좌이체',     icon: '🏦' },
]

const groupedServices = computed(() => {
  const g = {}
  services.value.forEach(s => {
    const c = s.category || '기타'
    if (!g[c]) g[c] = []
    g[c].push(s)
  })
  return g
})

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

// Calendar
const today        = new Date()
const currentYear  = ref(today.getFullYear())
const currentMonth = ref(today.getMonth())

const daysInMonth = computed(() => new Date(currentYear.value, currentMonth.value + 1, 0).getDate())
const startDay    = computed(() => new Date(currentYear.value, currentMonth.value, 1).getDay())

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
function isClosedDay(day) {
  if (!stylist.value.workingHours?.length) return true  // 영업시간 미설정 → 모든 날 비활성화
  const d = new Date(currentYear.value, currentMonth.value, day)
  const jsDay = d.getDay()
  const backendDay = jsDay === 0 ? 6 : jsDay - 1
  const h = stylist.value.workingHours.find(h => h.dayOfWeek === backendDay)
  return !h || h.isDayOff || !h.openTime || !h.closeTime
}

const timeSlots = computed(() => {
  if (!selectedDate.value || !stylist.value.workingHours) return []
  const d = new Date(currentYear.value, currentMonth.value, selectedDate.value)
  const jsDay = d.getDay()
  const backendDay = jsDay === 0 ? 6 : jsDay - 1

  const h = stylist.value.workingHours.find(h => h.dayOfWeek === backendDay)
  if (!h || h.isDayOff || !h.openTime || !h.closeTime) return []

  const [oH, oM] = h.openTime.split(':').map(Number)
  const [cH, cM] = h.closeTime.split(':').map(Number)
  const slots = []
  let curH = oH, curM = oM

  while (curH < cH || (curH === cH && curM < cM)) {
    const t = `${String(curH).padStart(2,'0')}:${String(curM).padStart(2,'0')}`
    slots.push({ time: t, booked: bookedTimes.value.includes(t) })
    curM += 30
    if (curM >= 60) { curH++; curM -= 60 }
  }
  return slots
})

const reservedAtDisplay = computed(() => {
  if (!selectedDate.value || !selectedTime.value) return ''
  const mm = String(currentMonth.value + 1).padStart(2,'0')
  const dd = String(selectedDate.value).padStart(2,'0')
  return `${currentYear.value}.${mm}.${dd} ${selectedTime.value}`
})

watch(selectedDate, async (val) => {
  selectedTime.value = null
  bookedTimes.value = []
  if (!val) return
  const mm = String(currentMonth.value + 1).padStart(2,'0')
  const dd = String(val).padStart(2,'0')
  try {
    const res = await reservationApi.getBookedTimes(stylistId, `${currentYear.value}-${mm}-${dd}`)
    bookedTimes.value = res.data || []
  } catch (e) {
    console.error('예약 시간 조회 실패', e)
  }
})

onMounted(async () => {
  servicesLoading.value = true
  try {
    const res = await stylistApi.getStylist(stylistId)
    stylist.value  = res.data
    services.value = res.data.services ?? []
  } catch (e) {
    console.error('스타일리스트 로드 실패', e)
  } finally {
    servicesLoading.value = false
  }
  await loadTossScript()
})

async function submitReservation() {
  booking.value      = true
  bookingError.value = ''
  try {
    const mm = String(currentMonth.value + 1).padStart(2,'0')
    const dd = String(selectedDate.value).padStart(2,'0')
    const reservedAt = `${currentYear.value}-${mm}-${dd}T${selectedTime.value}:00`

    // 1. 예약 생성
    const res = await reservationApi.create({
      stylistId: Number(stylistId),
      serviceId: selectedService.value.id,
      reservedAt,
      requestMemo: requestText.value || null,
    })
    const createdId = res.data.id
    pendingReservationId.value = createdId  // catch에서 접근 가능하도록 저장

    // 2. 이미지 업로드 (선택)
    if (imageFiles.value.length > 0) {
      const formData = new FormData()
      imageFiles.value.forEach(f => formData.append('images', f))
      await reservationApi.uploadImages(createdId, formData)
    }

    // 3. 결제 준비
    const prepareRes = await paymentApi.prepare(createdId)
    const orderId    = prepareRes.data.orderId

    // 3. Toss v1 결제창 호출
    if (!window.TossPayments) {
      try { await reservationApi.cancel(createdId) } catch (_) {}
      bookingError.value = '결제 모듈을 불러오지 못했습니다. 잠시 후 다시 시도하세요.'
      return
    }
    const toss = window.TossPayments(import.meta.env.VITE_TOSS_CLIENT_KEY || 'test_ck_placeholder')
    await toss.requestPayment(payMethod.value, {
      amount:       selectedService.value.price,
      orderId,
      orderName:    selectedService.value.name,
      customerName: authStore.user?.name || '고객',
      successUrl:   `${window.location.origin}/payment/success`,
      failUrl:      `${window.location.origin}/payment/fail`,
    })
    // 리다이렉트 발생 — 아래 코드는 실행되지 않음
  } catch (e) {
    if (e.code === 'USER_CANCEL') {
      // 팝업 취소 → 생성된 PENDING 예약을 즉시 취소해서 슬롯 해방
      if (pendingReservationId.value) {
        try { await reservationApi.cancel(pendingReservationId.value) } catch (_) {}
        pendingReservationId.value = null
      }
      bookingError.value = '결제가 취소되었습니다. 다시 시도해주세요.'
    } else {
      // 예약 생성 이후 다른 오류 발생 시에도 슬롯 정리
      if (pendingReservationId.value) {
        try { await reservationApi.cancel(pendingReservationId.value) } catch (_) {}
        pendingReservationId.value = null
      }
      bookingError.value = e.response?.data?.message || e.message || '결제 중 오류가 발생했습니다.'
    }
  } finally {
    booking.value = false
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 24px; }
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: var(--text-sub);
  font-size: 14px;
  cursor: pointer;
  padding: 6px 0;
  margin-bottom: 12px;
  transition: var(--transition);
}
.back-btn:hover { color: var(--primary); }

/* Steps */
.steps {
  display: flex;
  align-items: center;
  margin-bottom: 28px;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
}
.step-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}
.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid var(--border);
  background: #fff;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: var(--transition);
}
.step-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  white-space: nowrap;
}
.step-line {
  flex: 1;
  height: 1px;
  background: var(--border);
  margin: 0 8px;
}
.step-item.active .step-dot { border-color: var(--primary); color: var(--primary); }
.step-item.active .step-label { color: var(--primary); font-weight: 700; }
.step-item.done .step-dot { background: var(--primary); border-color: var(--primary); color: #fff; }
.step-item.done .step-label { color: var(--text-sub); }

/* Step Card */
.booking-body { max-width: 640px; margin: 0 auto; }
.step-card { padding: 28px; }
.step-title { font-size: 18px; font-weight: 700; margin-bottom: 24px; }

/* Calendar */
.calendar { margin-bottom: 24px; }
.cal-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.cal-month { font-size: 16px; font-weight: 700; }
.cal-nav-btn {
  width: 32px; height: 32px;
  border: 1px solid var(--border);
  background: #fff;
  border-radius: var(--radius-sm);
  font-size: 18px;
  color: var(--text-sub);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: var(--transition);
}
.cal-nav-btn:hover { border-color: var(--primary); color: var(--primary); }
.cal-head {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 8px;
}
.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 3px;
}
.cal-empty { pointer-events: none; }
.cal-day {
  aspect-ratio: 1;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: var(--transition);
}
.cal-day:hover:not(:disabled) { background: var(--primary-light); color: var(--primary); }
.cal-day.selected { background: var(--primary); color: #fff; font-weight: 700; }
.cal-day.today:not(.selected) { font-weight: 700; color: var(--primary); }
.cal-day:disabled { color: var(--text-muted); cursor: not-allowed; }
.cal-day.closed { background: #f5f5f5; color: var(--text-muted); text-decoration: line-through; }

/* Time Slots */
.time-section { border-top: 1px solid var(--border); padding-top: 20px; }
.time-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.no-slots { color: var(--text-muted); font-size: 14px; }
.time-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.time-slot {
  padding: 8px 14px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-full);
  background: #fff;
  font-size: 13px;
  font-weight: 500;
  color: var(--text);
  cursor: pointer;
  transition: var(--transition);
}
.time-slot:hover:not(.booked) { border-color: var(--primary); color: var(--primary); }
.time-slot.selected { background: var(--primary); border-color: var(--primary); color: #fff; }
.time-slot.booked { background: var(--bg); color: var(--text-muted); cursor: not-allowed; text-decoration: line-through; }

/* Services */
.service-group { margin-bottom: 24px; }
.service-cat {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-sub);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border);
}
.service-list { display: flex; flex-direction: column; gap: 8px; }
.service-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-md);
  background: #fff;
  cursor: pointer;
  transition: var(--transition);
  text-align: left;
}
.service-item:hover { border-color: var(--primary); }
.service-item.selected { border-color: var(--primary); background: var(--primary-light); }
.si-info { display: flex; flex-direction: column; gap: 2px; }
.si-name { font-size: 15px; font-weight: 600; }
.si-desc { font-size: 12px; color: var(--text-muted); }
.si-price { font-size: 15px; font-weight: 700; color: var(--primary); flex-shrink: 0; margin-left: 12px; }
.no-service { color: var(--text-muted); font-size: 14px; }

/* Confirm */
.confirm-box {
  background: var(--bg);
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.confirm-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.cr-label { font-size: 13px; color: var(--text-muted); flex-shrink: 0; }
.cr-value { font-size: 14px; color: var(--text); text-align: right; }
.total-row { padding-top: 12px; border-top: 1px solid var(--border); }
.cr-price { font-size: 18px; font-weight: 700; color: var(--primary); }

/* Payment method */
.method-section { margin-bottom: 24px; }
.method-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.method-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.method-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 10px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-md);
  background: #fff;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-sub);
  cursor: pointer;
  transition: var(--transition);
}
.method-btn:hover { border-color: var(--primary); color: var(--primary); }
.method-btn.selected { border-color: var(--primary); background: var(--primary-light); color: var(--primary); font-weight: 600; }
.method-icon { font-size: 22px; }

.msg-error { color: var(--red); font-size: 13px; margin-bottom: 12px; }

/* Footer */
.step-footer { margin-top: 28px; }
.step-footer.two-btn { display: flex; gap: 10px; }
.step-footer.two-btn .btn:last-child { flex: 2; }
.step-footer.two-btn .btn:first-child { flex: 1; }

/* Image Upload */
.image-upload-label { display: inline-block; cursor: pointer; }
.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1.5px dashed var(--border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-sub);
  background: var(--bg);
  transition: var(--transition);
}
.upload-btn:hover { border-color: var(--primary); color: var(--primary); }

.image-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.image-preview-item {
  position: relative;
  width: 72px;
  height: 72px;
}
.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
}
.remove-img-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  background: var(--red);
  color: #fff;
  border: none;
  border-radius: 50%;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.upload-hint { font-size: 12px; color: var(--text-muted); margin-top: 6px; }

.no-hours-notice {
  background: #fff8e1;
  border: 1px solid #ffe082;
  border-radius: var(--radius-sm);
  padding: 12px 16px;
  font-size: 13px;
  color: #795548;
  margin-bottom: 20px;
}

.loading-center { text-align: center; padding: 40px; }

@media (max-width: 768px) {
  .steps { padding: 12px 14px; gap: 0; }
  .step-label { display: none; }
  .step-card { padding: 20px 16px; }
  .method-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
