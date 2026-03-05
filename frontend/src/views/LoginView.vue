<template>
  <div class="auth-shell">
    <div class="ambient ambient-left"></div>
    <div class="ambient ambient-right"></div>
    <div class="grid-overlay"></div>

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
          <h3><el-icon><Warning /></el-icon> 系统通知</h3>
          <ul>
            <li>新生社团招新将于 9 月 1 日正式启动。</li>
            <li>系统维护时间为每周日 02:00 - 04:00。</li>
          </ul>
        </div>
      </section>

      <el-card class="auth-card" shadow="never">
        <div class="auth-header">
          <div class="logo-badge">
            <el-icon :size="28"><School /></el-icon>
          </div>
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
import { Bell, Calendar, Lock, School, Search, User, Warning } from '@element-plus/icons-vue'
import axios from '@/api/axios'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
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
  --muted: #516276;
  --surface: rgba(255, 255, 255, 0.82);
  --border: rgba(15, 28, 42, 0.14);
  --teal: #0f766e;
  --orange: #c2410c;

  position: relative;
  min-height: 100vh;
  padding: 32px;
  overflow: hidden;
  background: radial-gradient(circle at 8% 12%, rgba(15, 118, 110, 0.16), transparent 30%),
    radial-gradient(circle at 92% 88%, rgba(194, 65, 12, 0.16), transparent 34%),
    linear-gradient(170deg, #f8f4ea 0%, #efe8da 52%, #e5ece9 100%);
  font-family: 'Outfit', sans-serif;
  color: var(--ink);
}

.ambient {
  position: absolute;
  border-radius: 999px;
  filter: blur(60px);
  pointer-events: none;
}

.ambient-left {
  width: 320px;
  height: 320px;
  top: -110px;
  left: -60px;
  background: rgba(15, 118, 110, 0.2);
}

.ambient-right {
  width: 260px;
  height: 260px;
  right: -60px;
  bottom: -100px;
  background: rgba(194, 65, 12, 0.2);
}

.grid-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: linear-gradient(rgba(15, 28, 42, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 28, 42, 0.03) 1px, transparent 1px);
  background-size: 34px 34px;
}

.auth-layout {
  position: relative;
  z-index: 1;
  max-width: 1120px;
  margin: 0 auto;
  min-height: calc(100vh - 64px);
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 26px;
  align-items: center;
}

.intro-panel {
  padding: 26px 8px;
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
  font-size: clamp(2rem, 4vw, 3.2rem);
  line-height: 1.2;
}

.intro-desc {
  margin: 14px 0 28px;
  color: var(--muted);
  font-size: 1.05rem;
  max-width: 540px;
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.link-card {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(6px);
  min-height: 92px;
  color: var(--ink);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  font-family: inherit;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.link-card .el-icon {
  font-size: 22px;
}

.link-card:hover {
  transform: translateY(-2px);
  border-color: rgba(15, 118, 110, 0.42);
  color: var(--teal);
}

.notice-box {
  border-radius: 18px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(6px);
  padding: 16px 18px;
}

.notice-box h3 {
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--orange);
  font-size: 1rem;
}

.notice-box ul {
  margin: 0;
  padding-left: 20px;
  color: var(--muted);
  line-height: 1.6;
}

.auth-card {
  border-radius: 22px;
  border: 1px solid var(--border);
  background: var(--surface);
  backdrop-filter: blur(12px);
  box-shadow: 0 20px 40px rgba(15, 28, 42, 0.12);
}

.auth-header {
  text-align: center;
  margin-bottom: 20px;
}

.logo-badge {
  width: 58px;
  height: 58px;
  margin: 0 auto 12px;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, #0f766e, #115e59);
  display: grid;
  place-items: center;
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.3);
}

.auth-header h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.52rem;
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
  margin-top: 8px;
}

:deep(.el-card__body) {
  padding: 28px;
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
  .auth-layout {
    grid-template-columns: 1fr;
    align-items: start;
    padding-top: 36px;
    padding-bottom: 28px;
  }

  .intro-panel {
    order: 2;
    padding-top: 10px;
  }

  .auth-card {
    order: 1;
  }
}

@media (max-width: 640px) {
  .auth-shell {
    padding: 14px;
  }

  .auth-layout {
    gap: 16px;
    min-height: auto;
  }

  .links-grid {
    grid-template-columns: 1fr 1fr;
  }

  .auth-header h2 {
    font-size: 1.36rem;
  }

  :deep(.el-card__body) {
    padding: 18px;
  }
}
</style>
