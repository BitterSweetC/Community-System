<template>
  <div class="notice-page">
    <section class="hero-section">
      <div class="hero-grid"></div>
      <div class="hero-container">
        <p class="hero-kicker">Notice Center</p>
        <h1>公告信息</h1>
        <p>查看最新校园公告，掌握第一手资讯与通知。</p>
        <div class="hero-meta">
          <span>当前结果：{{ total }} 条公告</span>
          <span>每页展示：{{ pageSize }} 条</span>
        </div>
      </div>
    </section>

    <section class="content-container">
      <div class="filter-panel">
        <el-form :inline="true" class="search-form" @submit.prevent="handleSearch">
          <el-form-item label="标题搜索">
            <el-input
              v-model="searchTitle"
              placeholder="请输入公告标题"
              clearable
              style="width: 260px"
              @keyup.enter="handleSearch"
            >
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
              value-format="YYYY-MM-DD"
            />
          </el-form-item>

          <el-form-item class="action-group">
            <el-button type="primary" class="btn-primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="table-panel">
        <el-table
          :data="notices"
          stripe
          border
          header-cell-class-name="table-header"
          v-loading="loading"
          style="width: 100%"
        >
          <el-table-column prop="publishedAt" label="发布日期" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.publishedAt) }}
            </template>
          </el-table-column>

          <el-table-column prop="scope" label="分类" width="120" align="center">
            <template #default="scope">
              <el-tag size="small">{{ scope.row.scope === 'PUBLIC' ? '全校' : '社团' }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="title" label="公告标题" min-width="360">
            <template #default="scope">
              <button class="table-link" @click="viewNotice(scope.row)">
                {{ scope.row.title }}
              </button>
            </template>
          </el-table-column>

          <el-table-column prop="publisherName" label="发布单位" width="180" />
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
    </section>

    <el-dialog v-model="dialogVisible" :title="currentNotice.title || '公告详情'" width="62%">
      <div class="notice-detail">
        <div class="detail-meta">
          <span>发布时间：{{ formatDate(currentNotice.publishedAt) || '-' }}</span>
          <span>发布单位：{{ currentNotice.publisherName || '管理员' }}</span>
        </div>
        <div class="detail-content">
          {{ currentNotice.content || '暂无公告内容' }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
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

const normalizeList = (payload) => {
  if (!payload) {
    return []
  }

  if (Array.isArray(payload)) {
    return payload
  }

  if (Array.isArray(payload.list)) {
    return payload.list
  }

  if (Array.isArray(payload.content)) {
    return payload.content
  }

  return []
}

const fetchNotices = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }

    if (searchTitle.value.trim()) {
      params.title = searchTitle.value.trim()
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const res = await axios.get('/notices', { params })
    const data = normalizeList(res)

    if (Array.isArray(res?.list)) {
      total.value = res.total ?? data.length
    } else if (Array.isArray(res?.content)) {
      total.value = res.totalElements ?? data.length
    } else {
      total.value = data.length
    }

    notices.value = data.map((item) => ({
      ...item,
      publisherName: item.publisherName || (item.clubId ? '社团管理员' : '教务处')
    }))
  } catch (error) {
    console.error('Failed to fetch notices:', error)
    ElMessage.error(error.response?.data?.message || error.message || '公告加载失败')
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
  if (!dateStr) {
    return ''
  }

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
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;700&display=swap');

* {
  box-sizing: border-box;
}

.notice-page {
  --ink: #0f1c2a;
  --muted: #55667a;
  --surface: rgba(255, 255, 255, 0.88);
  --border: rgba(15, 28, 42, 0.12);

  min-height: 100vh;
  padding-bottom: 30px;
  background: linear-gradient(180deg, #f7f1e7 0%, #efe8dc 44%, #e8eeea 100%);
  color: var(--ink);
  font-family: 'Outfit', sans-serif;
}

.hero-section {
  position: relative;
  padding: 104px 24px 34px;
  overflow: hidden;
}

.hero-grid {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 8% 14%, rgba(15, 118, 110, 0.18), transparent 28%),
    radial-gradient(circle at 92% 20%, rgba(194, 65, 12, 0.18), transparent 34%),
    linear-gradient(165deg, #f8f3e8 0%, #eee7d8 60%, #e4ece8 100%);
}

.hero-grid::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(15, 28, 42, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 28, 42, 0.03) 1px, transparent 1px);
  background-size: 34px 34px;
}

.hero-container {
  position: relative;
  z-index: 1;
  max-width: 1240px;
  margin: 0 auto;
}

.hero-kicker {
  margin: 0 0 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-weight: 700;
  color: var(--muted);
  font-size: 0.8rem;
}

.hero-container h1 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2rem, 4vw, 3rem);
}

.hero-container p {
  margin: 10px 0 0;
  color: var(--muted);
  line-height: 1.7;
}

.hero-meta {
  margin-top: 14px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-meta span {
  font-size: 0.86rem;
  border-radius: 999px;
  padding: 6px 12px;
  color: #355069;
  border: 1px solid rgba(15, 28, 42, 0.12);
  background: rgba(255, 255, 255, 0.66);
}

.content-container {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px;
}

.filter-panel {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--surface);
  backdrop-filter: blur(8px);
  box-shadow: 0 12px 24px rgba(15, 28, 42, 0.08);
  padding: 14px 16px 0;
  margin-bottom: 16px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
}

.action-group {
  margin-left: auto;
}

.btn-primary {
  border: none;
  background: linear-gradient(135deg, #c2410c 0%, #9a3412 100%);
}

.table-panel {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--surface);
  backdrop-filter: blur(8px);
  box-shadow: 0 12px 24px rgba(15, 28, 42, 0.08);
  overflow: hidden;
}

:deep(.table-header) {
  background: #f4f7f7 !important;
  color: var(--ink);
  font-weight: 700;
}

.table-link {
  border: 0;
  background: transparent;
  color: #1f6fa8;
  font-family: inherit;
  font-size: 0.95rem;
  cursor: pointer;
  padding: 0;
  text-align: left;
}

.table-link:hover {
  text-decoration: underline;
}

.pagination-container {
  padding: 18px;
  display: flex;
  justify-content: center;
}

.notice-detail {
  padding: 6px;
}

.detail-meta {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  color: #6f7f91;
  font-size: 0.85rem;
  border-bottom: 1px solid #e7ecee;
  padding-bottom: 10px;
  margin-bottom: 14px;
}

.detail-content {
  line-height: 1.8;
  font-size: 0.98rem;
  white-space: pre-wrap;
  color: var(--ink);
}

@media (max-width: 900px) {
  .hero-section {
    padding-top: 90px;
  }

  .content-container {
    padding: 0 14px;
  }

  .action-group {
    margin-left: 0;
  }

  :deep(.el-form-item) {
    margin-right: 8px;
  }
}
</style>
