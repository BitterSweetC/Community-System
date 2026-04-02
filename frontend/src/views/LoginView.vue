<template>
  <div class="auth-shell">
    <div class="ambient ambient-left"></div>
    <div class="ambient ambient-right"></div>
    <div class="grid-overlay"></div>

    <div class="horse-hero" aria-hidden="true">
      <div class="horse-banner">
        <img class="horse-svg horse-image" :src="horseSilhouette" alt="" />
      </div>
    </div>

    <div class="auth-layout">
      <section class="intro-panel">
        <p class="kicker">Campus Orbit</p>
        <h1>欢迎回来，继续你的社团旅程</h1>
        <p class="intro-desc">
          统一身份认证入口，快速访问公告、社团与活动。
        </p>

        <div class="links-grid">
          <button class="link-card" @click="$router.push('/home/notices')">
            <el-icon><Bell /></el-icon>
            <span>公告信息</span>
          </button>
          <button class="link-card" @click="$router.push('/home/clubs')">
            <el-icon><Search /></el-icon>
            <span>社团信息</span>
          </button>
          <button class="link-card" @click="$router.push('/home/activities')">
            <el-icon><Calendar /></el-icon>
            <span>社团活动</span>
          </button>
          <button class="link-card" @click="$router.push('/home/schedule')">
            <el-icon><Warning /></el-icon>
            <span>社团日程</span>
          </button>
        </div>

        <div class="notice-box">
          <div class="notice-head">
            <h3><el-icon><Warning /></el-icon> 系统通知</h3>
            <button
              type="button"
              class="notice-toggle"
              :aria-expanded="noticeExpanded"
              @click="noticeExpanded = !noticeExpanded"
            >
              {{ noticeExpanded ? '收起' : '展开' }}
            </button>
          </div>
          <p v-if="!noticeExpanded" class="notice-summary">
            {{ noticeSummary }}
          </p>
          <ul v-else>
            <li>新生社团招新将于 9 月 1 日正式启动。</li>
            <li>系统维护时间为每周日 02:00 - 04:00。</li>
          </ul>
        </div>
      </section>

      <el-card class="auth-card" shadow="never">
        <div class="auth-header">
          <h2>校园社团管理系统</h2>
          <p>统一身份认证登录</p>
        </div>

        <el-form :model="form" label-position="top" size="large" @submit.prevent>
          <el-form-item label="学号 / 工号">
            <el-input v-model="form.username" placeholder="请输入账号" @keyup.enter="handleLogin">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
              立即登录
            </el-button>
          </el-form-item>

          <div class="auth-footer">
            <el-button link @click="$router.push('/register')">账号激活 / 注册</el-button>
            <el-divider direction="vertical" />
            <el-button link @click="$router.push('/forgot-password')">忘记密码？</el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Calendar, Lock, Search, User, Warning } from '@element-plus/icons-vue'
import axios from '@/api/axios'
import { useAuthStore } from '@/stores/auth'
import horseSilhouette from '@/assets/horse-silhouette.svg'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const noticeExpanded = ref(false)
const noticeSummary = '新生社团招新将于 9 月 1 日启动，系统每周日 02:00 - 04:00 维护。'
const form = ref({
  username: '',
  password: ''
})

const handleLogin = async () => {
  loading.value = true
  try {
    const res = await axios.post('/auth/login', form.value)
    authStore.setToken(res.token)
    authStore.setUser(res.user)

    if (route.query.redirect) {
      router.push(route.query.redirect)
      return
    }

    const roles = res.user.roles || []
    const isAdmin = roles.some((r) => {
      const code = typeof r === 'string' ? r : r.code
      return code === 'ADMIN'
    })

    router.push(isAdmin ? '/admin' : '/home')
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '登录失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!authStore.token) {
    return
  }

  if (route.query.redirect) {
    router.push(route.query.redirect)
    return
  }

  const roles = authStore.user?.roles || []
  const isAdmin = roles.some((r) => (typeof r === 'string' ? r : r.code) === 'ADMIN')
  router.push(isAdmin ? '/admin' : '/home')
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;700&display=swap');

* {
  box-sizing: border-box;
}

.auth-shell {
  --ink: #0f1c2a;
  --headline: #1f3652;
  --muted: #516276;
  --surface: rgba(255, 255, 255, 0.82);
  --border: rgba(15, 28, 42, 0.14);
  --teal: #0f766e;
  --orange: #c2410c;

  position: relative;
  min-height: 100vh;
  padding: 24px clamp(16px, 3vw, 38px) 30px;
  overflow: hidden;
  isolation: isolate;
  background: radial-gradient(circle at 8% 12%, rgba(15, 118, 110, 0.12), transparent 30%),
    radial-gradient(circle at 92% 88%, rgba(194, 65, 12, 0.12), transparent 34%),
    linear-gradient(170deg, #f8f4ea 0%, #efe8da 52%, #e5ece9 100%);
  font-family: 'Outfit', sans-serif;
  color: var(--ink);
}

.ambient {
  position: absolute;
  border-radius: 999px;
  filter: blur(48px);
  pointer-events: none;
}

.ambient-left {
  width: 320px;
  height: 320px;
  top: -110px;
  left: -60px;
  background: rgba(15, 118, 110, 0.14);
}

.ambient-right {
  width: 260px;
  height: 260px;
  right: -60px;
  bottom: -100px;
  background: rgba(194, 65, 12, 0.14);
}

.grid-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: linear-gradient(rgba(15, 28, 42, 0.012) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 28, 42, 0.012) 1px, transparent 1px);
  background-size: 34px 34px;
}

.horse-hero {
  position: absolute;
  z-index: 1;
  left: 50%;
  top: clamp(10px, 2.2vh, 26px);
  transform: translateX(-50%);
  width: min(1040px, 88vw);
  display: flex;
  justify-content: center;
  align-items: center;
  pointer-events: none;
}

.horse-banner {
  position: relative;
  width: min(660px, 62vw);
}

.horse-banner::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 60%;
  width: 76%;
  height: 46%;
  transform: translate(-50%, -50%);
  border-radius: 999px;
  background: radial-gradient(circle, rgba(15, 118, 110, 0.18) 0%, rgba(15, 118, 110, 0.07) 56%, transparent 100%);
  filter: blur(16px);
}

.horse-svg {
  width: 100%;
  display: block;
  filter: drop-shadow(0 10px 20px rgba(15, 28, 42, 0.16));
}

.horse-image {
  opacity: 0.9;
}

.auth-layout {
  position: relative;
  z-index: 3;
  max-width: 1120px;
  margin: 0 auto;
  min-height: calc(100vh - 56px);
  padding-top: clamp(170px, 20vh, 220px);
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(0, 0.92fr);
  gap: clamp(20px, 2vw, 28px);
  align-items: start;
}

.intro-panel {
  padding: 8px 8px 0;
}

.kicker {
  margin: 0 0 12px;
  font-size: 0.8rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--muted);
  font-weight: 700;
}

.intro-panel h1 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  color: var(--headline);
  max-width: 10.6ch;
  font-size: clamp(2.15rem, 4.1vw, 3.3rem);
  line-height: 1.14;
  letter-spacing: 0;
  text-wrap: balance;
}

.intro-desc {
  margin: 16px 0 22px;
  color: var(--muted);
  font-size: clamp(1rem, 1.14vw, 1.12rem);
  max-width: 36ch;
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.link-card {
  border: 1px solid rgba(15, 28, 42, 0.14);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(6px);
  min-height: 96px;
  padding: 14px 12px;
  color: var(--ink);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  font-family: inherit;
  font-size: 0.94rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 12px rgba(15, 28, 42, 0.05);
  transition: all 0.2s ease;
}

.link-card .el-icon {
  font-size: 22px;
}

.link-card:hover {
  transform: translateY(-2px);
  border-color: rgba(15, 118, 110, 0.5);
  color: var(--teal);
}

.notice-box {
  border-radius: 18px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(6px);
  padding: 16px 18px;
}

.notice-head {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.notice-box h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--orange);
  font-size: 0.98rem;
}

.notice-toggle {
  border: 0;
  padding: 4px 0;
  background: transparent;
  color: var(--muted);
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
}

.notice-toggle:hover {
  color: var(--teal);
}

.notice-summary {
  margin: 0;
  color: var(--muted);
  line-height: 1.5;
  font-size: 0.96rem;
}

.notice-box ul {
  margin: 0;
  padding-left: 20px;
  color: var(--muted);
  line-height: 1.6;
}

.auth-card {
  align-self: start;
  justify-self: end;
  width: min(100%, 560px);
  transform: translate(-24px, 75px);
  border-radius: 22px;
  border: 1px solid rgba(15, 28, 42, 0.14);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  box-shadow: 0 18px 32px rgba(15, 28, 42, 0.12);
}

.auth-header {
  text-align: center;
  margin-bottom: 24px;
}

.auth-header h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  color: var(--headline);
  font-size: 1.46rem;
}

.auth-header p {
  margin: 8px 0 0;
  color: var(--muted);
}

.login-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #c2410c 0%, #9a3412 100%);
  font-family: inherit;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.login-btn:hover {
  transform: translateY(-1px);
}

.auth-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 6px;
}

:deep(.el-card__body) {
  padding: 30px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--ink);
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 0 1px rgba(15, 28, 42, 0.08) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(15, 118, 110, 0.6) inset;
}

@media (max-width: 992px) {
  .auth-shell {
    padding: 18px 14px 22px;
  }

  .horse-hero {
    top: 10px;
    width: min(760px, 92vw);
  }

  .horse-banner {
    width: min(500px, 74vw);
  }

  .auth-layout {
    grid-template-columns: 1fr;
    align-items: start;
    min-height: auto;
    padding-top: clamp(132px, 24vw, 180px);
    padding-bottom: 18px;
    gap: 16px;
  }

  .intro-panel {
    order: 2;
    padding-top: 0;
  }

  .auth-card {
    order: 1;
    transform: translateY(4px);
  }

  .intro-panel h1 {
    max-width: 13ch;
    font-size: clamp(2rem, 5.3vw, 2.9rem);
    line-height: 1.16;
  }
}

@media (max-width: 640px) {
  .auth-shell {
    padding: 12px 12px 18px;
  }

  .horse-hero {
    top: 10px;
    width: 100%;
  }

  .horse-banner {
    width: min(340px, 80vw);
  }

  .horse-banner::before {
    width: 82%;
    height: 50%;
    filter: blur(12px);
  }

  .auth-layout {
    gap: 12px;
    min-height: auto;
    padding-top: 102px;
  }

  .links-grid {
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }

  .link-card {
    min-height: 94px;
    border-radius: 16px;
    font-size: 0.9rem;
  }

  .intro-panel h1 {
    max-width: none;
    font-size: clamp(1.8rem, 8.8vw, 2.24rem);
    line-height: 1.2;
  }

  .intro-desc {
    margin: 12px 0 16px;
  }

  .notice-box {
    padding: 14px 16px;
  }

  .notice-summary {
    font-size: 0.94rem;
  }

  .notice-toggle {
    font-size: 0.82rem;
  }

  .auth-card {
    transform: none;
  }

  .auth-header h2 {
    font-size: 1.36rem;
  }

  .auth-footer {
    flex-wrap: wrap;
    gap: 4px;
  }

  :deep(.el-card__body) {
    padding: 18px;
  }
}
</style>
