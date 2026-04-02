<template>
  <div class="club-admin-dashboard">
    <div class="dashboard-head">
      <div>
        <h2>社团运营总览</h2>
        <p class="welcome-text">欢迎回来，{{ authStore.user?.username || '管理员' }}</p>
      </div>
      <el-tag class="head-pill" type="primary" effect="plain">社团数：{{ myClubs.length }}</el-tag>
    </div>

    <div class="dashboard-grid">
      <el-card v-if="todoOverview" class="todo-overview-card">
        <template #header>
          <div class="todo-head">
            <div>
              <h3 class="section-title">统一待办中心</h3>
              <p class="todo-subtext">招新审核、状态提醒和待跟进申请已经按当前账号聚合。</p>
            </div>
            <el-button type="primary" plain @click="handleNavigate('/clubadmin/todos')">进入待办</el-button>
          </div>
        </template>

        <el-row :gutter="12" class="todo-summary-row">
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
            @click="handleNavigate(section.actionPath || '/clubadmin/todos')"
          >
            <span class="todo-preview-title">{{ section.title }}</span>
            <span class="todo-preview-meta">
              <el-tag size="small" :type="toTagType(section.tone)" effect="plain">{{ section.pendingCount }}</el-tag>
              <span>{{ section.description }}</span>
            </span>
          </button>
        </div>
      </el-card>

      <div v-if="myClubs.length > 0" class="my-clubs-section">
        <h3 class="section-title">管理中的社团</h3>

        <el-row :gutter="16">
          <el-col :span="12" v-for="club in myClubs" :key="club.id">
            <el-card
              class="club-dashboard-card"
              :class="{ 'active-card': selectedClubId === club.id }"
              @click="selectClub(club.id)"
            >
              <template #header>
                <div class="card-header">
                  <span>{{ club.name }}</span>
                  <el-tag size="small">{{ getClubStatusText(club.status) }}</el-tag>
                </div>
              </template>

              <div class="club-actions">
                <el-button
                  size="small"
                  type="primary"
                  plain
                  :disabled="isClubDisabled(club.status)"
                  :title="isClubDisabled(club.status) ? '该社团当前非活跃状态，部分管理功能不可用。' : ''"
                  @click.stop="handleNavigate(`/clubadmin/settings/${club.id}`)"
                >
                  修改资料
                </el-button>

                <el-button
                  v-if="club.status === 'DISSOLVING'"
                  size="small"
                  type="warning"
                  @click.stop="cancelDissolve(club.id)"
                >
                  撤销解散
                </el-button>

                <el-button
                  v-else
                  size="small"
                  type="danger"
                  :disabled="isClubDisabled(club.status)"
                  @click.stop="requestDissolve(club)"
                >
                  申请解散
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <div v-if="stats" class="stats-section">
          <h3 class="section-title">数据看板 - {{ getSelectedClubName() }}</h3>

          <el-row :gutter="16" class="stats-cards">
            <el-col :span="6">
              <el-card shadow="hover" class="stats-card">
                <template #header>成员总数</template>
                <div class="stats-value">{{ stats.memberCount }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" class="stats-card">
                <template #header>活动总数</template>
                <div class="stats-value">{{ stats.activityCount }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" class="stats-card">
                <template #header>即将开始</template>
                <div class="stats-value text-primary">{{ stats.activityStats?.upcoming || 0 }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" class="stats-card">
                <template #header>已结束</template>
                <div class="stats-value text-info">{{ stats.activityStats?.past || 0 }}</div>
              </el-card>
            </el-col>
          </el-row>

          <el-row :gutter="16" class="charts-row">
            <el-col :span="12">
              <el-card class="chart-card">
                <template #header>近7天新增成员趋势</template>
                <div ref="trendChartRef" style="height: 300px"></div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card class="chart-card">
                <template #header>角色分布</template>
                <div ref="roleChartRef" style="height: 300px"></div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </div>

      <div v-else class="no-clubs">
        <el-empty description="暂无可管理社团" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import { getDashboardTodos } from '@/api/dashboard'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const myClubs = ref([])
const selectedClubId = ref(null)
const stats = ref(null)
const todoOverview = ref(null)
const todoSections = ref([])

const handleNavigate = (path) => {
  console.log('导航到:', path)
  router.push(path).catch(err => {
    console.error('路由跳转失败:', err)
  })
}

const trendChartRef = ref(null)
const roleChartRef = ref(null)
let trendChart = null
let roleChart = null

const getSelectedClubName = () => {
  const club = myClubs.value.find((c) => c.id === selectedClubId.value)
  return club ? club.name : ''
}

const isClubDisabled = (status) => {
  return ['DISSOLVED', 'DISSOLVING', 'PENDING', 'REJECTED'].includes(status)
}

const getClubStatusText = (status) => {
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

const getRoleLabel = (role) => {
  switch (role) {
    case 'PRESIDENT':
      return '会长'
    case 'VICE_PRESIDENT':
      return '副会长'
    case 'MANAGER':
      return '管理员'
    case 'MEMBER':
      return '成员'
    default:
      return role || '-'
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
    console.error('加载统计数据失败', error)
  }
}

const loadTodos = async () => {
  try {
    todoOverview.value = await getDashboardTodos()
    todoSections.value = (todoOverview.value?.sections || [])
      .filter((section) => section.pendingCount > 0)
      .slice(0, 4)
  } catch (error) {
    console.error('加载统一待办失败', error)
  }
}

const initCharts = () => {
  if (!stats.value) {
    return
  }

  if (trendChartRef.value) {
    if (trendChart) trendChart.dispose()
    trendChart = echarts.init(trendChartRef.value)

    const dates = Object.keys(stats.value.recentJoins || {})
    const counts = Object.values(stats.value.recentJoins || {})

    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: { color: '#4a5e75' }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { color: '#4a5e75' }
      },
      grid: { left: 38, right: 10, top: 20, bottom: 30 },
      series: [
        {
          data: counts,
          type: 'line',
          smooth: true,
          lineStyle: { color: '#1f76b6', width: 3 },
          itemStyle: { color: '#1f76b6' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(31,118,182,0.35)' },
                { offset: 1, color: 'rgba(31,118,182,0.02)' }
              ]
            }
          }
        }
      ]
    })
  }

  if (roleChartRef.value) {
    if (roleChart) roleChart.dispose()
    roleChart = echarts.init(roleChartRef.value)

    const roleData = Object.entries(stats.value.roleDistribution || {}).map(([key, value]) => ({
      name: getRoleLabel(key),
      value
    }))

    roleChart.setOption({
      tooltip: { trigger: 'item' },
      legend: {
        top: '2%',
        left: 'center'
      },
      series: [
        {
          name: '角色分布',
          type: 'pie',
          radius: ['40%', '70%'],
          itemStyle: {
            borderRadius: 8,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: false },
          labelLine: { show: false },
          data: roleData
        }
      ]
    })
  }
}

onMounted(async () => {
  loadTodos()

  try {
    const res = await axios.get('/clubs/my')
    if (res.list) myClubs.value = res.list
    else if (Array.isArray(res)) myClubs.value = res
    else myClubs.value = res.content || []

    if (myClubs.value.length > 0) {
      await selectClub(myClubs.value[0].id)
    }
  } catch (error) {
    console.error('加载我的社团失败', error)
  }

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  roleChart?.dispose()
})

const requestDissolve = async (club) => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      `确定要申请解散社团“${club.name}”吗？`,
      '解散申请确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入解散原因',
        inputValidator: (value) => {
          if (!value || value.trim() === '') {
            return '请输入解散原因'
          }
          return true
        },
        inputErrorMessage: '解散原因不能为空'
      }
    )

    await axios.post(`/clubs/${club.id}/dissolve`, { reason })
    ElMessage.success('解散申请已提交')

    const res = await axios.get('/clubs/my')
    if (res.list) myClubs.value = res.list
    else if (Array.isArray(res)) myClubs.value = res
    else myClubs.value = res.content || []
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('提交失败，请稍后重试')
    }
  }
}

const cancelDissolve = async (clubId) => {
  try {
    await ElMessageBox.confirm('确定要撤销解散申请吗？', '撤销确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })

    await axios.post(`/clubs/${clubId}/dissolve/withdraw`)
    ElMessage.success('已撤销解散申请')

    const res = await axios.get('/clubs/my')
    if (res.list) myClubs.value = res.list
    else if (Array.isArray(res)) myClubs.value = res
    else myClubs.value = res.content || []
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('撤销失败，请稍后重试')
    }
  }
}

const handleResize = () => {
  trendChart?.resize()
  roleChart?.resize()
}
</script>

<style scoped>
.club-admin-dashboard {
  padding: 4px 0 10px;
  color: var(--text-main, #14314a);
}

.dashboard-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.welcome-text {
  margin: 8px 0 0;
  color: var(--text-muted, #5f7289);
}

.head-pill {
  white-space: nowrap;
}

.dashboard-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.todo-overview-card {
  border-radius: 16px;
  border: 1px solid var(--panel-border, rgba(14, 55, 94, 0.14));
  background: var(--panel-bg, rgba(255, 255, 255, 0.84));
  box-shadow: var(--panel-shadow, 0 14px 34px rgba(17, 46, 77, 0.1));
}

.todo-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.todo-subtext {
  margin: 6px 0 0;
  color: var(--text-muted, #60748c);
}

.todo-summary-row {
  margin-bottom: 16px;
}

.todo-summary-box {
  min-height: 92px;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid rgba(20, 83, 138, 0.1);
  background: linear-gradient(135deg, var(--accent-soft, rgba(29, 95, 159, 0.12)), rgba(255, 255, 255, 0.28));
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.todo-summary-label {
  color: var(--text-muted, #5f7289);
}

.todo-summary-value {
  font-size: clamp(1.4rem, 2.5vw, 1.8rem);
  color: var(--text-main, #173551);
}

.todo-preview-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-preview-item {
  width: 100%;
  border: 1px solid var(--panel-border, rgba(16, 63, 105, 0.14));
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  padding: 14px 16px;
  text-align: left;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.todo-preview-item:hover {
  transform: translateY(-2px);
  border-color: rgba(29, 95, 159, 0.34);
  box-shadow: 0 8px 18px rgba(15, 61, 104, 0.1);
}

.todo-preview-title {
  font-weight: 600;
  color: var(--text-main, #183650);
}

.todo-preview-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted, #60748c);
}

.my-clubs-section {
  display: grid;
  gap: 16px;
}

.section-title {
  margin: 0;
  font-size: 1.14rem;
  color: var(--text-main, #1d3750);
  letter-spacing: 0.01em;
}

.club-dashboard-card {
  cursor: pointer;
  border: 2px solid transparent;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  min-height: 162px;
}

.club-dashboard-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(11, 48, 82, 0.14);
}

.active-card {
  border-color: var(--accent, #1f76b6);
  background: linear-gradient(180deg, var(--accent-soft, rgba(31, 118, 182, 0.12)), rgba(255, 255, 255, 0.14));
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.club-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stats-section {
  margin-top: 2px;
  padding: 18px 18px 16px;
  display: grid;
  gap: 16px;
  border: 1px solid var(--panel-border, rgba(14, 55, 94, 0.14));
  border-radius: 16px;
  background: var(--panel-bg, rgba(255, 255, 255, 0.84));
  box-shadow: var(--panel-shadow, 0 14px 34px rgba(17, 46, 77, 0.1));
}

.stats-section .section-title {
  padding-left: 2px;
  margin-bottom: 2px;
}

.stats-cards {
  margin-bottom: 2px;
}

.stats-card :deep(.el-card__header) {
  font-weight: 600;
  color: var(--text-muted, #3a5168);
  background: linear-gradient(180deg, #f7fbff 0%, #eef5fd 100%);
  border-bottom: 1px solid rgba(17, 64, 106, 0.1);
}

.stats-value {
  font-size: clamp(1.65rem, 2.8vw, 2rem);
  font-weight: 700;
  text-align: center;
  color: var(--text-main, #20384f);
}

.text-primary {
  color: var(--accent, #1f76b6);
}

.text-info {
  color: #177f6e;
}

.chart-card :deep(.el-card__body) {
  padding-top: 10px;
}

.chart-card {
  border: 1px solid var(--panel-border, rgba(14, 55, 94, 0.14));
  border-radius: 14px;
  box-shadow: var(--panel-shadow, 0 14px 34px rgba(17, 46, 77, 0.1));
}

.chart-card :deep(.el-card__header) {
  background: linear-gradient(180deg, #f7fbff 0%, #eef5fd 100%);
  color: var(--text-main, #123b61);
  font-weight: 600;
}

.no-clubs {
  display: flex;
  justify-content: center;
  padding: 30px 0;
}

/* Light-gray dashboard visual alignment */
.dashboard-head {
  margin-bottom: 18px;
}

.todo-overview-card {
  border-radius: 12px;
  border: 1px solid var(--panel-border, #e5e7eb);
  background: var(--panel-bg, #fff);
  box-shadow: var(--panel-shadow, 0 2px 8px rgba(17, 24, 39, 0.06));
}

.todo-summary-box {
  min-height: 88px;
  border: 1px solid var(--panel-border, #e5e7eb);
  background: #f9fafb;
}

.todo-preview-item {
  border: 1px solid var(--panel-border, #e5e7eb);
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.todo-preview-item:hover {
  transform: none;
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(17, 24, 39, 0.08);
}

.club-dashboard-card {
  border: 1px solid var(--panel-border, #e5e7eb);
  border-radius: 12px;
  min-height: 156px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.club-dashboard-card:hover {
  transform: none;
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(17, 24, 39, 0.08);
}

.active-card {
  border-color: #93c5fd;
  background: #eff6ff;
}

.stats-section {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--panel-border, #e5e7eb);
  background: var(--panel-bg, #fff);
  box-shadow: var(--panel-shadow, 0 2px 8px rgba(17, 24, 39, 0.06));
}

.stats-card :deep(.el-card__header) {
  background: #f9fafb;
  border-bottom: 1px solid var(--panel-border, #e5e7eb);
}

.chart-card {
  border: 1px solid var(--panel-border, #e5e7eb);
  border-radius: 12px;
  box-shadow: var(--panel-shadow, 0 2px 8px rgba(17, 24, 39, 0.06));
}

.chart-card :deep(.el-card__header) {
  background: #f9fafb;
  color: var(--text-main, #111827);
}

@media (max-width: 980px) {
  .dashboard-head {
    flex-direction: column;
    align-items: stretch;
  }

  :deep(.my-clubs-section .el-col),
  :deep(.stats-cards .el-col),
  :deep(.charts-row .el-col) {
    max-width: 100%;
    flex: 0 0 100%;
    margin-bottom: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .todo-preview-item,
  .club-dashboard-card,
  .stats-card,
  .chart-card {
    transition: none !important;
  }

  .todo-preview-item:hover,
  .club-dashboard-card:hover {
    transform: none !important;
  }
}
</style>
