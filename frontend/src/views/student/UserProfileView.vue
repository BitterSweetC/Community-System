<template>
  <div class="profile-container premium-container">
    <div class="profile-header">
      <h2>个人资料</h2>
      <p>管理您的个人信息和账户设置。</p>
    </div>

    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <el-button type="primary" link @click="isEditing = !isEditing">
            {{ isEditing ? '取消' : '编辑' }}
          </el-button>
        </div>
      </template>

      <el-form :model="form" label-position="top" :disabled="!isEditing">
        <!-- Avatar Upload Section -->
        <div class="avatar-upload-container">
          <el-upload
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :http-request="uploadAvatar"
            :before-upload="beforeAvatarUpload"
            :disabled="!isEditing"
          >
            <div v-if="form.avatarUrl" class="avatar-wrapper">
               <el-avatar :size="100" :src="form.avatarUrl" />
               <div class="avatar-overlay" v-if="isEditing">
                 <el-icon><Camera /></el-icon>
                 <span>更换头像</span>
               </div>
            </div>
            <div v-else class="avatar-placeholder">
               <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
               <span>上传头像</span>
            </div>
          </el-upload>
        </div>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="form.username" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号码">
              <el-input v-model="form.mobile" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
               <el-tag :type="form.status === 'ACTIVE' ? 'success' : 'danger'">{{ form.status }}</el-tag>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="兴趣爱好">
          <InterestSelector v-model="selectedInterests" :readonly="!isEditing" />
        </el-form-item>

        <el-form-item label="角色">
          <div class="roles-tags">
            <el-tag v-for="role in roles" :key="role" class="role-tag">{{ role }}</el-tag>
          </div>
        </el-form-item>

        <div class="form-actions" v-if="isEditing">
          <el-button type="primary" @click="saveProfile" :loading="saving">保存更改</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'
import { Plus, Camera } from '@element-plus/icons-vue'
import InterestSelector from '@/components/InterestSelector.vue'

const authStore = useAuthStore()
const isEditing = ref(false)
const saving = ref(false)
const roles = ref([])
const selectedInterests = ref([])

const form = ref({
  username: '',
  realName: '',
  mobile: '',
  status: '',
  avatarUrl: ''
})

// Helper to process interests string <-> array
const updateInterestsFromStr = (str) => {
  if (!str) {
    selectedInterests.value = []
  } else {
    selectedInterests.value = str.split(',').filter(s => s)
  }
}

// Avatar Upload Logic
const beforeAvatarUpload = (rawFile) => {
  const isImage = rawFile.type.startsWith('image/')
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('头像必须是图片格式!')
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
    // Calling the new upload API
    const url = await axios.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    console.log('Upload response URL:', url)
    form.value.avatarUrl = url
    ElMessage.success('头像上传成功')
  } catch (error) {
    console.error('Upload failed:', error)
    ElMessage.error('头像上传失败')
  }
}

const handleAvatarError = () => {
  console.error('Avatar image failed to load:', form.value.avatarUrl)
  ElMessage.warning('头像加载失败，请检查网络或图片格式')
  return true
}

onMounted(async () => {
  // Initialize from store first
  const user = authStore.user
  if (user) {
    form.value.username = user.username
    form.value.realName = user.realName
    form.value.mobile = user.mobile
    form.value.status = user.status
    form.value.avatarUrl = user.avatarUrl
    updateInterestsFromStr(user.interests)
    // Parse roles if available (assuming user.roles is array of objects or strings)
    // If backend returns roles as list of objects {code: 'ADMIN', ...}
    if (user.roles) {
        roles.value = user.roles.map(r => r.name || r.code || r)
    }
  }
  
  // Fetch fresh data from backend
  try {
    const res = await axios.get('/users/me')
    // Update form with fresh data
    if (res) {
        form.value.realName = res.realName
        form.value.mobile = res.mobile
        form.value.avatarUrl = res.avatarUrl
        updateInterestsFromStr(res.interests)
        // Update other fields if needed, but username/status usually don't change
    }
  } catch (e) {
      console.error('Failed to fetch user profile', e)
  }
})

const saveProfile = async () => {
  saving.value = true
  try {
    const interestsStr = selectedInterests.value.join(',')
    
    // Update endpoint
    await axios.put('/users/me', {
        realName: form.value.realName,
        mobile: form.value.mobile,
        interests: interestsStr,
        avatarUrl: form.value.avatarUrl
    })
    
    // Update store
    const updatedUser = { 
        ...authStore.user, 
        realName: form.value.realName, 
        mobile: form.value.mobile,
        interests: interestsStr,
        avatarUrl: form.value.avatarUrl
    }
    authStore.setUser(updatedUser)
    
    ElMessage.success('资料更新成功')
    isEditing.value = false
  } catch (error) {
    ElMessage.error('更新资料失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-container {
  padding-top: 2rem;
  padding-bottom: 4rem;
}

.profile-header {
  margin-bottom: 2rem;
}

.profile-header h2 {
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 0.5rem;
}

.profile-header p {
  color: var(--color-text-light);
}

.profile-card {
  max-width: 800px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.roles-tags {
  display: flex;
  gap: 0.5rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

/* Avatar Styles */
.avatar-upload-container {
  display: flex;
  justify-content: center;
  margin-bottom: 2rem;
}

.avatar-uploader .avatar-wrapper {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-placeholder {
  width: 100px;
  height: 100px;
  border: 1px dashed var(--el-border-color);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: border-color 0.3s;
}

.avatar-placeholder:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.avatar-uploader-icon {
  font-size: 24px;
  margin-bottom: 4px;
}
</style>
