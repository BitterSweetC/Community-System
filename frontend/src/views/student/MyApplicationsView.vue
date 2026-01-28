<template>
  <div class="my-applications-container">
    <h2>我的申请</h2>
    
    <el-card class="applications-card">
      <el-tabs v-model="activeTab" class="application-tabs">
        <!-- Join Applications Tab -->
        <el-tab-pane label="入团申请" name="join">
          <el-table :data="applications" style="width: 100%" stripe>
            <el-table-column prop="batch.club.name" label="申请社团" min-width="150" />
            <el-table-column prop="batch.title" label="招新批次" min-width="150" />
            <el-table-column prop="createTime" label="申请时间" min-width="150">
               <template #default="scope">
                 {{ formatDate(scope.row.createTime) }}
               </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="scope">
                 <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核详情" min-width="200">
               <template #default="scope">
                 <div v-if="scope.row.firstReviewStatus">
                    <span class="review-label">初审:</span> 
                    <span :class="getStatusClass(scope.row.firstReviewStatus)">{{ scope.row.firstReviewStatus }}</span>
                 </div>
                 <div v-if="scope.row.finalReviewStatus">
                    <span class="review-label">复审:</span>
                    <span :class="getStatusClass(scope.row.finalReviewStatus)">{{ scope.row.finalReviewStatus }}</span>
                 </div>
               </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="applications.length === 0" description="暂无入团申请" />
        </el-tab-pane>

        <!-- Create Club Applications Tab -->
        <el-tab-pane label="建团申请" name="create">
          <el-table :data="createdClubs" style="width: 100%" stripe>
            <el-table-column prop="name" label="社团名称" min-width="150" />
            <el-table-column prop="category" label="分类" width="120">
               <template #default="scope">
                 <el-tag effect="plain">{{ scope.row.category }}</el-tag>
               </template>
            </el-table-column>
            <el-table-column prop="createTime" label="申请时间" min-width="150">
               <template #default="scope">
                 {{ formatDate(scope.row.createTime) }}
               </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="scope">
                 <el-tag :type="getClubStatusType(scope.row.status)">{{ getClubStatusText(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="申请理由/简介" show-overflow-tooltip />
          </el-table>
          <el-empty v-if="createdClubs.length === 0" description="暂无建团申请" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from '@/api/axios'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const activeTab = ref('join')
const applications = ref([])
const createdClubs = ref([])

onMounted(async () => {
  try {
    // 1. Fetch Join Applications
    applications.value = await axios.get('/recruit/applications')
    
    // 2. Fetch Created Clubs (Create Applications)
    // /clubs/my returns clubs where user is PRESIDENT or MANAGER
    // We filter for clubs created by the current user to represent "My Applications"
    const myClubs = await axios.get('/clubs/my')
    if (myClubs) {
        const userId = authStore.user?.id
        createdClubs.value = myClubs.filter(club => club.createdBy === userId)
    }
  } catch (error) {
    console.error('Failed to fetch applications', error)
  }
})

// Helpers for Join Applications
const getStatusType = (status) => {
    if (status === 'APPROVED' || status === 'PASSED') return 'success'
    if (status === 'REJECTED' || status === 'FAILED') return 'danger'
    return 'warning'
}

const getStatusText = (status) => {
    // Basic status mapping, adjust based on actual enum values from backend
    const map = {
        'PENDING': '审核中',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝',
        'PASSED': '已通过',
        'FAILED': '未通过'
    }
    return map[status] || status
}

const getStatusClass = (status) => {
    if (status === 'PASSED') return 'text-success'
    if (status === 'FAILED') return 'text-danger'
    return 'text-warning'
}

// Helpers for Create Applications
const getClubStatusType = (status) => {
    if (status === 'ACTIVE') return 'success'
    if (status === 'REJECTED') return 'danger'
    return 'warning'
}

const getClubStatusText = (status) => {
    const map = {
        'PENDING': '审核中',
        'ACTIVE': '已通过', // Active means approved
        'REJECTED': '已拒绝',
        'DRAFT': '草稿'
    }
    return map[status] || status
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<style scoped>
.my-applications-container {
  max-width: 1000px;
}

.my-applications-container h2 {
  margin-bottom: 20px;
  font-size: 1.5rem;
  color: var(--color-text);
}

.applications-card {
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
}

.text-success { color: #67c23a; font-weight: bold; }
.text-danger { color: #f56c6c; font-weight: bold; }
.text-warning { color: #e6a23c; font-weight: bold; }

.review-label {
    color: #909399;
    margin-right: 5px;
    font-size: 0.9em;
}
</style>
