<template>
  <div class="profile-page">
    <section class="profile-panel">
      <div class="panel-header">
        <div>
          <p class="panel-kicker">Account</p>
          <h1>我的头像</h1>
          <p>上传并更新你的个人头像</p>
        </div>
      </div>

      <el-card class="profile-card" shadow="never">
        <div class="avatar-content">
          <div class="upload-section">
            <el-upload
              class="avatar-uploader-custom"
              action="#"
              :show-file-list="false"
              accept="image/jpeg,image/png"
              :http-request="uploadAvatar"
              :before-upload="beforeAvatarUpload"
            >
              <el-button size="large" class="upload-btn">
                <el-icon class="el-icon--left"><Picture /></el-icon>
                选择本地图片
              </el-button>
            </el-upload>
            <p class="upload-hint">支持 JPG / PNG，文件大小不超过 2MB。</p>
          </div>

          <div class="divider"></div>

          <div class="preview-section">
            <el-avatar :size="120" :src="avatarUrl" @error="handleAvatarError">
              {{ (realName || username || 'U').charAt(0).toUpperCase() }}
            </el-avatar>
            <span class="avatar-label">当前头像</span>
          </div>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'

const authStore = useAuthStore()
const avatarUrl = ref('')
const username = ref('')
const realName = ref('')

const beforeAvatarUpload = (rawFile) => {
  const isImage = rawFile.type === 'image/jpeg' || rawFile.type === 'image/png'
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('上传头像图片只能是 JPG/PNG 格式')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  return true
}

const extractUploadUrl = (payload) => {
  if (typeof payload === 'string') {
    return payload
  }
  if (!payload || typeof payload !== 'object') {
    return ''
  }
  return payload.url || payload.fileUrl || payload.path || payload.data?.url || payload.data?.fileUrl || ''
}

const uploadAvatar = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)

  try {
    const payload = await axios.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    const url = extractUploadUrl(payload)
    if (!url) {
      throw new Error('上传成功但未返回头像地址')
    }

    avatarUrl.value = url
    await axios.put('/users/me', { avatarUrl: url })

    if (authStore.user) {
      authStore.setUser({
        ...authStore.user,
        avatarUrl: url
      })
    }

    ElMessage.success('头像上传并保存成功')
  } catch (error) {
    console.error('Upload failed:', error)
    ElMessage.error(error.response?.data?.message || error.message || '头像上传失败')
  }
}

const handleAvatarError = () => true

const fillUser = (user) => {
  if (!user) {
    return
  }
  avatarUrl.value = user.avatarUrl || ''
  username.value = user.username || ''
  realName.value = user.realName || ''
}

onMounted(async () => {
  fillUser(authStore.user)

  try {
    const res = await axios.get('/users/me')
    fillUser(res)
  } catch (error) {
    console.error('Failed to fetch user profile', error)
  }
})
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.profile-page {
  --ink: #142132;
  --muted: #5b6f86;
  --border: rgba(20, 33, 50, 0.12);
  --panel-bg: rgba(255, 255, 255, 0.64);
  --card-bg: rgba(255, 255, 255, 0.9);

  min-height: auto;
  padding: 8px 0 0;
  background: transparent;
  color: var(--ink);
}

.profile-panel {
  border-radius: 18px;
  border: 1px solid var(--border);
  background: var(--panel-bg);
  backdrop-filter: blur(10px);
  box-shadow: 0 14px 28px rgba(15, 28, 42, 0.08);
  padding: 18px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 14px;
}

.panel-kicker {
  margin: 0 0 8px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-weight: 700;
  color: var(--muted);
  font-size: 0.8rem;
}

.panel-header h1 {
  margin: 0;
  font-size: clamp(1.3rem, 2.6vw, 1.85rem);
}

.panel-header p {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.55;
}

.profile-card {
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--card-bg);
  box-shadow: none;
  overflow: hidden;
}

:deep(.profile-card .el-card__body) {
  padding: 24px 20px;
}

.avatar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.upload-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.upload-btn {
  height: 48px;
  padding: 0 20px;
}

.upload-hint {
  margin: 0;
  color: #7f8fa4;
  font-size: 0.9rem;
}

.divider {
  width: 1px;
  align-self: stretch;
  background-color: rgba(20, 33, 50, 0.1);
}

.preview-section {
  width: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-label {
  color: var(--muted);
  font-size: 0.9rem;
}

@media (max-width: 860px) {
  .profile-page {
    padding-top: 4px;
  }

  .profile-panel {
    border-radius: 14px;
    padding: 14px;
  }

  .avatar-content {
    flex-direction: column;
    align-items: stretch;
  }

  .divider {
    width: 100%;
    height: 1px;
  }

  .upload-section,
  .preview-section {
    width: 100%;
    align-items: center;
  }
}
</style>
