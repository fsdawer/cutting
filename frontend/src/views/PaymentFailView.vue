<template>
  <main class="page">
    <div class="container" style="text-align:center;padding:80px 0">
      <div style="font-size:64px;margin-bottom:16px">⚠️</div>
      <h2 style="font-size:24px;font-weight:700">결제가 실패/취소되었습니다</h2>
      <p style="margin-top:12px;color:var(--color-text-secondary)">사유: {{ message }} ({{ code }})</p>
      <button class="btn btn-primary btn-lg" style="margin-top:32px" @click="$router.push('/')">홈으로 이동</button>
    </div>
  </main>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
const { code, message } = route.query
</script>
