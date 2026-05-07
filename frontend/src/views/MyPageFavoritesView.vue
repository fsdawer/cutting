<template>
  <main class="page">
    <div class="container">
      <div class="page-header">
        <button class="back-btn" @click="$router.back()">‹</button>
        <h1 class="page-title">관심 디자이너</h1>
      </div>

      <div v-if="loading" class="state-center"><div class="spinner"></div></div>
      <div v-else-if="favorites.length === 0" class="state-center">
        <p class="state-text">관심 디자이너가 없습니다.</p>
        <RouterLink to="/" class="btn btn-primary btn-sm">디자이너 찾기</RouterLink>
      </div>
      <div v-else class="fav-list">
        <RouterLink
          v-for="s in favorites" :key="s.id"
          :to="`/stylist/${s.id}`"
          class="fav-card card"
        >
          <div class="fav-avatar">
            <img v-if="s.profileImg" :src="s.profileImg" alt="" />
            <span v-else>{{ s.name?.[0] }}</span>
          </div>
          <div class="fav-info">
            <p class="fav-name">{{ s.name }}</p>
            <p class="fav-salon">{{ s.salonName || '프리랜서' }}</p>
            <div class="fav-meta">
              <span class="fav-rating">★ {{ s.rating ?? '0.0' }}</span>
              <span class="fav-review">리뷰 {{ s.reviewCount ?? 0 }}</span>
            </div>
          </div>
          <span class="fav-arrow">›</span>
        </RouterLink>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { favoriteApi } from '@/api/favorite'

const favorites = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try { favorites.value = (await favoriteApi.getMyFavorites()).data || [] }
  catch { favorites.value = [] }
  finally { loading.value = false }
})
</script>

<style scoped>
.container { max-width: 480px; padding: 16px; }
.page-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.back-btn { font-size: 24px; background: none; border: none; cursor: pointer; color: var(--text); padding: 0 4px; line-height: 1; }
.page-title { font-size: 20px; font-weight: 800; }

.fav-list { display: flex; flex-direction: column; gap: 10px; }
.fav-card {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px; text-decoration: none; color: var(--text);
  transition: background 0.15s;
}
.fav-card:hover { background: var(--bg-surface); }
.fav-avatar {
  width: 48px; height: 48px; border-radius: 50%; flex-shrink: 0;
  background: var(--primary); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; overflow: hidden;
}
.fav-avatar img { width: 100%; height: 100%; object-fit: cover; }
.fav-info { flex: 1; min-width: 0; }
.fav-name { font-size: 15px; font-weight: 700; margin-bottom: 2px; }
.fav-salon { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }
.fav-meta { display: flex; gap: 10px; }
.fav-rating { font-size: 12px; color: var(--gold, #f59e0b); font-weight: 600; }
.fav-review { font-size: 12px; color: var(--text-muted); }
.fav-arrow { font-size: 20px; color: var(--text-muted); }

.state-center { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 60px 0; }
.state-text { color: var(--text-muted); font-size: 15px; }
</style>
