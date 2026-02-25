<template>
  <div class="academic-container">
    <div class="page-hero">
      <div class="page-hero-inner">
        <div class="page-hero-title">
          <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8"><path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
          <h1>社团一览</h1>
        </div>
        <p class="page-hero-sub">发现感兴趣的社团，开启你的校园新旅程</p>
      </div>
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
        <el-table-column label="Logo" width="80" align="center">
          <template #default="scope">
            <el-avatar :size="40" :src="scope.row.logoUrl">
              {{ scope.row.name.charAt(0) }}
            </el-avatar>
          </template>
        </el-table-column>
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
        <div class="club-info-header" style="text-align: center; margin-bottom: 20px;">
            <el-avatar :size="100" :src="currentClub.logoUrl">
                {{ currentClub.name ? currentClub.name.charAt(0) : 'C' }}
            </el-avatar>
        </div>
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
  background-color: #f3f4f6;
  min-height: 80vh;
}

.page-hero {
  background: linear-gradient(135deg, #1e3a5f 0%, #3b82f6 100%);
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