<template>
  <div class="layout">
    <el-header class="header" :class="{ scrolled: isScrolled || forceSolidHeader }">
      <div class="header-content premium-container">
        <div class="logo">
          <span class="logo-text">校园社团</span>
        </div>

        <div class="nav">
          <router-link to="/home" class="nav-item">探索</router-link>

          <template v-if="authStore.token">
            <router-link to="/user/create-club" class="nav-item">新建社团</router-link>

            <router-link to="/user/notifications" class="nav-icon" aria-label="消息通知">
              <el-badge
                :value="notificationStore.unreadCount"
                :hidden="notificationStore.unreadCount === 0"
                class="notification-badge"
              >
                <span :class="{ 'shake-animation': isShaking }" class="bell-wrap">
                  <el-icon><Bell /></el-icon>
                </span>
              </el-badge>
            </router-link>

            <div class="user-profile">
              <el-dropdown trigger="hover" @command="handleCommand">
                <div class="user-info-link dropdown-trigger">
                  <el-avatar :size="32" :src="authStore.user.avatarUrl" class="user-avatar">
                    {{ (authStore.user.realName || authStore.user.username || 'U').charAt(0).toUpperCase() }}
                  </el-avatar>
                  <span class="user-info">{{ authStore.user.realName || authStore.user.username }}</span>
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                    <el-dropdown-item command="my-clubs">我的社团</el-dropdown-item>
                    <el-dropdown-item command="activities">我的活动</el-dropdown-item>
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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowDown, Bell } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import ChatWidget from '@/components/ChatWidget.vue'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const router = useRouter()
const route = useRoute()

const isScrolled = ref(false)
const isShaking = ref(false)
let pollingInterval = null

const forceSolidHeader = computed(() => route.path.startsWith('/user'))

watch(
  () => authStore.token,
  (newToken) => {
    if (newToken) {
      notificationStore.fetchUnreadCount()
      startPolling()
      return
    }

    notificationStore.clearCount()
    stopPolling()
  }
)

watch(
  () => notificationStore.unreadCount,
  (newVal, oldVal) => {
    if (newVal > oldVal) {
      isShaking.value = true
      setTimeout(() => {
        isShaking.value = false
      }, 500)
    }
  }
)

const startPolling = () => {
  stopPolling()
  if (authStore.token) {
    pollingInterval = setInterval(() => {
      notificationStore.fetchUnreadCount()
    }, 5000)
  }
}

const stopPolling = () => {
  if (pollingInterval) {
    clearInterval(pollingInterval)
    pollingInterval = null
  }
}

const handleLogout = async () => {
  stopPolling()
  await authStore.logout()
  router.push('/login')
}

const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'my-clubs': {
      await authStore.refreshUser()
      const roles = authStore.user?.roles || []
      const isClubAdmin = roles.some((r) => (typeof r === 'string' ? r : r.code) === 'CLUB_ADMIN')
      router.push(isClubAdmin ? '/clubadmin' : '/user/clubs')
      break
    }
    case 'activities':
      router.push('/user/activities')
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
  isScrolled.value = window.scrollY > 30
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  if (authStore.token) {
    notificationStore.fetchUnreadCount()
    startPolling()
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  stopPolling()
})
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
}

.header {
  background: rgba(255, 255, 255, 0.72);
  color: #12263a;
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(15, 28, 42, 0.08);
  padding: 0;
  height: 76px !important;
  position: fixed;
  top: 0;
  width: 100%;
  z-index: 100;
  transition: all 0.25s ease;
}

.header.scrolled {
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 8px 18px rgba(15, 28, 42, 0.08);
  height: 70px !important;
}

.header-content {
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0;
  max-width: 100%;
  padding: 0 20px;
}

.logo-text {
  font-family: var(--font-heading);
  font-size: 1.36rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: #0f1c2a;
}

.nav {
  display: flex;
  align-items: center;
  gap: 1.8rem;
}

.nav-item {
  color: inherit;
  text-decoration: none;
  font-weight: 600;
  font-size: 0.92rem;
  position: relative;
  transition: opacity 0.2s;
}

.nav-item:hover {
  opacity: 0.7;
}

.nav-btn {
  background: linear-gradient(135deg, #c2410c 0%, #9a3412 100%);
  color: white;
  padding: 9px 20px;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 700;
  font-size: 0.82rem;
  transition: transform 0.2s;
}

.nav-btn:hover {
  transform: translateY(-1px);
}

.nav-icon {
  text-decoration: none;
  font-size: 1.05rem;
  display: flex;
  align-items: center;
  color: #1f3348;
}

.bell-wrap {
  display: inline-flex;
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
  display: flex;
  align-items: center;
  cursor: pointer;
  outline: none;
}

.user-info-link:hover {
  color: #0f5f59;
}

.main-content {
  padding: 0;
  flex: 1;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes shake {
  0% { transform: rotate(0deg); }
  25% { transform: rotate(15deg); }
  50% { transform: rotate(0deg); }
  75% { transform: rotate(-15deg); }
  100% { transform: rotate(0deg); }
}

.shake-animation {
  animation: shake 0.5s ease-in-out;
}

.notification-badge :deep(.el-badge__content) {
  background: var(--color-danger);
  border: 2px solid white;
}

.user-avatar {
  border: 2px solid rgba(15, 28, 42, 0.14);
  transition: all 0.3s;
}

.dropdown-trigger {
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.dropdown-trigger:hover {
  background: rgba(15, 28, 42, 0.06);
}

:deep(.el-dropdown-menu) {
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
  padding: 8px;
}

:deep(.el-dropdown-menu__item) {
  border-radius: var(--radius-sm);
  padding: 10px 16px;
  font-weight: 500;
  transition: all 0.2s;
}

:deep(.el-dropdown-menu__item:hover) {
  background: var(--color-bg-secondary);
  color: var(--color-primary);
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 14px;
  }

  .nav {
    gap: 0.9rem;
  }

  .nav-item {
    font-size: 0.84rem;
  }

  .user-info {
    display: none;
  }
}
</style>
