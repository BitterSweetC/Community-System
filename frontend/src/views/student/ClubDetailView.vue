<template>
  <div class="club-detail-page">
    <section v-if="club" class="club-hero">
      <div class="hero-grid"></div>
      <div class="hero-content premium-container">
        <div class="hero-main">
          <p class="hero-kicker">Club Profile</p>
          <div class="hero-title-wrap">
            <h1>{{ club.name }}</h1>
            <el-tag class="hero-tag">{{ club.category || '未分类' }}</el-tag>
          </div>
          <p class="hero-desc">{{ club.description || '该社团暂未填写简介。' }}</p>
          <div class="hero-meta">
            <span>成立年份：{{ club.foundedYear || '2024' }}</span>
            <span>状态：{{ club.status === 'ACTIVE' ? '活跃中' : '筹备中' }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="premium-container content-section" v-if="club">
      <el-tabs v-model="activeTab" class="club-tabs">
        <el-tab-pane label="社团概况" name="overview">
          <div class="tab-content">
            <el-row :gutter="20">
              <el-col :xs="24" :lg="16">
                <article class="panel-block" v-if="showcaseImages.length > 0">
                  <h3 class="section-title">社团风采</h3>
                  <div class="showcase-grid">
                    <div v-for="(item, index) in showcaseImages" :key="`${item.url}-${index}`" class="showcase-item">
                      <img :src="item.url" :alt="item.label" loading="lazy" />
                      <span class="showcase-tag">{{ item.type }}</span>
                    </div>
                  </div>
                </article>
                <article class="panel-block" v-else>
                  <h3 class="section-title">社团风采</h3>
                  <el-empty description="暂无社团图片，先去看看活动内容吧" />
                </article>

                <article class="panel-block">
                  <h3 class="section-title">关于我们</h3>
                  <p class="description-text">{{ club.description || '暂无简介' }}</p>
                </article>

                <article class="panel-block" v-if="notices.length > 0">
                  <h3 class="section-title">最新动态</h3>
                  <div class="notice-list">
                    <el-card v-for="notice in notices" :key="notice.id" class="notice-item" shadow="hover">
                      <div class="notice-head">
                        <span>{{ formatDate(notice.publishedAt) }}</span>
                        <el-tag size="small" effect="plain">公告</el-tag>
                      </div>
                      <h4>{{ notice.title }}</h4>
                      <p>{{ notice.content || '暂无内容' }}</p>
                    </el-card>
                  </div>
                </article>
              </el-col>

              <el-col :xs="24" :lg="8">
                <el-card class="action-card" shadow="never">
                  <h3>加入我们</h3>
                  <p>查看招新信息，提交申请成为社团成员。</p>
                  <el-button class="full-btn" type="primary" @click="activeTab = 'recruit'">查看招新</el-button>
                  <el-divider />
                  <div class="action-meta">
                    <p><strong>联系角色：</strong>{{ club.presidentName || '社长' }}</p>
                    <el-button link @click="activeTab = 'activities'">先看近期活动</el-button>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <el-tab-pane label="社团活动" name="activities">
          <div class="tab-content">
            <div class="activities-grid" v-if="activities.length > 0">
              <el-card v-for="activity in activities" :key="activity.id" class="activity-card" shadow="hover">
                <div class="activity-cover">
                  <img
                    v-if="activity.coverUrl"
                    :src="activity.coverUrl"
                    :alt="activity.title"
                    class="activity-cover-image"
                    loading="lazy"
                  />
                  <div
                    v-else
                    class="activity-cover-fallback"
                    :style="{ background: getRandomGradient(activity.id) }"
                  >
                    <span>活动封面</span>
                  </div>
                  <div class="date-badge">
                    <span class="day">{{ getDay(activity.startTime) }}</span>
                    <span class="month">{{ getMonth(activity.startTime) }}月</span>
                  </div>
                </div>
                <div class="activity-body">
                  <h3>{{ activity.title }}</h3>
                  <div class="meta-line">
                    <span>地点：{{ activity.location || '待定' }}</span>
                    <span>时间：{{ formatTime(activity.startTime) }}</span>
                  </div>
                  <p>{{ activity.description || '暂无活动描述。' }}</p>
                  <el-button class="full-btn" type="primary" plain @click="handleActivitySignup(activity)">
                    立即报名
                  </el-button>
                </div>
              </el-card>
            </div>
            <el-empty v-else description="暂无活动" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="招新信息" name="recruit">
          <div class="tab-content">
            <h3 class="section-title">开放批次</h3>
            <div class="recruit-list" v-if="batches.length > 0">
              <el-card v-for="batch in batches" :key="batch.id" class="recruit-card" shadow="hover">
                <div class="recruit-header">
                  <div>
                    <h4>{{ batch.title }}</h4>
                    <p>{{ formatDate(batch.startTime) }} - {{ formatDate(batch.endTime) }}</p>
                  </div>
                  <div class="recruit-actions">
                    <el-tag :type="getBatchStatusType(batch)">{{ getBatchStatus(batch) }}</el-tag>
                    <el-button
                      type="primary"
                      @click="handleApplyClick(batch)"
                      :disabled="getBatchStatus(batch) !== '进行中'"
                    >
                      立即申请
                    </el-button>
                  </div>
                </div>
              </el-card>
            </div>
            <el-empty v-else description="暂无招新批次" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <section v-else class="premium-container empty-wrap">
      <el-empty description="未找到社团信息" />
    </section>

    <el-dialog v-model="applyDialogVisible" title="申请表单" width="560px" destroy-on-close align-center>
      <el-form :model="applyForm" label-position="top" size="large">
        <div v-for="field in currentFields" :key="field.id">
          <el-form-item :label="field.label" :required="field.required">
            <el-input
              v-if="field.type === 'TEXT'"
              v-model="applyData[field.fieldKey]"
              :placeholder="`请输入${field.label}`"
            />
            <el-input
              v-else-if="field.type === 'TEXTAREA'"
              type="textarea"
              :rows="4"
              v-model="applyData[field.fieldKey]"
              :placeholder="`请输入${field.label}`"
            />
            <el-input
              v-else
              v-model="applyData[field.fieldKey]"
              :placeholder="`请输入${field.label}`"
            />
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
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '@/api/axios'
import { useAuthStore } from '@/stores/auth'

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

const showcaseImages = computed(() => {
  const items = []
  const seen = new Set()

  const pushUnique = (url, label, type) => {
    if (!url || seen.has(url)) {
      return
    }
    seen.add(url)
    items.push({ url, label, type })
  }

  if (club.value?.logoUrl) {
    pushUnique(club.value.logoUrl, `${club.value.name || '社团'} Logo`, '社团')
  }

  activities.value
    .filter((activity) => Boolean(activity.coverUrl))
    .slice(0, 8)
    .forEach((activity) => {
      pushUnique(activity.coverUrl, activity.title || '活动封面', '活动')
    })

  return items
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

const loadClubData = async () => {
  const clubId = route.params.id
  try {
    club.value = await axios.get(`/clubs/${clubId}`)

    const [batchRes, activityRes, noticeRes] = await Promise.allSettled([
      axios.get(`/recruit/batches?clubId=${clubId}`),
      axios.get(`/activities?clubId=${clubId}`),
      axios.get(`/notices?clubId=${clubId}`)
    ])

    if (batchRes.status === 'fulfilled') {
      batches.value = normalizeList(batchRes.value)
    }

    if (activityRes.status === 'fulfilled') {
      activities.value = normalizeList(activityRes.value)
    }

    if (noticeRes.status === 'fulfilled') {
      notices.value = normalizeList(noticeRes.value)
    }

    await handleAutoActions()
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || error.message || '社团信息加载失败')
  }
}

const handleAutoActions = async () => {
  if (route.query.action === 'apply' && route.query.batchId) {
    const batchId = Number(route.query.batchId)
    const batch = batches.value.find((item) => Number(item.id) === batchId)
    if (batch) {
      activeTab.value = 'recruit'
      await openApply(batch)
    }
    router.replace({ path: route.path, query: {} })
    return
  }

  if (route.query.action === 'signup' && route.query.activityId) {
    const activityId = Number(route.query.activityId)
    const activity = activities.value.find((item) => Number(item.id) === activityId)
    if (activity) {
      activeTab.value = 'activities'
      await performActivitySignup(activity)
    }
    router.replace({ path: route.path, query: {} })
  }
}

const getBatchStatus = (batch) => {
  const now = new Date()
  const start = new Date(batch.startTime)
  const end = new Date(batch.endTime)

  if (now < start) {
    return '未开始'
  }
  if (now > end) {
    return '已结束'
  }
  return '进行中'
}

const getBatchStatusType = (batch) => {
  const status = getBatchStatus(batch)
  if (status === '进行中') {
    return 'success'
  }
  if (status === '未开始') {
    return 'warning'
  }
  return 'info'
}

const handleApplyClick = (batch) => {
  if (!authStore.token) {
    router.push({
      path: '/login',
      query: {
        redirect: `${route.path}?action=apply&batchId=${batch.id}`
      }
    })
    return
  }

  openApply(batch)
}

const openApply = async (batch) => {
  try {
    currentBatch.value = batch
    currentFields.value = normalizeList(await axios.get(`/recruit/fields?batchId=${batch.id}`))
    applyData.value = {}

    currentFields.value.forEach((field) => {
      applyData.value[field.fieldKey] = ''
    })

    applyDialogVisible.value = true
  } catch (error) {
    ElMessage.error(`加载申请表失败: ${error.response?.data?.message || error.message}`)
  }
}

const submitApply = async () => {
  if (!currentBatch.value) {
    return
  }

  try {
    await axios.post('/recruit/applications', {
      batch: { id: currentBatch.value.id },
      applyData: JSON.stringify(applyData.value)
    })
    ElMessage.success('申请提交成功')
    applyDialogVisible.value = false
  } catch (error) {
    ElMessage.error(`提交失败: ${error.response?.data?.message || error.message}`)
  }
}

const handleActivitySignup = async (activity) => {
  if (!authStore.token) {
    router.push({
      path: '/login',
      query: {
        redirect: `${route.path}?action=signup&activityId=${activity.id}`
      }
    })
    return
  }

  await performActivitySignup(activity)
}

const performActivitySignup = async (activity) => {
  try {
    await axios.post(`/activities/${activity.id}/signup`)
    ElMessage.success('报名成功')
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '报名失败'
    if (msg.includes('请先加入')) {
      ElMessageBox.alert(msg, '提示', {
        confirmButtonText: '确定',
        type: 'warning'
      })
      return
    }
    ElMessage.error(`报名失败: ${msg}`)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) {
    return '-'
  }
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const formatTime = (dateStr) => {
  if (!dateStr) {
    return '-'
  }
  return new Date(dateStr).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getDay = (dateStr) => {
  if (!dateStr) {
    return '--'
  }
  return String(new Date(dateStr).getDate()).padStart(2, '0')
}

const getMonth = (dateStr) => {
  if (!dateStr) {
    return '--'
  }
  return String(new Date(dateStr).getMonth() + 1).padStart(2, '0')
}

const gradients = [
  'linear-gradient(135deg, #1f6fa8 0%, #15507e 100%)',
  'linear-gradient(135deg, #0f766e 0%, #115e59 100%)',
  'linear-gradient(135deg, #c2410c 0%, #9a3412 100%)'
]

const getRandomGradient = (id) => gradients[Math.abs(Number(id) || 0) % gradients.length]

onMounted(() => {
  loadClubData()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;700&display=swap');

* {
  box-sizing: border-box;
}

.club-detail-page {
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

.club-hero {
  position: relative;
  padding: 110px 0 40px;
  overflow: hidden;
}

.hero-grid {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 8% 16%, rgba(15, 118, 110, 0.18), transparent 30%),
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

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-kicker {
  margin: 0 0 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-weight: 700;
  color: var(--muted);
  font-size: 0.8rem;
}

.hero-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-title-wrap h1 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2rem, 4vw, 3rem);
}

.hero-tag {
  border: 1px solid rgba(15, 28, 42, 0.16);
  background: rgba(255, 255, 255, 0.72);
  color: #355069;
}

.hero-desc {
  margin: 12px 0 0;
  color: var(--muted);
  line-height: 1.7;
  max-width: 760px;
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

.content-section {
  margin-top: 4px;
}

.club-tabs {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--surface);
  backdrop-filter: blur(8px);
  box-shadow: 0 12px 24px rgba(15, 28, 42, 0.08);
  padding: 18px;
}

.tab-content {
  padding-top: 10px;
}

.panel-block {
  margin-bottom: 20px;
}

.section-title {
  margin: 0 0 12px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.42rem;
}

.description-text {
  margin: 0;
  line-height: 1.75;
  color: var(--muted);
}

.notice-list {
  display: grid;
  gap: 10px;
}

.showcase-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
}

.showcase-item {
  position: relative;
  height: 130px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(15, 28, 42, 0.1);
  background: #edf2f4;
}

.showcase-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.showcase-tag {
  position: absolute;
  left: 8px;
  bottom: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 0.74rem;
  color: #f8fafc;
  background: rgba(15, 28, 42, 0.72);
}

.notice-item {
  border-radius: 12px;
}

.notice-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  color: #6a7b8f;
  font-size: 0.85rem;
}

.notice-item h4 {
  margin: 0 0 6px;
}

.notice-item p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}

.action-card {
  border-radius: 14px;
  position: sticky;
  top: 90px;
}

.action-card h3 {
  margin: 0 0 8px;
}

.action-card p {
  margin: 0 0 10px;
  color: var(--muted);
  line-height: 1.6;
}

.full-btn {
  width: 100%;
}

.action-meta p {
  margin: 0;
  color: var(--muted);
}

.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.activity-card {
  border-radius: 14px;
  overflow: hidden;
}

.activity-cover {
  height: 140px;
  position: relative;
  overflow: hidden;
}

.activity-cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.activity-cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.86rem;
  letter-spacing: 0.04em;
}

.date-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  border-radius: 10px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.9);
  color: #1f6fa8;
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1;
}

.date-badge .day {
  font-weight: 800;
  font-size: 1.12rem;
}

.date-badge .month {
  margin-top: 3px;
  font-size: 0.74rem;
}

.activity-body {
  padding: 14px;
}

.activity-body h3 {
  margin: 0 0 8px;
  font-size: 1.08rem;
}

.meta-line {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  color: #4f6378;
  font-size: 0.84rem;
  margin-bottom: 8px;
}

.activity-body p {
  min-height: 44px;
  margin: 0 0 12px;
  color: var(--muted);
  line-height: 1.6;
  font-size: 0.9rem;
}

.recruit-list {
  display: grid;
  gap: 10px;
}

.recruit-card {
  border-radius: 12px;
}

.recruit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.recruit-header h4 {
  margin: 0 0 4px;
}

.recruit-header p {
  margin: 0;
  color: var(--muted);
  font-size: 0.88rem;
}

.recruit-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-wrap {
  padding-top: 110px;
}

@media (max-width: 900px) {
  .club-hero {
    padding-top: 92px;
  }

  .club-tabs {
    padding: 12px;
  }

  .action-card {
    position: static;
    margin-top: 10px;
  }
}
</style>
