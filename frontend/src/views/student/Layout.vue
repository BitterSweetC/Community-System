<template>
  <div class="layout">
    <el-header class="header" :class="{ 'scrolled': isScrolled }">
      <div class="header-content premium-container">
        <div class="logo">
          <span class="logo-text">校园社团</span>
        </div>
        <div class="nav">
          <router-link to="/home" class="nav-item">探索</router-link>
          <template v-if="authStore.token">
              <router-link to="/user/create-club" class="nav-item">创建社团</router-link>
              <router-link to="/user/notifications" class="nav-icon">
                <el-badge :value="notificationStore.unreadCount" :hidden="notificationStore.unreadCount === 0" class="notification-badge">
                   <span>🔔</span>
                </el-badge>
              </router-link>
              <div class="user-profile">
                <el-dropdown trigger="hover" @command="handleCommand">
                  <div class="user-info-link dropdown-trigger">
                    <el-avatar 
                      :size="32" 
                      :src="authStore.user.avatarUrl" 
                      class="user-avatar"
                    >
                      {{ (authStore.user.realName || authStore.user.username || 'U').charAt(0).toUpperCase() }}
                    </el-avatar>
                    <span class="user-info">{{ authStore.user.realName || authStore.user.username }}</span>
                    <el-icon class="el-icon--right"><arrow-down /></el-icon>
                  </div>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                      <el-dropdown-item command="messages">我的消息</el-dropdown-item>
                      <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
          </template>
          <template v-else>
              <router-link to="/login" class="nav-item">登录</router-link>
              <router-link to="/register" class="nav-btn">加入我们</router-link>
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
    <ChatWidget />
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { useRouter } from 'vue-router'
import { ref, onMounted, onUnmounted, watch } from 'vue'
import ChatWidget from '@/components/ChatWidget.vue'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const router = useRouter()
const isScrolled = ref(false)

// Watch for token changes to re-fetch when user logs in
watch(() => authStore.token, (newToken) => {
  if (newToken) {
    notificationStore.fetchUnreadCount()
  } else {
    notificationStore.clearCount()
  }
})

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'messages':
      router.push('/user/notifications')
      break
    case 'logout':
      handleLogout()
      break
  }
}

const handleScroll = () => {
  isScrolled.value = window.scrollY > 50
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  if (authStore.token) {
    notificationStore.fetchUnreadCount()
  }
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
  position: relative; /* Ensure child absolute positioning works relative to layout */
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
  display: flex;
  align-items: center;
  cursor: pointer;
  outline: none;
}

.user-info-link:hover {
  color: var(--color-primary);
}

.el-dropdown-link:focus {
  outline: none;
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
