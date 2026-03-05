<template>
  <div class="auth-shell">
    <div class="ambient ambient-left"></div>
    <div class="ambient ambient-right"></div>
    <div class="grid-overlay"></div>

    <div class="auth-layout">
      <section class="intro-panel">
        <p class="kicker">Account Recovery</p>
        <h1>找回密码，快速恢复访问权限</h1>
        <p class="intro-desc">通过注册邮箱完成身份验证后，即可重置密码并继续使用系统。</p>

        <div class="step-guide">
          <div class="step-item" :class="{ active: activeStep === 0 }">
            <span>1</span>
            <div>
              <h3>验证邮箱</h3>
              <p>输入注册邮箱并获取 6 位验证码。</p>
            </div>
          </div>
          <div class="step-item" :class="{ active: activeStep === 1 }">
            <span>2</span>
            <div>
              <h3>重置密码</h3>
              <p>填写验证码和新密码并确认提交。</p>
            </div>
          </div>
        </div>
      </section>

      <el-card class="auth-card" shadow="never">
        <div class="auth-header">
          <div class="logo-badge">
            <el-icon :size="26"><Lock /></el-icon>
          </div>
          <h2>找回密码</h2>
          <p>通过注册邮箱重置账号密码</p>
        </div>

        <el-steps :active="activeStep" finish-status="success" simple class="steps-line">
          <el-step title="验证身份" />
          <el-step title="重置密码" />
        </el-steps>

        <el-form v-if="activeStep === 0" :model="emailForm" label-position="top" size="large" @submit.prevent>
          <el-form-item label="邮箱地址">
            <el-input v-model="emailForm.email" placeholder="请输入注册邮箱">
              <template #prefix><el-icon><Message /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="action-btn" :loading="loading" @click="sendCode">获取验证码</el-button>
          </el-form-item>
        </el-form>

        <el-form v-else :model="resetForm" label-position="top" size="large" @submit.prevent>
          <el-alert title="验证码已发送，请检查邮箱" type="success" :closable="false" class="status-alert" />
          <el-form-item label="验证码">
            <el-input v-model="resetForm.code" placeholder="请输入 6 位验证码">
              <template #prefix><el-icon><Ticket /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="请输入新密码">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="action-btn" :loading="loading" @click="handleReset">确认重置</el-button>
          </el-form-item>
        </el-form>

        <div class="auth-footer">
          <el-button link @click="router.push('/login')">返回登录</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, Ticket } from '@element-plus/icons-vue'
import axios from '@/api/axios'

const router = useRouter()
const loading = ref(false)
const activeStep = ref(0)

const emailForm = reactive({
  email: ''
})

const resetForm = reactive({
  email: '',
  code: '',
  newPassword: ''
})

const sendCode = async () => {
  if (!emailForm.email) {
    ElMessage.warning('请输入邮箱地址')
    return
  }

  loading.value = true
  try {
    await axios.post('/auth/forgot-password', { email: emailForm.email })
    ElMessage.success('验证码已发送')
    resetForm.email = emailForm.email
    activeStep.value = 1
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '验证码发送失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const handleReset = async () => {
  if (!resetForm.code || !resetForm.newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }

  loading.value = true
  try {
    await axios.post('/auth/reset-password', resetForm)
    ElMessage.success('密码重置成功，请重新登录')
    router.push('/login')
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '密码重置失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;700&display=swap');

* {
  box-sizing: border-box;
}

.auth-shell {
  --ink: #0f1c2a;
  --muted: #516276;
  --surface: rgba(255, 255, 255, 0.84);
  --border: rgba(15, 28, 42, 0.14);
  --teal: #0f766e;
  --orange: #c2410c;

  position: relative;
  min-height: 100vh;
  padding: 30px;
  overflow: hidden;
  background: radial-gradient(circle at 12% 16%, rgba(15, 118, 110, 0.16), transparent 30%),
    radial-gradient(circle at 88% 88%, rgba(194, 65, 12, 0.16), transparent 34%),
    linear-gradient(168deg, #f8f3e8 0%, #eee7d8 56%, #e4ece8 100%);
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
  width: 300px;
  height: 300px;
  left: -70px;
  top: -110px;
  background: rgba(15, 118, 110, 0.2);
}

.ambient-right {
  width: 250px;
  height: 250px;
  right: -70px;
  bottom: -110px;
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
  max-width: 1080px;
  margin: 0 auto;
  min-height: calc(100vh - 60px);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 26px;
  align-items: center;
}

.intro-panel {
  padding: 10px 6px;
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
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 1.2;
}

.intro-desc {
  margin: 14px 0 24px;
  color: var(--muted);
  font-size: 1.04rem;
  max-width: 520px;
}

.step-guide {
  display: grid;
  gap: 12px;
}

.step-item {
  border-radius: 16px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.66);
  backdrop-filter: blur(6px);
  padding: 14px;
  display: grid;
  grid-template-columns: 30px 1fr;
  gap: 10px;
}

.step-item span {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  background: #8da0b2;
  font-weight: 700;
}

.step-item.active span {
  background: linear-gradient(135deg, var(--teal), #115e59);
}

.step-item h3 {
  margin: 0 0 4px;
  font-size: 1rem;
}

.step-item p {
  margin: 0;
  color: var(--muted);
  line-height: 1.5;
  font-size: 0.92rem;
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
  margin-bottom: 14px;
}

.logo-badge {
  width: 56px;
  height: 56px;
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
  font-size: 1.56rem;
}

.auth-header p {
  margin: 8px 0 0;
  color: var(--muted);
}

.steps-line {
  margin-bottom: 16px;
}

.status-alert {
  margin-bottom: 14px;
}

.action-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #c2410c 0%, #9a3412 100%);
  font-family: inherit;
  font-weight: 700;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.auth-footer {
  margin-top: 8px;
  text-align: center;
}

:deep(.el-card__body) {
  padding: 24px;
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

@media (max-width: 980px) {
  .auth-layout {
    grid-template-columns: 1fr;
    align-items: start;
    padding-top: 24px;
    padding-bottom: 20px;
  }

  .intro-panel {
    order: 2;
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

  :deep(.el-card__body) {
    padding: 18px;
  }
}
</style>
