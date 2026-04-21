<template>
  <main class="page">
    <div v-if="loading" class="loading-screen">
      <div class="spinner"></div>
    </div>

    <template v-else-if="error">
      <div class="container" style="text-align:center;padding:80px 0">
        <p style="font-size:48px">⚠️</p>
        <p style="color:var(--color-text-secondary);margin:16px 0">{{ error }}</p>
        <button class="btn btn-ghost" @click="$router.back()">돌아가기</button>
      </div>
    </template>

    <template v-else>
      <!-- Back Header -->
      <div class="container">
        <button class="back-btn" @click="$router.back()">← 목록으로</button>
      </div>

      <div class="detail-layout container">
        <!-- Left: Profile -->
        <aside class="profile-aside">
          <div class="profile-card card">
            <div class="profile-hero"></div>
            <img
              :src="stylist.profileImg || `https://i.pravatar.cc/200?u=${stylist.id}`"
              class="profile-avatar"
              :alt="stylist.name"
            />

            <div class="profile-info">
              <h1 class="profile-name">{{ stylist.name }}</h1>
              <p class="profile-salon">{{ stylist.salonName }}</p>
              <div class="profile-rating">
                <span class="stars">★★★★★</span>
                <span class="rating-val">{{ stylist.rating?.toFixed(1) ?? '0.0' }}</span>
                <span class="rating-cnt">({{ stylist.reviewCount ?? 0 }})</span>
              </div>
              <p class="profile-location">📍 {{ stylist.location }}</p>
              <p class="profile-exp" v-if="stylist.experience">경력 {{ stylist.experience }}년</p>

              <p class="bio-text" v-if="stylist.bio" style="margin:14px 0">{{ stylist.bio }}</p>

              <RouterLink
                v-if="isLoggedIn"
                :to="`/booking/${stylist.id}`"
                class="btn btn-primary btn-full btn-lg"
              >
                예약하기
              </RouterLink>
              <RouterLink
                v-else
                to="/login"
                class="btn btn-primary btn-full btn-lg"
              >
                로그인 후 예약하기
              </RouterLink>
            </div>
          </div>

          <!-- Hours -->
          <div class="card hours-card" v-if="workingHours.length">
            <h3 class="card-section-title">영업시간</h3>
            <ul class="hours-list">
              <li v-for="h in workingHours" :key="h.dayOfWeek">
                <span class="day">{{ dayName(h.dayOfWeek) }}</span>
                <span :class="h.isDayOff ? 'closed' : 'time'">
                  {{ h.isDayOff ? '휴무' : formatHours(h.openTime, h.closeTime) }}
                </span>
              </li>
            </ul>
          </div>
        </aside>

        <!-- Right: Content -->
        <section class="detail-content">
          <!-- Services -->
          <div class="card content-card" v-if="services.length">
            <h2 class="section-title">서비스 &amp; 가격</h2>
            <div class="service-list">
              <div
                v-for="service in services"
                :key="service.id"
                class="service-item"
              >
                <div>
                  <p class="service-name">{{ service.name }}</p>
                  <p class="service-duration" v-if="service.durationMinutes">⏱ {{ service.durationMinutes }}분</p>
                  <p class="service-desc" v-if="service.description">{{ service.description }}</p>
                </div>
                <span class="service-price">{{ service.price?.toLocaleString() }}원</span>
              </div>
            </div>
          </div>

          <!-- Portfolio -->
          <div class="card content-card" v-if="portfolios.length">
            <h2 class="section-title">포트폴리오</h2>
            <div class="portfolio-grid">
              <div
                v-for="item in portfolios"
                :key="item.id"
                class="portfolio-item"
              >
                <img
                  v-if="item.imageUrl"
                  :src="item.imageUrl"
                  :alt="item.caption"
                  class="portfolio-img"
                />
                <div v-else class="portfolio-placeholder">
                  <span class="portfolio-label">{{ item.caption || '포트폴리오' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty -->
          <div class="card content-card" v-if="!services.length && !portfolios.length">
            <p style="color:var(--color-text-muted);text-align:center;padding:40px 0">
              아직 등록된 서비스나 포트폴리오가 없습니다.
            </p>
          </div>
        </section>
      </div>
    </template>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { stylistApi } from '@/api/stylist'
import { useAuthStore } from '@/stores/authStore'

const route = useRoute()
const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)

const loading = ref(true)
const error = ref('')
const stylist = ref({})
const services = ref([])
const portfolios = ref([])
const workingHours = ref([])

const DAY_NAMES = ['월', '화', '수', '목', '금', '토', '일']
const dayName = (idx) => DAY_NAMES[idx] ?? String(idx)
const formatHours = (open, close) => {
  if (!open || !close) return '미설정'
  return `${open.slice(0, 5)} - ${close.slice(0, 5)}`
}

onMounted(async () => {
  try {
    const res = await stylistApi.getStylist(route.params.id)
    const data = res.data
    stylist.value = data
    services.value = data.services ?? []
    portfolios.value = data.portfolios ?? []
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
  background: none;
  border: none;
  color: var(--color-text-secondary);
  font-size: 14px;
  padding: 8px 0;
  cursor: pointer;
  margin-bottom: 28px;
  transition: var(--transition);
}
.back-btn:hover { color: var(--color-gold); }

.detail-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  align-items: start;
}

/* Profile aside */
.profile-card {
  padding: 0;
  overflow: hidden;
  margin-bottom: 16px;
}
.profile-hero { height: 100px; background: var(--color-bg-surface); }
.profile-avatar {
  width: 88px; height: 88px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid var(--color-bg-card);
  margin: -44px 0 0 20px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.5);
}
.profile-info { padding: 12px 20px 24px; }
.profile-name { font-size: 20px; font-weight: 700; margin-top: 12px; }
.profile-salon { font-size: 13px; color: var(--color-text-secondary); margin: 2px 0 8px; }
.profile-rating { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.stars { color: var(--color-gold); font-size: 14px; }
.rating-val { font-weight: 600; font-size: 14px; }
.rating-cnt { font-size: 13px; color: var(--color-text-muted); }
.profile-location { font-size: 13px; color: var(--color-text-muted); margin-bottom: 4px; }
.profile-exp { font-size: 13px; color: var(--color-text-secondary); margin-bottom: 8px; }
.bio-text { font-size: 13px; color: var(--color-text-secondary); line-height: 1.7; }

.hours-card { padding: 20px; }
.card-section-title { font-size: 15px; font-weight: 600; margin-bottom: 14px; }
.hours-list { list-style: none; display: flex; flex-direction: column; gap: 8px; }
.hours-list li { display: flex; justify-content: space-between; font-size: 13px; }
.day { color: var(--color-text-secondary); }
.time { color: var(--color-text-primary); }
.closed { color: var(--color-danger); }

/* Content */
.content-card { margin-bottom: 20px; }

.service-list { display: flex; flex-direction: column; gap: 0; }
.service-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--color-border);
}
.service-item:last-child { border-bottom: none; }
.service-name { font-size: 15px; font-weight: 500; }
.service-duration { font-size: 12px; color: var(--color-text-muted); margin-top: 2px; }
.service-desc { font-size: 12px; color: var(--color-text-muted); margin-top: 2px; }
.service-price { font-size: 15px; font-weight: 600; color: var(--color-gold); white-space: nowrap; }

.portfolio-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.portfolio-item {
  aspect-ratio: 1;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-bg-surface);
}
.portfolio-img { width: 100%; height: 100%; object-fit: cover; }
.portfolio-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: flex-end; padding: 10px;
  background: var(--color-bg-surface);
}
.portfolio-label { font-size: 11px; color: var(--color-text-muted); }

@media (max-width: 768px) {
  .detail-layout { grid-template-columns: 1fr; }
  .portfolio-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
