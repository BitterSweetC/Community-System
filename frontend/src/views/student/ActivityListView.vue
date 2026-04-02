<template>
  <div class="activity-page">
    <section class="hero-section">
      <div class="hero-grid"></div>
      <div class="hero-container">
        <p class="hero-kicker">Activity Center</p>
        <h1>活动一览</h1>
        <p>探索精彩活动，在线报名并参与签到，丰富你的校园体验。</p>
        <div class="hero-meta">
          <span>当前结果：{{ total }} 场活动</span>
          <span>每页展示：{{ pageSize }} 条</span>
        </div>
      </div>
    </section>

    <section class="content-container">
      <div class="filter-panel">
        <el-form :inline="true" class="search-form" @submit.prevent="handleSearch">
          <el-form-item label="活动名称" class="search-item keyword-item">
            <el-input
              v-model="searchKeyword"
              placeholder="请输入活动名称"
              clearable
              style="width: 220px"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="举办社团" class="search-item club-item">
            <el-input
              v-model="searchClub"
              placeholder="请输入社团名称"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="日期范围" class="search-item date-range-item">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>

          <el-form-item class="action-group">
            <el-button type="primary" class="btn-primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="table-panel">
        <el-table
          :data="activities"
          stripe
          border
          v-loading="loading"
          header-cell-class-name="table-header"
          style="width: 100%"
        >
          <el-table-column prop="status" label="状态" width="106" align="center">
            <template #default="scope">
              <el-tag :type="getActivityStatusType(scope.row)">{{ getActivityStatus(scope.row) }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="封面" width="130" align="center">
            <template #default="scope">
              <el-image style="width: 86px; height: 50px; border-radius: 6px" :src="scope.row.coverUrl" fit="cover">
                <template #error>
                  <div class="cover-fallback">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
            </template>
          </el-table-column>

          <el-table-column prop="title" label="活动主题" min-width="240">
            <template #default="scope">
              <button class="table-link" @click="showActivityDetail(scope.row)">
                {{ scope.row.title }}
              </button>
            </template>
          </el-table-column>

          <el-table-column prop="clubName" label="主办单位" width="170" />
          <el-table-column label="地点" width="150">
            <template #default="scope">
              {{ scope.row.location || '待定' }}
            </template>
          </el-table-column>

          <el-table-column prop="startTime" label="开始时间" width="186">
            <template #default="scope">
              {{ formatTime(scope.row.startTime) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="128" align="center">
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
              <el-button v-else type="primary" link size="small" @click="signUp(scope.row)">报名</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination
            background
            layout="total, prev, pager, next, jumper"
            :total="total"
            :page-size="pageSize"
            v-model:current-page="currentPage"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </section>

    <el-dialog v-model="detailDialogVisible" title="活动详情" width="560px">
      <div v-if="currentActivity" class="activity-detail">
        <div v-if="currentActivity.coverUrl" class="detail-cover">
          <el-image :src="currentActivity.coverUrl" fit="cover" style="width: 100%; height: 210px; border-radius: 10px" />
        </div>

        <div class="detail-item">
          <span class="label">活动主题：</span>
          <span class="value">{{ currentActivity.title }}</span>
        </div>

        <div class="detail-item">
          <span class="label">主办社团：</span>
          <span class="value">{{ currentActivity.clubName || '-' }}</span>
        </div>

        <div class="detail-item">
          <span class="label">活动时间：</span>
          <span class="value">
            {{ formatTime(currentActivity.startTime) }} ~ {{ formatTime(currentActivity.endTime) }}
          </span>
        </div>

        <div class="detail-item">
          <span class="label">活动地点：</span>
          <span class="value">{{ currentActivity.location || '线上活动' }}</span>
        </div>

        <div class="detail-item">
          <span class="label">活动描述：</span>
          <p class="value description">{{ currentActivity.description || '暂无描述' }}</p>
        </div>

        <div class="detail-item">
          <span class="label">活动状态：</span>
          <el-tag :type="getActivityStatusType(currentActivity)">{{ getActivityStatus(currentActivity) }}</el-tag>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button
            v-if="canSignUpInDetail"
            type="primary"
            @click="signUp(currentActivity)"
          >
            立即报名
          </el-button>
        </span>
      </template>
    </el-dialog>

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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Picture } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '@/api/axios'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const activities = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)
const searchKeyword = ref('')
const searchClub = ref('')
const dateRange = ref([])

const detailDialogVisible = ref(false)
const currentActivity = ref(null)

const signInDialogVisible = ref(false)
const signInCode = ref('')
const currentSignInActivity = ref(null)

const canSignUpInDetail = computed(() => {
  if (!currentActivity.value) {
    return false
  }

  const status = getActivityStatus(currentActivity.value)
  return (
    currentActivity.value.signupStatus !== 'SIGNED' &&
    currentActivity.value.signupStatus !== 'SIGNED_IN' &&
    status === '报名中'
  )
})

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

const fetchActivities = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }

    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }

    if (searchClub.value.trim()) {
      params.clubName = searchClub.value.trim()
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const res = await axios.get('/activities', { params })
    let list = normalizeList(res)

    if (Array.isArray(res?.list)) {
      total.value = res.total ?? list.length
    } else if (Array.isArray(res?.content)) {
      total.value = res.totalElements ?? list.length
    } else {
      total.value = list.length
    }

    if (authStore.token) {
      try {
        const mySignups = await axios.get('/activities/my-signups')
        const signupList = Array.isArray(mySignups) ? mySignups : normalizeList(mySignups)
        const signupMap = {}
        signupList.forEach((item) => {
          if (item?.id) {
            signupMap[item.id] = item.signupStatus
          }
        })

        list = list.map((activity) => ({
          ...activity,
          signupStatus: signupMap[activity.id]
        }))
      } catch (error) {
        console.warn('Failed to load my signups', error)
      }
    }

    activities.value = list

    if (route.query.action === 'signup' && route.query.activityId) {
      const activityId = Number(route.query.activityId)
      const target = list.find((item) => Number(item.id) === activityId)
      if (target) {
        await performSignUp(target)
      }
      router.replace({ path: route.path, query: {} })
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || error.message || '活动数据加载失败')
  } finally {
    loading.value = false
  }
}

const getActivityStatus = (activity) => {
  const now = new Date()
  const start = new Date(activity.startTime)
  const end = new Date(activity.endTime)

  if (now < start) {
    return '报名中'
  }
  if (now >= start && now <= end) {
    return '进行中'
  }
  return '已结束'
}

const getActivityStatusType = (activity) => {
  const status = getActivityStatus(activity)
  if (status === '报名中') {
    return 'success'
  }
  if (status === '进行中') {
    return 'warning'
  }
  return 'info'
}

const showActivityDetail = (activity) => {
  currentActivity.value = activity
  detailDialogVisible.value = true
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
    fetchActivities()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '签到失败，请检查签到码')
  }
}

const signUp = async (activity) => {
  if (!authStore.token) {
    const redirect = `${route.path}?action=signup&activityId=${activity.id}`
    router.push({
      path: '/login',
      query: { redirect }
    })
    return
  }

  await performSignUp(activity)
}

const performSignUp = async (activity) => {
  try {
    await axios.post(`/activities/${activity.id}/signup`)
    ElMessage.success('报名成功')
    fetchActivities()
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '报名失败'
    if (msg.includes('请先加入')) {
      ElMessageBox.alert(msg, '提示', {
        confirmButtonText: '确定',
        type: 'warning'
      })
      return
    }
    ElMessage.warning(`报名失败: ${msg}`)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchActivities()
}

const resetSearch = () => {
  searchKeyword.value = ''
  searchClub.value = ''
  dateRange.value = []
  handleSearch()
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchActivities()
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
  fetchActivities()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;700&display=swap');

* {
  box-sizing: border-box;
}

.activity-page {
  --ink: #0f1c2a;
  --muted: #55667a;
  --surface: rgba(255, 255, 255, 0.88);
  --border: rgba(15, 28, 42, 0.12);

  min-height: 100vh;
  padding-bottom: 30px;
  background: linear-gradient(180deg, #f7f1e7 0%, #efe8dc 44%, #e8eeea 100%);
  color: var(--ink);
  font-family: 'Outfit', sans-serif;
}

.hero-section {
  position: relative;
  padding: 104px 24px 34px;
  overflow: hidden;
}

.hero-grid {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 8% 14%, rgba(15, 118, 110, 0.18), transparent 28%),
    radial-gradient(circle at 92% 20%, rgba(194, 65, 12, 0.18), transparent 34%),
    linear-gradient(165deg, #f8f3e8 0%, #eee7d8 60%, #e4ece8 100%);
}

.hero-grid::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(15, 28, 42, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 28, 42, 0.03) 1px, transparent 1px);
  background-size: 34px 34px;
}

.hero-container {
  position: relative;
  z-index: 1;
  max-width: 1240px;
  margin: 0 auto;
}

.hero-kicker {
  margin: 0 0 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-weight: 700;
  color: var(--muted);
  font-size: 0.8rem;
}

.hero-container h1 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2rem, 4vw, 3rem);
}

.hero-container p {
  margin: 10px 0 0;
  color: var(--muted);
  line-height: 1.7;
}

.hero-meta {
  margin-top: 14px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-meta span {
  font-size: 0.86rem;
  border-radius: 999px;
  padding: 6px 12px;
  color: #355069;
  border: 1px solid rgba(15, 28, 42, 0.12);
  background: rgba(255, 255, 255, 0.66);
}

.content-container {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px;
}

.filter-panel {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--surface);
  backdrop-filter: blur(8px);
  box-shadow: 0 12px 24px rgba(15, 28, 42, 0.08);
  padding: 14px 16px 0;
  margin-bottom: 16px;
}

.search-form {
  display: flex;
  align-items: flex-end;
  flex-wrap: nowrap;
  gap: 0 18px;
}

:deep(.search-form > .el-form-item) {
  margin-right: 0;
  margin-bottom: 14px;
}

:deep(.search-form .search-item) {
  flex: 0 0 auto;
}

:deep(.search-form .date-range-item) {
  flex: 1 1 420px;
  min-width: 0;
}

:deep(.search-form .date-range-item .el-form-item__content) {
  width: 100%;
  min-width: 0;
}

:deep(.search-form .date-range-item .el-date-editor) {
  width: 100%;
  min-width: 320px;
  max-width: 100%;
}

.action-group {
  flex: 0 0 auto;
  margin-left: auto;
  padding-left: 40px;
  white-space: nowrap;
}

:deep(.action-group .el-form-item__content) {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: nowrap;
}

:deep(.action-group .el-button + .el-button) {
  margin-left: 0;
}

.btn-primary {
  border: none;
  background: linear-gradient(135deg, #c2410c 0%, #9a3412 100%);
}

.table-panel {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--surface);
  backdrop-filter: blur(8px);
  box-shadow: 0 12px 24px rgba(15, 28, 42, 0.08);
  overflow: hidden;
}

:deep(.table-header) {
  background: #f4f7f7 !important;
  color: var(--ink);
  font-weight: 700;
}

.cover-fallback {
  width: 86px;
  height: 50px;
  border-radius: 6px;
  display: grid;
  place-items: center;
  color: #7e8ea3;
  background: #eff3f4;
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

.pagination-container {
  padding: 18px;
  display: flex;
  justify-content: center;
}

.activity-detail {
  padding: 4px;
}

.detail-cover {
  margin-bottom: 16px;
}

.detail-item {
  margin-bottom: 14px;
  display: flex;
  align-items: flex-start;
}

.detail-item .label {
  width: 94px;
  color: #4d6075;
  font-weight: 700;
  flex-shrink: 0;
}

.detail-item .value {
  color: var(--ink);
  line-height: 1.55;
  flex: 1;
}

.detail-item .description {
  margin: 0;
  white-space: pre-wrap;
}

.signin-content {
  text-align: center;
  margin-bottom: 16px;
}

.signin-content p {
  margin-bottom: 12px;
  color: var(--muted);
}

@media (max-width: 1200px) {
  .search-form {
    flex-wrap: wrap;
  }

  :deep(.search-form .date-range-item) {
    flex: 1 1 100%;
  }

  :deep(.search-form .date-range-item .el-date-editor) {
    min-width: 0;
  }

  .action-group {
    margin-left: 0;
  }
}

@media (max-width: 900px) {
  .hero-section {
    padding-top: 90px;
  }

  .content-container {
    padding: 0 14px;
  }

  .action-group {
    margin-left: 0;
  }

  :deep(.el-form-item) {
    margin-right: 8px;
  }

  :deep(.search-form .search-item),
  :deep(.search-form .date-range-item),
  :deep(.search-form .action-group) {
    flex: 1 1 100%;
  }

  :deep(.search-form .search-item .el-form-item__content),
  :deep(.search-form .date-range-item .el-form-item__content) {
    width: 100%;
  }

  :deep(.search-form .search-item .el-input),
  :deep(.search-form .date-range-item .el-date-editor) {
    width: 100% !important;
    min-width: 0;
  }
}
</style>
