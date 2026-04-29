<template>
  <main class="page">
    <div class="container state-center">
      <div class="result-box">
        <div class="result-icon fail">✗</div>
        <h2 class="result-title">결제가 실패/취소되었습니다</h2>
        <p class="result-desc">{{ message }} ({{ code }})</p>
        <p class="result-hint">예약이 자동으로 취소되었습니다.</p>
        <div class="result-actions">
          <button class="btn btn-ghost" @click="$router.back()">다시 시도</button>
          <button class="btn btn-primary" @click="$router.push('/')">홈으로 이동</button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { paymentApi } from '@/api/payment'

const route = useRoute()
const { code, message, orderId } = route.query

onMounted(async () => {
  if (orderId) {
    try {
      await paymentApi.cancelPending(orderId)
    } catch (e) {
      console.warn('즉시 취소 실패(스케줄러가 처리 예정):', e.message)
    }
  }
})
</script>

<style scoped>
.state-center { text-align: center; padding: 100px 0; display: flex; flex-direction: column; align-items: center; }
.result-box { display: flex; flex-direction: column; align-items: center; gap: 14px; }
.result-icon {
  width: 72px; height: 72px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 32px; font-weight: 700;
}
.result-icon.fail { background: #FFF0EE; color: var(--red); }
.result-title  { font-size: 20px; font-weight: 700; }
.result-desc   { font-size: 14px; color: var(--text-sub); }
.result-hint   { font-size: 13px; color: var(--text-muted); }
.result-actions { display: flex; gap: 10px; margin-top: 8px; }
</style>
