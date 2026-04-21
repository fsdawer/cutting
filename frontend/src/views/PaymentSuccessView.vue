<template>
  <main class="page">
    <div class="container" style="text-align:center;padding:80px 0">
      <div v-if="loading">
        <div class="spinner"></div>
        <h2 style="margin-top:20px">결제 검증 중입니다...</h2>
        <p style="color:var(--color-text-muted);margin-top:8px">창을 닫지 마세요.</p>
      </div>
      <div v-else-if="error" class="error-state">
        <h2 style="color:var(--color-danger)">결제 승인 실패</h2>
        <p style="margin-top:12px;color:var(--color-text-muted)">{{ error }}</p>
        <button class="btn btn-primary" style="margin-top:24px" @click="$router.push('/')">홈으로 이동</button>
      </div>
      <div v-else class="success-state">
        <div style="font-size:64px;margin-bottom:16px">🎉</div>
        <h2 style="font-size:24px;font-weight:700">결제가 성공적으로 완료되었습니다!</h2>
        <p style="margin-top:12px;color:var(--color-text-secondary)">예약 확인 및 결제 내역은 마이페이지에서 확인 가능합니다.</p>
        <button class="btn btn-primary btn-lg" style="margin-top:32px" @click="$router.push('/mypage')">마이페이지로 이동</button>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { paymentApi } from '@/api/payment'

const route = useRoute()
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  const { paymentKey, orderId, amount } = route.query
  if (!paymentKey || !orderId || !amount) {
    error.value = '결제 정보가 유효하지 않습니다.'
    loading.value = false
    return
  }

  try {
    // 백엔드로 승인 요청
    await paymentApi.confirm({ paymentKey, orderId, amount: Number(amount) })
  } catch (e) {
    error.value = e.response?.data?.message || '결제 승인 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
})
</script>
