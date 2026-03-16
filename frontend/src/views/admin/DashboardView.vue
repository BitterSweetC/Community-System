<template>
  <div class="admin-dashboard">
    <h2>管理员仪表盘</h2>
    <p class="welcome-text">欢迎回来，{{ authStore.user?.username }}</p>

    <div class="dashboard-grid">
      <div class="quick-actions">
        <el-card v-if="isAdmin" class="dashboard-card" @click="$router.push('/admin/clubs')">
          <template #header>
            <div class="card-header">
              <span>社团管理</span>
            </div>
          </template>
          <div class="card-content">
            管理社团申请、社团信息及状态。
          </div>
        </el-card>

        <el-card v-if="isAdmin" class="dashboard-card" @click="$router.push('/admin/notices')">
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
                <el-button size="small" @click="$router.push(`/admin/recruit/${club.id}`)">招新管理</el-button>
                <el-button size="small" @click="$router.push(`/admin/notices/${club.id}`)">发布公告</el-button>
                <el-button size="small" @click="$router.push(`/admin/activities/${club.id}`)">活动管理</el-button>
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
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import * as echarts from 'echarts'

const authStore = useAuthStore()
const myClubs = ref([])
const systemStats = ref(null)

const userGrowthChartRef = ref(null)
const clubStatusChartRef = ref(null)
let userGrowthChart = null
let clubStatusChart = null

const isAdmin = computed(() => {
  const roles = authStore.user?.roles || []
  return roles.some((r) => (typeof r === 'string' ? r : r.code) === 'ADMIN')
})

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
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}

.admin-dashboard h2 {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-text {
  margin-bottom: 30px;
  color: #606266;
  font-size: 16px;
}

.dashboard-grid {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.quick-actions {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.dashboard-card {
  cursor: pointer;
  transition: all 0.3s;
  width: 300px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.dashboard-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.25);
}

:deep(.dashboard-card .el-card__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.card-content {
  color: #606266;
  line-height: 1.6;
}

.my-clubs-section {
  margin-top: 20px;
}

.my-clubs-section h3 {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.club-dashboard-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  transition: all 0.3s;
}

.club-dashboard-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
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
  margin-top: 20px;
  padding-top: 20px;
}

.stats-section h3 {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}

.stats-cards {
  margin-bottom: 30px;
}

:deep(.stats-cards .el-card) {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  transition: all 0.3s;
  overflow: hidden;
}

:deep(.stats-cards .el-card:hover) {
  transform: translateY(-4px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.12);
}

:deep(.stats-cards .el-card__header) {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf6 100%);
  font-weight: 600;
  color: #606266;
  border-bottom: 2px solid #667eea;
}

.stats-value {
  font-size: 32px;
  font-weight: 700;
  text-align: center;
  color: #303133;
  padding: 20px 0;
}

.text-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

:deep(.charts-row .el-card__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
}
</style>


