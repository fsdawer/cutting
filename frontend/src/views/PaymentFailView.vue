<template>
  <main class="page">
    <div class="container">
      <div class="fail-wrap">
        <div class="fail-icon">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="24" cy="24" r="22" stroke="currentColor" stroke-width="2"/>
            <path d="M16 16l16 16M32 16L16 32" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
          </svg>
        </div>
        <h1 class="fail-title">결제 실패</h1>
        <p class="fail-sub">결제가 처리되지 않았습니다</p>

        <div class="detail-card" v-if="errorMessage || errorCode">
          <div class="detail-row" v-if="errorCode">
            <span class="detail-label">오류 코드</span>
            <span class="detail-val mono">{{ errorCode }}</span>
          </div>
          <div class="detail-row" v-if="errorMessage">
            <span class="detail-label">오류 메시지</span>
            <span class="detail-val">{{ errorMessage }}</span>
          </div>
        </div>

        <p class="fail-desc">예약은 유지됩니다. 결제 페이지로 돌아가 다시 시도하거나, 마이페이지에서 취소할 수 있습니다.</p>

        <div class="fail-actions">
          <button class="btn btn-primary" @click="$router.back()">다시 시도</button>
          <RouterLink to="/mypage" class="btn btn-ghost">마이페이지</RouterLink>
          <RouterLink to="/" class="btn btn-ghost">홈으로</RouterLink>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { paymentApi } from '@/api/payment'

const route = useRoute()
const errorCode = ref(route.query.code || '')
const errorMessage = ref(route.query.message || '')
const orderId = ref(route.query.orderId || '')

onMounted(async () => {
  if (orderId.value) {
    try { await paymentApi.cancelPending(orderId.value) } catch {}
  }
})
</script>

<style scoped>
.fail-wrap {
  max-width: 480px;
  margin: 0 auto;
  padding: 48px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}
.fail-icon {
  width: 72px; height: 72px;
  color: var(--danger);
  animation: pop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes pop {
  from { transform: scale(0); opacity: 0; }
  to   { transform: scale(1); opacity: 1; }
}

.fail-title { font-size: 28px; font-weight: 800; letter-spacing: -0.03em; text-align: center; }
.fail-sub { font-size: 15px; color: var(--text-muted); text-align: center; margin-top: -8px; }

.detail-card {
  width: 100%;
  background: var(--danger-light);
  border: 1px solid #fca5a5;
  border-radius: var(--radius-lg);
  padding: 16px 20px;
}
.detail-row { display: flex; justify-content: space-between; align-items: flex-start; padding: 8px 0; border-bottom: 1px solid #fca5a5; gap: 12px; }
.detail-row:last-child { border-bottom: none; }
.detail-label { font-size: 13px; color: var(--danger); font-weight: 600; flex-shrink: 0; }
.detail-val { font-size: 13px; color: var(--danger); text-align: right; }
.detail-val.mono { font-family: 'SF Mono', 'Fira Code', monospace; }

.fail-desc { font-size: 14px; color: var(--text-muted); text-align: center; line-height: 1.7; max-width: 360px; }
.fail-actions { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; }
</style>
