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
        <el-button @click="$router.push('/home')" plain size="small">返回首页</el-button>
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
            <span class="table-link">{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="clubName" label="主办单位" width="180" />
        <el-table-column prop="location" label="地点" width="150" />
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="scope">
             {{ formatTime(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
            <template #default="scope">
                <el-button size="small" type="primary" link @click="signUp(scope.row)">报名</el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'
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
        ElMessage.warning('报名失败: ' + (e.message || '未知错误'))
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
}

.pagination-container {
  padding: 20px;
  display: flex;
  justify-content: center;
}
</style>