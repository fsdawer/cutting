<template>
  <div id="app-wrapper">
    <Navbar />
    <RouterView v-slot="{ Component, route }">
      <Transition name="fade" mode="out-in">
        <component :is="Component" :key="route.path" />
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
