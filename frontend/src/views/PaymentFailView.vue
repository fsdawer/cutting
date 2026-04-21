<template>
  <main class="page">
    <div class="container" style="text-align:center;padding:80px 0">
      <div style="font-size:64px;margin-bottom:16px">⚠️</div>
      <h2 style="font-size:24px;font-weight:700">결제가 실패/취소되었습니다</h2>
      <p style="margin-top:12px;color:var(--color-text-secondary)">사유: {{ message }} ({{ code }})</p>
      <p style="margin-top:8px;color:var(--color-text-muted);font-size:13px">예약이 자동으로 취소되었습니다.</p>
      <button class="btn btn-primary btn-lg" style="margin-top:32px" @click="$router.push('/')">홈으로 이동</button>
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
  // 결제 실패/취소 시 PENDING 상태의 결제·예약을 즉시 취소
  if (orderId) {
    try {
      await paymentApi.cancelPending(orderId)
    } catch (e) {
      // 이미 만료된 경우 등 무시 (스케줄러가 10분 내 처리)
      console.warn('즉시 취소 실패(스케줄러가 처리 예정):', e.message)
    }
  }
})
</script>
