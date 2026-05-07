import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'

// 토스 결제 리다이렉트 후 브라우저 뒤로가기 시 bfcache로 복원된 경우
// onMounted가 재실행되지 않아 화면이 비는 문제를 방지
window.addEventListener('pageshow', (e) => {
  if (e.persisted) window.location.reload()
})

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
