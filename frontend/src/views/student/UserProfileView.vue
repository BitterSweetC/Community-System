<template>
  <div class="profile-page">
    <section class="profile-panel">
      <div class="panel-header">
        <div>
          <p class="panel-kicker">Account</p>
          <h1>个人资料</h1>
          <p>管理你的账号信息、兴趣偏好与基础联系方式。</p>
        </div>
        <el-button type="primary" plain @click="isEditing = !isEditing">
          {{ isEditing ? '取消编辑' : '编辑资料' }}
        </el-button>
      </div>

      <el-card class="profile-card" shadow="never">
        <el-form :model="form" label-position="top" :disabled="!isEditing">
          <div class="form-grid">
            <el-form-item label="用户名">
              <el-input v-model="form.username" disabled />
            </el-form-item>

            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" />
            </el-form-item>

            <el-form-item label="手机号码">
              <el-input v-model="form.mobile" />
            </el-form-item>

            <el-form-item label="状态">
              <el-tag :type="form.status === 'ACTIVE' ? 'success' : 'danger'">
                {{ form.status || 'UNKNOWN' }}
              </el-tag>
            </el-form-item>
          </div>

          <el-form-item label="兴趣爱好">
            <div v-if="selectedInterests.length > 0 || isEditing" class="interest-wrap">
              <InterestSelector v-model="selectedInterests" :readonly="!isEditing" />
            </div>
            <div v-else class="empty-text">暂无兴趣爱好</div>
          </el-form-item>

          <el-form-item label="角色">
            <div v-if="roles.length > 0" class="roles-tags">
              <el-tag v-for="role in roles" :key="role" class="role-tag">{{ role }}</el-tag>
            </div>
            <div v-else class="empty-text">暂无角色</div>
          </el-form-item>

          <div v-if="isEditing" class="form-actions">
            <el-button type="primary" :loading="saving" @click="saveProfile">保存更改</el-button>
          </div>
        </el-form>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
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

const updateInterestsFromStr = (interestsStr) => {
  if (interestsStr && interestsStr.trim() !== '') {
    selectedInterests.value = interestsStr.split(',').map((item) => item.trim())
    return
  }
  selectedInterests.value = []
}

const fillForm = (user) => {
  if (!user) {
    return
  }

  form.value.username = user.username || ''
  form.value.realName = user.realName || ''
  form.value.mobile = user.mobile || ''
  form.value.status = user.status || ''
  form.value.avatarUrl = user.avatarUrl || ''
  updateInterestsFromStr(user.interests)

  if (user.roles) {
    roles.value = user.roles.map((role) => role.name || role.code || role)
  } else {
    roles.value = []
  }
}

onMounted(async () => {
  fillForm(authStore.user)

  try {
    const res = await axios.get('/users/me')
    fillForm(res?.data ?? res)
  } catch (error) {
    console.error('Failed to fetch user profile', error)
  }
})

const saveProfile = async () => {
  saving.value = true
  try {
    const interests = selectedInterests.value.join(',')

    await axios.put('/users/me', {
      realName: form.value.realName,
      mobile: form.value.mobile,
      interests,
      avatarUrl: form.value.avatarUrl
    })

    authStore.setUser({
      ...authStore.user,
      realName: form.value.realName,
      mobile: form.value.mobile,
      interests,
      avatarUrl: form.value.avatarUrl
    })

    ElMessage.success('资料更新成功')
    isEditing.value = false
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '更新资料失败')
  } finally {
    saving.value = false
  }
}
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
  padding: 18px 20px 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.interest-wrap {
  width: 100%;
}

.roles-tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.role-tag {
  margin: 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}

.empty-text {
  color: #7f8fa4;
  font-size: 0.9rem;
  font-style: italic;
}

@media (max-width: 860px) {
  .profile-page {
    padding-top: 4px;
  }

  .profile-panel {
    border-radius: 14px;
    padding: 14px;
  }

  .panel-header {
    flex-direction: column;
    align-items: stretch;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
