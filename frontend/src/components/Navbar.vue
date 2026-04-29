<template>
  <nav class="navbar">
    <div class="container nav-inner">
      <RouterLink to="/" class="nav-logo">
        <span class="logo-mark">✂</span>
        <span class="logo-text">CutIng</span>
      </RouterLink>

      <ul class="nav-links">
        <li><RouterLink to="/" class="nav-link">홈</RouterLink></li>
        <li v-if="auth.isStylist">
          <RouterLink to="/stylist/reservations" class="nav-link">예약 관리</RouterLink>
        </li>
        <li v-if="auth.isStylist">
          <RouterLink to="/stylist/manage" class="nav-link">프로필 관리</RouterLink>
        </li>
        <li v-if="auth.isLoggedIn">
          <RouterLink to="/mypage" class="nav-link">마이페이지</RouterLink>
        </li>
      </ul>

      <div class="nav-actions">
        <template v-if="!auth.isLoggedIn">
          <RouterLink to="/login" class="btn btn-ghost btn-sm">로그인</RouterLink>
          <RouterLink to="/register" class="btn btn-primary btn-sm">회원가입</RouterLink>
        </template>
        <template v-else>
          <span class="nav-user">{{ auth.user?.name }}</span>
          <button class="btn btn-ghost btn-sm" @click="handleLogout">로그아웃</button>
        </template>

        <button class="hamburger" @click="mobileOpen = !mobileOpen" aria-label="메뉴">
          <span :class="{ open: mobileOpen }"></span>
        </button>
      </div>
    </div>

    <Transition name="drawer">
      <div v-if="mobileOpen" class="mobile-drawer">
        <RouterLink to="/" class="drawer-item" @click="mobileOpen = false">홈</RouterLink>
        <RouterLink v-if="auth.isStylist" to="/stylist/reservations" class="drawer-item" @click="mobileOpen = false">예약 관리</RouterLink>
        <RouterLink v-if="auth.isStylist" to="/stylist/manage" class="drawer-item" @click="mobileOpen = false">프로필 관리</RouterLink>
        <RouterLink v-if="auth.isLoggedIn" to="/mypage" class="drawer-item" @click="mobileOpen = false">마이페이지</RouterLink>
        <template v-if="!auth.isLoggedIn">
          <RouterLink to="/login" class="drawer-item" @click="mobileOpen = false">로그인</RouterLink>
          <RouterLink to="/register" class="drawer-item drawer-item--primary" @click="mobileOpen = false">회원가입</RouterLink>
        </template>
        <template v-else>
          <button class="drawer-item drawer-item--danger" @click="handleLogout">로그아웃</button>
        </template>
      </div>
    </Transition>
  </nav>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()
const router = useRouter()
const mobileOpen = ref(false)

async function handleLogout() {
  await auth.logout()
  mobileOpen.value = false
  router.push('/')
}
</script>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 200;
  height: var(--navbar-h);
  background: #fff;
  border-bottom: 1px solid var(--border);
  box-shadow: 0 1px 0 rgba(0,0,0,0.04);
}

.nav-inner {
  height: var(--navbar-h);
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  text-decoration: none;
}
.logo-mark {
  font-size: 20px;
  color: var(--primary);
  line-height: 1;
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: -0.5px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 2px;
  list-style: none;
  flex: 1;
}
.nav-link {
  padding: 7px 14px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-sub);
  transition: var(--transition);
}
.nav-link:hover { color: var(--text); background: var(--bg); }
.nav-link.router-link-exact-active { color: var(--primary); font-weight: 600; }

.nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.nav-user {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

/* Hamburger */
.hamburger {
  display: none;
  width: 34px;
  height: 34px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0;
}
.hamburger span,
.hamburger span::before,
.hamburger span::after {
  display: block;
  width: 16px;
  height: 1.5px;
  background: var(--text);
  transition: var(--transition);
  position: relative;
}
.hamburger span::before,
.hamburger span::after {
  content: '';
  position: absolute;
}
.hamburger span::before { top: -5px; }
.hamburger span::after  { top: 5px; }
.hamburger span.open { background: transparent; }
.hamburger span.open::before { transform: rotate(45deg) translate(3.5px, 3.5px); }
.hamburger span.open::after  { transform: rotate(-45deg) translate(3.5px, -3.5px); }

/* Mobile drawer */
.mobile-drawer {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-bottom: 1px solid var(--border);
  padding: 8px 16px 16px;
  gap: 2px;
}
.drawer-item {
  display: block;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  font-size: 15px;
  font-weight: 500;
  color: var(--text-sub);
  background: transparent;
  border: none;
  text-align: left;
  transition: var(--transition);
  cursor: pointer;
  width: 100%;
}
.drawer-item:hover { background: var(--bg); color: var(--text); }
.drawer-item--primary { color: var(--primary); font-weight: 600; }
.drawer-item--danger  { color: var(--red); }

/* Drawer transition */
.drawer-enter-active, .drawer-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}
.drawer-enter-from, .drawer-leave-to {
  max-height: 0;
  opacity: 0;
}
.drawer-enter-to, .drawer-leave-from {
  max-height: 500px;
  opacity: 1;
}

@media (max-width: 768px) {
  .nav-links { display: none; }
  .nav-actions .btn { display: none; }
  .nav-user { display: none; }
  .hamburger { display: flex; }
}
</style>
