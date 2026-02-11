<template>
  <div class="academic-container">
    <div class="page-header">
      <div class="breadcrumb">
        <router-link to="/home" class="breadcrumb-link">首页</router-link>
        <span class="separator">/</span>
        <span>公告信息</span>
      </div>
      <h2>公告信息</h2>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" class="search-form">
        <el-form-item label="标题搜索">
          <el-input v-model="searchTitle" placeholder="请输入公告标题">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="data-table-wrapper">
      <el-table 
        :data="notices" 
        style="width: 100%" 
        stripe 
        border
        header-cell-class-name="table-header"
        v-loading="loading"
      >
        <el-table-column prop="publishedAt" label="发布日期" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.publishedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="scope" label="分类" width="120">
            <template #default="scope">
                <el-tag size="small">{{ scope.row.scope === 'PUBLIC' ? '全校' : '社团' }}</el-tag>
            </template>
        </el-table-column>
        <el-table-column prop="title" label="公告标题" min-width="300">
          <template #default="scope">
            <a class="table-link" @click="viewNotice(scope.row)">{{ scope.row.title }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布单位" width="150" />
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

    <!-- Notice Detail Dialog -->
    <el-dialog v-model="dialogVisible" :title="currentNotice.title" width="60%">
      <div class="notice-detail">
        <div class="detail-meta">
          <span>发布时间：{{ formatDate(currentNotice.publishedAt) }}</span>
          <span>发布人：{{ currentNotice.publisherName || '管理员' }}</span>
        </div>
        <div class="detail-content">
          {{ currentNotice.content }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/api/axios'

const notices = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const searchTitle = ref('')
const dateRange = ref([])

const dialogVisible = ref(false)
const currentNotice = ref({})

const fetchNotices = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    // Simulate filtering in frontend or backend if supported
    const res = await axios.get('/notices', { params })
    let data = []
    if (res.list) {
      data = res.list
      total.value = res.total
    } else if (res.content) {
      data = res.content
      total.value = res.totalElements
    } else {
      data = res
      total.value = res.length
    }
    
    // Frontend filter simulation (since backend might strictly filter by keyword only)
    if (searchTitle.value) {
        data = data.filter(n => n.title.includes(searchTitle.value))
    }
    
    // Mock publisher name since it's an ID usually
    data = data.map(n => ({...n, publisherName: n.clubId ? '社团管理员' : '教务处'}))
    
    notices.value = data
  } catch (error) {
    console.error('Failed to fetch notices:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchNotices()
}

const resetSearch = () => {
  searchTitle.value = ''
  dateRange.value = []
  handleSearch()
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchNotices()
}

const viewNotice = (notice) => {
  currentNotice.value = notice
  dialogVisible.value = true
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit'
  })
}

onMounted(() => {
  fetchNotices()
})
</script>

<style scoped>
.academic-container {
  padding: 20px;
  background-color: #fff;
  min-height: 80vh;
}

.page-header {
  border-bottom: 2px solid #1f2937; /* Dark header line */
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
  cursor: pointer;
  text-decoration: none;
}

.table-link:hover {
  color: #409EFF;
  text-decoration: underline;
}

.pagination-container {
  padding: 20px;
  display: flex;
  justify-content: center;
}

.notice-detail {
  padding: 10px;
}

.detail-meta {
  color: #999;
  font-size: 12px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
  margin-bottom: 20px;
  display: flex;
  gap: 20px;
}

.detail-content {
  line-height: 1.8;
  font-size: 16px;
  white-space: pre-wrap;
}
</style>