<template>
  <main class="page">
    <div class="container">
      <div class="page-header">
        <div>
          <h1 class="section-title">지역별 랭킹</h1>
          <p class="section-subtitle">예약 수, 평점, 리뷰를 종합한 실시간 스타일리스트 랭킹</p>
        </div>
      </div>

      <!-- 지역 탭 -->
      <div class="district-grid">
        <button
          v-for="d in districts"
          :key="d"
          class="d-btn"
          :class="{ active: activeDistrict === d }"
          @click="selectDistrict(d)"
        >{{ d }}</button>
      </div>

      <!-- 로딩 -->
      <div v-if="loading" class="state-center">
        <div class="spinner"></div>
        <p class="state-text">랭킹을 불러오는 중...</p>
      </div>

      <!-- 결과 없음 -->
      <div v-else-if="rankList.length === 0" class="state-center">
        <p class="state-text">해당 지역의 랭킹 데이터가 없습니다.</p>
      </div>

      <!-- 랭킹 리스트 -->
      <div v-else class="rank-list">
        <!-- Top 3 강조 카드 -->
        <div class="podium" v-if="rankList.length >= 1">
          <RouterLink
            v-for="(item, idx) in rankList.slice(0, Math.min(3, rankList.length))"
            :key="item.stylistId"
            :to="`/stylist/${item.stylistId}`"
            class="podium-card"
            :class="[`rank-${idx + 1}`]"
          >
            <div class="podium-rank">{{ idx + 1 }}</div>
            <div class="podium-avatar">
              <img :src="`https://i.pravatar.cc/80?u=${item.stylistId}`" :alt="item.stylistName" />
            </div>
            <p class="podium-name">{{ item.stylistName }}</p>
            <p class="podium-salon">{{ item.salonName }}</p>
            <div class="podium-stats">
              <span class="pstat">★ {{ item.avgRating?.toFixed(1) }}</span>
              <span class="pstat-divider">|</span>
              <span class="pstat">리뷰 {{ item.reviewCount }}</span>
            </div>
          </RouterLink>
        </div>

        <!-- 4위 이하 -->
        <div class="rank-rows" v-if="rankList.length > 3">
          <RouterLink
            v-for="(item, idx) in rankList.slice(3)"
            :key="item.stylistId"
            :to="`/stylist/${item.stylistId}`"
            class="rank-row"
          >
            <span class="rrow-num">{{ idx + 4 }}</span>
            <div class="rrow-avatar">
              <img :src="`https://i.pravatar.cc/40?u=${item.stylistId}`" :alt="item.stylistName" />
            </div>
            <div class="rrow-info">
              <p class="rrow-name">{{ item.stylistName }}</p>
              <p class="rrow-salon">{{ item.salonName }} · {{ item.district }}</p>
            </div>
            <div class="rrow-stats">
              <div class="rrow-stat">
                <span class="stat-label">평점</span>
                <span class="stat-val gold">★ {{ item.avgRating?.toFixed(1) }}</span>
              </div>
              <div class="rrow-stat">
                <span class="stat-label">리뷰</span>
                <span class="stat-val">{{ item.reviewCount }}</span>
              </div>
              <div class="rrow-stat">
                <span class="stat-label">예약</span>
                <span class="stat-val">{{ item.recentBookings ?? '-' }}</span>
              </div>
            </div>
            <span class="rrow-arrow">→</span>
          </RouterLink>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { rankingApi } from '@/api/ranking'

const districts = ['강남구', '서초구', '마포구', '용산구', '종로구', '송파구', '성동구', '광진구', '영등포구', '동작구']
const activeDistrict = ref('강남구')
const loading  = ref(false)
const rankList = ref([])

async function selectDistrict(d) {
  activeDistrict.value = d
  await loadRanking()
}

async function loadRanking() {
  loading.value = true
  try {
    const res = await rankingApi.getRanking(activeDistrict.value)
    rankList.value = res.data
  } catch { rankList.value = [] }
  finally { loading.value = false }
}

onMounted(loadRanking)
</script>

<style scoped>
.page-header { margin-bottom: 28px; }

/* District buttons */
.district-grid { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 32px; }
.d-btn {
  padding: 7px 16px; border-radius: var(--radius-full);
  border: 1px solid var(--border); background: var(--bg-card);
  font-size: 13px; font-weight: 500; color: var(--text-muted);
  cursor: pointer; transition: var(--transition);
}
.d-btn:hover { border-color: var(--border-strong); color: var(--text-sub); }
.d-btn.active { border-color: var(--primary); background: var(--primary); color: #fff; }

/* Podium */
.podium {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}
.podium-card {
  display: flex; flex-direction: column; align-items: center;
  padding: 24px 16px; border-radius: var(--radius-lg);
  background: var(--bg-card); border: 1px solid var(--border);
  transition: var(--transition); gap: 6px; text-align: center;
}
.podium-card:hover { box-shadow: var(--shadow-md); transform: translateY(-3px); }

.rank-1 { border-color: var(--gold); box-shadow: 0 0 0 1px var(--gold-light); }
.rank-2 { border-color: var(--border-strong); }
.rank-3 { border-color: #d97706; }

.podium-rank {
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 800;
}
.rank-1 .podium-rank { background: var(--gold); color: #fff; }
.rank-2 .podium-rank { background: #9ca3af; color: #fff; }
.rank-3 .podium-rank { background: #d97706; color: #fff; }

.podium-avatar { width: 64px; height: 64px; border-radius: 50%; overflow: hidden; border: 2px solid var(--border); }
.podium-avatar img { width: 100%; height: 100%; object-fit: cover; }
.podium-name { font-size: 15px; font-weight: 700; color: var(--text); }
.podium-salon { font-size: 12px; color: var(--text-muted); }
.podium-stats { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.pstat { font-weight: 600; color: var(--text-sub); }
.pstat-divider { color: var(--border-strong); }

/* Rank rows */
.rank-rows { display: flex; flex-direction: column; gap: 4px; }
.rank-row {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px; border-radius: var(--radius-md);
  background: var(--bg-card); border: 1px solid var(--border);
  transition: var(--transition);
}
.rank-row:hover { border-color: var(--border-strong); box-shadow: var(--shadow); }

.rrow-num { width: 28px; text-align: center; font-size: 15px; font-weight: 800; color: var(--text-muted); flex-shrink: 0; }
.rrow-avatar { width: 40px; height: 40px; border-radius: 50%; overflow: hidden; flex-shrink: 0; }
.rrow-avatar img { width: 100%; height: 100%; object-fit: cover; }
.rrow-info { flex: 1; min-width: 0; }
.rrow-name { font-size: 15px; font-weight: 700; color: var(--text); }
.rrow-salon { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.rrow-stats { display: flex; gap: 16px; flex-shrink: 0; }
.rrow-stat { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.stat-label { font-size: 10px; color: var(--text-muted); font-weight: 600; text-transform: uppercase; letter-spacing: 0.04em; }
.stat-val { font-size: 14px; font-weight: 700; color: var(--text); }
.stat-val.gold { color: var(--gold); }

.rrow-arrow { font-size: 14px; color: var(--text-muted); flex-shrink: 0; }

@media (max-width: 640px) {
  .podium { grid-template-columns: 1fr; }
  .rrow-stats { display: none; }
}
</style>
