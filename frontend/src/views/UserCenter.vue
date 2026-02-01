<template>
  <div class="user-center-layout">
    <el-container class="layout-container">
      <el-aside width="240px" class="sidebar">
        <div class="sidebar-header">
          <h3>用户中心</h3>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical"
          router
        >
          <el-menu-item index="/user/profile">
            <el-icon><User /></el-icon>
            <span>个人资料</span>
          </el-menu-item>
          <el-menu-item index="/user/applications">
            <el-icon><Document /></el-icon>
            <span>我的申请</span>
          </el-menu-item>
          <el-menu-item index="/user/activities">
            <el-icon><Calendar /></el-icon>
            <span>我的活动</span>
          </el-menu-item>
          <el-menu-item index="/user/notifications">
            <el-icon><Bell /></el-icon>
            <span>消息通知</span>
          </el-menu-item>
          <!-- Role Based Items -->
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
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Document, Bell, Plus, School, HomeFilled, Calendar } from '@element-plus/icons-vue'

const route = useRoute()
const authStore = useAuthStore()
const activeMenu = computed(() => route.path)

const isClubAdmin = computed(() => {
    const roles = authStore.user?.roles || []
    return roles.some(r => (typeof r === 'string' ? r : r.code) === 'CLUB_ADMIN')
})

const isStudent = computed(() => {
    const roles = authStore.user?.roles || []
    return roles.some(r => (typeof r === 'string' ? r : r.code) === 'STUDENT')
})
</script>

<style scoped>
.user-center-layout {
  /* Adjust based on the main layout header height if it's rendered inside it, 
     or if this is a standalone layout. 
     Assuming this is rendered INSIDE the main student Layout (which has the header).
     The main Layout has a header of height 80px.
  */
  padding-top: 80px; 
  min-height: 100vh;
  background-color: var(--color-bg);
}

.layout-container {
  max-width: 1200px;
  margin: 0 auto;
  min-height: calc(100vh - 80px);
}

.sidebar {
  background-color: white;
  margin: 20px 0 20px 20px;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid var(--color-border);
}

.sidebar-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--color-text);
}

.el-menu-vertical {
  border-right: none;
}

.main-content {
  padding: 20px;
}

/* Adjust element plus menu items */
:deep(.el-menu-item.is-active) {
  background-color: var(--color-primary-light);
  color: var(--color-primary);
  border-right: 3px solid var(--color-primary);
}
</style>
