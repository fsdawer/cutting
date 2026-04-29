<template>
  <main class="page">
    <!-- Hero Search -->
    <section class="hero">
      <div class="container">
        <h1 class="hero-title">내 근처 미용사 예약</h1>
        <p class="hero-sub">믿을 수 있는 스타일리스트를 바로 예약하세요</p>

        <div class="search-bar">
          <div class="search-input-wrap">
            <span class="search-icon">🔍</span>
            <input
              v-model="searchQuery"
              class="search-input"
              placeholder="미용사 이름, 미용실 검색"
              @keyup.enter="search"
            />
          </div>
          <div class="search-input-wrap">
            <span class="search-icon">📍</span>
            <input
              v-model="locationQuery"
              class="search-input"
              placeholder="지역"
              @keyup.enter="search"
            />
          </div>
          <button class="btn btn-primary search-btn" @click="search">검색</button>
        </div>

        <div class="quick-tags">
          <button
            v-for="tag in popularTags"
            :key="tag"
            class="tag-chip"
            @click="searchQuery = tag; search()"
          >{{ tag }}</button>
        </div>
      </div>
    </section>

    <div class="container content-area">
      <!-- Filter Bar -->
      <div class="filter-bar">
        <div class="filter-chips">
          <button
            v-for="f in filters"
            :key="f.value"
            class="f-chip"
            :class="{ active: activeFilter === f.value }"
            @click="activeFilter = f.value"
          >{{ f.label }}</button>
        </div>
        <select v-model="sortBy" class="sort-select">
          <option value="rating">평점순</option>
          <option value="review">리뷰순</option>
        </select>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="state-center">
        <div class="spinner"></div>
        <p class="state-text">스타일리스트를 불러오는 중...</p>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="state-center">
        <p class="state-icon">⚠️</p>
        <p class="state-text">{{ error }}</p>
        <button class="btn btn-ghost" @click="fetchStylists()">다시 시도</button>
      </div>

      <!-- Grid -->
      <div v-else-if="filteredStylists.length" class="stylists-grid">
        <StylistCard
          v-for="stylist in filteredStylists"
          :key="stylist.id"
          :stylist="stylist"
        />
      </div>

      <!-- Empty -->
      <div v-else class="state-center">
        <p class="state-icon">🔍</p>
        <p class="state-text">검색 결과가 없습니다</p>
        <button class="btn btn-ghost" @click="resetSearch">필터 초기화</button>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import StylistCard from '@/components/StylistCard.vue'
import { stylistApi } from '@/api/stylist'

const searchQuery = ref('')
const locationQuery = ref('')
const activeFilter = ref('all')
const sortBy = ref('rating')
const loading = ref(false)
const error = ref('')
const stylists = ref([])

const popularTags = ['헤어컷', '펌', '염색', '드라이', '케라틴']
const filters = [
  { label: '전체', value: 'all' },
  { label: '헤어컷', value: '헤어컷' },
  { label: '펌', value: '펌' },
  { label: '염색', value: '염색' },
  { label: '케라틴', value: '케라틴' },
  { label: '드라이', value: '드라이' },
]

onMounted(() => fetchStylists())

async function fetchStylists(keyword = '', location = '') {
  loading.value = true
  error.value = ''
  try {
    const params = {}
    if (keyword) params.keyword = keyword
    if (location) params.location = location
    const res = await stylistApi.getStylists(params)
    stylists.value = res.data
  } catch (e) {
    error.value = '스타일리스트 목록을 불러오지 못했습니다.'
    console.error(e)
  } finally {
    loading.value = false
  }
}

const filteredStylists = computed(() => {
  let list = stylists.value
  if (activeFilter.value !== 'all') {
    list = list.filter(s => s.services?.some(svc => svc.name?.includes(activeFilter.value)))
  }
  if (sortBy.value === 'rating') return [...list].sort((a, b) => b.rating - a.rating)
  if (sortBy.value === 'review') return [...list].sort((a, b) => b.reviewCount - a.reviewCount)
  return list
})

function search() { fetchStylists(searchQuery.value.trim(), locationQuery.value.trim()) }
function resetSearch() { searchQuery.value = ''; locationQuery.value = ''; activeFilter.value = 'all'; fetchStylists() }
</script>

<style scoped>
/* Hero */
.hero {
  background: #fff;
  border-bottom: 1px solid var(--border);
  padding: 40px 0 28px;
  margin-bottom: 24px;
}
.hero-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 6px;
}
.hero-sub {
  font-size: 14px;
  color: var(--text-sub);
  margin-bottom: 24px;
}

.search-bar {
  display: flex;
  gap: 10px;
  max-width: 680px;
  margin-bottom: 16px;
}
.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: var(--bg);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  transition: var(--transition);
}
.search-input-wrap:focus-within {
  border-color: var(--primary);
  background: #fff;
}
.search-icon { font-size: 15px; margin-right: 8px; }
.search-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 11px 0;
  font-size: 14px;
  color: var(--text);
}
.search-input::placeholder { color: var(--text-muted); }
.search-btn { flex-shrink: 0; padding: 11px 24px; }

.quick-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tag-chip {
  padding: 5px 13px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border);
  background: #fff;
  color: var(--text-sub);
  font-size: 13px;
  transition: var(--transition);
}
.tag-chip:hover { border-color: var(--primary); color: var(--primary); }

/* Content */
.content-area { padding-bottom: 48px; }

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.filter-chips { display: flex; gap: 6px; flex-wrap: wrap; }
.f-chip {
  padding: 6px 16px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border);
  background: #fff;
  color: var(--text-sub);
  font-size: 13px;
  font-weight: 500;
  transition: var(--transition);
}
.f-chip:hover { border-color: var(--primary); color: var(--primary); }
.f-chip.active { background: var(--primary); border-color: var(--primary); color: #fff; font-weight: 600; }

.sort-select {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 7px 10px;
  font-size: 13px;
  background: #fff;
  color: var(--text-sub);
  cursor: pointer;
}

/* Grid — horizontal cards */
.stylists-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

/* States */
.state-center { text-align: center; padding: 80px 0; display: flex; flex-direction: column; align-items: center; gap: 12px; }
.state-icon { font-size: 44px; }
.state-text { color: var(--text-sub); font-size: 15px; }

@media (max-width: 768px) {
  .hero { padding: 28px 0 20px; }
  .search-bar { flex-direction: column; }
  .stylists-grid { grid-template-columns: 1fr; }
  .filter-bar { flex-direction: column; align-items: flex-start; }
}
</style>
