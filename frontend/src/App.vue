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
import Navbar from '@/components/Navbar.vue'
import ChatWidget from '@/components/ChatWidget.vue'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()
</script>

<style scoped>
#app-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
