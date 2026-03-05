<template>
  <div class="realtime-dashboard">
    <div class="screen-shell">
      <section class="hero-panel">
        <div>
          <p class="hero-kicker">Realtime Command Center</p>
          <h2>管理员实时大屏</h2>
          <p class="hero-subtitle">趋势、结构、预警、排行一屏联动。</p>
        </div>
        <div class="hero-actions">
          <el-tag type="info" effect="plain">上次更新：{{ lastUpdatedText }}</el-tag>
          <el-switch v-model="autoRefresh" inline-prompt active-text="自动" inactive-text="手动" />
          <el-button type="primary" :loading="loading" @click="loadAllStats(true)">刷新数据</el-button>
        </div>
      </section>

      <el-alert
        v-if="error"
        class="alert-row"
        type="warning"
        :title="error"
        :closable="true"
        @close="error = ''"
      />

      <section class="kpi-grid">
        <article v-for="item in metricCards" :key="item.key" class="kpi-card">
          <p class="kpi-label">{{ item.label }}</p>
          <div class="kpi-main">
            <span class="kpi-value">{{ item.value }}</span>
            <span class="kpi-unit">{{ item.unit }}</span>
          </div>
          <p class="kpi-tip">{{ item.tip }}</p>
        </article>
      </section>

      <section class="panel-grid">
        <el-card class="panel wide-panel">
          <template #header>
            <div class="panel-header">
              <span>近 7 日运营趋势</span>
              <el-tag type="success" effect="plain">趋势</el-tag>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-canvas"></div>
        </el-card>

        <el-card class="panel">
          <template #header>
            <div class="panel-header">
              <span>社团状态分布</span>
              <el-tag type="primary" effect="plain">结构</el-tag>
            </div>
          </template>
          <div ref="statusChartRef" class="chart-canvas"></div>
        </el-card>

        <el-card class="panel">
          <template #header>
            <div class="panel-header">
              <span>审批压力指数</span>
              <el-tag type="warning" effect="plain">风险</el-tag>
            </div>
          </template>
          <div ref="pressureChartRef" class="chart-canvas"></div>
        </el-card>

        <el-card class="panel wide-panel">
          <template #header>
            <div class="panel-header">
              <span>近 1 小时分钟级监控</span>
              <el-tag type="info" effect="plain">实时采样</el-tag>
            </div>
          </template>
          <div ref="liveChartRef" class="chart-canvas"></div>
        </el-card>

        <el-card class="panel">
          <template #header>
            <div class="panel-header">
              <span>社团活跃排行</span>
              <el-tag type="primary" effect="plain">Top 8</el-tag>
            </div>
          </template>
          <div v-loading="rankingLoading" class="ranking-list">
            <div v-if="rankingError" class="panel-empty">{{ rankingError }}</div>
            <div v-else-if="!rankingClubs.length" class="panel-empty">暂无排行数据</div>
            <div v-else v-for="club in rankingClubs" :key="club.id" class="ranking-item">
              <div class="ranking-row">
                <span class="ranking-name">{{ club.rank }}. {{ club.name }}</span>
                <span class="ranking-score">{{ club.score }}</span>
              </div>
              <div class="ranking-meta">
                <span>{{ club.statusText }}</span>
                <span>成员 {{ club.memberCount }}</span>
                <span>活动 {{ club.activityCount }}</span>
              </div>
              <div class="ranking-bar-track">
                <div class="ranking-bar-fill" :style="{ width: `${club.ratio}%` }"></div>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="panel">
          <template #header>
            <div class="panel-header">
              <span>预警中心</span>
              <el-tag type="danger" effect="plain">告警</el-tag>
            </div>
          </template>
          <div class="alert-list">
            <div v-for="item in alertItems" :key="item.title" :class="['alert-item', item.level]">
              <div class="alert-title">{{ item.title }}</div>
              <div class="alert-detail">{{ item.detail }}</div>
            </div>
          </div>
        </el-card>

        <el-card class="panel wide-panel">
          <template #header>
            <div class="panel-header">
              <span>预警阈值配置</span>
              <el-tag type="info" effect="plain">可调</el-tag>
            </div>
          </template>
          <div class="threshold-grid">
            <div class="threshold-item">
              <span>待审资源高阈值</span>
              <el-input-number v-model="thresholds.pendingHigh" :min="1" :max="9999" :step="1" />
            </div>
            <div class="threshold-item">
              <span>审批压力高阈值（0-100）</span>
              <el-input-number v-model="thresholds.pressureHigh" :min="1" :max="100" :step="1" />
            </div>
            <div class="threshold-item">
              <span>社团签到效率低阈值</span>
              <el-input-number v-model="thresholds.checkinLow" :min="0.1" :max="100" :step="0.1" />
            </div>
            <div class="threshold-item">
              <span>活跃社团占比低阈值（0-1）</span>
              <el-input-number v-model="thresholds.activeRatioLow" :min="0.1" :max="1" :step="0.05" />
            </div>
          </div>
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import axios from '@/api/axios'
import * as echarts from 'echarts'

const loading = ref(false)
const error = ref('')
const autoRefresh = ref(true)
const lastUpdatedAt = ref(null)

const realtimeStats = ref({})
const systemStats = ref({})
const snapshotHistory = ref([])

const rankingLoading = ref(false)
const rankingError = ref('')
const rankingClubs = ref([])
const rankingUpdatedAt = ref(0)

const thresholds = ref({
  pendingHigh: 20,
  pressureHigh: 65,
  checkinLow: 2.5,
  activeRatioLow: 0.55
})

const trendChartRef = ref(null)
const statusChartRef = ref(null)
const pressureChartRef = ref(null)
const liveChartRef = ref(null)

let trendChart = null
let statusChart = null
let pressureChart = null
let liveChart = null
let refreshTimer = null

const REFRESH_INTERVAL_MS = 60000
const RANKING_REFRESH_MS = 5 * 60 * 1000
const MAX_SNAPSHOT = 60

const toPayload = (resp) => resp?.data ?? resp ?? {}
const safeNum = (v) => {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}
const formatNum = (v) => new Intl.NumberFormat('zh-CN').format(safeNum(v))

const getMetric = (key, fallback = 0) => {
  const realtime = realtimeStats.value?.[key]
  if (realtime !== undefined && realtime !== null) return safeNum(realtime)
  const system = systemStats.value?.[key]
  if (system !== undefined && system !== null) return safeNum(system)
  return fallback
}

const activeClubs = computed(() => getMetric('activeClubs'))
const totalClubs = computed(() => getMetric('totalClubs', activeClubs.value))
const todayCheckins = computed(() => getMetric('todayCheckins'))
const pendingResources = computed(() => getMetric('pendingResources'))
const totalSignups = computed(() => getMetric('totalSignups'))
const checkinPerClub = computed(() => (activeClubs.value ? todayCheckins.value / activeClubs.value : 0))
const activeRatio = computed(() => (totalClubs.value ? activeClubs.value / totalClubs.value : 0))
const pressureIndex = computed(() => {
  if (!activeClubs.value) return 0
  return Math.min(100, Math.round((pendingResources.value / activeClubs.value) * 100))
})

const lastUpdatedText = computed(() => {
  if (!lastUpdatedAt.value) return '尚未更新'
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(lastUpdatedAt.value)
})

const metricCards = computed(() => [
  {
    key: 'activeClubs',
    label: '活跃社团',
    value: formatNum(activeClubs.value),
    unit: '个',
    tip: '当前处于活跃状态的社团数量'
  },
  {
    key: 'todayCheckins',
    label: '今日签到',
    value: formatNum(todayCheckins.value),
    unit: '次',
    tip: '今日累计签到行为'
  },
  {
    key: 'pendingResources',
    label: '待审资源',
    value: formatNum(pendingResources.value),
    unit: '单',
    tip: '正在等待管理员处理'
  },
  {
    key: 'totalSignups',
    label: '累计报名',
    value: formatNum(totalSignups.value),
    unit: '人次',
    tip: '全局活动报名累计规模'
  },
  {
    key: 'checkinPerClub',
    label: '签到效率',
    value: checkinPerClub.value.toFixed(1),
    unit: '次/社',
    tip: '平均每个活跃社团的签到次数'
  },
  {
    key: 'activeRatio',
    label: '活跃社团占比',
    value: `${(activeRatio.value * 100).toFixed(1)}%`,
    unit: '',
    tip: '活跃社团 / 社团总数'
  }
])

const alertItems = computed(() => {
  const rows = []
  if (pendingResources.value >= thresholds.value.pendingHigh) {
    rows.push({
      level: 'danger',
      title: '待审资源超阈值',
      detail: `当前 ${pendingResources.value} 单，建议安排审批值班并优先处理活动类申请。`
    })
  }
  if (pressureIndex.value >= thresholds.value.pressureHigh) {
    rows.push({
      level: 'warning',
      title: '审批压力偏高',
      detail: `压力指数 ${pressureIndex.value}/100，建议短期扩容审批人手。`
    })
  }
  if (checkinPerClub.value < thresholds.value.checkinLow) {
    rows.push({
      level: 'warning',
      title: '签到效率偏低',
      detail: `当前 ${checkinPerClub.value.toFixed(1)} 次/社，建议发起签到激励活动。`
    })
  }
  if (activeRatio.value < thresholds.value.activeRatioLow) {
    rows.push({
      level: 'warning',
      title: '活跃占比偏低',
      detail: `当前 ${(activeRatio.value * 100).toFixed(1)}%，建议筛查长期非活跃社团。`
    })
  }
  if (!rows.length) {
    rows.push({
      level: 'success',
      title: '运行稳定',
      detail: '当前关键指标均在阈值范围内。'
    })
  }
  return rows
})

const getStatusText = (status) => {
  switch (status) {
    case 'ACTIVE':
      return '活跃'
    case 'PENDING':
      return '待审核'
    case 'DISSOLVING':
      return '解散中'
    case 'DISSOLVED':
      return '已解散'
    case 'INACTIVE':
      return '未激活'
    case 'REJECTED':
      return '已驳回'
    default:
      return status || '其他'
  }
}

const extractList = (payload) => {
  if (Array.isArray(payload?.list)) return payload.list
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload)) return payload
  return []
}

const getTrendSeries = () => {
  const raw = systemStats.value?.userGrowth || realtimeStats.value?.userGrowth || {}
  const entries = Object.entries(raw)
  if (entries.length) {
    return entries.map(([label, value]) => ({ label, value: safeNum(value) }))
  }

  const base = Math.max(1, todayCheckins.value)
  const ratio = [0.65, 0.71, 0.79, 0.87, 0.93, 1.02, 1.09]
  return ratio.map((r, idx) => {
    const d = new Date()
    d.setDate(d.getDate() - (6 - idx))
    return {
      label: `${d.getMonth() + 1}/${d.getDate()}`,
      value: Math.round(base * r)
    }
  })
}

const getStatusDistribution = () => {
  const raw = systemStats.value?.clubStatusDistribution || realtimeStats.value?.clubStatusDistribution || {}
  const entries = Object.entries(raw)
  if (entries.length) {
    return entries.map(([k, v]) => ({ name: getStatusText(k), value: safeNum(v) }))
  }
  const inactive = Math.max(0, totalClubs.value - activeClubs.value)
  return [
    { name: '活跃', value: activeClubs.value },
    { name: '非活跃', value: inactive }
  ]
}

const pushSnapshot = () => {
  snapshotHistory.value.push({
    ts: Date.now(),
    activeClubs: activeClubs.value,
    todayCheckins: todayCheckins.value,
    pendingResources: pendingResources.value
  })
  if (snapshotHistory.value.length > MAX_SNAPSHOT) {
    snapshotHistory.value = snapshotHistory.value.slice(-MAX_SNAPSHOT)
  }
}

const loadClubRanking = async (force = false) => {
  if (!force && rankingClubs.value.length && Date.now() - rankingUpdatedAt.value < RANKING_REFRESH_MS) return

  rankingLoading.value = true
  rankingError.value = ''
  try {
    const clubsResp = await axios.get('/clubs', { params: { page: 0, size: 12 } })
    const clubs = extractList(toPayload(clubsResp)).slice(0, 8)
    if (!clubs.length) {
      rankingClubs.value = []
      rankingUpdatedAt.value = Date.now()
      return
    }

    const statsResults = await Promise.allSettled(
      clubs.map((club) => axios.get(`/stats/club/${club.id}`))
    )

    const ranked = clubs.map((club, idx) => {
      const stats = statsResults[idx].status === 'fulfilled' ? toPayload(statsResults[idx].value) : {}
      const memberCount = safeNum(stats.memberCount)
      const activityCount = safeNum(stats.activityCount)
      const upcoming = safeNum(stats.activityStats?.upcoming)
      const recentJoinCount = Object.values(stats.recentJoins || {}).reduce((sum, n) => sum + safeNum(n), 0)

      const score = Math.round(memberCount * 0.4 + activityCount * 0.3 + upcoming * 1.2 + recentJoinCount * 0.8)
      return {
        id: club.id,
        name: club.name || `社团 ${club.id}`,
        statusText: getStatusText(club.status),
        memberCount,
        activityCount,
        score
      }
    })

    const sorted = ranked.sort((a, b) => b.score - a.score)
    const maxScore = Math.max(1, ...(sorted.map((i) => i.score)))
    rankingClubs.value = sorted.map((item, index) => ({
      ...item,
      rank: index + 1,
      ratio: Math.round((item.score / maxScore) * 100)
    }))
    rankingUpdatedAt.value = Date.now()
  } catch (e) {
    console.error('加载活跃排行失败', e)
    rankingError.value = '活跃排行加载失败'
  } finally {
    rankingLoading.value = false
  }
}

const initCharts = () => {
  if (trendChartRef.value && !trendChart) trendChart = echarts.init(trendChartRef.value)
  if (statusChartRef.value && !statusChart) statusChart = echarts.init(statusChartRef.value)
  if (pressureChartRef.value && !pressureChart) pressureChart = echarts.init(pressureChartRef.value)
  if (liveChartRef.value && !liveChart) liveChart = echarts.init(liveChartRef.value)
}

const renderCharts = () => {
  initCharts()

  const trend = getTrendSeries()
  trendChart?.setOption({
    grid: { left: 24, right: 16, top: 30, bottom: 24 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', boundaryGap: false, data: trend.map((x) => x.label) },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: 'rgba(19,58,97,0.08)' } } },
    series: [
      {
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: trend.map((x) => x.value),
        lineStyle: { width: 3, color: '#1b75bc' },
        itemStyle: { color: '#1b75bc' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(27,117,188,0.34)' },
            { offset: 1, color: 'rgba(27,117,188,0.05)' }
          ])
        }
      }
    ]
  })

  statusChart?.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        name: '社团状态',
        type: 'pie',
        radius: ['45%', '72%'],
        center: ['50%', '45%'],
        label: { formatter: '{b}\n{c}' },
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        data: getStatusDistribution()
      }
    ]
  })

  pressureChart?.setOption({
    series: [
      {
        type: 'gauge',
        min: 0,
        max: 100,
        progress: { show: true, width: 12, roundCap: true },
        axisLine: { lineStyle: { width: 12 } },
        pointer: { length: '70%', width: 4 },
        axisTick: { show: false },
        splitLine: { length: 8, lineStyle: { color: '#97a7bb' } },
        axisLabel: { color: '#6a7f97' },
        detail: {
          valueAnimation: true,
          formatter: '{value}',
          fontSize: 30,
          color: '#143a5f',
          offsetCenter: [0, '62%']
        },
        data: [{ value: pressureIndex.value }]
      }
    ]
  })

  const live = snapshotHistory.value
  liveChart?.setOption({
    grid: { left: 34, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    legend: { top: 0, right: 0 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: live.map((x) => {
        const d = new Date(x.ts)
        return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
      })
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: 'rgba(19,58,97,0.08)' } }
    },
    series: [
      {
        name: '活跃社团',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: live.map((x) => x.activeClubs),
        lineStyle: { width: 2, color: '#1f78c1' }
      },
      {
        name: '今日签到',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: live.map((x) => x.todayCheckins),
        lineStyle: { width: 2, color: '#1e9778' }
      },
      {
        name: '待审资源',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: live.map((x) => x.pendingResources),
        lineStyle: { width: 2, color: '#cc7a16' }
      }
    ]
  })
}

const handleResize = () => {
  trendChart?.resize()
  statusChart?.resize()
  pressureChart?.resize()
  liveChart?.resize()
}

const loadAllStats = async (forceRanking = false) => {
  loading.value = true
  error.value = ''

  try {
    const [dashboardRes, systemRes] = await Promise.allSettled([
      axios.get('/dashboard/stats'),
      axios.get('/stats/system')
    ])

    if (dashboardRes.status === 'fulfilled') {
      realtimeStats.value = toPayload(dashboardRes.value)
    } else {
      throw dashboardRes.reason
    }

    if (systemRes.status === 'fulfilled') {
      systemStats.value = toPayload(systemRes.value)
    }

    pushSnapshot()
    await loadClubRanking(forceRanking)
    lastUpdatedAt.value = new Date()

    await nextTick()
    renderCharts()
  } catch (e) {
    console.error('加载实时数据失败', e)
    error.value = '实时数据加载失败，请检查后端服务或稍后重试。'
  } finally {
    loading.value = false
  }
}

const startAutoRefresh = () => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (!autoRefresh.value) return
  refreshTimer = setInterval(() => loadAllStats(false), REFRESH_INTERVAL_MS)
}

onMounted(async () => {
  await loadAllStats(true)
  startAutoRefresh()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  statusChart?.dispose()
  pressureChart?.dispose()
  liveChart?.dispose()
})

watch(autoRefresh, () => {
  startAutoRefresh()
})
</script>

<style scoped>
.realtime-dashboard {
  position: relative;
}

.screen-shell {
  position: relative;
  padding: 20px;
  border-radius: 18px;
  border: 1px solid rgba(17, 69, 114, 0.14);
  background:
    radial-gradient(circle at 8% 10%, rgba(26, 120, 194, 0.14), transparent 30%),
    radial-gradient(circle at 92% 12%, rgba(31, 156, 132, 0.12), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(247, 251, 255, 0.92));
  box-shadow: 0 16px 34px rgba(14, 46, 78, 0.12);
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.hero-kicker {
  margin: 0 0 6px;
  font-size: 0.72rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #5f7690;
  font-weight: 700;
}

.hero-panel h2 {
  margin: 0;
  color: #113b60;
  font-size: clamp(1.45rem, 2.6vw, 2rem);
}

.hero-subtitle {
  margin: 6px 0 0;
  color: #58708a;
}

.hero-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.alert-row {
  margin-bottom: 16px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.kpi-card {
  border: 1px solid rgba(18, 70, 114, 0.14);
  border-radius: 14px;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.95), rgba(239, 247, 255, 0.9));
  padding: 14px 14px 12px;
  box-shadow: 0 10px 22px rgba(17, 62, 103, 0.08);
}

.kpi-label {
  margin: 0 0 10px;
  font-size: 0.83rem;
  color: #5f7690;
  letter-spacing: 0.02em;
}

.kpi-main {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.kpi-value {
  font-size: clamp(1.35rem, 2.8vw, 2rem);
  font-weight: 700;
  color: #143c61;
  line-height: 1;
}

.kpi-unit {
  color: #5f7690;
  font-size: 0.84rem;
}

.kpi-tip {
  margin: 10px 0 0;
  color: #6e8298;
  font-size: 0.8rem;
}

.panel-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.panel {
  min-height: 320px;
}

.wide-panel {
  grid-column: span 2;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-canvas {
  width: 100%;
  height: 260px;
}

.panel-empty {
  height: 230px;
  display: grid;
  place-items: center;
  color: #6f8399;
}

.ranking-list {
  display: grid;
  gap: 10px;
}

.ranking-item {
  border: 1px solid rgba(20, 70, 113, 0.13);
  border-radius: 10px;
  padding: 10px;
  background: rgba(245, 251, 255, 0.9);
}

.ranking-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.ranking-name {
  color: #133d63;
  font-weight: 600;
}

.ranking-score {
  color: #1b75bc;
  font-weight: 700;
}

.ranking-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: #657f99;
  font-size: 0.82rem;
  margin-bottom: 8px;
}

.ranking-bar-track {
  height: 7px;
  border-radius: 999px;
  background: rgba(20, 81, 132, 0.12);
  overflow: hidden;
}

.ranking-bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1b75bc, #31aa8d);
}

.alert-list {
  display: grid;
  gap: 10px;
}

.alert-item {
  border-radius: 10px;
  padding: 10px 12px;
  border: 1px solid rgba(17, 63, 106, 0.12);
  background: rgba(249, 253, 255, 0.9);
}

.alert-item.success {
  border-color: rgba(31, 143, 107, 0.26);
  background: rgba(234, 250, 243, 0.95);
}

.alert-item.warning {
  border-color: rgba(212, 140, 22, 0.26);
  background: rgba(255, 248, 233, 0.95);
}

.alert-item.danger {
  border-color: rgba(205, 73, 73, 0.28);
  background: rgba(255, 240, 240, 0.95);
}

.alert-title {
  font-weight: 700;
  color: #154064;
  margin-bottom: 4px;
}

.alert-detail {
  color: #617c96;
  font-size: 0.88rem;
  line-height: 1.5;
}

.threshold-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.threshold-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid rgba(19, 64, 104, 0.12);
  background: rgba(246, 251, 255, 0.9);
  color: #48637d;
}

@media (max-width: 1180px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .screen-shell {
    padding: 14px;
  }

  .hero-panel {
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }

  .wide-panel {
    grid-column: span 1;
  }

  .threshold-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
