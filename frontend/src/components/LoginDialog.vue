<template>
  <el-dialog
    v-model="visible"
    title="登录"
    width="400px"
    destroy-on-close
    align-center
    :close-on-click-modal="false"
  >
    <el-form :model="form" label-position="top" size="large">
        <el-form-item label="学号/职工号">
        <el-input v-model="form.username" placeholder="请输入账号">
            <template #prefix>
                <el-icon><User /></el-icon>
            </template>
        </el-input>
        </el-form-item>
        <el-form-item label="密码">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password>
            <template #prefix>
                <el-icon><Lock /></el-icon>
            </template>
        </el-input>
        </el-form-item>
    </el-form>
    <template #footer>
        <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleLogin" :loading="loading">登录</el-button>
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
