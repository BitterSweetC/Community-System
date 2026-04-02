<template>
  <div class="my-applications-container">
    <h2>我的申请</h2>

    <el-card class="applications-card">
      <el-tabs v-model="activeTab" class="application-tabs">
        <el-tab-pane label="入团申请" name="join">
          <el-table :data="applications" style="width: 100%" stripe>
            <el-table-column prop="batch.club.name" label="申请社团" min-width="150" />
            <el-table-column prop="batch.title" label="招新批次" min-width="180" />
            <el-table-column prop="createdAt" label="申请时间" min-width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getStatusType(resolveApplicationStatus(row))">
                  {{ getStatusText(resolveApplicationStatus(row)) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核详情" min-width="220">
              <template #default="{ row }">
                <div class="review-line">
                  <span class="review-label">初审:</span>
                  <span :class="getReviewStatusClass(row.firstReviewStatus)">
                    {{ getReviewStatusText(row.firstReviewStatus) }}
                  </span>
                </div>
                <div class="review-line">
                  <span class="review-label">复审:</span>
                  <span :class="getReviewStatusClass(row.finalReviewStatus)">
                    {{ getReviewStatusText(row.finalReviewStatus) }}
                  </span>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="applications.length === 0" description="暂无入团申请" />
        </el-tab-pane>

        <el-tab-pane label="建团申请" name="create">
          <el-table :data="createdClubs" style="width: 100%" stripe>
            <el-table-column prop="name" label="社团名称" min-width="150" />
            <el-table-column prop="category" label="分类" width="120">
              <template #default="{ row }">
                <el-tag effect="plain">{{ row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="申请时间" min-width="180">
              <template #default="{ row }">
                {{ formatDate(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getClubStatusType(row.status)">{{ getClubStatusText(row.status) }}</el-tag>
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
import { onMounted, ref } from 'vue'
import axios from '@/api/axios'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const activeTab = ref('join')
const applications = ref([])
const createdClubs = ref([])

const normalizeList = (data) => (Array.isArray(data) ? data : [])

onMounted(async () => {
  try {
    applications.value = normalizeList(await axios.get('/recruit/applications'))

    const myClubs = normalizeList(await axios.get('/clubs/my'))
    const userId = Number(authStore.user?.id)
    createdClubs.value = myClubs.filter((club) => Number(club.createdBy) === userId)
  } catch (error) {
    console.error('Failed to fetch applications', error)
  }
})

const resolveApplicationStatus = (application) => {
  const firstReviewStatus = application?.firstReviewStatus
  const finalReviewStatus = application?.finalReviewStatus

  if (firstReviewStatus === 'REJECTED' || finalReviewStatus === 'REJECTED') {
    return 'REJECTED'
  }

  if (firstReviewStatus === 'PASSED' && finalReviewStatus === 'PASSED') {
    return 'PASSED'
  }

  return 'PENDING'
}

const getStatusType = (status) => {
  if (status === 'PASSED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

const getStatusText = (status) => {
  const map = {
    PENDING: '待审核',
    PASSED: '已通过',
    REJECTED: '已驳回'
  }
  return map[status] || status || '-'
}

const getReviewStatusText = (status) => {
  const map = {
    PENDING: '待审核',
    PASSED: '已通过',
    REJECTED: '已驳回'
  }
  return map[status] || status || '-'
}

const getReviewStatusClass = (status) => {
  if (status === 'PASSED') return 'text-success'
  if (status === 'REJECTED') return 'text-danger'
  return 'text-warning'
}

const getClubStatusType = (status) => {
  if (status === 'ACTIVE') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

const getClubStatusText = (status) => {
  const map = {
    PENDING: '待审核',
    ACTIVE: '已通过',
    REJECTED: '已驳回',
    DRAFT: '草稿'
  }
  return map[status] || status || '-'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', { hour12: false })
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

.review-line + .review-line {
  margin-top: 6px;
}

.review-label {
  color: #909399;
  margin-right: 5px;
  font-size: 0.9em;
}

.text-success {
  color: #67c23a;
  font-weight: 600;
}

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}

.text-warning {
  color: #e6a23c;
  font-weight: 600;
}
</style>
