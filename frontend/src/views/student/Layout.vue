<template>
  <div class="layout">
    <el-header class="header" :class="{ 'scrolled': isScrolled }">
      <div class="header-content premium-container">
        <div class="logo">
          <span class="logo-text">COMMUNITY</span>
        </div>
        <div class="nav">
          <router-link to="/home" class="nav-item">Explore</router-link>
          <template v-if="authStore.token">
              <router-link to="/user/create-club" class="nav-item">Start a Club</router-link>
              <router-link to="/user/notifications" class="nav-icon">
                <el-badge is-dot class="notification-badge">
                   <span>🔔</span>
                </el-badge>
              </router-link>
              <div class="user-profile">
                <router-link to="/user/profile" class="user-info-link">
                  <el-avatar 
                    :size="32" 
                    :src="authStore.user.avatarUrl" 
                    class="user-avatar"
                  >
                    {{ (authStore.user.realName || authStore.user.username || 'U').charAt(0).toUpperCase() }}
                  </el-avatar>
                  <span class="user-info">{{ authStore.user.realName || authStore.user.username }}</span>
                </router-link>
                <el-button class="logout-btn" link @click="handleLogout">LOGOUT</el-button>
              </div>
          </template>
          <template v-else>
              <router-link to="/login" class="nav-item">Login</router-link>
              <router-link to="/register" class="nav-btn">JOIN US</router-link>
          </template>
        </div>
      </div>
    </el-header>
    <el-main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ref, onMounted, onUnmounted } from 'vue'

const authStore = useAuthStore()
const router = useRouter()
const isScrolled = ref(false)

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

const handleScroll = () => {
  isScrolled.value = window.scrollY > 50
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: transparent;
  color: white; /* Default to white for hero overlay */
  padding: 0;
  height: 80px !important;
  position: fixed;
  top: 0;
  width: 100%;
  z-index: 100;
  transition: all 0.3s ease;
}

.header.scrolled {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  color: var(--color-text);
  box-shadow: var(--shadow-sm);
  height: 70px !important;
}

.header-content {
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo-text {
  font-family: var(--font-heading);
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.05em;
  text-transform: uppercase;
}

.nav {
  display: flex;
  align-items: center;
  gap: 2.5rem;
}

.nav-item {
  color: inherit;
  text-decoration: none;
  font-weight: 500;
  font-size: 0.9rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  position: relative;
  transition: opacity 0.2s;
}

.nav-item:hover {
  opacity: 0.7;
}

.nav-btn {
  background-color: white;
  color: var(--color-text);
  padding: 10px 24px;
  border-radius: var(--radius-sm);
  text-decoration: none;
  font-weight: 700;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  transition: transform 0.2s, background-color 0.2s;
}

.header.scrolled .nav-btn {
  background-color: var(--color-primary);
  color: white;
}

.nav-btn:hover {
  transform: translateY(-2px);
}

.nav-icon {
  text-decoration: none;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-info {
  font-weight: 600;
  font-size: 0.9rem;
}

.user-info-link {
  text-decoration: none;
  color: inherit;
  transition: color 0.2s;
}

.user-info-link:hover {
  color: var(--color-primary);
}

.admin-link {
  margin-left: 1rem;
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  text-decoration: none;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.admin-link:hover {
  background-color: var(--color-primary);
  color: white;
}

.logout-btn {
  color: inherit !important;
  font-size: 0.75rem !important;
  padding: 0 !important;
}

.main-content {
  padding: 0;
  flex: 1;
}

/* Page Transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
