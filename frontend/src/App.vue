<template>
  <div id="app-wrapper">
    <Navbar />
    <RouterView v-slot="{ Component, route }">
      <Transition name="fade" mode="out-in">
        <!-- keep-alive: 보여준 페이지 컬러를 메모리에 유지 → 뒤로가기 시 re-mount 없이 즉시 복원 -->
        <keep-alive :include="['HomeView', 'RankingView']" :max="10">
          <component :is="Component" :key="route.path" />
        </keep-alive>
      </Transition>
    </RouterView>
    <ChatWidget v-if="auth.isLoggedIn" />
  </div>
</template>

<script setup>
import { watch } from 'vue'
import Navbar from '@/components/Navbar.vue'
import ChatWidget from '@/components/ChatWidget.vue'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationStore } from '@/stores/notificationStore'

const auth = useAuthStore()
const notif = useNotificationStore()

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn && auth.user?.id && auth.token) {
      notif.connect(auth.user.id, auth.token)
    } else {
      notif.disconnect()
      notif.clearAll()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
#app-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
