<template>
  <div class="auth-shell">
    <div class="ambient ambient-left"></div>
    <div class="ambient ambient-right"></div>
    <div class="grid-overlay"></div>

    <div class="auth-layout">
      <section class="intro-panel">
        <p class="kicker">Campus Orbit</p>
        <h1>创建账号，开始你的校园协作</h1>
        <p class="intro-desc">
          一次注册即可访问社团管理、活动报名、通知中心与个人档案。
        </p>

        <div class="feature-list">
          <div class="feature-item">
            <span class="dot"></span>
            <div>
              <h3>社团全景浏览</h3>
              <p>快速了解社团方向、活动安排与成员动态。</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="dot"></span>
            <div>
              <h3>活动一键报名</h3>
              <p>统一入口管理报名、签到与个人日程提醒。</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="dot"></span>
            <div>
              <h3>消息实时触达</h3>
              <p>重要公告和审核结果会第一时间推送给你。</p>
            </div>
          </div>
        </div>
      </section>

      <el-card class="auth-card" shadow="never">
        <div class="auth-header">
          <h2>加入我们</h2>
          <p>创建账号，开启你的校园社团体验</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent
        >
          <div class="form-grid">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入真实姓名">
                <template #prefix><el-icon><CreditCard /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="手机号" prop="mobile">
              <el-input v-model="form.mobile" placeholder="请输入手机号">
                <template #prefix><el-icon><Phone /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="电子邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入电子邮箱">
                <template #prefix><el-icon><Message /></el-icon></template>
              </el-input>
            </el-form-item>
          </div>

          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入密码"
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="register-btn" :loading="loading" @click="handleRegister(formRef)">
              立即注册
            </el-button>
          </el-form-item>

          <div class="auth-footer">
            <span>已有账号？</span>
            <el-button link @click="router.push('/login')">立即登录</el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CreditCard, Lock, Message, Phone, User } from '@element-plus/icons-vue'
import axios from '@/api/axios'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = ref({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  mobile: '',
  email: ''
})

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.value.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入电子邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' }
  ]
})

const handleRegister = async (formEl) => {
  if (!formEl) {
    return
  }

  await formEl.validate(async (valid) => {
    if (!valid) {
      return false
    }

    loading.value = true
    try {
      const { confirmPassword, ...payload } = form.value
      await axios.post('/auth/register', payload)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error) {
      const msg = error.response?.data?.message || error.message || '注册失败'
      ElMessage.error(msg)
    } finally {
      loading.value = false
    }

    return true
  })
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
  padding: 32px;
  overflow: hidden;
  background: radial-gradient(circle at 12% 14%, rgba(15, 118, 110, 0.16), transparent 30%),
    radial-gradient(circle at 88% 86%, rgba(194, 65, 12, 0.15), transparent 34%),
    linear-gradient(165deg, #f8f3e8 0%, #efe7d9 56%, #e4ece8 100%);
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
  width: 260px;
  height: 260px;
  right: -70px;
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
  max-width: 1160px;
  margin: 0 auto;
  min-height: calc(100vh - 64px);
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 28px;
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
  font-size: clamp(2rem, 4vw, 3.1rem);
  line-height: 1.2;
}

.intro-desc {
  margin: 14px 0 26px;
  color: var(--muted);
  font-size: 1.04rem;
  max-width: 540px;
}

.feature-list {
  display: grid;
  gap: 14px;
}

.feature-item {
  border-radius: 16px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.66);
  backdrop-filter: blur(6px);
  padding: 14px 14px;
  display: grid;
  grid-template-columns: 18px 1fr;
  gap: 10px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 8px;
  background: linear-gradient(135deg, var(--teal), #115e59);
}

.feature-item h3 {
  margin: 0 0 4px;
  font-size: 1rem;
}

.feature-item p {
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
  margin-bottom: 16px;
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

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 12px;
}

.register-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #c2410c 0%, #9a3412 100%);
  font-family: inherit;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.register-btn:hover {
  transform: translateY(-1px);
}

.auth-footer {
  margin-top: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  color: var(--muted);
}

:deep(.el-card__body) {
  padding: 26px;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
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

@media (max-width: 1024px) {
  .auth-layout {
    grid-template-columns: 1fr;
    align-items: start;
    padding-top: 26px;
    padding-bottom: 24px;
  }

  .intro-panel {
    order: 2;
  }

  .auth-card {
    order: 1;
  }
}

@media (max-width: 680px) {
  .auth-shell {
    padding: 14px;
  }

  .auth-layout {
    gap: 16px;
    min-height: auto;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  :deep(.el-card__body) {
    padding: 18px;
  }
}
</style>
