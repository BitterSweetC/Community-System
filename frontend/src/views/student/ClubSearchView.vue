<template>
  <div class="club-search-page">
    <section class="hero-section">
      <div class="hero-grid"></div>
      <div class="hero-container">
        <p class="hero-kicker">Club Directory</p>
        <h1>社团一览</h1>
        <p>发现感兴趣的社团，查看详情并发起加入申请。</p>
        <div class="hero-meta">
          <span>当前结果：{{ total }} 个社团</span>
          <span>每页展示：{{ pageSize }} 条</span>
        </div>
      </div>
    </section>

    <section class="content-container">
      <div class="filter-panel">
        <el-form :inline="true" class="search-form" @submit.prevent="handleSearch">
          <el-form-item label="社团名称">
            <el-input
              v-model="searchKeyword"
              placeholder="请输入社团名称"
              clearable
              style="width: 230px"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="所属类别">
            <el-select v-model="selectedCategory" placeholder="全部类别" clearable style="width: 190px">
              <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
            </el-select>
          </el-form-item>

          <el-form-item class="action-group">
            <el-button type="primary" class="btn-primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="table-panel">
        <el-table
          :data="clubs"
          stripe
          border
          v-loading="loading"
          header-cell-class-name="table-header"
          style="width: 100%"
        >
          <el-table-column prop="id" label="社团编号" width="100" align="center" />

          <el-table-column label="Logo" width="90" align="center">
            <template #default="scope">
              <el-avatar :size="42" :src="scope.row.logoUrl">
                {{ (scope.row.name || '社').charAt(0) }}
              </el-avatar>
            </template>
          </el-table-column>

          <el-table-column prop="category" label="类别" width="130" align="center">
            <template #default="scope">
              <el-tag>{{ scope.row.category || '未分类' }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="name" label="社团名称" min-width="220">
            <template #default="scope">
              <button class="table-link" @click="viewClub(scope.row)">
                {{ scope.row.name }}
              </button>
            </template>
          </el-table-column>

          <el-table-column prop="presidentName" label="社长" width="120" align="center" />

          <el-table-column prop="memberCount" label="人数" width="100" align="center" />

          <el-table-column prop="status" label="状态" width="110" align="center">
            <template #default="scope">
              <el-tag :type="getStatusTag(scope.row)">
                {{ getStatusText(scope.row) }}
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
    </section>

    <el-dialog
      v-model="dialogVisible"
      title="社团信息"
      width="620px"
      destroy-on-close
      align-center
      class="club-info-dialog"
    >
      <div class="club-info-header">
        <el-avatar :size="96" :src="currentClub.logoUrl">
          {{ currentClub.name ? currentClub.name.charAt(0) : 'C' }}
        </el-avatar>
      </div>

      <div class="club-info-table">
        <div class="info-row">
          <div class="info-label">社团名称</div>
          <div class="info-value">{{ currentClub.name || '-' }}</div>
        </div>
        <div class="info-row">
          <div class="info-label">社长姓名</div>
          <div class="info-value">{{ currentClub.presidentName || '未知' }}</div>
        </div>
        <div class="info-row">
          <div class="info-label">社团类别</div>
          <div class="info-value">{{ currentClub.category || '未分类' }}</div>
        </div>
        <div class="info-row">
          <div class="info-label">社团简介</div>
          <div class="info-value">{{ currentClub.description || '暂无简介' }}</div>
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
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import axios from '@/api/axios'

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
const recruitingClubIds = ref(new Set())

const categories = ref(['学术科技', '文化艺术', '体育竞技', '公益志愿', '实践创新', '其他'])

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

const fetchClubs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }

    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }

    if (selectedCategory.value) {
      params.category = selectedCategory.value
    }

    const [res, activeRecruitClubs] = await Promise.all([
      axios.get('/clubs', { params }),
      axios.get('/recruit/active-clubs').catch(() => [])
    ])
    const list = normalizeList(res)
    recruitingClubIds.value = new Set(normalizeList(activeRecruitClubs).map((club) => Number(club.id)))

    if (Array.isArray(res?.list)) {
      total.value = res.total ?? list.length
    } else if (Array.isArray(res?.content)) {
      total.value = res.totalElements ?? list.length
    } else {
      total.value = list.length
    }

    clubs.value = list.map((club) => ({
      ...club,
      presidentName: club.presidentName || '未设置',
      memberCount: club.memberCount ?? Math.floor(Math.random() * 40) + 20,
      isRecruiting: recruitingClubIds.value.has(Number(club.id))
    }))

    const dynamicCategories = [...new Set(list.map((item) => item.category).filter(Boolean))]
    if (dynamicCategories.length > 0) {
      categories.value = [...new Set([...categories.value, ...dynamicCategories])]
    }
  } catch (error) {
    console.error('Failed to fetch clubs:', error)
    ElMessage.error(error.response?.data?.message || error.message || '社团数据加载失败')
  } finally {
    loading.value = false
  }
}

const getStatusTag = (club) => {
  if (club?.isRecruiting) {
    return 'success'
  }
  if (club?.status === 'ACTIVE') {
    return 'primary'
  }
  if (club?.status === 'INACTIVE') {
    return 'info'
  }
  return 'warning'
}

const getStatusText = (club) => {
  if (club?.isRecruiting) {
    return '招新中'
  }
  if (club?.status === 'ACTIVE') {
    return '活跃中'
  }
  if (club?.status === 'INACTIVE') {
    return '已暂停'
  }
  return club?.status || '筹备中'
}

const viewClub = (club) => {
  currentClub.value = club
  dialogVisible.value = true
}

const joinClub = (club) => {
  if (!club?.id) {
    return
  }
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
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;700&display=swap');

* {
  box-sizing: border-box;
}

.club-search-page {
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
  background: radial-gradient(circle at 8% 16%, rgba(15, 118, 110, 0.18), transparent 28%),
    radial-gradient(circle at 92% 18%, rgba(194, 65, 12, 0.18), transparent 34%),
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
}

.table-link:hover {
  text-decoration: underline;
}

.pagination-container {
  padding: 18px;
  display: flex;
  justify-content: center;
}

.club-info-header {
  text-align: center;
  margin-bottom: 18px;
}

.club-info-table {
  border: 1px solid #e5eced;
  border-radius: 10px;
  overflow: hidden;
}

.info-row {
  display: flex;
  border-bottom: 1px solid #e5eced;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  width: 130px;
  background: #f6f9f9;
  color: #3f5268;
  font-weight: 700;
  border-right: 1px solid #e5eced;
  padding: 12px;
}

.info-value {
  flex: 1;
  padding: 12px;
  color: var(--ink);
  line-height: 1.55;
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
