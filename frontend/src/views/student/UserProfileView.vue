<template>
  <div class="profile-container premium-container">
    <div class="profile-header">
      <h2>User Profile</h2>
      <p>Manage your personal information and account settings.</p>
    </div>

    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>Basic Information</span>
          <el-button type="primary" link @click="isEditing = !isEditing">
            {{ isEditing ? 'Cancel' : 'Edit' }}
          </el-button>
        </div>
      </template>

      <el-form :model="form" label-position="top" :disabled="!isEditing">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Username">
              <el-input v-model="form.username" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Real Name">
              <el-input v-model="form.realName" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Mobile Number">
              <el-input v-model="form.mobile" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Status">
               <el-tag :type="form.status === 'ACTIVE' ? 'success' : 'danger'">{{ form.status }}</el-tag>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Roles">
          <div class="roles-tags">
            <el-tag v-for="role in roles" :key="role" class="role-tag">{{ role }}</el-tag>
          </div>
        </el-form-item>

        <div class="form-actions" v-if="isEditing">
          <el-button type="primary" @click="saveProfile" :loading="saving">Save Changes</el-button>
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

const authStore = useAuthStore()
const isEditing = ref(false)
const saving = ref(false)
const roles = ref([])

const form = ref({
  username: '',
  realName: '',
  mobile: '',
  status: ''
})

onMounted(async () => {
  // Initialize from store first
  const user = authStore.user
  if (user) {
    form.value.username = user.username
    form.value.realName = user.realName
    form.value.mobile = user.mobile
    form.value.status = user.status
    // Parse roles if available (assuming user.roles is array of objects or strings)
    // If backend returns roles as list of objects {code: 'ADMIN', ...}
    if (user.roles) {
        roles.value = user.roles.map(r => r.name || r.code || r)
    }
  }
  
  // Ideally fetch fresh data from backend
  // try {
  //   const res = await axios.get('/users/me')
  //   form.value = res
  // } catch (e) { ... }
})

const saveProfile = async () => {
  saving.value = true
  try {
    // Assuming we have an update endpoint
    await axios.put('/users/profile', {
        realName: form.value.realName,
        mobile: form.value.mobile
    })
    
    // Update store
    const updatedUser = { ...authStore.user, realName: form.value.realName, mobile: form.value.mobile }
    authStore.setUser(updatedUser)
    
    ElMessage.success('Profile updated successfully')
    isEditing.value = false
  } catch (error) {
    ElMessage.error('Failed to update profile')
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
</style>
