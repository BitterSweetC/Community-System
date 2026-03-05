<template>
  <div class="home-view">
    <section class="hero-section">
      <div class="hero-bg-layer"></div>
      <div class="premium-container hero-inner">
        <div class="hero-copy">
          <p class="hero-kicker">Campus Orbit</p>
          <h1>发现同频伙伴，点亮你的校园生活</h1>
          <p>
            这里汇聚社团、活动与公告信息。无论你想加入、发起还是组织，
            都可以从这里开始。
          </p>
          <div class="hero-actions">
            <el-button class="hero-btn primary" @click="router.push('/home/activities')">
              查看活动
            </el-button>
            <el-button class="hero-btn ghost" @click="router.push('/home/clubs')">
              浏览社团
            </el-button>
          </div>
        </div>

        <div class="hero-carousel-wrap">
          <el-carousel :interval="5000" arrow="always" height="420px" indicator-position="outside">
            <el-carousel-item v-for="slide in heroSlides" :key="slide.id || slide.title">
              <article class="carousel-card" :style="getHeroSlideStyle(slide)">
                <div class="overlay"></div>
                <div class="card-body">
                  <span class="slide-tag">{{ slide.clubName || '校园活动' }}</span>
                  <h2>{{ slide.title }}</h2>
                  <p>{{ slide.description }}</p>
                  <div class="slide-meta">
                    <span>
                      <CalendarIcon class="inline-icon" />
                      {{ formatDate(slide.startTime) || '时间待定' }}
                    </span>
                    <span>
                      <MapPinIcon class="inline-icon" />
                      {{ slide.location || '地点待定' }}
                    </span>
                  </div>
                  <el-button
                    class="slide-btn"
                    @click="slide.id ? handleActivitySignup(slide) : router.push('/home/activities')"
                  >
                    {{ slide.id ? '立即报名' : '去发现活动' }}
                  </el-button>
                </div>
              </article>
            </el-carousel-item>
          </el-carousel>
        </div>
      </div>
    </section>

    <section class="premium-container quick-access-section">
      <button class="access-card" @click="router.push('/home/clubs')">
        <div class="icon-badge bg-blue">
          <MagnifyingGlassIcon class="hero-icon" />
        </div>
        <h3>加入社团</h3>
        <p>探索社团方向，找到你的组织归属。</p>
      </button>
      <button class="access-card" @click="router.push('/home/activities')">
        <div class="icon-badge bg-green">
          <CalendarDaysIcon class="hero-icon" />
        </div>
        <h3>近期活动</h3>
        <p>精彩活动持续更新，报名更方便。</p>
      </button>
      <button class="access-card" @click="router.push('/user/create-club')">
        <div class="icon-badge bg-orange">
          <PlusCircleIcon class="hero-icon" />
        </div>
        <h3>创建社团</h3>
        <p>召集志同道合的伙伴，一起做点事情。</p>
      </button>
      <button class="access-card" @click="router.push('/home/notices')">
        <div class="icon-badge bg-teal">
          <BellAlertIcon class="hero-icon" />
        </div>
        <h3>公告通知</h3>
        <p>第一时间掌握学校与社团动态。</p>
      </button>
    </section>

    <section class="premium-container panel-grid">
      <article class="panel clubs-panel">
        <header class="panel-head">
          <h3><FireIcon class="section-icon" /> 热门社团</h3>
          <el-button link @click="router.push('/home/clubs')">更多</el-button>
        </header>
        <div v-if="clubs.length" class="clubs-list">
          <button
            v-for="club in clubs.slice(0, 6)"
            :key="club.id"
            class="mini-club-card"
            @click="router.push(`/home/clubs/${club.id}`)"
          >
            <div v-if="club.logoUrl" class="mini-club-logo">
              <img :src="club.logoUrl" alt="logo" />
            </div>
            <div v-else class="mini-club-logo fallback" :style="{ background: getRandomColor(club.id) }">
              {{ (club.name || '社').charAt(0) }}
            </div>
            <div class="mini-club-info">
              <h4>{{ club.name }}</h4>
              <span>{{ club.category || '未分类' }}</span>
            </div>
          </button>
        </div>
        <el-empty v-else description="暂无社团" />
      </article>

      <article class="panel activities-panel" id="activities-section">
        <header class="panel-head">
          <h3><CalendarDaysIcon class="section-icon" /> 精彩活动</h3>
          <el-button link @click="router.push('/home/activities')">更多</el-button>
        </header>

        <div v-if="activities.length" class="activity-list">
          <el-card v-for="activity in activitiesPreview" :key="activity.id" class="activity-card" shadow="hover">
            <div class="activity-main">
              <div class="activity-date">
                <span class="day">{{ getDay(activity.startTime) }}</span>
                <span class="month">{{ getMonth(activity.startTime) }}月</span>
              </div>
              <div class="activity-body">
                <h4>{{ activity.title }}</h4>
                <p>{{ activity.description || '欢迎加入本次活动，详情请点击报名查看。' }}</p>
                <div class="activity-meta">
                  <span><MapPinIcon class="inline-icon" /> {{ activity.location || '线上活动' }}</span>
                  <span>{{ activity.clubName || '社团活动' }}</span>
                </div>
              </div>
            </div>
            <div class="activity-footer">
              <span>{{ formatDate(activity.startTime) }}</span>
              <el-button link type="primary" @click="handleActivitySignup(activity)">报名</el-button>
            </div>
          </el-card>
          <button
            v-if="activities.length > activitiesPreview.length"
            class="more-link-btn"
            @click="router.push('/home/activities')"
          >
            还有 {{ activities.length - activitiesPreview.length }} 场活动，去活动页查看
          </button>
        </div>
        <el-empty v-else description="暂无活动" />
      </article>

      <article class="panel notices-panel">
        <header class="panel-head">
          <h3><MegaphoneIcon class="section-icon" /> 公告通知</h3>
          <el-button link @click="router.push('/home/notices')">更多</el-button>
        </header>
        <div v-if="notices.length" class="notice-list">
          <button v-for="notice in notices" :key="notice.id" class="notice-item" @click="router.push('/home/notices')">
            <span class="notice-dot"></span>
            <div class="notice-content">
              <h4>{{ notice.title }}</h4>
              <p>{{ formatDate(notice.publishedAt) || '刚刚发布' }}</p>
            </div>
          </button>
        </div>
        <el-empty v-else description="暂无公告" />
      </article>
    </section>

    <section class="premium-container calendar-section" id="calendar-section">
      <div class="section-header">
        <h2>活动日历</h2>
        <p>按日期查看活动安排，合理规划你的校园时间。</p>
      </div>
      <el-calendar v-model="calendarDate" class="custom-calendar">
        <template #date-cell="{ data }">
          <div class="calendar-cell" :class="{ 'has-activity': getActivitiesForDate(data.day).length > 0 }">
            <div class="calendar-day">{{ data.day.split('-').slice(2).join('') }}</div>
            <div class="calendar-activities">
              <div v-for="act in getActivitiesForDate(data.day).slice(0, 2)" :key="act.id" class="calendar-activity-item">
                {{ act.title }}
              </div>
              <div v-if="getActivitiesForDate(data.day).length > 2" class="more-activities">
                +{{ getActivitiesForDate(data.day).length - 2 }}
              </div>
            </div>
          </div>
        </template>
      </el-calendar>
    </section>

    <section class="premium-container recruit-section" id="recruit-section">
      <div class="section-header">
        <h2>社团招新专区</h2>
        <p>寻找正在招新的社团，加入你感兴趣的方向。</p>
      </div>

      <el-row :gutter="18" v-if="recruitingClubs.length">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="club in recruitingClubs" :key="club.id">
          <el-card class="recruit-card" shadow="hover">
            <div class="recruit-head" :style="{ background: getRandomColor(club.id) }">
              <h3>{{ club.name }}</h3>
            </div>
            <div class="recruit-body">
              <p>
                {{ club.description ? `${club.description.slice(0, 42)}...` : '欢迎加入我们，一起创造更多可能。' }}
              </p>
              <el-button class="detail-btn" @click="router.push(`/home/clubs/${club.id}`)">查看详情</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-else description="暂无招新社团" />
    </section>

    <footer class="site-footer">
      <div class="premium-container footer-content">
        <div class="footer-col">
          <h3>社团之家</h3>
          <p>连接每一位同学，丰富校园生活。</p>
        </div>
        <div class="footer-col">
          <h3>快速导航</h3>
          <a href="#" @click.prevent="router.push('/home/clubs')">浏览社团</a>
          <a href="#" @click.prevent="router.push('/home/activities')">近期活动</a>
          <a href="#" @click.prevent="router.push('/home/notices')">公告通知</a>
        </div>
        <div class="footer-col">
          <h3>我的</h3>
          <a href="#" @click.prevent="router.push('/user/profile')">个人资料</a>
          <a href="#" @click.prevent="router.push('/user/activities')">我的活动</a>
          <a href="#" @click.prevent="router.push('/user/create-club')">创建社团</a>
        </div>
      </div>
      <div class="footer-bottom">
        <p>© 2026 校园社团管理系统</p>
      </div>
    </footer>

    <el-dialog
      v-model="showInterestDialog"
      title="选择你的兴趣爱好"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      center
    >
      <div class="interest-dialog-content">
        <p class="dialog-desc">选择你感兴趣的领域，我们会推荐相关社团与活动。</p>
        <InterestSelector v-model="selectedInterests" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showInterestDialog = false">暂不选择</el-button>
          <el-button type="primary" :loading="savingInterests" @click="saveInterests">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import {
  BellAlertIcon,
  CalendarDaysIcon,
  CalendarIcon,
  FireIcon,
  MagnifyingGlassIcon,
  MapPinIcon,
  MegaphoneIcon,
  PlusCircleIcon
} from '@heroicons/vue/24/outline'
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

const showInterestDialog = ref(false)
const selectedInterests = ref([])
const savingInterests = ref(false)

const heroSlides = computed(() => {
  if (featuredActivities.value.length > 0) {
    return featuredActivities.value
  }

  return [
    {
      id: null,
      title: '探索多彩校园生活',
      description: '加入社团、参与活动、认识同频伙伴，开启更有趣的大学日常。',
      startTime: null,
      location: '校园内',
      clubName: '推荐活动',
      coverUrl: ''
    }
  ]
})

const activitiesPreview = computed(() => activities.value.slice(0, 4))

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

const toDateKey = (value) => {
  if (!value) {
    return ''
  }

  const d = new Date(value)
  if (Number.isNaN(d.getTime())) {
    return ''
  }

  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const loadHomeClubs = async () => {
  try {
    const recommended = await axios.get('/clubs/recommended')
    const recommendedList = normalizeList(recommended)
    if (recommendedList.length > 0) {
      clubs.value = recommendedList
      return
    }
  } catch (error) {
    console.warn('Failed to fetch recommended clubs, falling back to /clubs', error)
  }

  try {
    const fallback = await axios.get('/clubs')
    clubs.value = normalizeList(fallback)
  } catch (error) {
    console.warn('Failed to fetch fallback clubs', error)
    clubs.value = []
  }
}

onMounted(async () => {
  if (authStore.token && authStore.user && !authStore.user.interests) {
    try {
      const userRes = await axios.get('/users/me')
      if (userRes && !userRes.interests) {
        showInterestDialog.value = true
      }
    } catch (error) {
      console.warn('Failed to check user interests', error)
    }
  }

  try {
    const [noticeRes, actRes, calRes, recruitRes] = await Promise.allSettled([
      axios.get('/notices'),
      axios.get('/activities?size=6'),
      axios.get('/activities?size=50'),
      axios.get('/recruit/active-clubs')
    ])
    await loadHomeClubs()

    if (noticeRes.status === 'fulfilled') {
      notices.value = normalizeList(noticeRes.value).slice(0, 6)
    }

    if (actRes.status === 'fulfilled') {
      const list = normalizeList(actRes.value)
      activities.value = list
      featuredActivities.value = list.slice(0, 3)
    }

    if (calRes.status === 'fulfilled') {
      calendarActivities.value = normalizeList(calRes.value)
    }

    if (recruitRes.status === 'fulfilled') {
      recruitingClubs.value = normalizeList(recruitRes.value)
    }
  } catch (error) {
    console.error(error)
  }
})

const getHeroSlideStyle = (activity) => {
  if (!activity?.coverUrl) {
    return {}
  }

  return {
    backgroundImage: `linear-gradient(115deg, rgba(15, 28, 42, 0.72), rgba(15, 28, 42, 0.44)), url(${activity.coverUrl})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center'
  }
}

const getActivitiesForDate = (date) => {
  if (!date) {
    return []
  }

  return calendarActivities.value.filter((activity) => toDateKey(activity.startTime) === date)
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
    const msg = error.response?.data?.message || error.message || '报名失败'
    if (msg.includes('请先加入')) {
      ElMessageBox.alert(msg, '提示', {
        confirmButtonText: '确定',
        type: 'warning'
      })
      return
    }
    ElMessage.error(msg)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) {
    return ''
  }

  return new Date(dateStr).toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric'
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

const colors = ['#0f766e', '#115e59', '#c2410c', '#9a3412', '#1f6fa8', '#166534', '#7a3e00']
const getRandomColor = (id) => colors[Math.abs(Number(id) || 0) % colors.length]

const saveInterests = async () => {
  if (selectedInterests.value.length === 0) {
    ElMessage.warning('请至少选择一个兴趣')
    return
  }

  savingInterests.value = true
  try {
    const interestsStr = selectedInterests.value.join(',')
    await axios.put('/users/me', { interests: interestsStr })
    authStore.setUser({ ...authStore.user, interests: interestsStr })
    ElMessage.success('兴趣保存成功')
    showInterestDialog.value = false
  } catch (error) {
    ElMessage.error(`保存失败: ${error.response?.data?.message || error.message}`)
  } finally {
    savingInterests.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;700&display=swap');

* {
  box-sizing: border-box;
}

.home-view {
  --ink: #0f1c2a;
  --muted: #55667a;
  --border: rgba(15, 28, 42, 0.12);
  --surface: rgba(255, 255, 255, 0.88);
  --shadow: 0 16px 34px rgba(15, 28, 42, 0.1);

  background: linear-gradient(180deg, #f6f1e7 0%, #f0eadf 42%, #e9efe9 100%);
  color: var(--ink);
  font-family: 'Outfit', sans-serif;
}

.hero-section {
  position: relative;
  padding: 112px 0 40px;
  overflow: hidden;
}

.hero-bg-layer {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 8% 15%, rgba(15, 118, 110, 0.2), transparent 32%),
    radial-gradient(circle at 92% 20%, rgba(194, 65, 12, 0.2), transparent 36%),
    linear-gradient(165deg, #f7f2e7 0%, #eee6d8 58%, #e4ece8 100%);
}

.hero-bg-layer::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(15, 28, 42, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 28, 42, 0.03) 1px, transparent 1px);
  background-size: 34px 34px;
}

.hero-inner {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  gap: 24px;
  align-items: center;
}

.hero-copy {
  padding-right: 8px;
}

.hero-kicker {
  margin: 0 0 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-weight: 700;
  color: var(--muted);
  font-size: 0.8rem;
}

.hero-copy h1 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2rem, 4vw, 3.2rem);
  line-height: 1.18;
}

.hero-copy p {
  margin: 14px 0 24px;
  color: var(--muted);
  line-height: 1.7;
  max-width: 540px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-btn {
  min-width: 124px;
  border-radius: 999px;
  font-family: inherit;
  font-weight: 700;
}

.hero-btn.primary {
  border: none;
  color: #fff;
  background: linear-gradient(135deg, #c2410c 0%, #9a3412 100%);
}

.hero-btn.ghost {
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.72);
  color: var(--ink);
}

.hero-carousel-wrap {
  border-radius: 22px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.carousel-card {
  position: relative;
  height: 420px;
  padding: 26px;
  display: flex;
  align-items: flex-end;
  border-radius: 20px;
  background: linear-gradient(135deg, #243443 0%, #1b2a35 54%, #1f3b36 100%);
  color: #f7f8f4;
}

.overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.08) 0%, rgba(0, 0, 0, 0.4) 100%);
}

.card-body {
  position: relative;
  z-index: 1;
  width: 100%;
}

.slide-tag {
  display: inline-block;
  margin-bottom: 10px;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.75rem;
  letter-spacing: 0.06em;
  background: rgba(255, 255, 255, 0.16);
}

.card-body h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(1.5rem, 2.5vw, 2rem);
}

.card-body p {
  margin: 10px 0 14px;
  color: rgba(247, 248, 244, 0.88);
  line-height: 1.6;
  max-width: 680px;
}

.slide-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 0.9rem;
  color: rgba(247, 248, 244, 0.86);
  margin-bottom: 12px;
}

.slide-meta span {
  display: inline-flex;
  align-items: center;
}

.slide-btn {
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.38);
  color: #f7f8f4;
  background: rgba(255, 255, 255, 0.12);
}

.slide-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.quick-access-section {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.access-card {
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--surface);
  backdrop-filter: blur(8px);
  box-shadow: var(--shadow);
  padding: 18px;
  text-align: left;
  font-family: inherit;
  color: var(--ink);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.access-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 20px 36px rgba(15, 28, 42, 0.14);
}

.icon-badge {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.hero-icon {
  width: 22px;
  height: 22px;
  color: #fff;
  stroke-width: 2;
}

.bg-blue {
  background: linear-gradient(135deg, #1f6fa8, #15507e);
}

.bg-green {
  background: linear-gradient(135deg, #0f766e, #115e59);
}

.bg-orange {
  background: linear-gradient(135deg, #c2410c, #9a3412);
}

.bg-teal {
  background: linear-gradient(135deg, #0f766e, #155e75);
}

.access-card h3 {
  margin: 0 0 6px;
  font-size: 1.03rem;
}

.access-card p {
  margin: 0;
  color: var(--muted);
  line-height: 1.5;
  font-size: 0.9rem;
}

.panel-grid {
  margin-top: 22px;
  display: grid;
  grid-template-columns: 1fr 1.25fr 0.85fr;
  gap: 14px;
  align-items: stretch;
}

.panel {
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--surface);
  backdrop-filter: blur(8px);
  box-shadow: var(--shadow);
  padding: 16px;
  height: 620px;
  display: flex;
  flex-direction: column;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.panel-head h3 {
  margin: 0;
  font-size: 1.08rem;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.section-icon {
  width: 18px;
  height: 18px;
  stroke-width: 2.2;
}

.inline-icon {
  width: 14px;
  height: 14px;
  margin-right: 3px;
  stroke-width: 2;
}

.clubs-list {
  display: grid;
  gap: 10px;
  overflow: auto;
  padding-right: 4px;
}

.mini-club-card {
  border: 0;
  width: 100%;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.66);
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.mini-club-card:hover {
  background: rgba(255, 255, 255, 0.96);
}

.mini-club-logo {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.mini-club-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mini-club-logo.fallback {
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
}

.mini-club-info h4 {
  margin: 0;
  font-size: 0.95rem;
}

.mini-club-info span {
  color: var(--muted);
  font-size: 0.8rem;
}

.activity-list {
  display: grid;
  gap: 10px;
  overflow: auto;
  padding-right: 4px;
}

.activity-card {
  border-radius: 14px;
  border: 1px solid rgba(15, 28, 42, 0.08);
}

.activity-main {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
}

.activity-date {
  width: 58px;
  border-radius: 10px;
  background: linear-gradient(180deg, #dbeaf4, #cce2df);
  display: grid;
  place-items: center;
  align-content: center;
}

.activity-date .day {
  font-size: 1.3rem;
  font-weight: 800;
}

.activity-date .month {
  font-size: 0.78rem;
  color: #355069;
}

.activity-body h4 {
  margin: 0;
  font-size: 1rem;
}

.activity-body p {
  margin: 8px 0;
  color: var(--muted);
  font-size: 0.88rem;
  line-height: 1.55;
}

.activity-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  color: #3f5268;
  font-size: 0.83rem;
}

.activity-footer {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed rgba(15, 28, 42, 0.12);
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--muted);
  font-size: 0.8rem;
}

.notice-list {
  display: grid;
  gap: 8px;
  overflow: auto;
  padding-right: 4px;
}

.more-link-btn {
  width: 100%;
  border: 1px dashed rgba(15, 28, 42, 0.2);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.72);
  color: #355069;
  font-family: inherit;
  font-size: 0.86rem;
  padding: 9px 10px;
  cursor: pointer;
}

.more-link-btn:hover {
  border-color: rgba(15, 118, 110, 0.45);
  color: #0f766e;
}

.notice-item {
  width: 100%;
  border: 0;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.66);
  padding: 10px;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px;
  text-align: left;
  cursor: pointer;
}

.notice-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 7px;
  background: linear-gradient(135deg, #c2410c, #9a3412);
}

.notice-content h4 {
  margin: 0;
  font-size: 0.9rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-content p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 0.78rem;
}

.calendar-section,
.recruit-section {
  margin-top: 16px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--surface);
  backdrop-filter: blur(8px);
  box-shadow: var(--shadow);
  padding: 18px;
}

.section-header {
  text-align: center;
  margin-bottom: 14px;
}

.section-header h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(1.5rem, 2.5vw, 2rem);
}

.section-header p {
  margin: 8px 0 0;
  color: var(--muted);
}

.custom-calendar {
  border-radius: 14px;
}

.calendar-cell {
  height: 100%;
  padding: 3px;
  display: flex;
  flex-direction: column;
}

.calendar-day {
  font-size: 12px;
  text-align: center;
  margin-bottom: 4px;
}

.has-activity .calendar-day {
  color: #0f766e;
  font-weight: 700;
}

.calendar-activities {
  font-size: 11px;
  display: grid;
  gap: 2px;
}

.calendar-activity-item {
  border-radius: 4px;
  padding: 1px 4px;
  color: #0f766e;
  background: #e4f1ee;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.more-activities {
  text-align: center;
  color: var(--muted);
  font-size: 10px;
}

.recruit-card {
  border-radius: 14px;
  overflow: hidden;
}

.recruit-head {
  min-height: 78px;
  display: grid;
  place-items: center;
  padding: 10px;
}

.recruit-head h3 {
  margin: 0;
  color: #fff;
  text-align: center;
}

.recruit-body {
  padding: 14px;
  text-align: center;
}

.recruit-body p {
  min-height: 42px;
  margin: 0 0 12px;
  color: var(--muted);
  font-size: 0.88rem;
}

.detail-btn {
  width: 100%;
  border-radius: 999px;
  border: 1px solid rgba(15, 28, 42, 0.14);
}

.site-footer {
  margin-top: 20px;
  background: #152331;
  color: rgba(246, 248, 245, 0.78);
  padding: 42px 0 18px;
}

.footer-content {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  gap: 22px;
  padding-bottom: 26px;
  border-bottom: 1px solid rgba(246, 248, 245, 0.15);
}

.footer-col h3 {
  margin: 0 0 10px;
  color: #f6f8f5;
}

.footer-col p {
  margin: 0;
  line-height: 1.6;
}

.footer-col a {
  display: block;
  margin-bottom: 8px;
  color: rgba(246, 248, 245, 0.75);
  text-decoration: none;
}

.footer-col a:hover {
  color: #fff;
}

.footer-bottom {
  text-align: center;
  margin-top: 16px;
  font-size: 0.84rem;
}

.interest-dialog-content {
  padding: 8px 0;
}

.dialog-desc {
  color: var(--muted);
  margin-bottom: 14px;
  text-align: center;
}

@media (max-width: 1100px) {
  .hero-inner {
    grid-template-columns: 1fr;
  }

  .quick-access-section {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }

  .panel {
    height: auto;
  }

  .footer-content {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 680px) {
  .hero-section {
    padding-top: 94px;
  }

  .carousel-card {
    height: 390px;
    padding: 16px;
  }

  .quick-access-section {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-btn {
    flex: 1;
  }

  .footer-content {
    grid-template-columns: 1fr;
  }
}
</style>
