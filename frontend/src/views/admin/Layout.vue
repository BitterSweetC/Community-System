<template>
  <div class="admin-layout">
    <el-container>
      <el-aside width="200px" style="background-color: #545c64">
        <el-menu
          router
          active-text-color="#ffd04b"
          background-color="#545c64"
          text-color="#fff"
        >
          <el-menu-item index="/admin">仪表盘</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/clubs">社团管理</el-menu-item>
          <!-- Simplified: Recruit management usually linked from club list or separate -->
          <el-menu-item @click="logout">退出登录</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { computed } from 'vue'

const authStore = useAuthStore()
const router = useRouter()

const isAdmin = computed(() => {
    const roles = authStore.user?.roles || []
    return roles.some(r => (typeof r === 'string' ? r : r.code) === 'ADMIN')
})

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  display: flex;
}
</style>
