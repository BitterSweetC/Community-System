<template>
  <div class="club-admin-dashboard">
    <h2>我的社团管理</h2>
    <p class="welcome-text">欢迎回来，{{ authStore.user?.username }}</p>

    <div class="dashboard-grid">
      <!-- My Clubs List -->
      <div v-if="myClubs.length > 0" class="my-clubs-section">
        <h3>管理列表</h3>
        <el-row :gutter="20">
          <el-col :span="12" v-for="club in myClubs" :key="club.id">
            <el-card 
              class="club-dashboard-card" 
              :class="{ 'active-card': selectedClubId === club.id }"
              @click="selectClub(club.id)"
            >
              <template #header>
                <div class="card-header">
                  <span>{{ club.name }}</span>
                  <el-tag size="small">{{ club.status }}</el-tag>
                </div>
              </template>
              <div class="club-actions">
                <el-button 
                  size="small" 
                  :disabled="isClubDisabled(club.status)"
                  :title="isClubDisabled(club.status) ? '社团处于非活跃状态，功能已冻结' : ''"
                  @click.stop="$router.push(`/clubadmin/recruit/${club.id}`)"
                >
                  招新管理
                </el-button>
                <el-button 
                  size="small" 
                  :disabled="isClubDisabled(club.status)"
                  :title="isClubDisabled(club.status) ? '社团处于非活跃状态，功能已冻结' : ''"
                  @click.stop="$router.push(`/clubadmin/members/${club.id}`)"
                >
                  成员管理
                </el-button>
                <el-button 
                  size="small" 
                  :disabled="isClubDisabled(club.status)"
                  :title="isClubDisabled(club.status) ? '社团处于非活跃状态，功能已冻结' : ''"
                  @click.stop="$router.push(`/clubadmin/notices/${club.id}`)"
                >
                  发布公告
                </el-button>
                <el-button 
                  size="small" 
                  :disabled="isClubDisabled(club.status)"
                  :title="isClubDisabled(club.status) ? '社团处于非活跃状态，功能已冻结' : ''"
                  @click.stop="$router.push(`/clubadmin/activities/${club.id}`)"
                >
                  活动管理
                </el-button>
                <el-button 
                  size="small" 
                  type="primary"
                  plain
                  :disabled="isClubDisabled(club.status)"
                  :title="isClubDisabled(club.status) ? '社团处于非活跃状态，功能已冻结' : ''"
                  @click.stop="$router.push(`/clubadmin/settings/${club.id}`)"
                >
                  修改信息
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- Statistics Section -->
        <div v-if="stats" class="stats-section">
          <h3>数据看板 - {{ getSelectedClubName() }}</h3>
          
          <!-- Stats Cards -->
          <el-row :gutter="20" class="stats-cards">
            <el-col :span="6">
              <el-card shadow="hover">
                <template #header>成员总数</template>
                <div class="stats-value">{{ stats.memberCount }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <template #header>活动总数</template>
                <div class="stats-value">{{ stats.activityCount }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <template #header>即将开始活动</template>
                <div class="stats-value text-primary">{{ stats.activityStats?.upcoming || 0 }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <template #header>已结束活动</template>
                <div class="stats-value text-info">{{ stats.activityStats?.past || 0 }}</div>
              </el-card>
            </el-col>
          </el-row>

          <!-- Charts -->
          <el-row :gutter="20" class="charts-row">
            <el-col :span="12">
              <el-card>
                <template #header>近7天新增成员</template>
                <div ref="trendChartRef" style="height: 300px;"></div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card>
                <template #header>成员角色分布</template>
                <div ref="roleChartRef" style="height: 300px;"></div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </div>
      
      <div v-else class="no-clubs">
        <el-empty description="暂无管理的社团" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import * as echarts from 'echarts'

const authStore = useAuthStore()
const myClubs = ref([])
const selectedClubId = ref(null)
const stats = ref(null)

const trendChartRef = ref(null)
const roleChartRef = ref(null)
let trendChart = null
let roleChart = null

const getSelectedClubName = () => {
  const club = myClubs.value.find(c => c.id === selectedClubId.value)
  return club ? club.name : ''
}

const isClubDisabled = (status) => {
  return ['DISSOLVED', 'DISSOLVING', 'PENDING', 'REJECTED'].includes(status)
}

const selectClub = async (clubId) => {
  selectedClubId.value = clubId
  await loadStats(clubId)
}

const loadStats = async (clubId) => {
  try {
    const res = await axios.get(`/stats/club/${clubId}`)
    stats.value = res
    await nextTick()
    initCharts()
  } catch (error) {
    console.error('Failed to load stats', error)
  }
}

const initCharts = () => {
  if (!stats.value) return

  // Trend Chart
  if (trendChartRef.value) {
    if (trendChart) trendChart.dispose()
    trendChart = echarts.init(trendChartRef.value)
    
    const dates = Object.keys(stats.value.recentJoins || {})
    const counts = Object.values(stats.value.recentJoins || {})
    
    trendChart.setOption({
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
          areaStyle: {}
        }
      ]
    })
  }

  // Role Chart
  if (roleChartRef.value) {
    if (roleChart) roleChart.dispose()
    roleChart = echarts.init(roleChartRef.value)
    
    const roleData = Object.entries(stats.value.roleDistribution || {}).map(([key, value]) => ({
      name: key,
      value: value
    }))
    
    roleChart.setOption({
      tooltip: {
        trigger: 'item'
      },
      legend: {
        top: '5%',
        left: 'center'
      },
      series: [
        {
          name: '角色分布',
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
          labelLine: {
            show: false
          },
          data: roleData
        }
      ]
    })
  }
}

onMounted(async () => {
  try {
    const res = await axios.get('/clubs/my')
    if (res.list) myClubs.value = res.list
    else if (Array.isArray(res)) myClubs.value = res
    else myClubs.value = res.content || []
    
    if (myClubs.value.length > 0) {
      selectClub(myClubs.value[0].id)
    }
  } catch (error) {
    console.error('Failed to load my clubs', error)
  }
  
  window.addEventListener('resize', handleResize)
})

const handleResize = () => {
  trendChart?.resize()
  roleChart?.resize()
}
</script>

<style scoped>
.club-admin-dashboard {
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
.club-dashboard-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}
.club-dashboard-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.active-card {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
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
.text-info {
  color: var(--el-color-info);
}
.charts-row {
  margin-bottom: 20px;
}
.no-clubs {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
