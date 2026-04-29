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

            <div class="card content-card" v-if="portfolios.length">
              <h2 class="content-title">포트폴리오</h2>
              <div class="portfolio-grid">
                <div v-for="item in portfolios" :key="item.id" class="portfolio-item">
                  <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.caption" class="portfolio-img" />
                  <div v-else class="portfolio-empty">
                    <span>{{ item.caption || '사진' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="card content-card" v-if="!services.length && !portfolios.length">
              <p class="no-content">아직 등록된 서비스나 포트폴리오가 없습니다.</p>
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
import { useAuthStore } from '@/stores/authStore'

const route     = useRoute()
const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)

const loading  = ref(true)
const error    = ref('')
const stylist  = ref({})
const services = ref([])
const portfolios   = ref([])
const workingHours = ref([])

const DAY_NAMES = ['월', '화', '수', '목', '금', '토', '일']
const dayName = (idx) => DAY_NAMES[idx] ?? String(idx)
const formatHours = (open, close) => (!open || !close) ? '미설정' : `${open.slice(0,5)} - ${close.slice(0,5)}`

onMounted(async () => {
  try {
    const res  = await stylistApi.getStylist(route.params.id)
    const data = res.data
    stylist.value      = data
    services.value     = data.services ?? []
    portfolios.value   = data.portfolios ?? []
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

.portfolio-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.portfolio-item { aspect-ratio: 1; border-radius: var(--radius-sm); overflow: hidden; background: var(--bg); }
.portfolio-img  { width: 100%; height: 100%; object-fit: cover; }
.portfolio-empty { width: 100%; height: 100%; display: flex; align-items: flex-end; padding: 8px; }
.portfolio-empty span { font-size: 11px; color: var(--text-muted); }

.no-content { text-align: center; padding: 40px 0; color: var(--text-muted); font-size: 14px; }

@media (max-width: 768px) {
  .detail-layout { grid-template-columns: 1fr; }
  .portfolio-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
