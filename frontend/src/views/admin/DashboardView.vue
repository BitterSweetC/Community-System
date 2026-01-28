<template>
  <div class="admin-dashboard">
    <h2>管理员仪表盘</h2>
    <p class="welcome-text">欢迎回来，{{ authStore.user?.username }}</p>

    <div class="dashboard-grid">
      <el-card class="dashboard-card" @click="$router.push('/admin/clubs')">
        <template #header>
          <div class="card-header">
            <span>社团管理</span>
          </div>
        </template>
        <div class="card-content">
          管理社团申请、社团信息及状态。
        </div>
      </el-card>

      <!-- Show Notice Management for Admin -->
      <el-card class="dashboard-card" @click="$router.push('/admin/notices')">
        <template #header>
          <div class="card-header">
            <span>公告管理</span>
          </div>
        </template>
        <div class="card-content">
          发布系统公告、管理所有社团公告。
        </div>
      </el-card>

      <!-- If user is a Club Admin, show their clubs -->
      <div v-if="myClubs.length > 0" class="my-clubs-section">
        <h3>我管理的社团</h3>
        <el-row :gutter="20">
          <el-col :span="12" v-for="club in myClubs" :key="club.id">
            <el-card class="club-dashboard-card">
              <template #header>
                <div class="card-header">
                  <span>{{ club.name }}</span>
                  <el-tag size="small">{{ club.status }}</el-tag>
                </div>
              </template>
              <div class="club-actions">
                <el-button size="small" @click="$router.push(`/admin/recruit/${club.id}`)">招新管理</el-button>
                <el-button size="small" @click="$router.push(`/admin/notices/${club.id}`)">发布公告</el-button>
                <el-button size="small" disabled>活动管理 (开发中)</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'

const authStore = useAuthStore()
const myClubs = ref([])

onMounted(async () => {
  try {
    const res = await axios.get('/clubs/my')
    if (res.list) myClubs.value = res.list
    else if (Array.isArray(res)) myClubs.value = res
    else myClubs.value = res.content || []
  } catch (error) {
    console.error('Failed to load my clubs', error)
  }
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}
.welcome-text {
  margin-bottom: 30px;
  color: #666;
}
.dashboard-grid {
  display: flex;
  flex-direction: column;
  gap: 30px;
}
.dashboard-card {
  cursor: pointer;
  transition: transform 0.2s;
  max-width: 400px;
}
.dashboard-card:hover {
  transform: translateY(-5px);
}
.my-clubs-section {
  margin-top: 20px;
}
.club-dashboard-card {
  margin-bottom: 20px;
}
.club-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
