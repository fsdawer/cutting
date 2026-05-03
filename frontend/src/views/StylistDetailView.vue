<template>
  <main class="page">
    <div v-if="loading" class="loading-screen">
      <div class="spinner"></div>
    </div>

    <template v-else-if="error">
      <div class="container state-center">
        <p class="state-icon">⚠️</p>
        <p class="state-text">{{ error }}</p>
        <button class="btn btn-ghost" @click="$router.back()">돌아가기</button>
      </div>
    </template>

    <template v-else>
      <div class="container">
        <button class="back-btn" @click="$router.back()">← 목록으로</button>

        <div class="detail-layout">
          <!-- Left: Profile -->
          <aside class="profile-aside">
            <div class="card profile-card">
              <div class="profile-cover"></div>
              <img
                :src="stylist.profileImg || `https://i.pravatar.cc/200?u=${stylist.id}`"
                class="profile-avatar"
                :alt="stylist.name"
              />
              <div class="profile-body">
                <p class="profile-salon">{{ stylist.salonName }}</p>
                <h1 class="profile-name">{{ stylist.name }}</h1>
                <div class="profile-rating">
                  <span class="star">★</span>
                  <span class="rating-num">{{ stylist.rating?.toFixed(1) ?? '0.0' }}</span>
                  <span class="rating-cnt">({{ stylist.reviewCount ?? 0 }})</span>
                </div>
                <p class="profile-location">📍 {{ stylist.location }}</p>
                <p class="profile-phone" v-if="stylist.salonPhone">📞 {{ stylist.salonPhone }}</p>
                <p class="profile-exp" v-if="stylist.experience">경력 {{ stylist.experience }}년</p>
                <p class="profile-salon-desc" v-if="stylist.salonDescription">{{ stylist.salonDescription }}</p>
                <p class="profile-bio" v-if="stylist.bio">{{ stylist.bio }}</p>

                <RouterLink v-if="isLoggedIn" :to="`/booking/${stylist.id}`" class="btn btn-primary btn-full" style="margin-top:16px;">
                  예약하기
                </RouterLink>
                <RouterLink v-else to="/login" class="btn btn-primary btn-full" style="margin-top:16px;">
                  로그인 후 예약하기
                </RouterLink>
              </div>
            </div>

            <div class="card hours-card" v-if="workingHours.length">
              <h3 class="card-sub-title">영업시간</h3>
              <ul class="hours-list">
                <li v-for="h in workingHours" :key="h.dayOfWeek" class="hours-item">
                  <span class="day-name">{{ dayName(h.dayOfWeek) }}</span>
                  <span :class="h.isDayOff ? 'time-closed' : 'time-open'">
                    {{ h.isDayOff ? '휴무' : formatHours(h.openTime, h.closeTime) }}
                  </span>
                </li>
              </ul>
            </div>
          </aside>

          <!-- Right: Content -->
          <section class="detail-content">
            <div class="card content-card" v-if="services.length">
              <h2 class="content-title">서비스 &amp; 가격</h2>
              <div class="service-list">
                <div v-for="service in services" :key="service.id" class="service-row">
                  <div>
                    <p class="svc-name">{{ service.name }}</p>
                    <p class="svc-meta" v-if="service.durationMinutes">⏱ {{ service.durationMinutes }}분</p>
                    <p class="svc-desc" v-if="service.description">{{ service.description }}</p>
                  </div>
                  <span class="svc-price">{{ service.price?.toLocaleString() }}원</span>
                </div>
              </div>
            </div>

            <div class="card content-card" v-if="!services.length">
              <p class="no-content">아직 등록된 서비스가 없습니다.</p>
            </div>

            <!-- 리뷰 탭 -->
            <div class="card content-card">
              <h2 class="content-title">
                리뷰
                <span class="review-count-badge">{{ reviews.length }}</span>
              </h2>

              <!-- 리뷰 작성 폼 -->
              <div v-if="canWriteReview" class="review-form-wrap">
                <p class="review-form-hint">완료된 예약에 대한 리뷰를 남겨보세요.</p>
                <div class="star-row">
                  <button
                    v-for="n in 5" :key="n"
                    class="star-btn"
                    :class="{ filled: n <= newRating }"
                    @click="newRating = n"
                  >★</button>
                </div>
                <textarea
                  v-model="newContent"
                  class="review-textarea"
                  placeholder="리뷰 내용을 입력하세요"
                  rows="3"
                ></textarea>
                <p v-if="reviewError" class="review-error">{{ reviewError }}</p>
                <button class="btn btn-primary btn-sm" :disabled="reviewSubmitting" @click="submitReview">
                  {{ reviewSubmitting ? '등록 중...' : '리뷰 등록' }}
                </button>
              </div>

              <!-- 내가 쓴 리뷰 -->
              <div v-if="myReview" class="review-item my-review">
                <div class="review-header">
                  <img :src="myReview.userProfileImg || `https://i.pravatar.cc/36?u=${myReview.userId}`" class="review-avatar" />
                  <div>
                    <p class="review-author">{{ myReview.userName }} <span class="my-badge">내 리뷰</span></p>
                    <div class="review-stars">
                      <span v-for="n in 5" :key="n" class="star-text" :class="{ filled: n <= myReview.rating }">★</span>
                    </div>
                  </div>
                  <div class="review-actions">
                    <button class="btn btn-ghost btn-xs" @click="startEdit">수정</button>
                    <button class="btn btn-ghost btn-xs" style="color:var(--red)" @click="deleteMyReview">삭제</button>
                  </div>
                </div>
                <div v-if="editMode" class="edit-form-wrap">
                  <div class="star-row">
                    <button v-for="n in 5" :key="n" class="star-btn" :class="{ filled: n <= editRating }" @click="editRating = n">★</button>
                  </div>
                  <textarea v-model="editContent" class="review-textarea" rows="3"></textarea>
                  <div class="edit-actions">
                    <button class="btn btn-ghost btn-sm" @click="editMode = false">취소</button>
                    <button class="btn btn-primary btn-sm" @click="saveEdit">저장</button>
                  </div>
                </div>
                <p v-else class="review-content">{{ myReview.content }}</p>
                <p class="review-date">{{ formatDate(myReview.createdAt) }}</p>
              </div>

              <!-- 리뷰 목록 -->
              <div v-if="otherReviews.length === 0 && !myReview" class="no-content">
                아직 리뷰가 없습니다.
              </div>
              <div v-for="r in otherReviews" :key="r.id" class="review-item">
                <div class="review-header">
                  <img :src="r.userProfileImg || `https://i.pravatar.cc/36?u=${r.userId}`" class="review-avatar" />
                  <div>
                    <p class="review-author">{{ r.userName }}</p>
                    <div class="review-stars">
                      <span v-for="n in 5" :key="n" class="star-text" :class="{ filled: n <= r.rating }">★</span>
                    </div>
                  </div>
                  <p class="review-service-badge">{{ r.serviceName }}</p>
                </div>
                <p class="review-content">{{ r.content }}</p>
                <p class="review-date">{{ formatDate(r.createdAt) }}</p>
              </div>
            </div>
          </section>
        </div>
      </div>
    </template>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { stylistApi } from '@/api/stylist'
import { reviewApi } from '@/api/review'
import { reservationApi } from '@/api/reservation'
import { useAuthStore } from '@/stores/authStore'

const route     = useRoute()
const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)

const loading  = ref(true)
const error    = ref('')
const stylist  = ref({})
const services = ref([])
const workingHours = ref([])

// 리뷰
const reviews    = ref([])
const newRating  = ref(5)
const newContent = ref('')
const reviewError      = ref('')
const reviewSubmitting = ref(false)
const eligibleReservationId = ref(null) // 리뷰 작성 가능한 예약 ID

const myReview    = computed(() => reviews.value.find(r => r.userId === authStore.user?.id) ?? null)
const otherReviews = computed(() => reviews.value.filter(r => r.userId !== authStore.user?.id))
const canWriteReview = computed(() => isLoggedIn.value && !myReview.value && !!eligibleReservationId.value)

const editMode    = ref(false)
const editRating  = ref(5)
const editContent = ref('')

function startEdit() {
  editRating.value  = myReview.value.rating
  editContent.value = myReview.value.content
  editMode.value = true
}

async function loadEligibleReservation() {
  if (!isLoggedIn.value) return
  try {
    const { data } = await reservationApi.getMyReservations()
    const stylistId = Number(route.params.id)
    const done = data.find(r => r.stylistId === stylistId && r.status === 'DONE')
    eligibleReservationId.value = done?.id ?? null
  } catch (e) { console.error(e) }
}

async function submitReview() {
  if (!newRating.value) { reviewError.value = '별점을 선택하세요.'; return }
  if (!eligibleReservationId.value) { reviewError.value = '완료된 예약이 있어야 리뷰를 작성할 수 있습니다.'; return }
  reviewSubmitting.value = true; reviewError.value = ''
  try {
    await reviewApi.create({ reservationId: eligibleReservationId.value, rating: newRating.value, content: newContent.value })
    newContent.value = ''; newRating.value = 5
    await loadReviews()
  } catch (e) {
    reviewError.value = e.response?.data?.message || '리뷰 등록 중 오류가 발생했습니다.'
  } finally { reviewSubmitting.value = false }
}

async function saveEdit() {
  try {
    await reviewApi.update(myReview.value.id, { rating: editRating.value, content: editContent.value })
    editMode.value = false
    await loadReviews()
  } catch (e) { alert(e.response?.data?.message || '수정 중 오류가 발생했습니다.') }
}

async function deleteMyReview() {
  if (!confirm('리뷰를 삭제하시겠습니까?')) return
  try {
    await reviewApi.remove(myReview.value.id)
    await loadReviews()
  } catch (e) { alert(e.response?.data?.message || '삭제 중 오류가 발생했습니다.') }
}

async function loadReviews() {
  try {
    const res = await reviewApi.getByStylist(route.params.id)
    reviews.value = res.data
  } catch (e) { console.error(e) }
}

const DAY_NAMES = ['월', '화', '수', '목', '금', '토', '일']
const dayName = (idx) => DAY_NAMES[idx] ?? String(idx)
const formatHours = (open, close) => (!open || !close) ? '미설정' : `${open.slice(0,5)} - ${close.slice(0,5)}`
function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`
}

onMounted(async () => {
  try {
    const [profileRes] = await Promise.all([
      stylistApi.getStylist(route.params.id),
      loadReviews(),
      loadEligibleReservation(),
    ])
    const data = profileRes.data
    stylist.value      = data
    services.value     = data.services ?? []
    workingHours.value = (data.workingHours ?? []).sort((a, b) => a.dayOfWeek - b.dayOfWeek)
  } catch (e) {
    error.value = '스타일리스트 정보를 불러오지 못했습니다.'
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.loading-screen { display: flex; justify-content: center; align-items: center; height: 400px; }
.back-btn {
  background: none; border: none;
  color: var(--text-sub); font-size: 14px;
  padding: 8px 0; cursor: pointer;
  margin-bottom: 20px; display: block;
  transition: var(--transition);
}
.back-btn:hover { color: var(--primary); }

.state-center { text-align: center; padding: 80px 0; display: flex; flex-direction: column; align-items: center; gap: 12px; }
.state-icon { font-size: 44px; }
.state-text { color: var(--text-sub); }

.detail-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  align-items: start;
}

/* Profile aside */
.profile-card { padding: 0; overflow: hidden; margin-bottom: 14px; }
.profile-cover { height: 90px; background: var(--bg); }
.profile-avatar {
  width: 80px; height: 80px;
  border-radius: 50%; object-fit: cover;
  border: 3px solid #fff;
  margin: -40px 0 0 18px;
  box-shadow: var(--shadow-md);
}
.profile-body  { padding: 12px 18px 22px; }
.profile-salon { font-size: 12px; font-weight: 600; color: var(--primary); letter-spacing: 0.02em; margin-bottom: 2px; }
.profile-name  { font-size: 20px; font-weight: 700; margin-bottom: 8px; }
.profile-rating { display: flex; align-items: center; gap: 4px; margin-bottom: 8px; }
.star { color: #FFCC00; font-size: 15px; }
.rating-num { font-weight: 700; font-size: 14px; }
.rating-cnt { font-size: 13px; color: var(--text-muted); }
.profile-location { font-size: 13px; color: var(--text-muted); margin-bottom: 3px; }
.profile-phone    { font-size: 13px; color: var(--text-muted); margin-bottom: 3px; }
.profile-exp      { font-size: 13px; color: var(--text-sub); margin-bottom: 6px; }
.profile-salon-desc { font-size: 12px; color: var(--text-muted); line-height: 1.6; margin-bottom: 6px; padding: 8px; background: var(--bg); border-radius: var(--radius-sm); }
.profile-bio  { font-size: 13px; color: var(--text-sub); line-height: 1.7; margin-top: 8px; }

.hours-card { padding: 18px; }
.card-sub-title { font-size: 14px; font-weight: 700; margin-bottom: 12px; }
.hours-list { list-style: none; display: flex; flex-direction: column; gap: 8px; }
.hours-item { display: flex; justify-content: space-between; font-size: 13px; }
.day-name   { color: var(--text-sub); }
.time-open  { color: var(--text); font-weight: 500; }
.time-closed { color: var(--red); font-weight: 500; }

/* Content */
.content-card { margin-bottom: 16px; }
.content-title { font-size: 17px; font-weight: 700; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--border); }

.service-list { display: flex; flex-direction: column; }
.service-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 0; border-bottom: 1px solid var(--border);
}
.service-row:last-child { border-bottom: none; }
.svc-name  { font-size: 15px; font-weight: 600; }
.svc-meta  { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.svc-desc  { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.svc-price { font-size: 15px; font-weight: 700; color: var(--primary); white-space: nowrap; margin-left: 16px; }

/* Reviews */
.review-count-badge {
  display: inline-flex; align-items: center; justify-content: center;
  background: var(--primary); color: #fff;
  font-size: 11px; font-weight: 700;
  border-radius: var(--radius-full);
  min-width: 20px; height: 20px; padding: 0 6px;
  margin-left: 8px; vertical-align: middle;
}
.review-form-wrap {
  background: var(--bg); border-radius: var(--radius-sm);
  padding: 16px; margin-bottom: 20px;
  display: flex; flex-direction: column; gap: 10px;
}
.review-form-hint { font-size: 13px; color: var(--text-muted); }
.star-row { display: flex; gap: 4px; }
.star-btn {
  font-size: 22px; background: none; border: none; cursor: pointer;
  color: var(--border); transition: color 0.1s;
  padding: 0; line-height: 1;
}
.star-btn.filled { color: #FFCC00; }
.review-textarea {
  width: 100%; border: 1.5px solid var(--border); border-radius: var(--radius-sm);
  padding: 10px 12px; font-size: 14px; resize: vertical;
  font-family: inherit;
}
.review-textarea:focus { outline: none; border-color: var(--primary); }
.review-error { font-size: 13px; color: var(--red); }

.review-item {
  padding: 16px 0; border-bottom: 1px solid var(--border);
}
.review-item:last-child { border-bottom: none; }
.my-review { background: #fffdf0; border-radius: var(--radius-sm); padding: 14px; margin-bottom: 8px; border-bottom: none; }
.review-header { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 8px; }
.review-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.review-author { font-size: 14px; font-weight: 600; margin-bottom: 3px; }
.my-badge {
  display: inline-block; background: var(--primary); color: #fff;
  font-size: 10px; font-weight: 700; border-radius: 4px;
  padding: 1px 5px; margin-left: 6px; vertical-align: middle;
}
.review-stars { display: flex; gap: 2px; }
.star-text { font-size: 14px; color: var(--border); }
.star-text.filled { color: #FFCC00; }
.review-service-badge {
  margin-left: auto; font-size: 11px; color: var(--text-muted);
  background: var(--bg); padding: 3px 8px; border-radius: var(--radius-full);
  white-space: nowrap;
}
.review-actions { margin-left: auto; display: flex; gap: 4px; }
.review-content { font-size: 14px; color: var(--text); line-height: 1.7; margin-bottom: 6px; }
.review-date { font-size: 12px; color: var(--text-muted); }
.edit-form-wrap { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
.edit-actions { display: flex; gap: 6px; justify-content: flex-end; }

.no-content { text-align: center; padding: 40px 0; color: var(--text-muted); font-size: 14px; }

@media (max-width: 768px) {
  .detail-layout { grid-template-columns: 1fr; }
}
</style>
