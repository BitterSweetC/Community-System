<template>
  <div class="admin-dashboard">
    <h2>管理员仪表盘</h2>
    <p class="welcome-text">欢迎回来，{{ authStore.user?.username }}</p>

    <div class="dashboard-grid">
      <!-- Quick Actions -->
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

      <!-- System Stats (Admin Only) -->
      <div v-if="isAdmin && systemStats" class="stats-section">
        <h3>系统数据看板</h3>
        
        <!-- Stats Cards -->
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

        <!-- Charts -->
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

      <!-- If user is a Club Admin, show their clubs -->
      <div v-if="myClubs.length > 0" class="my-clubs-section">
        <h3>我管理的社团</h3>
        <el-row :gutter="20">
          <el-col :span="12" v-for="club in myClubs" :key="club.id">
            <el-card class="club-dashboard-card">
              <template #header>
                <div class="card-header">
                  <span>{{ club.name }}</span>
                  <el-tag size="small">{{ club.status }}</el-tag>
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
import { ref, onMounted, computed, nextTick } from 'vue'
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
    return roles.some(r => (typeof r === 'string' ? r : r.code) === 'ADMIN')
})

const loadSystemStats = async () => {
  try {
    const res = await axios.get('/stats/system')
    systemStats.value = res
    await nextTick()
    initCharts()
  } catch (error) {
    console.error('Failed to load system stats', error)
  }
}

const initCharts = () => {
  if (!systemStats.value) return

  // User Growth Chart
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

  // Club Status Chart
  if (clubStatusChartRef.value) {
    if (clubStatusChart) clubStatusChart.dispose()
    clubStatusChart = echarts.init(clubStatusChartRef.value)
    
    const statusData = Object.entries(systemStats.value.clubStatusDistribution || {}).map(([key, value]) => ({
      name: key,
      value: value
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

onMounted(async () => {
  try {
    const res = await axios.get('/clubs/my')
    if (res.list) myClubs.value = res.list
    else if (Array.isArray(res)) myClubs.value = res
    else myClubs.value = res.content || []
  } catch (error) {
    console.error('Failed to load my clubs', error)
  }

  if (isAdmin.value) {
    loadSystemStats()
    window.addEventListener('resize', handleResize)
  }
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}
.welcome-text {
  margin-bottom: 30px;
  color: #666;
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
  transition: transform 0.2s;
  width: 300px;
}
.dashboard-card:hover {
  transform: translateY(-5px);
}
.my-clubs-section {
  margin-top: 20px;
}
.club-dashboard-card {
  margin-bottom: 20px;
}
.club-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.stats-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}
.stats-cards {
  margin-bottom: 30px;
}
.stats-value {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  color: #303133;
}
.text-primary {
  color: var(--el-color-primary);
}
.text-success {
  color: var(--el-color-success);
}
.charts-row {
  margin-bottom: 20px;
}
</style>
