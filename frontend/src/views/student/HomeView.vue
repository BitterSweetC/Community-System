<template>
  <div class="home-container">
    <!-- Hero Section with Activity Carousel -->
    <div class="hero-section">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <el-carousel :interval="5000" arrow="always" height="480px" indicator-position="outside">
          <el-carousel-item v-for="activity in featuredActivities" :key="activity.id">
            <div class="carousel-content" :style="activity.coverUrl ? { backgroundImage: `linear-gradient(rgba(0,0,0,0.5), rgba(0,0,0,0.5)), url(${activity.coverUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}">
              <h1 class="hero-title">{{ activity.title }}</h1>
              <p class="hero-subtitle">{{ activity.description }}</p>
              <div class="hero-meta">
                <span><CalendarIcon class="inline-icon" /> {{ formatDate(activity.startTime) }}</span>
                <span><MapPinIcon class="inline-icon" /> {{ activity.location }}</span>
              </div>
              <el-button type="primary" class="hero-btn" @click="handleActivitySignup(activity)">立即报名</el-button>
            </div>
          </el-carousel-item>
          <!-- Fallback if no activities -->
          <el-carousel-item v-if="featuredActivities.length === 0">
            <div class="carousel-content">
              <h1 class="hero-title">探索多彩校园生活</h1>
              <p class="hero-subtitle">加入社团，发现兴趣，结识志同道合的朋友。</p>
              <el-button type="primary" class="hero-btn" @click="router.push('/home/clubs')">浏览社团</el-button>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>
    </div>

    <div class="premium-container main-content">
      <!-- Quick Access Grid -->
      <div class="quick-access-grid">
        <div class="access-item" @click="router.push('/home/clubs')">
          <div class="icon-circle bg-blue">
            <MagnifyingGlassIcon class="hero-icon" />
          </div>
          <h3>加入社团</h3>
          <p>百会社团等你来选</p>
        </div>
        <div class="access-item" @click="router.push('/home/activities')">
          <div class="icon-circle bg-green">
            <CalendarDaysIcon class="hero-icon" />
          </div>
          <h3>近期活动</h3>
          <p>精彩活动不容错过</p>
        </div>
        <div class="access-item" @click="router.push('/user/create-club')">
          <div class="icon-circle bg-purple">
            <PlusCircleIcon class="hero-icon" />
          </div>
          <h3>创建社团</h3>
          <p>开启你的社长之旅</p>
        </div>
        <div class="access-item" @click="router.push('/home/notices')">
          <div class="icon-circle bg-orange">
            <BellAlertIcon class="hero-icon" />
          </div>
          <h3>最新公告</h3>
          <p>校园资讯一手掌握</p>
        </div>
      </div>

      <el-row :gutter="40" class="content-row">
        <!-- Left: Popular Clubs -->
        <el-col :md="8" class="section-col">
          <div class="section-header-small">
            <h3><FireIcon class="section-icon" /> 热门社团</h3>
            <el-button link @click="router.push('/home/clubs')">更多</el-button>
          </div>
          <div class="popular-clubs-list">
            <div v-for="club in clubs.slice(0, 5)" :key="club.id" class="mini-club-card" @click="router.push(`/home/clubs/${club.id}`)">
              <div v-if="club.logoUrl" class="mini-club-logo">
                <img :src="club.logoUrl" alt="logo" style="width: 100%; height: 100%; object-fit: cover; border-radius: 50%;" />
              </div>
              <div v-else class="mini-club-logo" :style="{ background: getRandomColor(club.id) }">
                {{ club.name.charAt(0) }}
              </div>
              <div class="mini-club-info">
                <h4>{{ club.name }}</h4>
                <span class="category-tag">{{ club.category }}</span>
              </div>
            </div>
          </div>
        </el-col>

        <!-- Center: Activity Cards -->
        <el-col :md="10" class="section-col" id="activities-section">
          <div class="section-header-small">
            <h3><CalendarDaysIcon class="section-icon" /> 精彩活动</h3>
            <el-button link @click="router.push('/home/activities')">更多</el-button>
          </div>
          <div class="activity-feed">
            <el-card v-for="activity in activities" :key="activity.id" class="feed-activity-card" shadow="hover">
              <div v-if="activity.coverUrl" class="feed-cover">
                <img :src="activity.coverUrl" alt="cover" style="width: 100%; height: 120px; object-fit: cover; border-radius: 8px 8px 0 0;" />
              </div>
              <div class="feed-date">
                <span class="day">{{ new Date(activity.startTime).getDate() }}</span>
                <span class="month">{{ new Date(activity.startTime).getMonth() + 1 }}月</span>
              </div>
              <div class="feed-content">
                <h4>{{ activity.title }}</h4>
                <p class="location"><MapPinIcon class="inline-icon" /> {{ activity.location || '线上活动' }}</p>
                <div class="feed-footer">
                  <span class="club-name">{{ activity.clubName }}</span>
                  <el-button type="primary" link size="small" @click="handleActivitySignup(activity)">报名</el-button>
                </div>
              </div>
            </el-card>
            <el-empty v-if="activities.length === 0" description="暂无活动" />
          </div>
        </el-col>

        <!-- Right: Notices -->
        <el-col :md="6" class="section-col">
          <div class="section-header-small">
            <h3><MegaphoneIcon class="section-icon" /> 公告通知</h3>
            <el-button link @click="router.push('/home/notices')">更多</el-button>
          </div>
          <div class="notice-list-simple">
            <div v-for="notice in notices" :key="notice.id" class="simple-notice-item">
              <span class="dot"></span>
              <div class="notice-text">
                <div class="notice-title">{{ notice.title }}</div>
                <div class="notice-time">{{ formatDate(notice.publishedAt) }}</div>
              </div>
            </div>
            <el-empty v-if="notices.length === 0" description="暂无公告" />
          </div>
        </el-col>
      </el-row>

      <!-- Activity Calendar Section -->
      <div class="calendar-section-wrapper" id="calendar-section">
        <div class="section-header">
          <h2>交互式活动日历</h2>
          <p>查看每日精彩，合理安排时间</p>
        </div>
        <el-calendar v-model="calendarDate">
          <template #date-cell="{ data }">
            <div class="calendar-cell" :class="{ 'has-activity': getActivitiesForDate(data.day).length > 0 }">
              <div class="calendar-day">{{ data.day.split('-').slice(2).join('') }}</div>
              <div class="calendar-activities">
                <div v-for="act in getActivitiesForDate(data.day).slice(0, 2)" :key="act.id" class="calendar-activity-item">
                   {{ act.title }}
                </div>
                <div v-if="getActivitiesForDate(data.day).length > 2" class="more-activities">
                  +{{ getActivitiesForDate(data.day).length - 2 }} 更多
                </div>
              </div>
            </div>
          </template>
        </el-calendar>
      </div>

      <!-- Recruit Section -->
      <div class="recruit-section-wrapper" id="recruit-section">
        <div class="section-header">
          <h2>社团招新专区</h2>
          <p>加入我们，共创精彩</p>
        </div>
        <el-row :gutter="24" v-if="recruitingClubs.length > 0">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="club in recruitingClubs" :key="club.id">
             <el-card class="recruit-poster-card" shadow="hover">
               <div class="poster-header" :style="{ background: getRandomColor(club.id) }">
                 <h3>{{ club.name }}</h3>
               </div>
               <div class="poster-body">
                 <p class="slogan">{{ club.description ? club.description.substring(0, 30) + '...' : '欢迎加入我们！' }}</p>
                 <el-button type="primary" round class="w-full" @click="router.push(`/home/clubs/${club.id}`)">查看详情</el-button>
               </div>
             </el-card>
          </el-col>
        </el-row>
        <el-empty v-else description="暂无招新的社团" />
      </div>
    </div>

    <!-- Footer -->
    <footer class="site-footer">
      <div class="premium-container footer-content">
        <div class="footer-col">
          <h3>社团之家</h3>
          <p>连接每一位同学，丰富校园生活。</p>
        </div>
        <div class="footer-col">
          <h3>快速导航</h3>
          <a @click.prevent="router.push('/home/clubs')" href="#">浏览社团</a>
          <a @click.prevent="router.push('/home/activities')" href="#">近期活动</a>
          <a @click.prevent="router.push('/home/notices')" href="#">公告通知</a>
        </div>
        <div class="footer-col">
          <h3>我的</h3>
          <a @click.prevent="router.push('/user/profile')" href="#">个人资料</a>
          <a @click.prevent="router.push('/user/activities')" href="#">我的活动</a>
          <a @click.prevent="router.push('/user/create-club')" href="#">创建社团</a>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2025 校园社团管理系统. 版权所有。</p>
      </div>
    </footer>

    <!-- Chat Widget -->
    <ChatWidget />

    <!-- Interest Selection Dialog -->
    <el-dialog
      v-model="showInterestDialog"
      title="选择您的兴趣爱好"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      center
    >
      <div class="interest-dialog-content">
        <p class="dialog-desc">选择您感兴趣的领域，我们将为您推荐相关的社团和活动。</p>
        <InterestSelector v-model="selectedInterests" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showInterestDialog = false">暂不选择</el-button>
          <el-button type="primary" @click="saveInterests" :loading="savingInterests">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import { MagnifyingGlassIcon, CalendarDaysIcon, PlusCircleIcon, BellAlertIcon, MapPinIcon, CalendarIcon, FireIcon, MegaphoneIcon } from '@heroicons/vue/24/outline'
import { ElMessage, ElMessageBox } from 'element-plus'
import InterestSelector from '@/components/InterestSelector.vue'

const router = useRouter()
const authStore = useAuthStore()
const clubs = ref([])
const notices = ref([])
const activities = ref([])
const featuredActivities = ref([])
const calendarDate = ref(new Date())
const calendarActivities = ref([])
const recruitingClubs = ref([])

// Interest Dialog State
const showInterestDialog = ref(false)
const selectedInterests = ref([])
const savingInterests = ref(false)

onMounted(async () => {
  // Check if user needs to set interests
  if (authStore.user && !authStore.user.interests) {
    // Double check with backend to be sure
    try {
      const userRes = await axios.get('/users/me')
      if (userRes && !userRes.interests) {
        showInterestDialog.value = true
      }
    } catch (e) {
      console.warn('Failed to check user interests', e)
    }
  }

  try {
    // 1. Fetch Clubs
    const clubRes = await axios.get('/clubs')
    if (clubRes.list) clubs.value = clubRes.list
    else if (clubRes.content) clubs.value = clubRes.content
    else clubs.value = clubRes

    // 2. Fetch Notices
    try {
        const noticeRes = await axios.get('/notices')
        if (noticeRes.list) notices.value = noticeRes.list.slice(0, 5)
        else notices.value = noticeRes.slice(0, 5)
    } catch (e) { console.warn("Failed to fetch notices", e) }

    // 3. Fetch Activities
    try {
        const actRes = await axios.get('/activities?size=5') 
        if (actRes.list) {
            activities.value = actRes.list
            featuredActivities.value = actRes.list.slice(0, 3) // Top 3 for carousel
        }
        
        // Fetch more for calendar
        const calRes = await axios.get('/activities?size=50')
        if (calRes.list) {
            calendarActivities.value = calRes.list
        }
    } catch (e) { console.warn("Failed to fetch activities", e) }

    // Fetch recruiting clubs
    try {
        const recruitRes = await axios.get('/recruit/active-clubs')
        if (recruitRes) {
            if (Array.isArray(recruitRes)) {
                recruitingClubs.value = recruitRes
            } else if (recruitRes.list && Array.isArray(recruitRes.list)) {
                recruitingClubs.value = recruitRes.list
            } else if (recruitRes.content && Array.isArray(recruitRes.content)) {
                recruitingClubs.value = recruitRes.content
            } else {
                // Keep empty array if format is not recognized to avoid template errors
                console.warn("Unexpected response format for recruiting clubs", recruitRes)
            }
        }
    } catch (e) { console.warn("Failed to fetch recruiting clubs", e) }

  } catch (error) {
    console.error(error)
  }
})

const getActivitiesForDate = (date) => {
    const dateStr = new Date(date).toISOString().split('T')[0]
    return calendarActivities.value.filter(act => {
        if (!act.startTime) return false
        return new Date(act.startTime).toISOString().split('T')[0] === dateStr
    })
}

const scrollToActivities = () => {
  document.getElementById('activities-section').scrollIntoView({ behavior: 'smooth' })
}

const handleActivitySignup = async (activity) => {
    if (!authStore.token) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
    }
    try {
        await axios.post(`/activities/${activity.id}/signup`)
        ElMessage.success('报名成功')
    } catch (error) {
        if (error.message && error.message.includes('请先加入')) {
            ElMessageBox.alert(error.message, '提示', {
                confirmButtonText: '确定',
                type: 'warning'
            })
        } else {
            ElMessage.error('报名失败: ' + error.message)
        }
    }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const colors = ['#1F2937', '#374151', '#4B5563', '#111827', '#000000', '#4f46e5', '#059669', '#d97706']
const getRandomColor = (id) => colors[id % colors.length]

const saveInterests = async () => {
  if (selectedInterests.value.length === 0) {
    ElMessage.warning('请至少选择一个兴趣')
    return
  }
  
  savingInterests.value = true
  try {
    const interestsStr = selectedInterests.value.join(',')
    
    // First get current user data to avoid overwriting other fields with nulls if update endpoint is partial
    // Assuming backend supports partial updates via PUT or we need to send full object. 
    // The UserController.updateProfile uses PUT and checks for nulls, so we can just send what we update.
    // BUT, we need to be safe. Let's re-use the update logic from UserProfileView roughly.
    
    // We can just send the interests field if the backend implementation supports it (which we checked earlier, it does)
    await axios.put('/users/me', {
      interests: interestsStr
    })
    
    // Update local store
    const updatedUser = { ...authStore.user, interests: interestsStr }
    authStore.setUser(updatedUser)
    
    ElMessage.success('兴趣保存成功')
    showInterestDialog.value = false
  } catch (error) {
    ElMessage.error('保存失败: ' + (error.response?.data?.message || error.message))
  } finally {
    savingInterests.value = false
  }
}
</script>

<style scoped>
.interest-dialog-content {
  padding: 10px 0;
}

.dialog-desc {
  color: #6b7280;
  margin-bottom: 20px;
  text-align: center;
}

.home-container {
  background-color: #f9fafb;
}

/* Hero Section */
.hero-section {
  position: relative;
  background-color: #111827;
  color: white;
  width: 100%;
  overflow: hidden;
}

.hero-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 1;
}

.hero-content {
  position: relative;
  z-index: 2;
  padding-top: 80px; /* Space for navbar */
  width: 100%;
}

.carousel-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 0 40px;
  background-size: cover;
  background-position: center;
}

.hero-title {
  font-size: 3.2rem;
  font-weight: 900;
  margin-bottom: 1rem;
  text-shadow: 0 2px 12px rgba(0,0,0,0.5);
  letter-spacing: -0.02em;
}

.hero-subtitle {
  font-size: 1.2rem;
  margin-bottom: 1.5rem;
  opacity: 0.92;
  max-width: 680px;
  line-height: 1.7;
  text-shadow: 0 1px 6px rgba(0,0,0,0.4);
}

.hero-meta {
  display: flex;
  gap: 24px;
  margin-bottom: 2rem;
  font-size: 1rem;
  opacity: 0.9;
  background: rgba(0,0,0,0.25);
  padding: 10px 20px;
  border-radius: 30px;
  backdrop-filter: blur(4px);
}

/* Quick Access */
.quick-access-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-top: 0;
  position: relative;
  z-index: 3;
  margin-bottom: 40px;
  padding-top: 32px;
}

.access-item {
  background: white;
  padding: 24px 20px;
  border-radius: 16px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s;
  border: 1px solid rgba(0,0,0,0.04);
}

.access-item:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.14);
}

.icon-circle {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
  color: white;
  font-size: 1.5rem;
}

.hero-icon {
  width: 26px;
  height: 26px;
  color: white;
  stroke-width: 1.8;
}

.section-icon {
  width: 18px;
  height: 18px;
  display: inline-block;
  vertical-align: middle;
  margin-right: 4px;
  color: inherit;
  stroke-width: 2;
}

.inline-icon {
  width: 14px;
  height: 14px;
  display: inline-block;
  vertical-align: middle;
  margin-right: 3px;
  stroke-width: 2;
}

.bg-blue { background-color: #3b82f6; }
.bg-green { background-color: #10b981; }
.bg-purple { background-color: #8b5cf6; }
.bg-orange { background-color: #f59e0b; }

.access-item h3 {
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 5px;
  color: #1f2937;
}

.access-item p {
  font-size: 0.85rem;
  color: #6b7280;
}

/* Main Content Layout */
.content-row {
  margin-bottom: 40px;
}

.section-col {
  margin-bottom: 20px;
}

.section-header-small {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  border-bottom: 2px solid #e5e7eb;
  padding-bottom: 10px;
}

.section-header-small h3 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #374151;
  margin: 0;
}

/* Popular Clubs */
.popular-clubs-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.mini-club-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.mini-club-card:hover {
  background-color: #f3f4f6;
}

.mini-club-logo {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
}

.mini-club-info h4 {
  font-size: 1rem;
  margin: 0 0 4px;
  color: #1f2937;
}

.category-tag {
  font-size: 0.75rem;
  color: #6b7280;
  background: #e5e7eb;
  padding: 2px 6px;
  border-radius: 4px;
}

/* Activity Feed */
.activity-feed {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.feed-activity-card {
  display: flex;
  flex-direction: row !important; /* Force row layout if card overrides */
}

.feed-activity-card :deep(.el-card__body) {
  display: flex;
  width: 100%;
  padding: 15px;
}

.feed-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  color: #3b82f6;
  padding: 10px;
  border-radius: 8px;
  margin-right: 15px;
  min-width: 60px;
}

.feed-date .day {
  font-size: 1.5rem;
  font-weight: 800;
  line-height: 1;
}

.feed-date .month {
  font-size: 0.8rem;
}

.feed-content {
  flex: 1;
}

.feed-content h4 {
  margin: 0 0 5px;
  font-size: 1.1rem;
  color: #1f2937;
}

.feed-content .location {
  font-size: 0.9rem;
  color: #6b7280;
  margin-bottom: 8px;
}

.feed-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.club-name {
  font-size: 0.85rem;
  color: #9ca3af;
}

/* Notices Simple */
.notice-list-simple {
  background: white;
  border-radius: 8px;
  padding: 15px;
}

.simple-notice-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f3f4f6;
}

.simple-notice-item:last-child {
  border-bottom: none;
}

.dot {
  width: 6px;
  height: 6px;
  background: #f59e0b;
  border-radius: 50%;
  margin-top: 8px;
  flex-shrink: 0;
}

.notice-text {
  flex: 1;
}

.notice-title {
  font-size: 0.95rem;
  color: #374151;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-time {
  font-size: 0.75rem;
  color: #9ca3af;
}

/* Recruit Section */
.recruit-section-wrapper {
  background: white;
  padding: 30px;
  border-radius: 12px;
  margin-bottom: 40px;
}

/* Calendar Section */
.calendar-section-wrapper {
  background: white;
  padding: 30px;
  border-radius: 12px;
  margin-bottom: 40px;
}

.calendar-cell {
  height: 100%;
  padding: 4px;
  display: flex;
  flex-direction: column;
}

.calendar-day {
  font-size: 14px;
  margin-bottom: 4px;
  text-align: center;
}

.has-activity .calendar-day {
  color: #3b82f6;
  font-weight: bold;
}

.calendar-activities {
  flex: 1;
  overflow: hidden;
  font-size: 12px;
}

.calendar-activity-item {
  background-color: #eff6ff;
  color: #3b82f6;
  padding: 2px 4px;
  border-radius: 4px;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.more-activities {
  text-align: center;
  color: #9ca3af;
  font-size: 10px;
}

.section-header {
  text-align: center;
  margin-bottom: 30px;
}

.section-header h2 {
  font-size: 2rem;
  color: #1f2937;
  margin-bottom: 10px;
}

.section-header p {
  color: #6b7280;
}

.recruit-poster-card {
  height: 100%;
  border: none;
  background: #f9fafb;
  transition: transform 0.2s;
}

.recruit-poster-card:hover {
  transform: translateY(-5px);
}

.poster-header {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  border-radius: 8px 8px 0 0;
}

.poster-body {
  padding: 20px;
  text-align: center;
}

.slogan {
  color: #4b5563;
  margin-bottom: 20px;
  height: 40px;
  font-size: 0.9rem;
}

.w-full {
  width: 100%;
}

/* Footer */
.site-footer {
  background: #1f2937;
  color: #d1d5db;
  padding: 60px 0 20px;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 40px;
  margin-bottom: 40px;
  border-bottom: 1px solid #374151;
  padding-bottom: 40px;
}

.footer-col h3 {
  color: white;
  margin-bottom: 20px;
}

.footer-col a {
  display: block;
  color: #9ca3af;
  text-decoration: none;
  margin-bottom: 10px;
  transition: color 0.2s;
}

.footer-col a:hover {
  color: white;
}

.footer-bottom {
  text-align: center;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .quick-access-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .hero-title {
    font-size: 2rem;
  }
}
</style>
