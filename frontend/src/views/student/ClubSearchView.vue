<template>
  <div class="academic-container">
    <div class="page-header">
      <div class="breadcrumb">
        <router-link to="/home" class="breadcrumb-link">首页</router-link>
        <span class="separator">/</span>
        <span>社团信息</span>
      </div>
      <h2>社团一览</h2>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" class="search-form">
        <el-form-item label="社团名称">
          <el-input v-model="searchKeyword" placeholder="请输入社团名称" style="width: 200px;">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="所属类别" class="category-filter-item">
          <el-select v-model="selectedCategory" placeholder="全部类别" clearable style="width: 160px;">
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="data-table-wrapper">
      <el-table 
        :data="clubs" 
        style="width: 100%" 
        stripe 
        border
        header-cell-class-name="table-header"
        v-loading="loading"
      >
        <el-table-column prop="id" label="社团编号" width="100" align="center" />
        <el-table-column prop="category" label="类别" width="120" align="center">
           <template #default="scope">
             <el-tag>{{ scope.row.category }}</el-tag>
           </template>
        </el-table-column>
        <el-table-column prop="name" label="社团名称" min-width="200">
          <template #default="scope">
            <a class="table-link" @click="viewClub(scope.row)">{{ scope.row.name }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="presidentName" label="社长" width="120" />
        <el-table-column prop="memberCount" label="人数" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
                <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">
                    {{ scope.row.status === 'ACTIVE' ? '招新中' : '筹备中' }}
                </el-tag>
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

    <!-- Club Info Dialog -->
    <el-dialog v-model="dialogVisible" title="社团信息" width="600px" destroy-on-close align-center class="club-info-dialog">
        <div class="club-info-table">
            <div class="info-row">
                <div class="info-label">社团名称(中)</div>
                <div class="info-value">{{ currentClub.name }}</div>
            </div>

            <div class="info-row">
                <div class="info-label">社长姓名</div>
                <div class="info-value">{{ currentClub.presidentName || '未知' }}</div>
            </div>
             <div class="info-row">
                <div class="info-label">社团类别</div>
                <div class="info-value">{{ currentClub.category }}</div>
            </div>
            <div class="info-row">
                <div class="info-label">社团简介</div>
                <div class="info-value">{{ currentClub.description }}</div>
            </div>
        </div>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="dialogVisible = false">关闭</el-button>
                <el-button type="primary" @click="joinClub(currentClub)">申请加入</el-button>
            </span>
        </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/api/axios'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const clubs = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const searchKeyword = ref('')
const selectedCategory = ref('')

const dialogVisible = ref(false)
const currentClub = ref({})

const categories = ref(['学术科技', '文化艺术', '体育竞技', '公益志愿', '实践创新', '其他'])

const fetchClubs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (selectedCategory.value) params.category = selectedCategory.value

    const res = await axios.get('/clubs', { params })
    
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
    
    // Mock missing data for table display
    clubs.value = list.map(c => ({
        ...c,
        presidentName: c.presidentName || '张三', // Mock if not in API
        memberCount: c.memberCount || Math.floor(Math.random() * 50) + 10 // Mock
    }))
    
  } catch (error) {
    console.error('Failed to fetch clubs:', error)
  } finally {
    loading.value = false
  }
}

const viewClub = (club) => {
    currentClub.value = club
    dialogVisible.value = true
}

const joinClub = (club) => {
    // Navigate to detail page for application
    router.push(`/home/clubs/${club.id}`)
}

const handleSearch = () => {
  currentPage.value = 1
  fetchClubs()
}

const resetSearch = () => {
  searchKeyword.value = ''
  selectedCategory.value = ''
  handleSearch()
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchClubs()
}

onMounted(() => {
  fetchClubs()
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
  color: #3b82f6;
  cursor: pointer;
  text-decoration: none;
}

.table-link:hover {
  text-decoration: underline;
}

.pagination-container {
  padding: 20px;
  display: flex;
  justify-content: center;
}

.club-info-table {
    border: 1px solid #e4e7ed;
    border-radius: 4px;
}

.info-row {
    display: flex;
    border-bottom: 1px solid #e4e7ed;
}

.info-row:last-child {
    border-bottom: none;
}

.info-label {
    width: 140px;
    background-color: #f5f7fa;
    padding: 12px;
    font-weight: bold;
    color: #606266;
    border-right: 1px solid #e4e7ed;
    display: flex;
    align-items: center;
}

.info-value {
    flex: 1;
    padding: 12px;
    color: #303133;
    line-height: 1.5;
}
</style>