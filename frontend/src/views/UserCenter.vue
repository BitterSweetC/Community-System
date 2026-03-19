<template>
  <div class="user-center-layout">
    <el-container class="layout-container">
      <el-aside width="240px" class="sidebar">
        <div class="sidebar-header">
          <h3>用户中心</h3>
        </div>
        <el-menu :default-active="activeMenu" class="el-menu-vertical" router>
          <el-menu-item index="/user/profile">
            <el-icon><User /></el-icon>
            <span>个人资料</span>
          </el-menu-item>
          <el-menu-item index="/user/avatar">
            <el-icon><Picture /></el-icon>
            <span>我的头像</span>
          </el-menu-item>
          <el-menu-item index="/user/applications">
            <el-icon><Document /></el-icon>
            <span>我的申请</span>
          </el-menu-item>
          <el-menu-item index="/user/clubs">
            <el-icon><School /></el-icon>
            <span>我的社团</span>
          </el-menu-item>
          <el-menu-item index="/user/archive">
            <el-icon><Document /></el-icon>
            <span>我的积分档案</span>
          </el-menu-item>
          <el-menu-item index="/user/activities">
            <el-icon><Calendar /></el-icon>
            <span>我的活动</span>
          </el-menu-item>
          <el-menu-item index="/user/notifications">
            <el-icon><Bell /></el-icon>
            <span>消息通知</span>
          </el-menu-item>

          <el-menu-item v-if="isClubAdmin" index="/clubadmin">
            <el-icon><School /></el-icon>
            <span>我的社团管理</span>
          </el-menu-item>
          <el-menu-item v-if="isStudent" index="/user/create-club">
            <el-icon><Plus /></el-icon>
            <span>申请创建社团</span>
          </el-menu-item>
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <span>返回首页</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Document, Bell, Plus, School, HomeFilled, Calendar, Picture } from '@element-plus/icons-vue'

const route = useRoute()
const authStore = useAuthStore()
const activeMenu = computed(() => route.path)

const isClubAdmin = computed(() => {
  const roles = authStore.user?.roles || []
  return roles.some((r) => (typeof r === 'string' ? r : r.code) === 'CLUB_ADMIN')
})

const isStudent = computed(() => {
  const roles = authStore.user?.roles || []
  return roles.some((r) => (typeof r === 'string' ? r : r.code) === 'USER')
})

onMounted(async () => {
  await authStore.refreshUser()
})
</script>

<style scoped>
.user-center-layout {
  padding-top: 80px;
  min-height: 100vh;
  background: linear-gradient(180deg, #eaf3ff 0%, #e9f7f2 48%, #f0f6ff 100%);
}

.layout-container {
  max-width: 1360px;
  margin: 0 auto;
  min-height: calc(100vh - 80px);
}

.sidebar {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(8px);
  margin: 16px 0 16px 16px;
  border-radius: 14px;
  border: 1px solid rgba(15, 28, 42, 0.1);
  box-shadow: 0 12px 24px rgba(15, 28, 42, 0.08);
  overflow: hidden;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid rgba(15, 28, 42, 0.1);
}

.sidebar-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #0f1c2a;
}

.el-menu-vertical {
  border-right: none;
  background: transparent;
}

.main-content {
  padding: 16px 16px 16px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

:deep(.el-menu-item.is-active) {
  background-color: rgba(21, 111, 168, 0.14);
  color: #15507e;
  border-right: 3px solid #1f6fa8;
}

@media (max-width: 900px) {
  .user-center-layout {
    padding-top: 72px;
  }

  .layout-container {
    max-width: 100%;
    display: block;
    min-height: auto;
  }

  .sidebar {
    margin: 12px 12px 8px;
    width: auto !important;
  }

  .main-content {
    padding: 12px;
  }
}
</style>
