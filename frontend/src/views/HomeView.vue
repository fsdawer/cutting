<template>
  <main class="page">
    <!-- Hero -->
    <section class="hero">
      <div class="container">
        <div class="hero-inner">
          <div class="hero-text">
            <p class="hero-eyebrow">내 주변 미용사 예약 플랫폼</p>
            <h1 class="hero-title">믿을 수 있는<br>스타일리스트</h1>
            <p class="hero-sub">검증된 미용사를 쉽고 빠르게 예약하세요</p>
          </div>
          <div class="hero-search">
            <div class="search-row">
              <input
                v-model="searchQuery"
                class="search-input"
                placeholder="미용사 이름, 미용실 검색"
                @keyup.enter="search"
              />
              <input
                v-model="locationQuery"
                class="search-input"
                placeholder="지역 (예: 강남구)"
                @keyup.enter="search"
              />
              <button class="btn btn-primary search-btn" @click="search">검색</button>
            </div>
            <div class="search-actions">
              <button
                class="btn btn-ghost btn-sm nearby-btn"
                :class="{ active: isNearbyMode }"
                @click="toggleNearby"
              >
                {{ isNearbyMode ? '주변 검색 끄기' : '내 주변 검색' }}
              </button>
              <div class="quick-tags">
                <button v-for="tag in popularTags" :key="tag" class="tag-chip" @click="searchQuery = tag; search()">{{ tag }}</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 지도 -->
    <div v-if="isNearbyMode" class="map-section">
      <div class="container">
        <p v-if="nearbyError" class="nearby-error">{{ nearbyError }}</p>
        <!-- 지도 + 내 위치 이동 버튼 래퍼 -->
        <div class="map-wrap">
          <div ref="mapContainer" class="map-container"></div>
          <!-- 과녁 버튼: 클릭 시 내 위치로 지도 중심 이동 -->
          <button
            v-if="mapInstance"
            class="recenter-btn"
            title="내 위치로 이동"
            @click="recenterMap"
          >
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" width="20" height="20">
              <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8"/>
              <circle cx="12" cy="12" r="4" stroke="currentColor" stroke-width="1.8"/>
              <line x1="12" y1="2" x2="12" y2="5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              <line x1="12" y1="19" x2="12" y2="22" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              <line x1="2" y1="12" x2="5" y2="12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              <line x1="19" y1="12" x2="22" y2="12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div class="container content-area">
      <!-- 필터 바 -->
      <div class="filter-bar">
        <div class="filter-chips">
          <button v-for="f in filters" :key="f.value" class="f-chip" :class="{ active: activeFilter === f.value }" @click="activeFilter = f.value">
            {{ f.label }}
          </button>
        </div>
        <div class="sort-wrap">
          <select v-model="sortBy" class="sort-select">
            <option value="rating">평점순</option>
            <option value="review">리뷰순</option>
            <option value="exp">경력순</option>
          </select>
        </div>
      </div>

      <!-- 로딩 -->
      <div v-if="loading" class="state-center">
        <div class="spinner"></div>
        <p class="state-text">검색 중...</p>
      </div>

      <!-- 결과 없음 -->
      <div v-else-if="sortedStylists.length === 0" class="state-center">
        <p class="state-text">검색 결과가 없습니다.</p>
        <button class="btn btn-ghost btn-sm" @click="resetSearch">검색 초기화</button>
      </div>

      <!-- 스타일리스트 목록 -->
      <div v-else class="stylists-grid">
        <StylistCard v-for="s in sortedStylists" :key="s.id" :stylist="s" />
      </div>

      <!-- 랭킹 섹션 -->
      <section class="ranking-section">
        <div class="section-header">
          <div>
            <h2 class="section-title">지역별 랭킹</h2>
            <p class="section-subtitle">예약 수와 평점을 종합한 실시간 랭킹입니다</p>
          </div>
          <RouterLink to="/ranking" class="btn btn-ghost btn-sm">전체보기 →</RouterLink>
        </div>

        <div class="rank-district-tabs">
          <button
            v-for="d in districts"
            :key="d"
            class="d-chip"
            :class="{ active: rankDistrict === d }"
            @click="rankDistrict = d; loadRanking()"
          >{{ d }}</button>
        </div>

        <div v-if="rankLoading" class="state-center" style="padding: 40px 0">
          <div class="spinner"></div>
        </div>

        <div v-else-if="rankList.length === 0" class="state-center" style="padding: 40px 0">
          <p class="state-text">랭킹 데이터가 없습니다.</p>
        </div>

        <div v-else class="rank-list">
          <RouterLink
            v-for="(item, idx) in rankList.slice(0, 5)"
            :key="item.stylistId"
            :to="`/stylist/${item.stylistId}`"
            class="rank-item"
          >
            <span class="rank-num" :class="{ top3: idx < 3 }">{{ idx + 1 }}</span>
            <div class="rank-info">
              <p class="rank-name">{{ item.stylistName }}</p>
              <p class="rank-salon">{{ item.salonName }} · {{ item.district }}</p>
            </div>
            <div class="rank-stats">
              <span class="rank-rating">★ {{ item.avgRating?.toFixed(1) }}</span>
              <span class="rank-divider">|</span>
              <span class="rank-review">리뷰 {{ item.reviewCount }}</span>
            </div>
          </RouterLink>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import StylistCard from '@/components/StylistCard.vue'
import { stylistApi } from '@/api/stylist'
import { rankingApi } from '@/api/ranking'

const searchQuery  = ref('')
const locationQuery = ref('')
const activeFilter = ref('all')
const sortBy       = ref('rating')
const loading      = ref(false)
const stylists     = ref([])
const isNearbyMode = ref(false)
const nearbyError  = ref('')
const mapContainer = ref(null)
const mapInstance  = ref(null)   // 카카오 Map 인스턴스 보관 → recenterMap()에서 재활용
const myLocation   = ref(null)   // { lat, lng } — 내 위치 좌표 보관

const rankDistrict = ref('강남구')
const rankLoading  = ref(false)
const rankList     = ref([])

const districts = ['강남구', '서초구', '마포구', '용산구', '종로구', '송파구']
const popularTags = ['커트', '펌', '염색', '클리닉', '헤어케어']
const filters = [
  { label: '전체', value: 'all' },
  { label: '커트', value: '커트' },
  { label: '펌', value: '펌' },
  { label: '염색', value: '염색' },
  { label: '클리닉', value: '클리닉' },
]

const sortedStylists = computed(() => {
  let list = [...stylists.value]
  if (activeFilter.value !== 'all') {
    list = list.filter(s => s.services?.some(sv => sv.category === activeFilter.value))
  }
  if (sortBy.value === 'rating') return list.sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0))
  if (sortBy.value === 'review') return list.sort((a, b) => (b.reviewCount ?? 0) - (a.reviewCount ?? 0))
  if (sortBy.value === 'exp')    return list.sort((a, b) => (b.experience ?? 0) - (a.experience ?? 0))
  return list
})

async function search() {
  loading.value = true
  try {
    const res = await stylistApi.getStylists({ keyword: searchQuery.value, location: locationQuery.value })
    stylists.value = res.data
  } catch { stylists.value = [] }
  finally { loading.value = false }
}

function resetSearch() {
  searchQuery.value = ''
  locationQuery.value = ''
  search()
}

// 카카오 지도 SDK가 완전히 로드될 때까지 최대 5초 폴링 대기
// index.html의 <script> 태그가 SPA 번들보다 늦게 실행될 수 있기 때문에 필요
function waitForKakaoSDK(timeout = 5000) {
  return new Promise((resolve, reject) => {
    console.log('[Kakao] SDK 체크 시작 — window.kakao:', !!window.kakao, '/ window.kakao?.maps:', !!(window.kakao?.maps))
    if (window.kakao && window.kakao.maps) {
      console.log('[Kakao] SDK 이미 로드됨 → 즉시 resolve')
      return resolve()
    }
    const interval = 100
    let elapsed = 0
    const timer = setInterval(() => {
      elapsed += interval
      if (window.kakao && window.kakao.maps) {
        clearInterval(timer)
        console.log(`[Kakao] SDK 로드 완료 (${elapsed}ms 경과)`)
        resolve()
      } else if (elapsed >= timeout) {
        clearInterval(timer)
        console.error('[Kakao] SDK 로드 타임아웃 — index.html <script> 태그 확인 필요')
        nearbyError.value = '카카오 지도 SDK 로드에 실패했습니다. 페이지를 새로고침 해주세요.'
        reject(new Error('Kakao SDK timeout'))
      }
    }, interval)
  })
}

async function toggleNearby() {
  if (isNearbyMode.value) { isNearbyMode.value = false; mapInstance.value = null; return }
  nearbyError.value = ''
  if (!navigator.geolocation) { nearbyError.value = '위치 서비스를 지원하지 않는 브라우저입니다.'; return }
  navigator.geolocation.getCurrentPosition(async pos => {
    const { latitude: lat, longitude: lng } = pos.coords
    console.log(`[Map] ① 위치 획득 성공 — lat=${lat}, lng=${lng}`)
    isNearbyMode.value = true
    loading.value = true
    try {
      // [1] 주변 미용사 조회 — 실패해도 지도는 보여줌
      try {
        const res = await stylistApi.getNearbyStylists(lat, lng, 3000)
        stylists.value = res.data
        console.log(`[Map] ② API 응답 — 스타일리스트 수: ${res.data.length}`, res.data)
        if (res.data.length === 0) {
          nearbyError.value = '반경 3km 내 등록된 미용사가 없습니다.'
        }
      } catch (apiErr) {
        stylists.value = []
        console.error('[Map] ② API 호출 실패:', apiErr)
        nearbyError.value = '주변 미용사 정보를 불러올 수 없습니다.'
      }
      // [2] DOM 렌더 대기 후 지도 초기화 (API 성공/실패 무관)
      await nextTick()
      console.log('[Map] ③ nextTick 완료 — mapContainer el:', mapContainer.value)
      await waitForKakaoSDK()
      console.log('[Map] ④ kakao.maps.load() 호출 시도')
      window.kakao.maps.load(() => {
        console.log('[Map] ⑤ kakao.maps.load 콜백 진입 — initMap 호출')
        initMap(lat, lng)
      })
    } catch (e) {
      console.error('[Map] 치명적 오류:', e)
      nearbyError.value = nearbyError.value || '오류가 발생했습니다.'
    } finally { loading.value = false }
  }, (geoErr) => {
    console.error('[Map] 위치 획득 실패:', geoErr)
    nearbyError.value = '위치 정보를 가져올 수 없습니다. 브라우저 위치 권한을 허용해주세요.'
  })
}

function initMap(lat, lng) {
  console.log('[Map] ⑥ initMap 진입 — mapContainer.value:', mapContainer.value)
  if (!mapContainer.value) {
    console.error('[Map] ⑥ initMap 실패 — mapContainer ref가 null (v-if DOM 미렌더)') 
    return
  }
  myLocation.value = { lat, lng }
  const center = new window.kakao.maps.LatLng(lat, lng)
  console.log('[Map] ⑦ Map 생성 시작 — center:', center)
  const map = new window.kakao.maps.Map(mapContainer.value, { center, level: 4 })
  mapInstance.value = map
  console.log('[Map] ⑧ Map 생성 완료 — mapInstance 저장됨')
  // 내 위치 마커
  const myMarker = new window.kakao.maps.Marker({ map, position: center })
  const myInfo = new window.kakao.maps.InfoWindow({
    content: '<div style="padding:6px 10px;font-size:12px;font-weight:700;color:#059669">내 위치</div>',
  })
  myInfo.open(map, myMarker)
  // 주변 스타일리스트 마커
  let markerCount = 0
  stylists.value.forEach(s => {
    if (!s.latitude || !s.longitude) return
    const pos = new window.kakao.maps.LatLng(s.latitude, s.longitude)
    const marker = new window.kakao.maps.Marker({ map, position: pos })
    const info = new window.kakao.maps.InfoWindow({
      content: `<div style="padding:6px 10px;font-size:13px;font-weight:600;cursor:pointer" onclick="location.href='/stylist/${s.id}'">${s.name}<br/><span style="font-size:11px;color:#6b7280">${s.salonName || ''}</span></div>`,
    })
    window.kakao.maps.event.addListener(marker, 'click', () => info.open(map, marker))
    markerCount++
  })
  console.log(`[Map] ⑨ 마커 렌더 완료 — 내 위치 1개 + 스타일리스트 ${markerCount}개`)
}

// 과녁 버튼 클릭 → 저장된 내 위치로 지도 중심 재이동
function recenterMap() {
  if (!mapInstance.value || !myLocation.value) return
  const { lat, lng } = myLocation.value
  const center = new window.kakao.maps.LatLng(lat, lng)
  mapInstance.value.setCenter(center)
  mapInstance.value.setLevel(4)  // zoom 레벨도 원래대로
}

async function loadRanking() {
  rankLoading.value = true
  try {
    const res = await rankingApi.getRanking(rankDistrict.value)
    rankList.value = res.data
  } catch { rankList.value = [] }
  finally { rankLoading.value = false }
}

onMounted(() => { search(); loadRanking() })
</script>

<style scoped>
/* Hero */
.hero {
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  padding: 48px 0 40px;
}
.hero-inner {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 48px;
  align-items: center;
}
.hero-eyebrow { font-size: 12px; font-weight: 700; color: var(--accent); letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 12px; }
.hero-title { font-size: 40px; font-weight: 800; line-height: 1.2; letter-spacing: -0.03em; margin-bottom: 12px; }
.hero-sub { font-size: 15px; color: var(--text-muted); }

.search-row { display: flex; gap: 8px; margin-bottom: 12px; }
.search-input {
  flex: 1; padding: 12px 16px; border: 1.5px solid var(--border);
  border-radius: var(--radius-md); font-size: 14px; font-family: var(--font);
  background: var(--bg); color: var(--text); transition: var(--transition);
}
.search-input:focus { border-color: var(--primary); outline: none; background: #fff; }
.search-btn { padding: 12px 24px; flex-shrink: 0; }

.search-actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.nearby-btn { font-size: 13px; }
.nearby-btn.active { border-color: var(--primary); color: var(--primary); background: var(--bg-surface); }

.quick-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.tag-chip {
  padding: 4px 12px; border-radius: var(--radius-full);
  border: 1px solid var(--border); background: transparent;
  font-size: 12px; font-weight: 500; color: var(--text-muted);
  cursor: pointer; transition: var(--transition);
}
.tag-chip:hover { border-color: var(--primary); color: var(--primary); background: var(--bg-surface); }

/* Map */
.map-section { padding: 0 0 24px; }
.map-wrap { position: relative; }
.map-container { width: 100%; height: 320px; border-radius: var(--radius-lg); overflow: hidden; border: 1px solid var(--border); }
.nearby-error { color: var(--danger); font-size: 13px; margin-bottom: 8px; padding: 10px 14px; background: var(--danger-light); border-radius: var(--radius-sm); }

/* 과녁(내 위치로 이동) 버튼 */
.recenter-btn {
  position: absolute;
  bottom: 12px;
  right: 12px;
  z-index: 10;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--bg-card, #fff);
  border: 1.5px solid var(--border);
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--primary);
  transition: transform 0.15s, box-shadow 0.15s;
}
.recenter-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 14px rgba(0,0,0,0.2);
}

/* Content */
.content-area { padding-top: 28px; }

/* Filter */
.filter-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; gap: 12px; flex-wrap: wrap; }
.filter-chips { display: flex; gap: 6px; flex-wrap: wrap; }
.f-chip {
  padding: 6px 14px; border-radius: var(--radius-full);
  border: 1px solid var(--border); background: var(--bg-card);
  font-size: 13px; font-weight: 500; color: var(--text-muted);
  cursor: pointer; transition: var(--transition);
}
.f-chip:hover { border-color: var(--border-strong); color: var(--text-sub); }
.f-chip.active { border-color: var(--primary); background: var(--primary); color: #fff; }

.sort-select {
  padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm);
  font-size: 13px; background: var(--bg-card); color: var(--text-sub); cursor: pointer;
}

/* Grid */
.stylists-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 48px; }

/* Ranking Section */
.ranking-section { padding-top: 40px; border-top: 1px solid var(--border); }
.section-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }

.rank-district-tabs { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 16px; }
.d-chip {
  padding: 6px 14px; border-radius: var(--radius-full);
  border: 1px solid var(--border); background: var(--bg-card);
  font-size: 13px; font-weight: 500; color: var(--text-muted);
  cursor: pointer; transition: var(--transition);
}
.d-chip:hover { border-color: var(--border-strong); color: var(--text-sub); }
.d-chip.active { border-color: var(--primary); background: var(--primary); color: #fff; }

.rank-list { display: flex; flex-direction: column; gap: 2px; }
.rank-item {
  display: flex; align-items: center; gap: 16px;
  padding: 14px 16px; border-radius: var(--radius-md);
  background: var(--bg-card); border: 1px solid var(--border);
  transition: var(--transition);
}
.rank-item:hover { border-color: var(--border-strong); box-shadow: var(--shadow); transform: translateX(2px); }

.rank-num {
  width: 28px; height: 28px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 800; flex-shrink: 0;
  background: var(--bg-surface); color: var(--text-muted);
}
.rank-num.top3 { background: var(--primary); color: #fff; }

.rank-info { flex: 1; min-width: 0; }
.rank-name { font-size: 15px; font-weight: 700; color: var(--text); }
.rank-salon { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.rank-stats { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-muted); flex-shrink: 0; }
.rank-rating { color: var(--gold); font-weight: 700; }
.rank-divider { color: var(--border-strong); }

@media (max-width: 768px) {
  .hero-inner { grid-template-columns: 1fr; gap: 24px; }
  .hero-title { font-size: 28px; }
  .search-row { flex-direction: column; }
  .stylists-grid { grid-template-columns: 1fr; }
  .section-header { flex-direction: column; gap: 12px; }
}
</style>
