<template>
  <div class="stylist-card-wrap">
    <RouterLink :to="`/stylist/${stylist.id}`" class="stylist-card">
      <div class="card-img-wrap">
        <img
          :src="stylist.profileImg || `https://i.pravatar.cc/200?u=${stylist.id}`"
          :alt="stylist.name"
          class="card-img"
        />
        <span v-if="stylist.todayAvailable" class="avail-dot">빈자리</span>
      </div>
      <div class="card-body">
        <div class="card-top">
          <p class="card-salon">{{ stylist.salonName || '미용실' }}</p>
          <h3 class="card-name">{{ stylist.name }}</h3>
          <p class="card-location">{{ stylist.location }}</p>
        </div>
        <div class="card-meta">
          <div class="card-rating">
            <span class="star-icon">★</span>
            <span class="rating-val">{{ stylist.rating?.toFixed(1) ?? '신규' }}</span>
            <span class="rating-cnt" v-if="stylist.reviewCount">({{ stylist.reviewCount }})</span>
          </div>
          <span class="card-exp">경력 {{ stylist.experience }}년</span>
        </div>
        <div class="card-footer">
          <span class="review-hint">리뷰 {{ stylist.reviewCount ?? 0 }}개</span>
          <span class="book-cta">예약하기 →</span>
        </div>
      </div>
    </RouterLink>

    <!-- 찜하기 버튼 (RouterLink 밖에 absolute 배치) -->
    <button
      v-if="auth.isLoggedIn"
      class="heart-btn"
      :class="{ active: favorited }"
      :title="favorited ? '찜 취소' : '찜하기'"
      @click="toggle"
    >
      {{ favorited ? '♥' : '♡' }}
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { favoriteApi } from '@/api/favorite'

const props = defineProps({
  stylist: Object,
  favorited: { type: Boolean, default: false },
})

const auth = useAuthStore()
const favorited = ref(props.favorited)

async function toggle() {
  const prev = favorited.value
  favorited.value = !favorited.value  // 낙관적 업데이트
  try {
    await favoriteApi.toggle(props.stylist.id)
  } catch {
    favorited.value = prev            // 실패 시 되돌리기
  }
}
</script>

<style scoped>
.stylist-card-wrap { position: relative; }

.stylist-card {
  display: flex;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: var(--transition);
  box-shadow: var(--shadow);
}
.stylist-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--border-strong);
  transform: translateY(-2px);
}

.card-img-wrap { width: 112px; flex-shrink: 0; background: var(--bg-surface); position: relative; }
.card-img { width: 100%; height: 100%; object-fit: cover; min-height: 112px; }

.avail-dot {
  position: absolute; top: 8px; left: 8px;
  background: var(--success); color: #fff;
  font-size: 10px; font-weight: 700;
  padding: 2px 6px; border-radius: var(--radius-full);
}

.card-body {
  flex: 1; padding: 14px 16px;
  display: flex; flex-direction: column; justify-content: space-between; min-width: 0;
}
.card-top { margin-bottom: 8px; }
.card-salon { font-size: 11px; font-weight: 700; color: var(--accent); letter-spacing: 0.03em; margin-bottom: 2px; text-transform: uppercase; }
.card-name { font-size: 16px; font-weight: 700; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card-location { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.card-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.card-rating { display: flex; align-items: center; gap: 3px; }
.star-icon { color: var(--gold); font-size: 13px; }
.rating-val { font-size: 13px; font-weight: 700; color: var(--text); }
.rating-cnt { font-size: 12px; color: var(--text-muted); }
.card-exp {
  font-size: 12px; color: var(--text-muted);
  background: var(--bg-surface); padding: 2px 8px;
  border-radius: var(--radius-full); border: 1px solid var(--border);
}

.card-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 10px; border-top: 1px solid var(--border);
}
.review-hint { font-size: 12px; color: var(--text-muted); }
.book-cta { font-size: 13px; font-weight: 700; color: var(--primary); }

/* 찜하기 하트 버튼 */
.heart-btn {
  position: absolute; top: 8px; right: 8px;
  width: 32px; height: 32px; border-radius: 50%;
  background: rgba(255,255,255,0.92); backdrop-filter: blur(4px);
  border: 1px solid var(--border);
  font-size: 17px; line-height: 1;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; color: var(--text-muted);
  transition: transform 0.15s, color 0.15s, background 0.15s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.heart-btn:hover { transform: scale(1.15); }
.heart-btn.active { color: #e53e3e; background: #fff0f0; border-color: #f9c6c6; }
</style>
