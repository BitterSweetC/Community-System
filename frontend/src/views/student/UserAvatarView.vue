<template>
  <div class="avatar-view-container premium-container">
    <div class="profile-header">
      <h2>我的头像</h2>
      <p>自定义您的个人头像。</p>
    </div>

    <el-card class="avatar-card">
      <div class="avatar-content">
        <div class="upload-section">
          <el-upload
            class="avatar-uploader-custom"
            action="#"
            :show-file-list="false"
            :http-request="uploadAvatar"
            :before-upload="beforeAvatarUpload"
          >
            <el-button size="large" class="upload-btn">
              <el-icon class="el-icon--left"><Picture /></el-icon>
              选择本地图片
            </el-button>
          </el-upload>
          
          <!-- Removed "Digital Art Avatar" option as requested -->
        </div>

        <div class="divider"></div>

        <div class="preview-section">
          <div class="avatar-display">
             <el-avatar :size="100" :src="avatarUrl" @error="handleAvatarError">
                {{ (realName || username || 'U').charAt(0).toUpperCase() }}
             </el-avatar>
             <span class="avatar-label">当前头像</span>
          </div>
        </div>
      </div>

      <div class="avatar-tips">
        请选择图片上传：大小180 * 180像素支持JPG、PNG等格式，图片需小于2M
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const avatarUrl = ref('')
const username = ref('')
const realName = ref('')

const beforeAvatarUpload = (rawFile) => {
  const isImage = rawFile.type === 'image/jpeg' || rawFile.type === 'image/png'
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('上传头像图片只能是 JPG/PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

const uploadAvatar = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)

  try {
    const url = await axios.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    // 1. Update local display
    avatarUrl.value = url
    
    // 2. Update user profile in backend
    await axios.put('/users/me', {
        avatarUrl: url
    })

    // 3. Update store
    if (authStore.user) {
        authStore.user.avatarUrl = url
    }
    
    ElMessage.success('头像上传并保存成功')
  } catch (error) {
    console.error('Upload failed:', error)
    ElMessage.error('头像上传失败')
  }
}

const handleAvatarError = () => {
  return true
}

onMounted(async () => {
  // Init from store
  if (authStore.user) {
      avatarUrl.value = authStore.user.avatarUrl
      username.value = authStore.user.username
      realName.value = authStore.user.realName
  }
  
  // Fetch fresh
  try {
      const res = await axios.get('/users/me')
      if (res) {
          avatarUrl.value = res.avatarUrl
          username.value = res.username
          realName.value = res.realName
      }
  } catch(e) {
      console.error(e)
  }
})
</script>

<style scoped>
.avatar-view-container {
  max-width: 800px;
}

.profile-header {
  margin-bottom: 20px;
}

.profile-header h2 {
  margin: 0 0 10px 0;
  font-size: 1.5rem;
  color: var(--color-heading);
}

.profile-header p {
  color: var(--color-text-light);
  margin: 0;
}

.avatar-card {
  padding: 40px;
}

.avatar-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 60px;
  margin-bottom: 40px;
}

.upload-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-btn {
  width: 200px;
  height: 80px;
  font-size: 1.1rem;
  background-color: #f5f7fa;
  border: 1px dashed #dcdfe6;
  color: var(--color-text);
  display: flex;
  flex-direction: column; /* Icon top, text bottom if needed, but here row is fine */
  justify-content: center;
}

.upload-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background-color: var(--color-primary-light);
}

.divider {
  width: 1px;
  height: 120px;
  background-color: #e4e7ed;
}

.preview-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.avatar-label {
  color: var(--color-text-light);
  font-size: 0.9rem;
}

.avatar-tips {
  text-align: center;
  color: var(--color-text-light);
  font-size: 0.85rem;
  margin-top: 20px;
}
</style>
