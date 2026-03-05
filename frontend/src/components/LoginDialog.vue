<template>
  <el-dialog
    v-model="visible"
    title="登录"
    width="420px"
    destroy-on-close
    align-center
    :close-on-click-modal="false"
    class="login-dialog"
  >
    <el-form :model="form" label-position="top" size="large" class="login-form">
        <el-form-item label="学号/职工号">
        <el-input v-model="form.username" placeholder="请输入账号" class="form-input">
            <template #prefix>
                <el-icon><User /></el-icon>
            </template>
        </el-input>
        </el-form-item>
        <el-form-item label="密码">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password class="form-input">
            <template #prefix>
                <el-icon><Lock /></el-icon>
            </template>
        </el-input>
        </el-form-item>
    </el-form>
    <template #footer>
        <span class="dialog-footer">
        <el-button @click="visible = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleLogin" :loading="loading" size="large">登录</el-button>
        </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  onSuccess: Function
})

const visible = ref(false)
const loading = ref(false)
const form = ref({
  username: '',
  password: ''
})

const authStore = useAuthStore()

const open = () => {
  visible.value = true
}

const handleLogin = async () => {
  loading.value = true
  try {
    const res = await axios.post('/auth/login', form.value)
    authStore.setToken(res.token)
    authStore.setUser(res.user)
    ElMessage.success('登录成功')
    visible.value = false
    if (props.onSuccess) {
        props.onSuccess()
    }
  } catch (error) {
    ElMessage.error('登录失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

defineExpose({
  open
})
</script>

<style scoped>
:deep(.login-dialog) {
  border-radius: var(--radius-lg);
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 24px 24px 16px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  color: white;
  margin: 0;
}

:deep(.el-dialog__title) {
  color: white;
  font-size: 1.25rem;
  font-weight: 600;
}

:deep(.el-dialog__headerbtn .el-dialog__close) {
  color: white;
  font-size: 20px;
}

:deep(.el-dialog__body) {
  padding: 32px 24px;
}

.login-form {
  margin-top: 8px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 8px;
}

:deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px var(--color-border) inset;
  transition: all 0.3s;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-primary-light) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary) inset;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  width: 100%;
}

:deep(.el-button) {
  border-radius: var(--radius-sm);
  font-weight: 500;
  padding: 12px 24px;
  transition: all 0.3s;
}

:deep(.el-button--primary) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

:deep(.el-button--primary:hover) {
  background: var(--color-primary-light);
  border-color: var(--color-primary-light);
  transform: translateY(-1px);
}
</style>
