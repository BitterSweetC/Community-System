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
      
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" :prefix-icon="CreditCard" />
        </el-form-item>

        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" placeholder="请输入手机号" :prefix-icon="Phone" />
        </el-form-item>

        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入电子邮箱" :prefix-icon="Message" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" :prefix-icon="Lock" show-password />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleRegister(formRef)" class="w-full">立即注册</el-button>
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/api/axios'
import { User, Lock, CreditCard, Message, Phone } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref()
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
    callback(new Error('两次输入密码不一致!'))
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
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' }
  ]
})

const handleRegister = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid, fields) => {
    if (valid) {
      try {
        // 构建提交数据，排除 confirmPassword
        const { confirmPassword, ...payload } = form.value
        await axios.post('/auth/register', payload)
        alert('注册成功，请登录')
        router.push('/login')
      } catch (error) {
        // 尝试获取后端返回的具体错误信息
        const msg = error.response?.data?.message || error.message || '注册失败'
        alert('注册失败: ' + msg)
      }
    } else {
      console.log('Validation failed:', fields)
      return false
    }
  })
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
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
}
</style>
