<template>
  <div class="auth-container">
    <div class="background-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
    </div>
    
    <div class="portal-content">
      <!-- Left Side: Login Form -->
      <el-card class="auth-card">
        <div class="auth-header">
          <div class="logo-area">
             <!-- Placeholder for Logo -->
             <el-icon :size="40" color="#3b82f6"><School /></el-icon>
          </div>
          <h2 class="text-gradient">校园社团管理系统</h2>
          <p class="auth-subtitle">统一身份认证入口</p>
        </div>
        
        <el-form :model="form" label-position="top" size="large">
          <el-form-item label="学号/职工号">
            <el-input v-model="form.username" placeholder="请输入账号" @keyup.enter="handleLogin">
                <template #prefix>
                    <el-icon><User /></el-icon>
                </template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="handleLogin">
                <template #prefix>
                    <el-icon><Lock /></el-icon>
                </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleLogin" class="w-full" :loading="loading">立即登录</el-button>
          </el-form-item>
          <div class="auth-footer">
            <el-button link @click="$router.push('/register')">账号激活/注册</el-button>
            <el-divider direction="vertical" />
            <el-button link @click="$router.push('/forgot-password')">忘记密码?</el-button>
          </div>
        </el-form>
      </el-card>

      <!-- Right Side: Quick Links (Guest Access) -->
      <div class="quick-links-panel">
        <h3 class="panel-title">快速通道</h3>
        <div class="links-grid">
          <div class="link-card" @click="$router.push('/home/notices')">
            <el-icon><Bell /></el-icon>
            <span>公告信息</span>
          </div>
          <div class="link-card" @click="$router.push('/home/clubs')">
            <el-icon><Search /></el-icon>
            <span>社团信息</span>
          </div>
          <div class="link-card" @click="$router.push('/home/activities')">
            <el-icon><Calendar /></el-icon>
            <span>社团活动</span>
          </div>
          <div class="link-card" @click="$router.push('/home/schedule')">
            <el-icon><Warning /></el-icon>
            <span>社团日程</span>
          </div>
        </div>
        
        <div class="system-notice">
          <h4><el-icon><Warning /></el-icon> 系统通知</h4>
          <ul>
            <li>新生社团招新将于9月1日正式启动</li>
            <li>系统维护时间：每周日凌晨 02:00-04:00</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import { User, Lock, School, HomeFilled, Search, Bell, Calendar, Warning } from '@element-plus/icons-vue'

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
    
    // Check for redirect query param
    if (route.query.redirect) {
        router.push(route.query.redirect)
        return
    }

    // Check for admin roles
    const roles = res.user.roles || []
    const isAdmin = roles.some(r => {
        const code = typeof r === 'string' ? r : r.code
        return code === 'ADMIN'
    })
    
    const isClubAdmin = roles.some(r => {
        const code = typeof r === 'string' ? r : r.code
        return code === 'CLUB_ADMIN'
    })
    
    if (isAdmin) {
        router.push('/admin')
    } else if (isClubAdmin) {
        // Club Admin goes to home page but has special permissions in user center
        router.push('/home') 
    } else {
        // Student goes to home page
        router.push('/home')
    }
  } catch (error) {
    alert('登录失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// Ensure logout when visiting login page
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'

onMounted(() => {
    if (authStore.token) {
        if (route.query.redirect) {
            router.push(route.query.redirect)
            return
        }
        const roles = authStore.user?.roles || []
        const isAdmin = roles.some(r => (typeof r === 'string' ? r : r.code) === 'ADMIN')
        router.push(isAdmin ? '/admin' : '/home')
    }
})
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f3f4f6;
  position: relative;
  overflow: hidden;
  background-image: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.background-shapes .shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.6;
}

.shape-1 {
  width: 400px;
  height: 400px;
  background: #3b82f6;
  top: -100px;
  left: -100px;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: #8b5cf6;
  bottom: -50px;
  right: -50px;
}

.portal-content {
  display: flex;
  gap: 40px;
  z-index: 10;
  max-width: 900px;
  width: 90%;
}

.auth-card {
  flex: 1;
  max-width: 400px;
  padding: 2rem;
  backdrop-filter: blur(10px);
  background-color: rgba(255, 255, 255, 0.95) !important;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.1);
}

.auth-header {
  text-align: center;
  margin-bottom: 2rem;
}

.logo-area {
  margin-bottom: 10px;
}

h2 {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: #1f2937;
  font-weight: 800;
}

.auth-subtitle {
  color: #6b7280;
  font-size: 0.95rem;
}

.w-full {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.auth-footer {
  text-align: center;
  margin-top: 1rem;
}

/* Quick Links Panel */
.quick-links-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.panel-title {
  font-size: 1.5rem;
  color: #1f2937;
  margin-bottom: 20px;
  font-weight: 700;
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 30px;
}

.link-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  color: #4b5563;
}

.link-card:hover {
  transform: translateY(-5px);
  background: #3b82f6;
  color: white;
}

.link-card .el-icon {
  font-size: 24px;
}

.system-notice {
  background: rgba(255, 255, 255, 0.6);
  padding: 20px;
  border-radius: 12px;
  backdrop-filter: blur(5px);
}

.system-notice h4 {
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #d97706;
}

.system-notice ul {
  margin: 0;
  padding-left: 20px;
  color: #4b5563;
  font-size: 0.9rem;
}

.system-notice li {
  margin-bottom: 5px;
}

@media (max-width: 768px) {
  .portal-content {
    flex-direction: column;
    align-items: center;
  }
  
  .quick-links-panel {
    width: 100%;
    max-width: 400px;
  }
}
</style>
