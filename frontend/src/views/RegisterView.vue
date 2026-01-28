<template>
  <div class="auth-container">
    <div class="background-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
    </div>
    
    <el-card class="auth-card">
      <div class="auth-header">
        <h2 class="text-gradient">加入我们</h2>
        <p class="auth-subtitle">创建账号，开始探索社团</p>
      </div>
      
      <el-form :model="form" label-position="top" size="large">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" prefix-icon="CreditCard" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" class="w-full">立即注册</el-button>
        </el-form-item>
        <div class="auth-footer">
          <span>已有账号?</span>
          <el-button type="primary" link @click="$router.push('/login')">立即登录</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/api/axios'
import { User, Lock, CreditCard } from '@element-plus/icons-vue'

const router = useRouter()
const form = ref({
  username: '',
  password: '',
  realName: ''
})

const handleRegister = async () => {
  try {
    await axios.post('/auth/register', form.value)
    alert('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    alert('注册失败: ' + error.message)
  }
}
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
  background: var(--color-primary-start);
  top: -100px;
  left: -100px;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: var(--color-primary-end);
  bottom: -50px;
  right: -50px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 1rem;
  z-index: 10;
  backdrop-filter: blur(10px);
  background-color: rgba(255, 255, 255, 0.9) !important;
}

.auth-header {
  text-align: center;
  margin-bottom: 2rem;
}

h2 {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.auth-subtitle {
  color: #6b7280;
  font-size: 0.95rem;
}

.w-full {
  width: 100%;
  margin-top: 1rem;
}

.auth-footer {
  text-align: center;
  margin-top: 1rem;
  font-size: 0.9rem;
  color: #6b7280;
}
</style>
