<template>
  <div class="profile-page">
    <section class="profile-panel">
      <div class="panel-header">
        <div>
          <p class="panel-kicker">Account</p>
          <h1>我的活动</h1>
          <p>查看你已报名的活动，管理签到状态与活动详情。</p>
        </div>
        <span class="meta-pill">已报名：{{ activities.length }} 场</span>
      </div>

      <el-card class="profile-card" shadow="never">
        <div class="table-panel">
          <el-table
            :data="activities"
            stripe
            border
            header-cell-class-name="table-header"
            v-loading="loading"
            style="width: 100%"
          >
            <el-table-column prop="signupStatus" label="报名状态" width="110" align="center">
              <template #default="scope">
                <el-tag :type="getSignupStatusType(scope.row.signupStatus)">
                  {{ getSignupStatus(scope.row.signupStatus) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="title" label="活动主题" min-width="240">
              <template #default="scope">
                <button class="table-link" @click="showDetails(scope.row)">
                  {{ scope.row.title }}
                </button>
              </template>
            </el-table-column>

            <el-table-column prop="clubName" label="主办单位" width="170" />
            <el-table-column prop="location" label="地点" width="140" />

            <el-table-column prop="startTime" label="开始时间" width="186">
              <template #default="scope">
                {{ formatTime(scope.row.startTime) }}
              </template>
            </el-table-column>

            <el-table-column prop="signupTime" label="报名时间" width="186">
              <template #default="scope">
                {{ formatTime(scope.row.signupTime) }}
              </template>
            </el-table-column>

            <el-table-column label="操作" width="110" align="center">
              <template #default="scope">
                <el-button
                  v-if="scope.row.signupStatus === 'SIGNED'"
                  type="primary"
                  size="small"
                  @click="openSignInDialog(scope.row)"
                >
                  签到
                </el-button>
                <span v-else-if="scope.row.signupStatus === 'SIGNED_IN'" class="signed-text">已签到</span>
                <span v-else class="signed-text muted">-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
    </section>

    <el-dialog v-model="signInDialogVisible" title="活动签到" width="420px">
      <div class="signin-content">
        <p>请输入活动签到码进行签到</p>
        <el-input v-model="signInCode" placeholder="请输入签到码" style="max-width: 220px" />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="signInDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitSignIn">签到</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="活动详情" width="620px">
      <div v-if="currentActivity" class="activity-detail">
        <h3 class="detail-title">{{ currentActivity.title }}</h3>

        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">主办单位：</span>
            <span>{{ currentActivity.clubName || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">地点：</span>
            <span>{{ currentActivity.location || '待定' }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="label">时间：</span>
            <span>{{ formatTime(currentActivity.startTime) }} ~ {{ formatTime(currentActivity.endTime) }}</span>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">活动介绍</div>
          <div class="description">{{ currentActivity.description || '暂无介绍' }}</div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button
            v-if="currentActivity && currentActivity.signupStatus === 'SIGNED'"
            type="primary"
            @click="openSignInDialog(currentActivity)"
          >
            签到
          </el-button>
          <el-tag
            v-else-if="currentActivity && currentActivity.signupStatus === 'SIGNED_IN'"
            type="success"
            size="large"
            style="margin-right: 10px"
          >
            已签到
          </el-tag>
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const activities = ref([])
const loading = ref(false)
const detailDialogVisible = ref(false)
const currentActivity = ref(null)

const signInDialogVisible = ref(false)
const signInCode = ref('')
const currentSignInActivity = ref(null)

const normalizeList = (payload) => {
  if (!payload) {
    return []
  }
  if (Array.isArray(payload)) {
    return payload
  }
  if (Array.isArray(payload.list)) {
    return payload.list
  }
  if (Array.isArray(payload.content)) {
    return payload.content
  }
  return []
}

const openSignInDialog = (activity) => {
  currentSignInActivity.value = activity
  signInCode.value = ''
  signInDialogVisible.value = true
}

const submitSignIn = async () => {
  if (!currentSignInActivity.value) {
    return
  }

  try {
    await axios.post(`/activities/${currentSignInActivity.value.id}/signin`, {
      code: signInCode.value
    })
    ElMessage.success('签到成功')
    signInDialogVisible.value = false
    await fetchMyActivities()

    if (currentActivity.value && currentActivity.value.id === currentSignInActivity.value.id) {
      const updated = activities.value.find((item) => item.id === currentActivity.value.id)
      if (updated) {
        currentActivity.value = updated
      }
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '签到失败，请检查签到码')
  }
}

const showDetails = (activity) => {
  currentActivity.value = activity
  detailDialogVisible.value = true
}

const fetchMyActivities = async () => {
  loading.value = true
  try {
    const res = await axios.get('/activities/my-signups')
    activities.value = normalizeList(res)
  } catch (error) {
    console.error(error)
    ElMessage.error('获取活动列表失败')
  } finally {
    loading.value = false
  }
}

const getSignupStatus = (status) => {
  const map = {
    SIGNED: '已报名',
    SIGNED_IN: '已签到',
    CANCELLED: '已取消'
  }
  return map[status] || status || '未知'
}

const getSignupStatusType = (status) => {
  if (status === 'SIGNED') {
    return 'primary'
  }
  if (status === 'SIGNED_IN') {
    return 'success'
  }
  if (status === 'CANCELLED') {
    return 'info'
  }
  return 'warning'
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchMyActivities()
})
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.profile-page {
  --ink: #142132;
  --muted: #5b6f86;
  --border: rgba(20, 33, 50, 0.12);
  --panel-bg: rgba(255, 255, 255, 0.64);
  --card-bg: rgba(255, 255, 255, 0.9);

  min-height: auto;
  padding: 8px 0 0;
  background: transparent;
  color: var(--ink);
}

.profile-panel {
  border-radius: 18px;
  border: 1px solid var(--border);
  background: var(--panel-bg);
  backdrop-filter: blur(10px);
  box-shadow: 0 14px 28px rgba(15, 28, 42, 0.08);
  padding: 18px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 14px;
}

.panel-kicker {
  margin: 0 0 8px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-weight: 700;
  color: var(--muted);
  font-size: 0.8rem;
}

.panel-header h1 {
  margin: 0;
  font-size: clamp(1.3rem, 2.6vw, 1.85rem);
}

.panel-header p {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.55;
}

.meta-pill {
  font-size: 0.86rem;
  border-radius: 999px;
  padding: 6px 12px;
  color: #355069;
  border: 1px solid rgba(15, 28, 42, 0.12);
  background: rgba(255, 255, 255, 0.66);
  white-space: nowrap;
}

.profile-card {
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--card-bg);
  box-shadow: none;
  overflow: hidden;
}

:deep(.profile-card .el-card__body) {
  padding: 18px;
}

.table-panel {
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.8);
}

:deep(.table-header) {
  background: #f4f7f7 !important;
  color: var(--ink);
  font-weight: 700;
}

.table-link {
  border: 0;
  background: transparent;
  color: #1f6fa8;
  font-family: inherit;
  font-size: 0.95rem;
  cursor: pointer;
  padding: 0;
  text-align: left;
}

.table-link:hover {
  text-decoration: underline;
}

.signed-text {
  color: #16a34a;
  font-size: 0.85rem;
  font-weight: 600;
}

.signed-text.muted {
  color: #90a2b6;
}

.signin-content {
  text-align: center;
  margin-bottom: 16px;
}

.signin-content p {
  margin-bottom: 12px;
  color: var(--muted);
}

.activity-detail {
  padding: 6px;
}

.detail-title {
  margin: 0 0 14px;
  font-size: 1.25rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  color: var(--ink);
}

.detail-item.full-width {
  grid-column: span 2;
}

.label {
  font-weight: 700;
  color: #4d6075;
  margin-right: 6px;
}

.section-title {
  font-weight: 700;
  color: #1c3147;
  margin-bottom: 8px;
}

.description {
  line-height: 1.7;
  color: var(--muted);
  background: #f7faf9;
  padding: 12px;
  border-radius: 8px;
  white-space: pre-wrap;
}

@media (max-width: 860px) {
  .profile-page {
    padding-top: 4px;
  }

  .profile-panel {
    border-radius: 14px;
    padding: 14px;
  }

  .panel-header {
    flex-direction: column;
    align-items: stretch;
  }

  .meta-pill {
    width: fit-content;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-item.full-width {
    grid-column: span 1;
  }
}
</style>
