<template>
  <div :class="['admin-layout', { 'clubadmin-theme': isClubAdminArea }]">
    <el-container class="admin-shell">
      <el-aside width="248px" class="admin-sidebar">
        <div class="sidebar-header">
          <span class="logo-icon">社管</span>
          <span class="logo-text">管理后台</span>
        </div>

        <el-menu
          router
          :default-active="route.path"
          background-color="transparent"
          class="admin-menu"
        >
          <el-menu-item index="/admin" v-if="isAdmin">仪表盘</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/realtime">实时大屏</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/clubs">社团管理</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/notices">公告管理</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/resources">资源审批</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/resources/definitions">资源定义</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/audit">审计日志</el-menu-item>

          <el-menu-item index="/clubadmin" v-if="isClubAdmin">仪表盘</el-menu-item>

          <template v-if="isClubAdmin">
            <el-sub-menu v-for="club in myClubs" :key="club.id" :index="`club-${club.id}`">
              <template #title>
                <span>{{ club.name }}</span>
              </template>
              <el-menu-item :index="`/clubadmin/recruit/${club.id}`">招新管理</el-menu-item>
              <el-menu-item :index="`/clubadmin/members/${club.id}`">成员管理</el-menu-item>
              <el-menu-item :index="`/clubadmin/notices/${club.id}`">公告管理</el-menu-item>
              <el-menu-item :index="`/clubadmin/activities/${club.id}`">活动管理</el-menu-item>
              <el-menu-item :index="`/clubadmin/resources/${club.id}`">资源管理</el-menu-item>
              <el-menu-item :index="`/clubadmin/finance/${club.id}`">财务管理</el-menu-item>
              <el-menu-item :index="`/clubadmin/settings/${club.id}`">社团设置</el-menu-item>
            </el-sub-menu>
          </template>

          <el-menu-item @click="logout">退出登录</el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="admin-main">
        <div class="main-inner">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const myClubs = ref([])

const isAdmin = computed(() => {
  const roles = authStore.user?.roles || []
  return roles.some((r) => (typeof r === 'string' ? r : r.code) === 'ADMIN')
})

const isClubAdmin = computed(() => {
  const roles = authStore.user?.roles || []
  return roles.some((r) => (typeof r === 'string' ? r : r.code) === 'CLUB_ADMIN')
})

const isClubAdminArea = computed(() => route.path.startsWith('/clubadmin') || route.path.startsWith('/admin'))

const loadMyClubs = async () => {
  if (!isClubAdmin.value) {
    return
  }
  try {
    const res = await axios.get('/clubs/my')
    const data = res?.data ?? res
    if (Array.isArray(data?.list)) {
      myClubs.value = data.list
      return
    }
    if (Array.isArray(data)) {
      myClubs.value = data
      return
    }
    myClubs.value = data?.content || []
  } catch (error) {
    console.error('加载我的社团失败', error)
  }
}

const logout = async () => {
  await authStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadMyClubs()
})
</script>
<style scoped>
.admin-layout {
  height: 100vh;
  background: #eef3f9;
}

.admin-shell {
  height: 100%;
}

.admin-sidebar {
  background: #fff;
  border-right: 1px solid #d9e1ec;
}

.sidebar-header {
  height: 64px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #d9e1ec;
}

.logo-icon {
  font-size: 0.84rem;
  letter-spacing: 0.08em;
  font-weight: 700;
  color: #0f4c81;
}

.logo-text {
  font-size: 1rem;
  font-weight: 700;
  color: #1f2b3a;
}

:deep(.admin-menu) {
  border-right: 0;
  padding: 10px 8px;
}

:deep(.admin-menu .el-menu-item),
:deep(.admin-menu .el-sub-menu__title) {
  height: 38px;
  line-height: 38px;
  border-radius: 8px;
  margin: 4px 0;
}

.admin-main {
  padding: 20px;
  overflow-y: auto;
}

.main-inner {
  min-height: calc(100vh - 40px);
}

.clubadmin-theme {
  background:
    radial-gradient(circle at 8% 14%, rgba(15, 76, 129, 0.18), transparent 32%),
    radial-gradient(circle at 90% 16%, rgba(11, 126, 107, 0.15), transparent 36%),
    linear-gradient(180deg, #edf3fa 0%, #eaf1f9 46%, #e8f0f7 100%);
}

.clubadmin-theme .admin-sidebar {
  background: linear-gradient(180deg, #0f2740 0%, #133454 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.09);
  box-shadow: 16px 0 30px rgba(7, 20, 34, 0.2);
}

.clubadmin-theme .sidebar-header {
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.clubadmin-theme .logo-icon,
.clubadmin-theme .logo-text {
  color: #e7f1ff;
}

.clubadmin-theme :deep(.admin-menu .el-menu-item),
.clubadmin-theme :deep(.admin-menu .el-sub-menu__title) {
  color: rgba(231, 241, 255, 0.78) !important;
  font-weight: 600;
}

.clubadmin-theme :deep(.admin-menu .el-menu-item:hover),
.clubadmin-theme :deep(.admin-menu .el-sub-menu__title:hover) {
  background: rgba(231, 241, 255, 0.12) !important;
  color: #f3f8ff !important;
}

.clubadmin-theme :deep(.admin-menu .el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(26, 97, 168, 0.45), rgba(52, 163, 153, 0.36)) !important;
  color: #fff !important;
}

.clubadmin-theme :deep(.admin-menu .el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff !important;
}

.clubadmin-theme :deep(.el-main) {
  --panel-border: rgba(14, 55, 94, 0.14);
  --panel-shadow: 0 14px 34px rgba(17, 46, 77, 0.1);
  --panel-muted: #5f738b;
  --panel-heading: #14273a;
}

.clubadmin-theme :deep(.club-admin-dashboard),
.clubadmin-theme :deep(.admin-club-management),
.clubadmin-theme :deep(.admin-resource-approval),
.clubadmin-theme :deep(.resource-definition),
.clubadmin-theme :deep(.audit-log-management),
.clubadmin-theme :deep(.realtime-dashboard),
.clubadmin-theme :deep(.recruit-management),
.clubadmin-theme :deep(.member-management),
.clubadmin-theme :deep(.notice-management),
.clubadmin-theme :deep(.activity-management),
.clubadmin-theme :deep(.resource-management),
.clubadmin-theme :deep(.finance-management),
.clubadmin-theme :deep(.club-settings) {
  max-width: 1260px;
  margin: 0 auto;
  padding: 0 !important;
  color: var(--panel-heading);
}

.clubadmin-theme :deep(.admin-dashboard) {
  max-width: 1260px;
  margin: 0 auto;
  padding: 20px !important;
  border: 1px solid var(--panel-border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--panel-shadow);
  color: var(--panel-heading);
}

.clubadmin-theme :deep(.recruit-management > .el-table),
.clubadmin-theme :deep(.member-management > .el-table),
.clubadmin-theme :deep(.notice-management > .el-table),
.clubadmin-theme :deep(.activity-management > .el-table),
.clubadmin-theme :deep(.resource-management > .el-table),
.clubadmin-theme :deep(.finance-management > .el-table),
.clubadmin-theme :deep(.admin-club-management > .el-table),
.clubadmin-theme :deep(.admin-resource-approval > .el-table),
.clubadmin-theme :deep(.resource-definition > .el-table),
.clubadmin-theme :deep(.audit-log-management > .el-table) {
  border: 1px solid var(--panel-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: var(--panel-shadow);
  overflow: hidden;
}

.clubadmin-theme :deep(.club-admin-dashboard h2),
.clubadmin-theme :deep(.admin-dashboard h2),
.clubadmin-theme :deep(.admin-club-management h2),
.clubadmin-theme :deep(.admin-resource-approval h2),
.clubadmin-theme :deep(.resource-definition h2),
.clubadmin-theme :deep(.audit-log-management h2),
.clubadmin-theme :deep(.realtime-dashboard h2),
.clubadmin-theme :deep(.recruit-management h2),
.clubadmin-theme :deep(.member-management h2),
.clubadmin-theme :deep(.notice-management h2),
.clubadmin-theme :deep(.activity-management h2),
.clubadmin-theme :deep(.resource-management h2),
.clubadmin-theme :deep(.finance-management h2) {
  margin: 0 0 6px;
  font-size: clamp(1.35rem, 2vw, 1.7rem);
  color: #0f2e4a;
  letter-spacing: 0.01em;
}

.clubadmin-theme :deep(.welcome-text) {
  margin: 0 0 18px;
  color: var(--panel-muted);
}

.clubadmin-theme :deep(.actions),
.clubadmin-theme :deep(.header) {
  margin: 10px 0 16px !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.clubadmin-theme :deep(.settings-card),
.clubadmin-theme :deep(.stats-section),
.clubadmin-theme :deep(.table-panel),
.clubadmin-theme :deep(.balance-card) {
  border: 1px solid var(--panel-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--panel-shadow);
  backdrop-filter: blur(8px);
}

.clubadmin-theme :deep(.el-card) {
  border: 1px solid var(--panel-border) !important;
  border-radius: 14px !important;
  box-shadow: 0 10px 28px rgba(16, 44, 74, 0.08) !important;
  background: rgba(255, 255, 255, 0.9) !important;
}

.clubadmin-theme :deep(.club-dashboard-card) {
  border: 1px solid rgba(23, 79, 128, 0.14) !important;
}

.clubadmin-theme :deep(.active-card) {
  border-color: #1d5f9f !important;
  background: linear-gradient(180deg, rgba(38, 118, 191, 0.08), rgba(57, 161, 138, 0.08)) !important;
}

.clubadmin-theme :deep(.el-table) {
  --el-table-border-color: rgba(17, 64, 106, 0.14);
  --el-table-header-bg-color: #edf3fb;
  --el-table-header-text-color: #13324c;
  --el-table-row-hover-bg-color: #f5f9ff;
  border-radius: 12px;
  overflow: hidden;
}

.clubadmin-theme :deep(.el-table th.el-table__cell) {
  font-weight: 700;
}

.clubadmin-theme :deep(.el-tabs__item.is-active) {
  color: #165998;
}

.clubadmin-theme :deep(.el-tabs__active-bar) {
  background-color: #165998;
}

.clubadmin-theme :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(19, 73, 120, 0.14);
}

.clubadmin-theme :deep(.el-page-header) {
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid var(--panel-border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 8px 20px rgba(18, 50, 82, 0.08);
}

.clubadmin-theme :deep(.el-input__wrapper),
.clubadmin-theme :deep(.el-textarea__inner),
.clubadmin-theme :deep(.el-select__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(16, 63, 105, 0.14) inset;
}

.clubadmin-theme :deep(.el-input__wrapper.is-focus),
.clubadmin-theme :deep(.el-textarea__inner:focus),
.clubadmin-theme :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #1a67ad inset, 0 0 0 3px rgba(38, 118, 191, 0.12);
}

.clubadmin-theme :deep(.el-button--primary) {
  background: linear-gradient(135deg, #175d9e, #1f76b6) !important;
  border-color: #175d9e !important;
}

.clubadmin-theme :deep(.el-button--success) {
  background: linear-gradient(135deg, #177f6e, #23a089) !important;
  border-color: #177f6e !important;
}

.clubadmin-theme :deep(.el-button--danger) {
  background: linear-gradient(135deg, #b43f45, #c9575d) !important;
  border-color: #b43f45 !important;
}

.clubadmin-theme :deep(.action-buttons) {
  display: flex;
  align-items: center;
  gap: 8px !important;
}

.clubadmin-theme :deep(.el-tag--primary) {
  background: #e7f0ff;
  border-color: #bdd4ff;
  color: #1a5693;
}

.clubadmin-theme :deep(.el-tag--success) {
  background: #e6f6f1;
  border-color: #b6e5d8;
  color: #157761;
}

.clubadmin-theme :deep(.el-tag--warning) {
  background: #fff5e6;
  border-color: #ffe1ae;
  color: #9c6112;
}

.clubadmin-theme :deep(.el-tag--danger) {
  background: #fdeced;
  border-color: #f8c6ca;
  color: #9d3037;
}

.clubadmin-theme :deep(.el-dialog) {
  border-radius: 14px;
  overflow: hidden;
}

.clubadmin-theme :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(18, 67, 111, 0.12);
  background: #f3f8ff;
}

.clubadmin-theme :deep(.el-dialog__body) {
  background: #f8fbff;
}

.clubadmin-theme :deep(.el-form-item__label) {
  font-weight: 600;
  color: #183a59;
}

.clubadmin-theme :deep(.stats-value) {
  color: #123c61;
}

.clubadmin-theme :deep(.balance-card) {
  margin: 0;
  background: linear-gradient(135deg, #0f4f88, #2a7ab7);
  color: #f2f8ff;
}

.clubadmin-theme :deep(.balance-card .label),
.clubadmin-theme :deep(.balance-card .amount) {
  color: inherit;
}

@media (max-width: 1100px) {
  .admin-sidebar {
    width: 216px !important;
  }

  .admin-main {
    padding: 14px;
  }
}

@media (max-width: 880px) {
  .admin-layout {
    height: auto;
    min-height: 100vh;
  }

  .admin-shell {
    display: block;
  }

  .admin-sidebar {
    width: 100% !important;
  }

  .main-inner {
    min-height: auto;
  }
}
</style>


