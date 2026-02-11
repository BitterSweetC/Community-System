<template>
  <div class="academic-container">
    <div class="page-header">
      <div class="breadcrumb">
        <router-link to="/home" class="breadcrumb-link">首页</router-link>
        <span class="separator">/</span>
        <span>社团活动</span>
      </div>
      <div class="header-content">
        <h2>活动一览</h2>
      </div>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" class="search-form">
        <el-form-item label="活动名称">
          <el-input v-model="searchKeyword" placeholder="请输入活动名称">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="举办社团">
           <el-input v-model="searchClub" placeholder="请输入社团名称">
             <template #prefix>
               <el-icon><Search /></el-icon>
             </template>
           </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
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
        <el-table-column prop="status" label="状态" width="100" align="center">
           <template #default="scope">
              <el-tag :type="getActivityStatusType(scope.row)">{{ getActivityStatus(scope.row) }}</el-tag>
           </template>
        </el-table-column>
        <el-table-column prop="title" label="活动主题" min-width="250">
          <template #default="scope">
            <span class="table-link" @click="showActivityDetail(scope.row)">{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="clubName" label="主办单位" width="180" />
        <el-table-column prop="location" label="地点" width="150" />
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="scope">
             {{ formatTime(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
            <template #default="scope">
                <el-button 
                  v-if="scope.row.signupStatus === 'SIGNED'" 
                  type="primary" 
                  size="small" 
                  @click="openSignInDialog(scope.row)"
                >
                  签到
                </el-button>
                <span v-else-if="scope.row.signupStatus === 'SIGNED_IN'" style="color: #67C23A">
                   已签到
                </span>
                <el-button 
                  v-else 
                  size="small" 
                  type="primary" 
                  link 
                  @click="signUp(scope.row)"
                >
                  报名
                </el-button>
            </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="total"
          :page-size="pageSize"
          v-model:current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- Activity Detail Dialog -->
    <el-dialog v-model="detailDialogVisible" title="活动详情" width="500px">
      <div v-if="currentActivity" class="activity-detail">
        <div class="detail-item">
          <span class="label">活动主题：</span>
          <span class="value">{{ currentActivity.title }}</span>
        </div>
        <div class="detail-item">
          <span class="label">主办社团：</span>
          <span class="value">{{ currentActivity.clubName }}</span>
        </div>
        <div class="detail-item">
          <span class="label">活动时间：</span>
          <span class="value">{{ formatTime(currentActivity.startTime) }} ~ {{ formatTime(currentActivity.endTime) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">活动地点：</span>
          <span class="value">{{ currentActivity.location }}</span>
        </div>
        <div class="detail-item">
          <span class="label">活动描述：</span>
          <p class="value description">{{ currentActivity.description || '暂无描述' }}</p>
        </div>
        <div class="detail-item">
          <span class="label">活动状态：</span>
          <el-tag :type="getActivityStatusType(currentActivity)">{{ getActivityStatus(currentActivity) }}</el-tag>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button 
            type="primary" 
            v-if="currentActivity && currentActivity.signupStatus !== 'SIGNED' && currentActivity.signupStatus !== 'SIGNED_IN' && getActivityStatus(currentActivity) === '报名中'" 
            @click="signUp(currentActivity)"
          >
            立即报名
          </el-button>
        </span>
      </template>
    </el-dialog>

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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const currentActivity = ref(null)

const activities = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)
const searchKeyword = ref('')
const searchClub = ref('')

const fetchActivities = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    const res = await axios.get('/activities', { params })
    
    let list = []
    if (res.list) {
      list = res.list
      total.value = res.total
    } else if (res.content) {
      list = res.content
      total.value = res.totalElements
    } else {
      list = res
      total.value = res.length
    }
    
    // Filter simulation
    if (searchKeyword.value) {
        list = list.filter(a => a.title.includes(searchKeyword.value))
    }
    if (searchClub.value) {
        list = list.filter(a => a.clubName && a.clubName.includes(searchClub.value))
    }
    
    // Check signup status
    if (authStore.token) {
        try {
            const mySignups = await axios.get('/activities/my-signups')
            const signupMap = {}
            mySignups.forEach(s => {
                signupMap[s.id] = s.signupStatus
            })
            list = list.map(a => ({
                ...a,
                signupStatus: signupMap[a.id]
            }))
        } catch (e) {
            console.warn('Failed to load my signups', e)
        }
    }

    activities.value = list
    
    // Check if we need to auto-trigger signup (redirected from login)
    if (route.query.action === 'signup' && route.query.activityId) {
        const activityId = parseInt(route.query.activityId)
        // Find the activity in the loaded list
        const activity = list.find(a => a.id === activityId)
        if (activity) {
            // Remove query param to avoid re-triggering on refresh
            // router.replace({ query: {} }) // Optional
            performSignUp(activity)
        }
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const getActivityStatus = (activity) => {
    const now = new Date()
    const start = new Date(activity.startTime)
    const end = new Date(activity.endTime)
    
    if (now < start) return '报名中'
    if (now >= start && now <= end) return '进行中'
    return '已结束'
}

const getActivityStatusType = (activity) => {
    const s = getActivityStatus(activity)
    if (s === '报名中') return 'success'
    if (s === '进行中') return 'warning'
    return 'info'
}

const detailDialogVisible = ref(false)

const showActivityDetail = (activity) => {
    currentActivity.value = activity
    detailDialogVisible.value = true
}

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
        fetchActivities() // Refresh list
    } catch (error) {
        ElMessage.error(error.message || '签到失败，请检查签到码')
    }
}

const signUp = async (activity) => {
    if (!authStore.token) {
        router.push({
            path: '/login',
            query: {
                redirect: route.fullPath + '?action=signup&activityId=' + activity.id
            }
        })
        return
    }
    await performSignUp(activity)
}

const performSignUp = async (activity) => {
    try {
        await axios.post(`/activities/${activity.id}/signup`)
        ElMessage.success('报名成功')
    } catch (e) {
        if (e.message && e.message.includes('请先加入')) {
            ElMessageBox.alert(e.message, '提示', {
                confirmButtonText: '确定',
                type: 'warning'
            })
        } else {
            ElMessage.warning('报名失败: ' + (e.message || '未知错误'))
        }
    }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchActivities()
}

const resetSearch = () => {
  searchKeyword.value = ''
  searchClub.value = ''
  handleSearch()
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchActivities()
}

const formatTime = (str) => {
    if(!str) return ''
    return new Date(str).toLocaleString()
}

onMounted(() => {
  fetchActivities()
})
</script>

<style scoped>
/* Reusing the academic styles */
.academic-container {
  padding: 20px;
  background-color: #fff;
  min-height: 80vh;
}

.page-header {
  border-bottom: 2px solid #1f2937;
  margin-bottom: 20px;
  padding-bottom: 10px;
}

.breadcrumb {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.breadcrumb-link {
  color: #606266;
  text-decoration: none;
  transition: color 0.2s;
}

.breadcrumb-link:hover {
  color: #3b82f6;
}

.separator {
  margin: 0 8px;
  color: #c0c4cc;
}

.filter-bar {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
}

.search-form {
  margin-bottom: 0;
}

.data-table-wrapper {
  border: 1px solid #ebeef5;
}

:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #333;
  font-weight: bold;
}

.table-link {
  color: #333;
  font-weight: 500;
  transition: all 0.3s;
}

.table-link:hover {
  color: #409EFF;
  cursor: pointer;
  text-decoration: underline;
}

.activity-detail {
  padding: 10px;
}

.detail-item {
  margin-bottom: 15px;
  display: flex;
}

.detail-item .label {
  font-weight: bold;
  width: 100px;
  flex-shrink: 0;
  color: #606266;
}

.detail-item .value {
  color: #303133;
  line-height: 1.5;
}

.detail-item .description {
  margin: 0;
  white-space: pre-wrap;
}

.pagination-container {
  padding: 20px;
  display: flex;
  justify-content: center;
}
</style>