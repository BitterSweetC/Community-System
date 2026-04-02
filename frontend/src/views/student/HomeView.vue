<template>
  <div class="home-view">
    <section class="hero-section">
      <div class="hero-bg-layer"></div>
      <div class="hero-inner">
        <div class="hero-carousel-wrap">
          <el-carousel :interval="5000" arrow="always" height="560px" indicator-position="none">
            <el-carousel-item v-for="(slide, index) in heroSlides" :key="slide.id || `${slide.title}-${index}`">
              <article class="carousel-card">
                <img
                  v-if="getHeroImage(slide, index)"
                  :src="getHeroImage(slide, index)"
                  :alt="slide.title || '轮播封面'"
                  class="carousel-card-image"
                  @error="handleHeroImageError(slide, index)"
                />
                <div class="image-mask"></div>
              </article>
            </el-carousel-item>
          </el-carousel>
        </div>
        <div class="hero-accent-line" aria-hidden="true"></div>
      </div>
    </section>

    <section class="premium-container hot-clubs-section">
      <header class="section-head">
        <h3>热门社团</h3>
        <a href="#" class="section-link" @click.prevent="router.push('/home/clubs')">
          更多
          <span>→</span>
        </a>
      </header>

      <div v-if="clubs.length" class="club-feature-grid">
        <button
          v-for="club in clubs.slice(0, 3)"
          :key="club.id"
          class="club-feature-card"
          @click="router.push(`/home/clubs/${club.id}`)"
        >
          <div class="club-feature-image-wrap">
            <img
              v-if="getClubImage(club)"
              :src="getClubImage(club)"
              :alt="club.name || '社团封面'"
              class="club-feature-image"
              @error="handleClubImageError(club)"
            />
            <div v-else class="club-feature-image fallback-image">暂无社团图片</div>
          </div>
          <h4>{{ club.name }}</h4>
        </button>
      </div>
      <el-empty v-else description="暂无社团" />
    </section>

    <section class="notice-highlight-section">
      <div class="premium-container notice-highlight-shell">
        <article class="notice-showcase-panel">
          <div class="notice-showcase">
            <div class="notice-showcase-copy">
              <p class="notice-kicker">Campus bulletin</p>
              <h3>公告通知</h3>
              <p class="notice-showcase-desc">{{ getNoticeIntro(primaryNotice) }}</p>
              <a href="#" class="notice-text-link" @click.prevent="router.push('/home/notices')">
                查看全部公告
                <span>→</span>
              </a>
              <a href="#" class="notice-text-link" @click.prevent="router.push('/home/activities')">
                查看近期活动安排
                <span>→</span>
              </a>
            </div>

            <div class="notice-showcase-visual">
              <img v-if="noticeHeroImage" :src="noticeHeroImage" alt="公告图片" class="notice-showcase-image" />
              <div v-else class="notice-showcase-image fallback-image">暂无公告图片</div>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section class="premium-container info-section-shell">
      <article class="panel activities-panel" id="activities-section">
        <header class="panel-head">
          <h3>精彩活动</h3>
          <a href="#" class="section-link" @click.prevent="router.push('/home/activities')">
            更多
            <span>→</span>
          </a>
        </header>

        <div v-if="activities.length" class="activity-list">
          <button
            v-for="activity in activitiesPreview"
            :key="activity.id"
            class="activity-feature-card"
            @click="router.push('/home/activities')"
          >
            <div class="activity-feature-image-wrap">
              <img
                v-if="getActivityImage(activity)"
                :src="getActivityImage(activity)"
                :alt="activity.title || '活动封面'"
                class="activity-feature-image"
                @error="handleActivityImageError(activity)"
              />
              <div v-else class="activity-feature-image fallback-image">暂无活动图片</div>
            </div>
            <div class="activity-feature-body">
              <h4>{{ activity.title || '未命名活动' }}</h4>
              <p>{{ getActivityIntro(activity) }}</p>
            </div>
          </button>
        </div>
        <el-empty v-else description="暂无活动" />
      </article>
    </section>

    <section class="calendar-section-wrap" id="calendar-section">
      <div class="premium-container calendar-section">
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
      </div>
    </section>

    <section class="recruit-assist-section" id="recruit-section">
      <div class="premium-container recruit-assist-shell">
        <div class="recruit-assist-title">
          <h2>想要加入我们吗？</h2>
          <p>右侧入口均会导航到社团一览页，你可以先浏览、再筛选，最后进入详情完成加入申请。</p>
        </div>

        <div class="recruit-assist-right">
          <div class="assist-method-grid">
            <article class="assist-method-card">
              <div class="assist-method-icon" aria-hidden="true">
                <svg viewBox="0 0 48 48">
                  <rect x="8" y="9" width="32" height="30" rx="4"></rect>
                  <path d="M16 17h16"></path>
                  <path d="M16 24h16"></path>
                  <path d="M16 31h10"></path>
                </svg>
              </div>
              <h4>浏览全部社团</h4>
              <p>进入社团一览页，查看当前开放展示的全部社团与基础信息。</p>
              <a href="#" class="assist-method-link" @click.prevent="router.push('/home/clubs')">
                前往社团一览
              </a>
            </article>
            <article class="assist-method-card">
              <div class="assist-method-icon" aria-hidden="true">
                <svg viewBox="0 0 48 48">
                  <path d="M9 12h30"></path>
                  <path d="M14 22h20"></path>
                  <path d="M19 32h10"></path>
                  <path d="M25 12v20"></path>
                </svg>
              </div>
              <h4>按兴趣筛选</h4>
              <p>在社团一览页使用关键词和分类筛选，快速定位与你方向匹配的社团。</p>
              <a href="#" class="assist-method-link" @click.prevent="router.push('/home/clubs')">
                去筛选社团
              </a>
            </article>
            <article class="assist-method-card">
              <div class="assist-method-icon" aria-hidden="true">
                <svg viewBox="0 0 48 48">
                  <rect x="11" y="8" width="26" height="32" rx="3"></rect>
                  <path d="M17 18h14"></path>
                  <path d="M17 25h14"></path>
                  <path d="M17 32h9"></path>
                </svg>
              </div>
              <h4>查看并申请加入</h4>
              <p>从社团一览进入详情页后可查看招新状态，符合条件即可发起加入申请。</p>
              <a href="#" class="assist-method-link" @click.prevent="router.push('/home/clubs')">
                查看可申请社团
              </a>
            </article>
          </div>
        </div>
      </div>
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
import { ElMessage } from 'element-plus'
import InterestSelector from '@/components/InterestSelector.vue'

const router = useRouter()
const authStore = useAuthStore()

const clubs = ref([])
const notices = ref([])
const activities = ref([])
const featuredActivities = ref([])
const brokenImageUrls = ref(new Set())
const calendarDate = ref(new Date())
const calendarActivities = ref([])
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

const primaryNotice = computed(() => notices.value[0] || null)
const clubImageFields = ['logoUrl', 'coverUrl', 'imageUrl', 'bannerUrl', 'thumbnailUrl']
const activityImageFields = ['coverUrl', 'imageUrl', 'bannerUrl', 'thumbnailUrl', 'posterUrl']

const resolveUsableImageUrl = (url) => {
  if (typeof url !== 'string') {
    return ''
  }

  const normalized = url.trim()
  if (!normalized || brokenImageUrls.value.has(normalized)) {
    return ''
  }

  return normalized
}

const markImageAsBroken = (url) => {
  const normalized = typeof url === 'string' ? url.trim() : ''
  if (!normalized || brokenImageUrls.value.has(normalized)) {
    return
  }

  const next = new Set(brokenImageUrls.value)
  next.add(normalized)
  brokenImageUrls.value = next
}

const readFirstImageField = (item, fields) => {
  if (!item || typeof item !== 'object') {
    return ''
  }

  for (const field of fields) {
    const value = item[field]
    if (typeof value === 'string' && value.trim()) {
      return value.trim()
    }
  }

  return ''
}

const extractUrlFromText = (value) => {
  const text = typeof value === 'string' ? value : ''
  if (!text.trim()) {
    return ''
  }

  const markdownMatch = text.match(/!\[[^\]]*]\((https?:\/\/[^)\s]+)\)/i)
  if (markdownMatch?.[1]) {
    return markdownMatch[1]
  }

  const htmlMatch = text.match(/<img[^>]+src=["'](https?:\/\/[^"']+)["']/i)
  if (htmlMatch?.[1]) {
    return htmlMatch[1]
  }

  const urlMatch = text.match(/https?:\/\/[^\s"'<>]+/i)
  return urlMatch?.[0] || ''
}

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
    const [noticeRes, actRes, calRes] = await Promise.allSettled([
      axios.get('/notices'),
      axios.get('/activities?size=6'),
      axios.get('/activities?size=50')
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

  } catch (error) {
    console.error(error)
  }
})

const getClubImage = (club) =>
  resolveUsableImageUrl(readFirstImageField(club, clubImageFields))

const handleClubImageError = (club) => {
  markImageAsBroken(readFirstImageField(club, clubImageFields))
}

const getActivityImage = (activity) =>
  resolveUsableImageUrl(readFirstImageField(activity, activityImageFields))

const handleActivityImageError = (activity) => {
  markImageAsBroken(readFirstImageField(activity, activityImageFields))
}

const activitiesPreview = computed(() => {
  const source = activities.value || []
  if (source.length <= 3) {
    return source.slice(0, 3)
  }

  const preview = []
  const usedImage = new Set()

  for (const activity of source) {
    const image = getActivityImage(activity)
    const key = image || `activity-${activity?.id ?? activity?.title ?? preview.length}`
    if (usedImage.has(key)) {
      continue
    }

    preview.push(activity)
    usedImage.add(key)
    if (preview.length === 3) {
      return preview
    }
  }

  for (const activity of source) {
    if (preview.includes(activity)) {
      continue
    }
    preview.push(activity)
    if (preview.length === 3) {
      break
    }
  }

  return preview
})

const getNoticeImage = (notice) =>
  readFirstImageField(notice, ['imageUrl', 'coverUrl', 'bannerUrl', 'thumbnailUrl', 'pictureUrl', 'picUrl']) ||
  extractUrlFromText(notice?.content) ||
  extractUrlFromText(notice?.summary) ||
  extractUrlFromText(notice?.description)

const heroImagePool = computed(() => {
  const urls = [
    ...featuredActivities.value.map((activity) => getActivityImage(activity)),
    ...activities.value.map((activity) => getActivityImage(activity)),
    ...clubs.value.map((club) => getClubImage(club)),
    ...notices.value.map((notice) => getNoticeImage(notice))
  ].filter(Boolean)

  return [...new Set(urls)]
})

const noticeHeroImage = computed(() => {
  return getNoticeImage(primaryNotice.value) || heroImagePool.value[0] || ''
})

const getHeroImage = (activity, index) => {
  const activityImage = getActivityImage(activity)
  if (activityImage) {
    return activityImage
  }

  if (heroImagePool.value.length === 0) {
    return ''
  }

  return heroImagePool.value[index % heroImagePool.value.length]
}

const handleHeroImageError = (activity, index) => {
  markImageAsBroken(getHeroImage(activity, index))
}

const getActivitiesForDate = (date) => {
  if (!date) {
    return []
  }

  return calendarActivities.value.filter((activity) => toDateKey(activity.startTime) === date)
}

const getActivityIntro = (activity) => {
  const text = activity?.description?.trim()
  if (!text) {
    return '欢迎加入本次活动，更多内容可前往活动页查看。'
  }
  return text.length > 70 ? `${text.slice(0, 70)}...` : text
}

const getNoticeIntro = (notice) => {
  const text = notice?.content || notice?.summary || notice?.description || ''
  const normalized = String(text).trim()
  if (!normalized) {
    return '查看最新校级与社团通知，及时了解活动安排、政策更新与重要提醒。'
  }
  return normalized.length > 90 ? `${normalized.slice(0, 90)}...` : normalized
}

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
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@500;700&family=Source+Sans+3:wght@400;500;600;700&display=swap');

* {
  box-sizing: border-box;
}

.home-view {
  --ink: #1a1714;
  --ink-soft: #2f2a25;
  --muted: #47413b;
  --border: rgba(26, 23, 20, 0.18);
  --accent: #d54a1f;
  --accent-dark: #aa3715;
  --surface: #ffffff;
  --surface-soft: #faf7f3;
  --surface-warm: #f4e8db;
  --surface-cool: #e5e9f2;
  --line-soft: rgba(33, 26, 20, 0.14);
  --elev-1: 0 8px 20px rgba(33, 24, 17, 0.08);
  --elev-2: 0 16px 34px rgba(33, 24, 17, 0.13);
  --focus-ring: 0 0 0 3px rgba(213, 74, 31, 0.34);

  background:
    radial-gradient(circle at 12% -12%, rgba(232, 214, 196, 0.38), transparent 40%),
    linear-gradient(180deg, #ffffff 0%, #fcfbf9 100%);
  color: var(--ink);
  font-family: 'Source Sans 3', sans-serif;
}

.home-view :deep(.premium-container) {
  max-width: 1320px;
  padding-left: 24px;
  padding-right: 24px;
}

.home-view :deep(h1),
.home-view :deep(h2),
.home-view :deep(h3),
.home-view :deep(h4),
.home-view :deep(h5),
.home-view :deep(h6) {
  color: #1b1714;
}

.hero-section {
  position: relative;
  padding: 82px 0 62px;
  overflow: hidden;
}

.hero-bg-layer {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, #fcfaf7 0%, #f8f4ef 100%);
}

.hero-bg-layer::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 10% 20%, rgba(228, 202, 177, 0.34) 0, transparent 44%),
    radial-gradient(circle at 88% 14%, rgba(220, 193, 164, 0.24) 0, transparent 38%);
  opacity: 0.42;
  pointer-events: none;
}

.hero-inner {
  position: relative;
  z-index: 1;
  width: 100%;
}

.hero-carousel-wrap {
  height: 540px;
  width: 100%;
  border-radius: 2px;
  border: 1px solid var(--line-soft);
  box-shadow: var(--elev-1);
  background: #201912;
  overflow: hidden;
  margin: 0;
}

.hero-accent-line {
  width: 100%;
  height: 5px;
  background: linear-gradient(90deg, #ca4217 0%, #de5829 44%, #f09059 100%);
}

.hero-carousel-wrap :deep(.el-carousel),
.hero-carousel-wrap :deep(.el-carousel__container),
.hero-carousel-wrap :deep(.el-carousel__item) {
  height: 100%;
}

.hero-carousel-wrap :deep(.el-carousel__arrow) {
  background-color: rgba(16, 13, 11, 0.44);
  border: 1px solid rgba(255, 255, 255, 0.26);
  backdrop-filter: blur(4px);
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.hero-carousel-wrap :deep(.el-carousel__arrow:hover) {
  background-color: rgba(16, 13, 11, 0.68);
  transform: scale(1.04);
}

.hero-carousel-wrap :deep(.el-carousel__indicators) {
  display: none;
}

.carousel-card {
  position: relative;
  height: 540px;
  border-radius: 0;
  background: linear-gradient(135deg, #2e2823 0%, #6d2f05 55%, #c8682e 100%);
}

.carousel-card-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}

.image-mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(14, 11, 9, 0.22) 0%, rgba(14, 11, 9, 0.06) 38%, rgba(14, 11, 9, 0.5) 100%),
    linear-gradient(90deg, rgba(14, 11, 9, 0.16) 0%, rgba(14, 11, 9, 0) 46%);
}

.hot-clubs-section {
  margin-top: 66px;
  border-top: 1px solid var(--line-soft);
  border-bottom: 1px solid var(--line-soft);
  background: linear-gradient(180deg, rgba(250, 247, 243, 0.96) 0%, rgba(255, 255, 255, 0.96) 100%);
  box-shadow: none;
  padding: 64px 0 52px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 34px;
}

.section-head h3 {
  margin: 0;
  font-size: clamp(1.95rem, 3vw, 2.95rem);
  font-family: 'Noto Serif SC', serif;
  line-height: 1.18;
  color: var(--ink);
}

.section-link {
  position: relative;
  text-decoration: none;
  color: var(--ink);
  font-size: 1.05rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 3px;
  transition: color 0.2s ease;
}

.section-link::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -2px;
  width: 100%;
  height: 1px;
  background: currentColor;
  opacity: 0.56;
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.24s ease;
}

.section-link:hover {
  color: var(--accent);
}

.section-link:hover::after,
.section-link:focus-visible::after {
  transform: scaleX(1);
}

.section-link span {
  transition: transform 0.2s ease;
}

.section-link:hover span,
.section-link:focus-visible span {
  transform: translateX(4px);
}

.club-feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 30px;
}

.club-feature-card {
  border: 1px solid var(--line-soft);
  border-radius: 2px;
  background: var(--surface);
  padding: 0;
  overflow: hidden;
  text-align: left;
  cursor: pointer;
  box-shadow: var(--elev-1);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.club-feature-image-wrap {
  width: 100%;
  aspect-ratio: 3 / 4;
  background: var(--surface-soft);
  overflow: hidden;
}

.club-feature-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
  transform: scale(1);
  transition: transform 0.32s ease;
  will-change: transform;
}

.fallback-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  text-align: center;
  background: linear-gradient(165deg, #f5ece1 0%, #ebddcd 100%);
  color: #6d5b48;
  font-size: 0.92rem;
  font-weight: 600;
  letter-spacing: 0.01em;
  padding: 0 18px;
}

.fallback-image::before {
  content: '✦';
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(126, 101, 76, 0.24);
  color: #aa5d2f;
  font-size: 1rem;
  line-height: 1;
}

.club-feature-image.fallback {
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
  font-size: 2rem;
}

.club-feature-card h4 {
  margin: 16px 0 0;
  padding: 0 16px 16px;
  font-size: clamp(1.45rem, 1.9vw, 2.2rem);
  line-height: 1.28;
  font-family: 'Source Sans 3', sans-serif;
  color: var(--ink);
  font-weight: 600;
  max-width: 18ch;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.info-section-shell {
  margin-top: 42px;
  border-top: 1px solid var(--line-soft);
  border-bottom: 1px solid var(--line-soft);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(248, 243, 237, 0.8) 100%);
  box-shadow: none;
  padding: 74px 0 60px;
}

.panel {
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  padding: 0;
  height: 620px;
  display: flex;
  flex-direction: column;
}

.info-section-shell .panel {
  height: auto;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 34px;
}

.panel-head h3 {
  margin: 0;
  font-size: clamp(1.95rem, 3vw, 2.95rem);
  font-family: 'Noto Serif SC', serif;
  line-height: 1.18;
  color: var(--ink);
}

.activity-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 30px;
}

.activity-feature-card {
  border: 1px solid var(--line-soft);
  border-radius: 2px;
  overflow: hidden;
  padding: 0;
  background: var(--surface);
  text-align: left;
  cursor: pointer;
  box-shadow: var(--elev-1);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.activity-feature-image-wrap {
  width: 100%;
  aspect-ratio: 3 / 4;
  background: var(--surface-soft);
  overflow: hidden;
}

.activity-feature-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
  transform: scale(1);
  transition: transform 0.32s ease;
  will-change: transform;
}

.activity-feature-image.fallback {
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 2rem;
  font-weight: 800;
}

@media (hover: hover) and (pointer: fine) {
  .club-feature-card:hover .club-feature-image,
  .club-feature-card:focus-visible .club-feature-image {
    transform: scale(1.06);
  }

  .club-feature-card:hover,
  .club-feature-card:focus-visible {
    transform: translateY(-2px);
    border-color: rgba(203, 84, 42, 0.36);
    box-shadow: var(--elev-2);
  }

  .activity-feature-card:hover .activity-feature-image,
  .activity-feature-card:focus-visible .activity-feature-image {
    transform: scale(1.06);
  }

  .activity-feature-card:hover,
  .activity-feature-card:focus-visible {
    transform: translateY(-2px);
    border-color: rgba(203, 84, 42, 0.36);
    box-shadow: var(--elev-2);
  }
}

.activity-feature-body {
  padding: 16px 16px 18px;
}

.activity-feature-body h4 {
  margin: 0 0 8px;
  color: #1b1714;
  font-size: clamp(1.45rem, 1.9vw, 2.2rem);
  line-height: 1.28;
  font-family: 'Source Sans 3', sans-serif;
  font-weight: 600;
}

.activity-feature-body p {
  margin: 0;
  color: var(--muted);
  font-size: clamp(1rem, 1.1vw, 1.2rem);
  line-height: 1.58;
  max-width: 30ch;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-showcase-panel {
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.notice-highlight-section {
  position: relative;
  margin-top: 28px;
  background: linear-gradient(180deg, #f5e8db 0%, #f1e2d1 100%);
  overflow: hidden;
}

.notice-highlight-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 12% 24%, rgba(230, 185, 145, 0.24) 0, transparent 38%),
    radial-gradient(circle at 84% 18%, rgba(205, 156, 112, 0.18) 0, transparent 44%);
  pointer-events: none;
  opacity: 0.56;
}

.notice-highlight-shell {
  position: relative;
  z-index: 1;
  padding-top: 72px;
  padding-bottom: 36px;
}

.notice-showcase {
  display: grid;
  grid-template-columns: minmax(320px, 0.92fr) minmax(0, 1.35fr);
  gap: 72px;
  align-items: flex-start;
  border: 0;
  border-radius: 0;
  overflow: visible;
  background: transparent;
}

.notice-showcase-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  padding: 18px 0;
  background: transparent;
  min-height: 0;
}

.notice-kicker {
  margin: 0 0 16px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.8rem;
  color: #b35a1f;
  font-weight: 700;
}

.notice-showcase-copy h3 {
  margin: 0 0 24px;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.7rem, 4vw, 4.2rem);
  line-height: 1.2;
  color: #171411;
}

.notice-showcase-desc {
  margin: 0 0 28px;
  color: var(--ink-soft);
  font-size: clamp(1.12rem, 1.22vw, 1.38rem);
  line-height: 1.72;
  max-width: 30ch;
}

.notice-text-link {
  position: relative;
  text-decoration: none;
  color: #171411;
  font-size: 1.08rem;
  font-weight: 520;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  width: min(100%, 560px);
  padding: 20px 4px;
  border-top: 1px solid rgba(95, 76, 58, 0.24);
  transition: color 0.2s ease, background-color 0.2s ease;
}

.notice-text-link + .notice-text-link {
  margin-top: 0;
}

.notice-showcase-copy .notice-text-link:last-of-type {
  border-bottom: 1px solid rgba(95, 76, 58, 0.24);
}

.notice-text-link:hover {
  color: var(--accent);
  background: rgba(255, 255, 255, 0.34);
}

.notice-text-link span {
  font-size: 1.36rem;
  color: var(--accent);
  transition: transform 0.2s ease;
}

.notice-text-link:hover span,
.notice-text-link:focus-visible span {
  transform: translateX(4px);
}

.notice-showcase-visual {
  min-height: 560px;
  aspect-ratio: 5 / 4;
  width: 100%;
  display: flex;
  overflow: hidden;
  border: 1px solid rgba(75, 53, 33, 0.24);
  box-shadow: var(--elev-2);
  transform: translateY(96px);
  position: relative;
  z-index: 1;
  background: #e9d9c7;
}

@media (min-width: 1101px) {
  .notice-highlight-section {
    overflow: visible;
  }

  .notice-highlight-shell {
    padding-bottom: 14px;
  }

  .notice-showcase-visual {
    transform: translateY(118px);
  }
}

.notice-showcase-image {
  flex: 1;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}

.calendar-section-wrap {
  position: relative;
  margin-top: 72px;
  --calendar-bg: #d8deeb;
  --calendar-cell-bg: #d2d8e6;
  --calendar-line: #b8c2d7;
  --calendar-accent: #405a86;
  --calendar-pill-bg: #e8edf8;
  --calendar-pill-text: #24334f;
  --calendar-selected-bg: #e2e8f7;
  background: linear-gradient(180deg, #d8deeb 0%, #d3d9e6 100%);
  overflow: hidden;
}

.calendar-section-wrap::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 15% 12%, rgba(255, 255, 255, 0.24) 0, transparent 40%),
    radial-gradient(circle at 80% 28%, rgba(131, 149, 181, 0.16) 0, transparent 44%);
  pointer-events: none;
}

.calendar-section {
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  padding: 64px 0 26px;
}

.recruit-assist-section {
  position: relative;
  margin-top: 72px;
  background: linear-gradient(180deg, #f8f7f4 0%, #f4f1ec 100%);
  overflow: hidden;
}

.recruit-assist-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 18% 32%, rgba(228, 218, 202, 0.34) 0, transparent 42%),
    radial-gradient(circle at 82% 24%, rgba(214, 202, 184, 0.24) 0, transparent 48%);
  pointer-events: none;
}

.recruit-assist-shell {
  position: relative;
  z-index: 1;
  display: grid;
  row-gap: 40px;
  padding-top: 40px;
  padding-bottom: 40px;
}

@media (min-width: 992px) {
  .recruit-assist-shell {
    grid-template-columns: 312px 1fr;
    column-gap: 56px;
    padding-top: 56px;
    padding-bottom: 56px;
  }
}

.recruit-assist-title h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2rem, 3vw, 2.7rem);
  line-height: 1.2;
  color: #111111;
}

.recruit-assist-title p {
  margin: 14px 0 0;
  color: var(--ink-soft);
  line-height: 1.72;
  font-size: 1rem;
}

.assist-method-grid {
  display: grid;
  row-gap: 40px;
}

@media (min-width: 576px) {
  .assist-method-grid {
    grid-template-columns: repeat(auto-fit, minmax(0, 1fr));
    column-gap: 28px;
  }
}

@media (min-width: 1200px) {
  .assist-method-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    column-gap: 44px;
  }
}

.assist-method-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-height: 236px;
  border: 1px solid var(--line-soft);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--elev-1);
  padding: 22px 18px 20px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.assist-method-icon {
  width: 48px;
  height: 48px;
  color: #24211e;
  margin-bottom: 14px;
}

.assist-method-icon svg {
  width: 100%;
  height: 100%;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.assist-method-card h4 {
  margin: 0;
  font-size: 2rem;
  line-height: 1.1;
  color: #191714;
  font-weight: 500;
}

.assist-method-card p {
  margin: 16px 0 0;
  color: #5f5b56;
  line-height: 1.7;
  font-size: 1.04rem;
}

.assist-method-link {
  margin-top: 26px;
  text-decoration: none;
  color: #cb4a15;
  font-weight: 600;
  display: inline-block;
  border-bottom: 1px solid currentColor;
  padding-bottom: 4px;
  transition: color 0.2s ease, transform 0.2s ease;
}

.assist-method-link:hover {
  color: #a53d10;
  transform: translateX(2px);
}

.section-header {
  text-align: left;
  margin-bottom: 26px;
}

.section-header h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(1.7rem, 2.4vw, 2.2rem);
}

.section-header p {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.6;
}

.custom-calendar {
  border-radius: 2px;
  border: 1px solid var(--calendar-line);
  background: var(--calendar-bg);
  box-shadow: var(--elev-1);
  padding: 0 20px 20px;
}

.calendar-cell {
  height: 100%;
  padding: 3px;
  display: flex;
  flex-direction: column;
  background: var(--calendar-cell-bg);
  border-radius: 3px;
}

.calendar-day {
  font-size: 12px;
  text-align: center;
  margin-bottom: 4px;
  color: #242a34;
}

.has-activity .calendar-day {
  color: var(--calendar-accent);
  font-weight: 700;
}

.calendar-activities {
  font-size: 11px;
  display: grid;
  gap: 2px;
}

.calendar-activity-item {
  border-radius: 4px;
  padding: 2px 6px;
  color: var(--calendar-pill-text);
  background: var(--calendar-pill-bg);
  border: 1px solid rgba(122, 137, 168, 0.28);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.more-activities {
  text-align: center;
  color: #4c5d7f;
  font-size: 10px;
}

.club-feature-card:focus-visible,
.activity-feature-card:focus-visible {
  outline: none;
  box-shadow: var(--elev-2), var(--focus-ring);
}

.section-link:focus-visible,
.notice-text-link:focus-visible,
.assist-method-link:focus-visible {
  outline: 2px solid rgba(203, 75, 31, 0.6);
  outline-offset: 3px;
  border-radius: 2px;
}

@media (hover: hover) and (pointer: fine) {
  .assist-method-card:hover {
    transform: translateY(-2px);
    border-color: rgba(203, 84, 42, 0.36);
    box-shadow: var(--elev-2);
  }
}

.home-view :deep(.el-button--primary) {
  --el-button-bg-color: #cf4a00;
  --el-button-border-color: #cf4a00;
  --el-button-hover-bg-color: #b23f00;
  --el-button-hover-border-color: #b23f00;
  --el-button-active-bg-color: #973500;
  --el-button-active-border-color: #973500;
}

.home-view :deep(.el-calendar-table td.is-selected .el-calendar-day) {
  background: var(--calendar-selected-bg);
}

.home-view :deep(.el-calendar-table td.today .el-calendar-day) {
  color: var(--calendar-accent);
  font-weight: 700;
}

.home-view :deep(.custom-calendar .el-calendar__header) {
  padding: 18px 0 14px;
  border-bottom: 1px solid var(--calendar-line);
  background: var(--calendar-bg);
}

.home-view :deep(.custom-calendar .el-calendar__title) {
  color: #2d3340;
  font-weight: 600;
}

.home-view :deep(.custom-calendar .el-calendar__button-group .el-button) {
  background: var(--calendar-bg);
  border-color: var(--calendar-line);
  color: #4f5768;
}

.home-view :deep(.custom-calendar .el-calendar__button-group .el-button:hover) {
  border-color: #97a2bd;
  color: #384357;
}

.home-view :deep(.custom-calendar .el-calendar__body) {
  padding: 10px 0 0;
  background: var(--calendar-bg);
}

.home-view :deep(.custom-calendar .el-calendar-table) {
  background: var(--calendar-bg);
}

.home-view :deep(.custom-calendar .el-calendar-table th) {
  background: var(--calendar-bg);
  color: #505867;
  font-weight: 500;
  border-bottom: 1px solid var(--calendar-line);
}

.home-view :deep(.custom-calendar .el-calendar-table td) {
  background: var(--calendar-cell-bg);
  border-color: var(--calendar-line);
}

.home-view :deep(.custom-calendar .el-calendar-day) {
  background: var(--calendar-cell-bg);
}

@media (prefers-reduced-motion: reduce) {
  .club-feature-card,
  .activity-feature-card,
  .assist-method-card,
  .club-feature-image,
  .activity-feature-image,
  .section-link,
  .section-link::after,
  .section-link span,
  .notice-text-link,
  .notice-text-link span,
  .assist-method-link,
  .hero-carousel-wrap :deep(.el-carousel__arrow) {
    transition: none !important;
  }

  .club-feature-card:hover,
  .club-feature-card:focus-visible,
  .activity-feature-card:hover,
  .activity-feature-card:focus-visible,
  .assist-method-card:hover {
    transform: none !important;
  }

  .section-link:hover span,
  .section-link:focus-visible span,
  .notice-text-link:hover span,
  .notice-text-link:focus-visible span,
  .assist-method-link:hover {
    transform: none !important;
  }

  .club-feature-image,
  .activity-feature-image,
  .notice-showcase-visual,
  .hero-carousel-wrap :deep(.el-carousel__arrow:hover) {
    transform: none !important;
  }
}

.site-footer {
  margin-top: 20px;
  background: #121212;
  color: rgba(246, 244, 239, 0.8);
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
  .home-view :deep(.premium-container) {
    padding-left: 18px;
    padding-right: 18px;
  }

  .section-head h3,
  .panel-head h3,
  .section-header h2,
  .recruit-assist-title h2 {
    font-size: clamp(1.65rem, 3.8vw, 2.2rem);
  }

  .club-feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .activity-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .notice-showcase {
    grid-template-columns: 1fr;
    gap: 34px;
  }

  .notice-showcase-visual {
    transform: none;
  }

  .notice-showcase-copy {
    padding: 10px 0 0;
    min-height: auto;
  }

  .club-feature-card h4,
  .activity-feature-body h4 {
    font-size: clamp(1.26rem, 2.7vw, 1.76rem);
  }

  .activity-feature-body {
    padding: 14px 14px 16px;
  }

  .activity-feature-body p {
    font-size: 0.98rem;
  }

  .notice-text-link {
    width: 100%;
  }

  .panel {
    height: auto;
  }

  .footer-content {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 680px) {
  .home-view :deep(.premium-container) {
    padding-left: 14px;
    padding-right: 14px;
  }

  .hero-section {
    padding-top: 90px;
    padding-bottom: 42px;
  }

  .section-head {
    margin-bottom: 20px;
  }

  .panel-head {
    margin-bottom: 20px;
  }

  .club-feature-grid {
    grid-template-columns: 1fr;
  }

  .activity-list {
    grid-template-columns: 1fr;
  }

  .hot-clubs-section,
  .info-section-shell {
    margin-top: 58px;
    padding-top: 34px;
  }

  .calendar-section-wrap {
    margin-top: 58px;
  }

  .calendar-section {
    padding-top: 34px;
  }

  .recruit-assist-section {
    margin-top: 58px;
  }

  .recruit-assist-shell {
    row-gap: 28px;
    padding-top: 34px;
    padding-bottom: 34px;
  }

  .notice-highlight-section {
    margin-top: 12px;
  }

  .notice-highlight-shell {
    padding-top: 48px;
    padding-bottom: 48px;
  }

  .notice-showcase-copy {
    padding: 0;
  }

  .notice-showcase-copy h3 {
    font-size: clamp(2.1rem, 10vw, 3rem);
  }

  .notice-showcase-visual {
    min-height: 300px;
    aspect-ratio: 5 / 4;
  }

  .hero-carousel-wrap {
    height: 390px;
  }

  .carousel-card {
    height: 390px;
  }

  .club-feature-card h4,
  .activity-feature-body h4 {
    font-size: 1.24rem;
  }

  .club-feature-card h4 {
    padding: 0 14px 14px;
  }

  .activity-feature-body {
    padding: 14px 14px 16px;
  }

  .activity-feature-body p {
    font-size: 0.94rem;
  }

  .assist-method-card {
    padding: 18px 14px 18px;
  }

  .notice-text-link {
    font-size: 0.96rem;
    padding: 16px 4px;
  }

  .footer-content {
    grid-template-columns: 1fr;
  }
}
</style>
