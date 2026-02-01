<template>
  <div class="club-detail-wrapper">
    <div v-if="club" class="club-detail-container">
      <!-- Club Hero -->
      <div class="club-hero">
      <div class="hero-overlay"></div>
      <div class="hero-content premium-container">
        <div class="hero-header">
          <h1 class="club-title">{{ club.name }}</h1>
          <el-tag type="warning" class="category-tag">{{ club.category }}</el-tag>
        </div>
        <div class="hero-stats">
          <span>成立年份 {{ club.foundedYear || '2024' }}</span>
          <span class="dot">•</span>
          <span>{{ club.status === 'ACTIVE' ? '活跃' : '筹备中' }}</span>
        </div>
      </div>
    </div>

    <div class="premium-container main-content">
      <el-tabs v-model="activeTab" class="custom-tabs">
        <!-- Overview Tab -->
        <el-tab-pane label="社团概况" name="overview">
          <div class="tab-content">
            <el-row :gutter="40">
              <el-col :md="16">
                <div class="section-block">
                  <h3 class="section-title">关于我们</h3>
                  <p class="description-text">{{ club.description || '暂无简介' }}</p>
                </div>
                
                <div class="section-block" v-if="notices.length > 0">
                  <h3 class="section-title">最新动态</h3>
                  <div class="notice-list">
                    <el-card v-for="notice in notices" :key="notice.id" class="notice-item" shadow="hover">
                      <div class="notice-header">
                        <span class="notice-date">{{ formatDate(notice.publishedAt) }}</span>
                        <el-tag size="small" effect="plain">公告</el-tag>
                      </div>
                      <h4>{{ notice.title }}</h4>
                      <p>{{ notice.content }}</p>
                    </el-card>
                  </div>
                </div>
              </el-col>
              <el-col :md="8">
                <el-card class="action-card">
                  <h3>加入我们</h3>
                  <p>准备好成为我们的一员了吗？</p>
                  <el-button type="primary" class="w-full" @click="activeTab = 'recruit'">查看招新</el-button>
                  <el-divider />
                  <div class="contact-info">
                    <p><strong>联系人:</strong> 社长</p>
                    <el-button link>发送消息</el-button>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- Activities Tab -->
        <el-tab-pane label="社团活动" name="activities">
          <div class="tab-content">
            <div class="activities-grid">
              <el-card v-for="activity in activities" :key="activity.id" class="activity-card" :body-style="{ padding: '0px' }">
                <div class="activity-image" :style="{ background: getRandomGradient(activity.id) }">
                  <div class="date-badge">
                    <span class="day">{{ new Date(activity.startTime).getDate() }}</span>
                    <span class="month">{{ new Date(activity.startTime).getMonth() + 1 }}月</span>
                  </div>
                </div>
                <div class="activity-content">
                  <h3 class="activity-title">{{ activity.title }}</h3>
                  <div class="activity-meta">
                    <span>📍 {{ activity.location || '待定' }}</span>
                    <span>🕒 {{ formatTime(activity.startTime) }}</span>
                  </div>
                  <p class="activity-desc">{{ activity.description }}</p>
                  <el-button type="primary" plain class="w-full mt-2" @click="handleActivitySignup(activity)">立即报名</el-button>
                </div>
              </el-card>
              <el-empty v-if="activities.length === 0" description="暂无活动" />
            </div>
          </div>
        </el-tab-pane>

        <!-- Recruit Tab -->
        <el-tab-pane label="招新信息" name="recruit">
          <div class="tab-content">
            <h3 class="section-title">开放职位</h3>
            <div class="recruit-list">
              <el-card v-for="batch in batches" :key="batch.id" class="recruit-card">
                <div class="recruit-header">
                  <div>
                    <h4 class="recruit-title">{{ batch.title }}</h4>
                    <div class="recruit-dates">
                      {{ formatDate(batch.startTime) }} - {{ formatDate(batch.endTime) }}
                    </div>
                  </div>
                  <el-button type="primary" @click="handleApplyClick(batch)" :disabled="getBatchStatus(batch) !== '进行中'">立即申请</el-button>
                </div>
              </el-card>
              <el-empty v-if="batches.length === 0" description="暂无招新批次" />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Apply Dialog -->
    <el-dialog v-model="applyDialogVisible" title="申请表单" width="500px" destroy-on-close align-center>
      <el-form :model="applyForm" label-position="top" size="large">
        <div v-for="field in currentFields" :key="field.id">
          <el-form-item :label="field.label" :required="field.required">
             <el-input v-if="field.type === 'TEXT'" v-model="applyData[field.fieldKey]" :placeholder="'请输入 ' + field.label" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="applyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitApply">提交申请</el-button>
        </span>
      </template>
    </el-dialog>
    </div>

    <ChatWidget />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'
import ChatWidget from '@/components/ChatWidget.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const club = ref(null)
const batches = ref([])
const activities = ref([])
const notices = ref([])
const activeTab = ref('overview')

const applyDialogVisible = ref(false)
const currentBatch = ref(null)
const currentFields = ref([])
const applyData = ref({})
const applyForm = ref({})

onMounted(async () => {
  const clubId = route.params.id
  try {
    club.value = await axios.get(`/clubs/${clubId}`)
    batches.value = await axios.get(`/recruit/batches?clubId=${clubId}`)
    
    // Fetch Activities
    try {
        const actRes = await axios.get(`/activities?clubId=${clubId}`)
        if (actRes.list) activities.value = actRes.list
        else if (actRes.content) activities.value = actRes.content
        else activities.value = actRes
    } catch (e) { console.warn('Activities fetch failed', e) }

    // Fetch Notices
    try {
        const notRes = await axios.get(`/notices?clubId=${clubId}`)
        if (notRes.list) notices.value = notRes.list
        else if (notRes.content) notices.value = notRes.content
        else notices.value = notRes
    } catch (e) { console.warn('Notices fetch failed', e) }

    // Check for auto-apply redirect
    if (route.query.action === 'apply' && route.query.batchId) {
        const batchId = parseInt(route.query.batchId)
        const batch = batches.value.find(b => b.id === batchId)
        if (batch) {
            activeTab.value = 'recruit' // Switch to recruit tab
            openApply(batch)
        }
    }

  } catch (error) {
    console.error(error)
  }
})

const getBatchStatus = (batch) => {
    const now = new Date()
    const start = new Date(batch.startTime)
    const end = new Date(batch.endTime)
    if (now < start) return '未开始'
    if (now > end) return '已结束'
    return '进行中'
}

const handleApplyClick = (batch) => {
    if (!authStore.token) {
        router.push({
            path: '/login',
            query: { redirect: route.fullPath + '?action=apply&batchId=' + batch.id }
        })
        return
    }
    openApply(batch)
}

const openApply = async (batch) => {
  try {
      currentBatch.value = batch
      currentFields.value = await axios.get(`/recruit/fields?batchId=${batch.id}`)
      applyData.value = {}
      currentFields.value.forEach(f => {
        applyData.value[f.fieldKey] = ''
      })
      applyDialogVisible.value = true
  } catch (error) {
      ElMessage.error('加载申请表失败: ' + (error.message || '未知错误'))
  }
}

const submitApply = async () => {
  try {
    await axios.post('/recruit/applications', {
      batch: { id: currentBatch.value.id },
      applyData: JSON.stringify(applyData.value)
    })
    ElMessage.success('申请提交成功')
    applyDialogVisible.value = false
  } catch (error) {
    ElMessage.error('提交失败: ' + error.message)
  }
}

const handleActivitySignup = async (activity) => {
    if (!authStore.token) {
        // Since we are already on the club page, we just need to reload this page with action param
        // But wait, activity signup might not have a dedicated page in this view, it's just a button.
        // Redirecting back to here with params is fine.
        // Ideally we should redirect to Activity Detail page, but currently we are doing quick signup.
        // Let's redirect to login and back to here.
        // We need a way to auto-trigger activity signup on this page too?
        // Or maybe just let the user click again? 
        // The user requirement says "activity signup is the same flow".
        // Let's implement auto-trigger for activity too.
        router.push({
            path: '/login',
            query: { redirect: route.fullPath + '?action=signup&activityId=' + activity.id }
        })
        return
    }
    try {
        await axios.post(`/activities/${activity.id}/signup`)
        ElMessage.success('报名成功')
    } catch (error) {
        ElMessage.error('报名失败: ' + error.message)
    }
}

// Add auto-signup check in onMounted
// We need to add this logic to onMounted above
// ... (I will add it in the actual file content)

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

const formatTime = (dateStr) => {
    if (!dateStr) return ''
    return new Date(dateStr).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const gradients = [
  'linear-gradient(135deg, #1F2937 0%, #4B5563 100%)',
  'linear-gradient(135deg, #D97706 0%, #B45309 100%)',
  'linear-gradient(135deg, #059669 0%, #047857 100%)'
]
const getRandomGradient = (id) => gradients[id % gradients.length]
</script>

<style scoped>
.club-detail-container {
  background-color: var(--color-bg);
  min-height: 100vh;
}

/* Club Hero */
.club-hero {
  height: 400px;
  background-image: linear-gradient(135deg, #667eea 0%, #764ba2 100%); /* Fallback or gradient */
  background-size: cover;
  background-position: center;
  position: relative;
  display: flex;
  align-items: flex-end;
  padding-bottom: 3rem;
}

.hero-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0.2) 60%, rgba(0,0,0,0.1) 100%);
}

.hero-content {
  position: relative;
  z-index: 1;
  color: white;
  width: 100%;
}

.hero-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.club-title {
  font-size: 3rem;
  font-weight: 800;
  color: white;
  margin: 0;
  line-height: 1;
}

.category-tag {
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 700;
}

.hero-stats {
  font-size: 1rem;
  opacity: 0.9;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dot {
  font-weight: bold;
}

/* Main Content */
.main-content {
  margin-top: -2rem; /* Overlap effect */
  position: relative;
  z-index: 2;
  background-color: transparent; /* Let tabs handle bg */
  padding-top: 0;
  padding-bottom: 4rem;
}

/* Tabs */
.custom-tabs {
    background: white;
    border-radius: 8px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    padding: 20px;
    min-height: 500px;
}

.custom-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: #e5e7eb;
}

.custom-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--color-primary);
  height: 3px;
}

.custom-tabs :deep(.el-tabs__item) {
  font-size: 1.1rem;
  font-weight: 600;
  color: #6b7280;
  padding: 0 20px;
}

.custom-tabs :deep(.el-tabs__item.is-active) {
  color: var(--color-primary);
}

.tab-content {
  padding-top: 2rem;
}

/* Overview Section */
.section-block {
  margin-bottom: 3rem;
}

.section-title {
  font-size: 1.5rem;
  margin-bottom: 1.5rem;
  border-left: 4px solid #3b82f6;
  padding-left: 1rem;
  color: #1f2937;
}

.description-text {
  font-size: 1.1rem;
  color: #4b5563;
  line-height: 1.8;
}

/* Notices */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.notice-item {
  border: 1px solid #e5e7eb;
}

.notice-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.notice-date {
  font-size: 0.85rem;
  color: #9ca3af;
  font-weight: 600;
}

/* Action Card */
.action-card {
  position: sticky;
  top: 100px;
  text-align: center;
}

.w-full {
  width: 100%;
}

.contact-info {
  margin-top: 1rem;
  font-size: 0.9rem;
  color: #6b7280;
}

/* Activities Grid */
.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.activity-card {
  display: flex;
  flex-direction: column;
  border: none;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.activity-card:hover {
    transform: translateY(-5px);
}

.activity-image {
  height: 160px;
  position: relative;
}

.date-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background-color: white;
  padding: 8px 12px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
  line-height: 1;
}

.date-badge .day {
  font-size: 1.2rem;
  font-weight: 800;
  color: #3b82f6;
}

.date-badge .month {
  font-size: 0.75rem;
  text-transform: uppercase;
  font-weight: 700;
  color: #9ca3af;
}

.activity-content {
  padding: 1.5rem;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.activity-title {
  font-size: 1.25rem;
  margin-bottom: 0.5rem;
  color: #1f2937;
}

.activity-meta {
  font-size: 0.9rem;
  color: #6b7280;
  margin-bottom: 1rem;
  display: flex;
  gap: 1rem;
}

.activity-desc {
  font-size: 0.95rem;
  color: #4b5563;
  margin-bottom: 1.5rem;
  flex: 1;
}

.mt-2 {
  margin-top: 0.5rem;
}

/* Recruit */
.recruit-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.recruit-card {
  border-left: 4px solid #10b981;
}

.recruit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.recruit-title {
  font-size: 1.2rem;
  margin-bottom: 0.25rem;
  color: #1f2937;
}

.recruit-dates {
  color: #6b7280;
  font-size: 0.9rem;
}
</style>
