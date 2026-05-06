<template>
  <RouterLink :to="`/stylist/${stylist.id}`" class="stylist-card">
    <div class="card-img-wrap">
      <img
        :src="stylist.profileImg || `https://i.pravatar.cc/200?u=${stylist.id}`"
        :alt="stylist.name"
        class="card-img"
      />
      <span v-if="stylist.todayAvailable" class="avail-dot" title="오늘 빈자리 있음">빈자리</span>
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
</template>

<script setup>
defineProps({ stylist: Object })
</script>

<style scoped>
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
  letter-spacing: 0.02em;
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
</style>
