<template>
  <div class="my-activities-container">
    <div class="page-header">
      <h2>我的活动</h2>
    </div>

    <div class="data-table-wrapper">
      <el-table 
        :data="activities" 
        style="width: 100%" 
        stripe 
        border
        header-cell-class-name="table-header"
        v-loading="loading"
      >
        <el-table-column prop="signupStatus" label="报名状态" width="100" align="center">
           <template #default="scope">
              <el-tag :type="getSignupStatusType(scope.row.signupStatus)">{{ getSignupStatus(scope.row.signupStatus) }}</el-tag>
           </template>
        </el-table-column>
        <el-table-column prop="title" label="活动主题" min-width="250">
          <template #default="scope">
            <span class="table-link" @click="showDetails(scope.row)">{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="clubName" label="主办单位" width="180" />
        <el-table-column prop="location" label="地点" width="150" />
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="scope">
             {{ formatTime(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="signupTime" label="报名时间" width="180">
          <template #default="scope">
             {{ formatTime(scope.row.signupTime) }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Sign In Dialog -->
    <el-dialog v-model="signInDialogVisible" title="活动签到" width="400px">
      <div style="text-align: center; margin-bottom: 20px;">
         <p>请输入活动签到码进行签到</p>
         <el-input v-model="signInCode" placeholder="请输入签到码" style="max-width: 200px" />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="signInDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitSignIn">签到</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="活动详情" width="600px">
      <div v-if="currentActivity" class="activity-detail">
        <h3 class="detail-title">{{ currentActivity.title }}</h3>
        
        <div class="detail-grid">
            <div class="detail-item">
              <span class="label">主办单位：</span>
              <span>{{ currentActivity.clubName }}</span>
            </div>
            <div class="detail-item">
              <span class="label">地点：</span>
              <span>{{ currentActivity.location || '待定' }}</span>
            </div>
            <div class="detail-item full-width">
              <span class="label">时间：</span>
              <span>{{ formatTime(currentActivity.startTime) }} ~ {{ formatTime(currentActivity.endTime) }}</span>
            </div>
        </div>

        <div class="detail-section">
          <div class="section-title">活动介绍</div>
          <div class="description">{{ currentActivity.description || '暂无介绍' }}</div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button 
            v-if="currentActivity && currentActivity.signupStatus === 'SIGNED'" 
            type="primary" 
            @click="openSignInDialog(currentActivity)"
          >
            签到
          </el-button>
           <el-tag v-else-if="currentActivity && currentActivity.signupStatus === 'SIGNED_IN'" type="success" size="large" style="margin-right: 10px">
              已签到
           </el-tag>
           <el-button @click="detailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const activities = ref([])
const loading = ref(false)
const detailDialogVisible = ref(false)
const currentActivity = ref(null)

const signInDialogVisible = ref(false)
const signInCode = ref('')
const currentSignInActivity = ref(null)

const openSignInDialog = (activity) => {
    currentSignInActivity.value = activity
    signInCode.value = ''
    signInDialogVisible.value = true
}

const submitSignIn = async () => {
    if (!currentSignInActivity.value) return
    
    try {
        await axios.post(`/activities/${currentSignInActivity.value.id}/signin`, {
            code: signInCode.value
        })
        ElMessage.success('签到成功')
        signInDialogVisible.value = false
        await fetchMyActivities() // Refresh list
        
        // Update currentActivity if open
        if (currentActivity.value && currentActivity.value.id === currentSignInActivity.value.id) {
             const updated = activities.value.find(a => a.id === currentActivity.value.id)
             if (updated) {
                 currentActivity.value = updated
             }
        }
    } catch (error) {
        ElMessage.error(error.message || '签到失败，请检查签到码')
    }
}

const showDetails = (activity) => {
    currentActivity.value = activity
    detailDialogVisible.value = true
}

const fetchMyActivities = async () => {
  loading.value = true
  try {
    const res = await axios.get('/activities/my-signups')
    activities.value = res
  } catch (error) {
    console.error(error)
    ElMessage.error('获取活动列表失败')
  } finally {
    loading.value = false
  }
}

const getSignupStatus = (status) => {
    const map = {
        'SIGNED': '已报名',
        'SIGNED_IN': '已签到',
        'CANCELLED': '已取消'
    }
    return map[status] || status
}

const getSignupStatusType = (status) => {
    if (status === 'SIGNED') return 'primary'
    if (status === 'SIGNED_IN') return 'success'
    return 'info'
}

const formatTime = (str) => {
    if(!str) return ''
    return new Date(str).toLocaleString()
}

onMounted(() => {
  fetchMyActivities()
})
</script>

<style scoped>
.my-activities-container {
  padding: 20px;
  background-color: #fff;
  min-height: 100%;
}

.page-header {
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
  padding-bottom: 10px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #333;
  font-weight: bold;
}

.table-link {
  color: var(--color-primary, #409eff);
  cursor: pointer;
  font-weight: 500;
}
.table-link:hover {
  text-decoration: underline;
}

.activity-detail {
    padding: 10px;
}
.detail-title {
    margin-top: 0;
    margin-bottom: 20px;
    font-size: 18px;
    color: #303133;
    border-bottom: 1px solid #eee;
    padding-bottom: 10px;
}
.detail-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 15px;
    margin-bottom: 20px;
}
.detail-item {
    display: flex;
    align-items: center;
}
.detail-item.full-width {
    grid-column: span 2;
}
.label {
    font-weight: bold;
    color: #606266;
    margin-right: 8px;
    white-space: nowrap;
}
.section-title {
    font-weight: bold;
    color: #303133;
    margin-bottom: 10px;
    font-size: 15px;
}
.description {
    line-height: 1.6;
    color: #606266;
    background-color: #f9faFc;
    padding: 15px;
    border-radius: 4px;
    white-space: pre-wrap;
}
</style>
