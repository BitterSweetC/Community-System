<template>
  <div class="admin-dashboard">
    <h2>管理员仪表盘</h2>
    <p class="welcome-text">欢迎回来，{{ authStore.user?.username }}</p>

    <div class="dashboard-grid">
      <div class="quick-actions">
        <el-card v-if="isAdmin" class="dashboard-card" @click="handleNavigate('/admin/clubs')">
          <template #header>
            <div class="card-header">
              <span>社团管理</span>
            </div>
          </template>
          <div class="card-content">
            管理社团申请、社团信息及状态。
          </div>
        </el-card>

        <el-card v-if="isAdmin" class="dashboard-card" @click="handleNavigate('/admin/notices')">
          <template #header>
            <div class="card-header">
              <span>公告管理</span>
            </div>
          </template>
          <div class="card-content">
            发布系统公告、管理所有社团公告。
          </div>
        </el-card>
      </div>

      <el-card v-if="todoOverview" class="todo-overview-card">
        <template #header>
          <div class="todo-head">
            <div>
              <h3>统一待办中心</h3>
              <p class="todo-subtext">管理员和社团管理员待办已经聚合，可直接跳到对应处理页。</p>
            </div>
            <el-button type="primary" plain @click="handleNavigate('/admin/todos')">进入待办</el-button>
          </div>
        </template>

        <el-row :gutter="16" class="todo-summary-row">
          <el-col :span="8">
            <div class="todo-summary-box">
              <span class="todo-summary-label">总待办</span>
              <strong class="todo-summary-value">{{ todoOverview.totalPending || 0 }}</strong>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="todo-summary-box">
              <span class="todo-summary-label">待处理模块</span>
              <strong class="todo-summary-value">{{ todoOverview.nonEmptySectionCount || 0 }}</strong>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="todo-summary-box">
              <span class="todo-summary-label">可见分组</span>
              <strong class="todo-summary-value">{{ todoOverview.sections?.length || 0 }}</strong>
            </div>
          </el-col>
        </el-row>

        <div v-if="todoSections.length" class="todo-preview-list">
          <button
            v-for="section in todoSections"
            :key="section.key"
            type="button"
            class="todo-preview-item"
            @click="handleNavigate(section.actionPath || '/admin/todos')"
          >
            <span class="todo-preview-title">{{ section.title }}</span>
            <span class="todo-preview-meta">
              <el-tag size="small" :type="toTagType(section.tone)" effect="plain">{{ section.pendingCount }}</el-tag>
              <span>{{ section.description }}</span>
            </span>
          </button>
        </div>
      </el-card>

      <div v-if="isAdmin && systemStats" class="stats-section">
        <h3>系统数据看板</h3>

        <el-row :gutter="20" class="stats-cards">
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>用户总数</template>
              <div class="stats-value">{{ systemStats.totalUsers }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>社团总数</template>
              <div class="stats-value">{{ systemStats.totalClubs }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>活跃社团</template>
              <div class="stats-value text-success">{{ systemStats.activeClubs }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>活动总数</template>
              <div class="stats-value text-primary">{{ systemStats.totalActivities }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="charts-row">
          <el-col :span="12">
            <el-card>
              <template #header>近7天用户增长</template>
              <div ref="userGrowthChartRef" style="height: 300px;"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header>社团状态分布</template>
              <div ref="clubStatusChartRef" style="height: 300px;"></div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <div v-if="myClubs.length > 0" class="my-clubs-section">
        <h3>我管理的社团</h3>
        <el-row :gutter="20">
          <el-col :span="12" v-for="club in myClubs" :key="club.id">
            <el-card class="club-dashboard-card">
              <template #header>
                <div class="card-header">
                  <span>{{ club.name }}</span>
                  <el-tag size="small">{{ getStatusText(club.status) }}</el-tag>
                </div>
              </template>
              <div class="club-actions">
                <el-button size="small" @click="handleNavigate(`/admin/recruit/${club.id}`)">招新管理</el-button>
                <el-button size="small" @click="handleNavigate(`/admin/notices/${club.id}`)">发布公告</el-button>
                <el-button size="small" @click="handleNavigate(`/admin/activities/${club.id}`)">活动管理</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import { getDashboardTodos } from '@/api/dashboard'
import * as echarts from 'echarts'

const router = useRouter()
const authStore = useAuthStore()
const myClubs = ref([])
const systemStats = ref(null)
const todoOverview = ref(null)

const handleNavigate = (path) => {
  console.log('导航到:', path)
  router.push(path).catch(err => {
    console.error('路由跳转失败:', err)
  })
}

const userGrowthChartRef = ref(null)
const clubStatusChartRef = ref(null)
let userGrowthChart = null
let clubStatusChart = null

const isAdmin = computed(() => {
  const roles = authStore.user?.roles || []
  return roles.some((r) => (typeof r === 'string' ? r : r.code) === 'ADMIN')
})

const todoSections = computed(() =>
  (todoOverview.value?.sections || []).filter((section) => section.pendingCount > 0).slice(0, 4)
)

const loadSystemStats = async () => {
  try {
    const res = await axios.get('/stats/system')
    systemStats.value = res
    await nextTick()
    initCharts()
  } catch (error) {
    console.error('加载系统统计失败', error)
  }
}

const loadTodos = async () => {
  try {
    todoOverview.value = await getDashboardTodos()
  } catch (error) {
    console.error('加载统一待办失败', error)
  }
}

const initCharts = () => {
  if (!systemStats.value) return

  if (userGrowthChartRef.value) {
    if (userGrowthChart) userGrowthChart.dispose()
    userGrowthChart = echarts.init(userGrowthChartRef.value)

    const dates = Object.keys(systemStats.value.userGrowth || {})
    const counts = Object.values(systemStats.value.userGrowth || {})

    userGrowthChart.setOption({
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: dates
      },
      yAxis: {
        type: 'value',
        minInterval: 1
      },
      series: [
        {
          data: counts,
          type: 'line',
          smooth: true,
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
            ])
          },
          itemStyle: {
            color: '#409EFF'
          }
        }
      ]
    })
  }

  if (clubStatusChartRef.value) {
    if (clubStatusChart) clubStatusChart.dispose()
    clubStatusChart = echarts.init(clubStatusChartRef.value)

    const statusData = Object.entries(systemStats.value.clubStatusDistribution || {}).map(([key, value]) => ({
      name: getStatusText(key),
      value
    }))

    clubStatusChart.setOption({
      tooltip: {
        trigger: 'item'
      },
      legend: {
        top: '5%',
        left: 'center'
      },
      series: [
        {
          name: '社团状态',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 20,
              fontWeight: 'bold'
            }
          },
          data: statusData
        }
      ]
    })
  }
}

const handleResize = () => {
  userGrowthChart?.resize()
  clubStatusChart?.resize()
}

const getStatusText = (status) => {
  switch (status) {
    case 'ACTIVE':
      return '活跃'
    case 'PENDING':
      return '待审批'
    case 'DISSOLVING':
      return '解散中'
    case 'DISSOLVED':
      return '已解散'
    case 'INACTIVE':
      return '未激活'
    case 'REJECTED':
      return '已驳回'
    default:
      return status || '-'
  }
}

const toTagType = (tone) => {
  switch (tone) {
    case 'danger':
      return 'danger'
    case 'warning':
      return 'warning'
    case 'success':
      return 'success'
    case 'primary':
      return 'primary'
    default:
      return 'info'
  }
}

onMounted(async () => {
  try {
    const res = await axios.get('/clubs/my')
    if (res.list) myClubs.value = res.list
    else if (Array.isArray(res)) myClubs.value = res
    else myClubs.value = res.content || []
  } catch (error) {
    console.error('加载我的社团失败', error)
  }

  if (isAdmin.value) {
    loadTodos()
    loadSystemStats()
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  userGrowthChart?.dispose()
  clubStatusChart?.dispose()
})
</script>
<style scoped>
.admin-dashboard {
  padding: 0;
  background: transparent;
  min-height: auto;
}

.admin-dashboard h2 {
  font-size: 28px;
  font-weight: 700;
  color: #0f2e4a;
  margin-bottom: 8px;
}

.welcome-text {
  margin-bottom: 22px;
  color: var(--panel-muted, #5f738b);
  font-size: 16px;
}

.dashboard-grid {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.todo-overview-card {
  border-radius: 16px;
  border: 1px solid var(--panel-border, rgba(14, 55, 94, 0.14));
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--panel-shadow, 0 14px 34px rgba(17, 46, 77, 0.1));
}

.todo-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.todo-head h3 {
  margin: 0;
  font-size: 20px;
  color: #173551;
}

.todo-subtext {
  margin: 6px 0 0;
  color: #60748c;
}

.todo-summary-row {
  margin-bottom: 18px;
}

.todo-summary-box {
  min-height: 94px;
  border-radius: 12px;
  padding: 18px 16px;
  background: linear-gradient(135deg, rgba(23, 93, 158, 0.12), rgba(31, 118, 182, 0.04));
  border: 1px solid rgba(23, 93, 158, 0.08);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.todo-summary-label {
  color: #60748c;
}

.todo-summary-value {
  font-size: 30px;
  color: #173551;
}

.todo-preview-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-preview-item {
  width: 100%;
  border: 1px solid rgba(17, 64, 106, 0.12);
  border-radius: 12px;
  background: #fff;
  padding: 14px 16px;
  text-align: left;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.todo-preview-item:hover {
  transform: translateY(-2px);
  border-color: rgba(27, 104, 173, 0.24);
  box-shadow: 0 8px 18px rgba(17, 45, 73, 0.08);
}

.todo-preview-title {
  font-weight: 600;
  color: #173551;
}

.todo-preview-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b8197;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.dashboard-card {
  cursor: pointer;
  transition: all 0.3s;
  width: auto;
  min-height: 168px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--panel-border, rgba(14, 55, 94, 0.14));
  background: rgba(255, 255, 255, 0.84);
  box-shadow: var(--panel-shadow, 0 14px 34px rgba(17, 46, 77, 0.1));
}

.dashboard-card:hover {
  transform: translateY(-4px);
  border-color: rgba(23, 93, 158, 0.22);
  box-shadow: 0 16px 30px rgba(17, 46, 77, 0.14);
}

:deep(.dashboard-card .el-card__header) {
  background: linear-gradient(180deg, #f7fbff 0%, #eef5fd 100%);
  color: #123b61;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid rgba(17, 64, 106, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-content {
  color: #60748c;
  line-height: 1.7;
}

.my-clubs-section {
  margin-top: 4px;
}

.my-clubs-section h3 {
  font-size: 20px;
  font-weight: 600;
  color: #173551;
  margin-bottom: 16px;
}

.club-dashboard-card {
  margin-bottom: 20px;
  border-radius: 14px;
  border: 1px solid var(--panel-border, rgba(14, 55, 94, 0.14));
  box-shadow: var(--panel-shadow, 0 14px 34px rgba(17, 46, 77, 0.1));
  transition: all 0.3s;
}

.club-dashboard-card:hover {
  box-shadow: 0 12px 24px rgba(17, 46, 77, 0.12);
}

.club-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

:deep(.club-actions .el-button) {
  border-radius: 6px;
  font-weight: 500;
}

.stats-section {
  margin-top: 4px;
  padding-top: 0;
}

.stats-section h3 {
  font-size: 20px;
  font-weight: 600;
  color: #173551;
  margin-bottom: 20px;
}

.stats-cards {
  margin-bottom: 30px;
}

:deep(.stats-cards .el-card) {
  border-radius: 14px;
  border: 1px solid var(--panel-border, rgba(14, 55, 94, 0.14));
  box-shadow: var(--panel-shadow, 0 14px 34px rgba(17, 46, 77, 0.1));
  transition: all 0.3s;
  overflow: hidden;
}

:deep(.stats-cards .el-card:hover) {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(17, 46, 77, 0.12);
}

:deep(.stats-cards .el-card__header) {
  background: linear-gradient(180deg, #f7fbff 0%, #eef5fd 100%);
  font-weight: 600;
  color: #60748c;
  border-bottom: 1px solid rgba(17, 64, 106, 0.1);
}

.stats-value {
  font-size: 32px;
  font-weight: 700;
  text-align: center;
  color: #173551;
  padding: 20px 0;
}

.text-primary {
  background: linear-gradient(135deg, #175d9e, #1f76b6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.text-success {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.charts-row {
  margin-bottom: 20px;
}

:deep(.charts-row .el-card) {
  border-radius: 14px;
  border: 1px solid var(--panel-border, rgba(14, 55, 94, 0.14));
  box-shadow: var(--panel-shadow, 0 14px 34px rgba(17, 46, 77, 0.1));
}

:deep(.charts-row .el-card__header) {
  background: linear-gradient(180deg, #f7fbff 0%, #eef5fd 100%);
  color: #123b61;
  font-weight: 600;
}
</style>

