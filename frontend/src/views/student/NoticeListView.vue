<template>
  <div class="academic-container">
    <div class="page-hero">
      <div class="page-hero-inner">
        <div class="page-hero-title">
          <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8"><path stroke-linecap="round" stroke-linejoin="round" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"/></svg>
          <h1>公告信息</h1>
        </div>
        <p class="page-hero-sub">查看最新校园公告，掌握第一手资讯</p>
      </div>
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
import { useRouter } from 'vue-router'
import axios from '@/api/axios'

const router = useRouter()
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
  background-color: #f3f4f6;
  min-height: 80vh;
}

.page-hero {
  background: linear-gradient(135deg, #92400e 0%, #f59e0b 100%);
  padding: 100px 32px 40px;
  margin-bottom: 24px;
}

.page-hero-inner {
  max-width: 1200px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.3);
  color: white;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 16px;
}

.back-btn:hover {
  background: rgba(255,255,255,0.25);
}

.breadcrumb {
  font-size: 13px;
  color: rgba(255,255,255,0.65);
  margin-bottom: 12px;
}

.breadcrumb-link {
  color: rgba(255,255,255,0.75);
  text-decoration: none;
  transition: color 0.2s;
}

.breadcrumb-link:hover {
  color: white;
}

.separator {
  margin: 0 8px;
  color: rgba(255,255,255,0.4);
}

.page-hero-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 8px;
  color: white;
}

.page-hero-title h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.page-hero-title svg {
  opacity: 0.9;
  flex-shrink: 0;
}

.page-hero-sub {
  margin: 0;
  font-size: 14px;
  color: rgba(255,255,255,0.75);
}

.filter-bar {
  background-color: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  margin: 0 32px 20px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.search-form {
  margin-bottom: 0;
}

.data-table-wrapper {
  margin: 0 32px 32px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
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