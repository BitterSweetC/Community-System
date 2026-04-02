<template>
  <el-config-provider :locale="adminLocale">
    <div
      :class="[
        'admin-layout',
        { 'system-admin-theme': isSystemAdminArea, 'club-admin-theme': isClubAdminArea }
      ]"
    >
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
          <el-menu-item v-if="isAdmin" index="/admin/todos">统一待办</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/realtime">实时大屏</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/clubs">社团管理</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/notices">公告管理</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/prohibited-words">违禁词管理</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/resources">资源审批</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/resources/definitions">资源定义</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/audit">审计日志</el-menu-item>

          <el-menu-item index="/clubadmin" v-if="isClubAdmin">
            <el-icon><component :is="resolveMenuIcon('/clubadmin')" /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/clubadmin/todos" v-if="isClubAdmin">
            <el-icon><component :is="resolveMenuIcon('/clubadmin/todos')" /></el-icon>
            <span>统一待办</span>
          </el-menu-item>

          <template v-if="isClubAdmin">
            <el-sub-menu v-for="club in myClubs" :key="club.id" :index="`club-${club.id}`">
              <template #title>
                <span>{{ club.name }}</span>
              </template>
              <el-menu-item :index="`/clubadmin/recruit/${club.id}`">
                <el-icon><component :is="resolveMenuIcon(`/clubadmin/recruit/${club.id}`)" /></el-icon>
                <span>招新管理</span>
              </el-menu-item>
              <el-menu-item :index="`/clubadmin/members/${club.id}`">
                <el-icon><component :is="resolveMenuIcon(`/clubadmin/members/${club.id}`)" /></el-icon>
                <span>成员管理</span>
              </el-menu-item>
              <el-menu-item :index="`/clubadmin/notices/${club.id}`">
                <el-icon><component :is="resolveMenuIcon(`/clubadmin/notices/${club.id}`)" /></el-icon>
                <span>公告管理</span>
              </el-menu-item>
              <el-menu-item :index="`/clubadmin/activities/${club.id}`">
                <el-icon><component :is="resolveMenuIcon(`/clubadmin/activities/${club.id}`)" /></el-icon>
                <span>活动管理</span>
              </el-menu-item>
              <el-menu-item :index="`/clubadmin/resources/${club.id}`">
                <el-icon><component :is="resolveMenuIcon(`/clubadmin/resources/${club.id}`)" /></el-icon>
                <span>资源管理</span>
              </el-menu-item>
              <el-menu-item :index="`/clubadmin/finance/${club.id}`">
                <el-icon><component :is="resolveMenuIcon(`/clubadmin/finance/${club.id}`)" /></el-icon>
                <span>财务管理</span>
              </el-menu-item>
              <el-menu-item :index="`/clubadmin/settings/${club.id}`">
                <el-icon><component :is="resolveMenuIcon(`/clubadmin/settings/${club.id}`)" /></el-icon>
                <span>社团设置</span>
              </el-menu-item>
            </el-sub-menu>
          </template>

        </el-menu>

        <div style="padding: 8px;">
          <el-button class="logout-button" style="width: 100%;" @click="logout">退出登录</el-button>
        </div>
      </el-aside>

      <el-main class="admin-main">
        <div class="main-inner">
          <router-view />
        </div>
      </el-main>
    </el-container>
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import {
  Bell,
  Calendar,
  DataBoard,
  Files,
  List,
  Promotion,
  Setting,
  User,
  Wallet
} from '@element-plus/icons-vue'

const adminLocale = {
  ...zhCn,
  el: {
    ...zhCn.el,
    pagination: {
      ...zhCn.el.pagination,
      pagesize: '/\u9875'
    }
  }
}

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

const isSystemAdminArea = computed(() => route.path.startsWith('/admin'))
const isClubAdminArea = computed(() => route.path.startsWith('/clubadmin'))

const clubMenuIconMap = Object.freeze([
  { prefix: '/clubadmin/todos', icon: List },
  { prefix: '/clubadmin/recruit/', icon: Promotion },
  { prefix: '/clubadmin/members/', icon: User },
  { prefix: '/clubadmin/notices/', icon: Bell },
  { prefix: '/clubadmin/activities/', icon: Calendar },
  { prefix: '/clubadmin/resources/', icon: Files },
  { prefix: '/clubadmin/finance/', icon: Wallet },
  { prefix: '/clubadmin/settings/', icon: Setting },
  { prefix: '/clubadmin', icon: DataBoard }
])

const resolveMenuIcon = (path) => {
  const normalizedPath = typeof path === 'string' ? path : ''
  return (
    clubMenuIconMap.find((item) => normalizedPath.startsWith(item.prefix))?.icon ||
    DataBoard
  )
}

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
  --panel-bg: rgba(255, 255, 255, 0.86);
  --panel-border: rgba(19, 57, 92, 0.14);
  --panel-shadow: 0 14px 30px rgba(16, 43, 72, 0.11);
  --text-main: #14314a;
  --text-muted: #5f748b;
  --accent: #1e66aa;
  --accent-soft: rgba(30, 102, 170, 0.12);
  --focus-ring: 0 0 0 3px rgba(30, 102, 170, 0.22);
  --layout-bg: #edf3f9;
  --sidebar-bg: #ffffff;
  --sidebar-border: #d9e1ec;
  --menu-color: #38536d;
  --menu-hover-bg: rgba(24, 93, 156, 0.08);
  --menu-active-bg: linear-gradient(90deg, rgba(23, 93, 158, 0.2), rgba(42, 121, 184, 0.08));
  --menu-active-color: #0f4d86;
  --menu-subtitle: #6a7f95;

  height: 100vh;
  background: var(--layout-bg);
}

.admin-shell {
  height: 100%;
}

.admin-sidebar {
  background: var(--sidebar-bg);
  border-right: 1px solid var(--sidebar-border);
  box-shadow: 14px 0 30px rgba(11, 33, 53, 0.08);
}

.sidebar-header {
  height: 64px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid var(--sidebar-border);
}

.logo-icon {
  font-size: 0.84rem;
  letter-spacing: 0.08em;
  font-weight: 700;
  color: var(--text-main);
}

.logo-text {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-main);
}

:deep(.admin-menu) {
  border-right: 0;
  padding: 12px 10px;
}

:deep(.admin-menu .el-menu-item),
:deep(.admin-menu .el-sub-menu__title) {
  position: relative;
  height: 40px;
  line-height: 40px;
  border-radius: 10px;
  margin: 5px 0;
  color: var(--menu-color) !important;
  font-weight: 600;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

:deep(.admin-menu .el-menu-item:hover),
:deep(.admin-menu .el-sub-menu__title:hover) {
  background: var(--menu-hover-bg) !important;
  color: var(--text-main) !important;
}

:deep(.admin-menu .el-menu-item.is-active) {
  background: var(--menu-active-bg) !important;
  color: var(--menu-active-color) !important;
}

:deep(.admin-menu .el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 2px;
  background: var(--accent);
}

:deep(.admin-menu .el-menu-item-group__title) {
  color: var(--menu-subtitle);
  font-size: 11px;
  letter-spacing: 0.08em;
  font-weight: 700;
  text-transform: uppercase;
  padding: 8px 12px 2px !important;
}

:deep(.admin-menu .el-menu-item:focus-visible),
:deep(.admin-menu .el-sub-menu__title:focus-visible) {
  outline: none;
  box-shadow: var(--focus-ring);
}

.admin-main {
  padding: 22px;
  overflow-y: auto;
}

.main-inner {
  min-height: calc(100vh - 40px);
}

.system-admin-theme {
  --layout-bg:
    radial-gradient(circle at 9% 10%, rgba(34, 92, 141, 0.12), transparent 34%),
    radial-gradient(circle at 90% 14%, rgba(30, 110, 96, 0.08), transparent 36%),
    linear-gradient(180deg, #eef3f9 0%, #eaf1f8 50%, #e7eef6 100%);
  --sidebar-bg: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
  --sidebar-border: rgba(18, 56, 89, 0.14);
  --panel-bg: rgba(255, 255, 255, 0.88);
  --panel-border: rgba(18, 64, 103, 0.14);
  --panel-shadow: 0 14px 30px rgba(15, 44, 73, 0.1);
  --text-main: #15324b;
  --text-muted: #5f748b;
  --accent: #1f6aaf;
  --accent-soft: rgba(31, 106, 175, 0.13);
  --menu-color: #35526d;
  --menu-hover-bg: rgba(31, 106, 175, 0.1);
  --menu-active-bg: linear-gradient(90deg, rgba(31, 106, 175, 0.2), rgba(31, 106, 175, 0.07));
  --menu-active-color: #0f4f88;
  --menu-subtitle: #697f95;
}

.club-admin-theme {
  --layout-bg:
    radial-gradient(circle at 8% 14%, rgba(15, 76, 129, 0.18), transparent 32%),
    radial-gradient(circle at 90% 16%, rgba(11, 126, 107, 0.15), transparent 36%),
    linear-gradient(180deg, #edf3fa 0%, #eaf1f9 46%, #e8f0f7 100%);
  --sidebar-bg: linear-gradient(180deg, #0f2740 0%, #133454 100%);
  --sidebar-border: rgba(255, 255, 255, 0.09);
  --panel-bg: rgba(255, 255, 255, 0.84);
  --panel-border: rgba(14, 55, 94, 0.14);
  --panel-shadow: 0 14px 34px rgba(17, 46, 77, 0.1);
  --text-main: #14273a;
  --text-muted: #5f738b;
  --accent: #1d5f9f;
  --accent-soft: rgba(29, 95, 159, 0.2);
  --menu-color: rgba(231, 241, 255, 0.78);
  --menu-hover-bg: rgba(231, 241, 255, 0.12);
  --menu-active-bg: linear-gradient(90deg, rgba(26, 97, 168, 0.45), rgba(52, 163, 153, 0.36));
  --menu-active-color: #ffffff;
  --menu-subtitle: rgba(231, 241, 255, 0.64);
}

.club-admin-theme {
  background: var(--layout-bg);
}

.club-admin-theme .admin-sidebar {
  background: linear-gradient(180deg, #0f2740 0%, #133454 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.09);
  box-shadow: 16px 0 30px rgba(7, 20, 34, 0.2);
}

.club-admin-theme .sidebar-header {
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.club-admin-theme .logo-icon,
.club-admin-theme .logo-text {
  color: #e7f1ff;
}

.club-admin-theme :deep(.admin-menu .el-menu-item),
.club-admin-theme :deep(.admin-menu .el-sub-menu__title) {
  color: rgba(231, 241, 255, 0.78) !important;
  font-weight: 600;
}

.club-admin-theme :deep(.admin-menu .el-menu-item:hover),
.club-admin-theme :deep(.admin-menu .el-sub-menu__title:hover) {
  background: rgba(231, 241, 255, 0.12) !important;
  color: #f3f8ff !important;
}

.club-admin-theme :deep(.admin-menu .el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(26, 97, 168, 0.45), rgba(52, 163, 153, 0.36)) !important;
  color: #fff !important;
}

.club-admin-theme :deep(.admin-menu .el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff !important;
}

.club-admin-theme :deep(.el-main) {
  --panel-border: rgba(14, 55, 94, 0.14);
  --panel-shadow: 0 14px 34px rgba(17, 46, 77, 0.1);
  --panel-muted: #5f738b;
  --panel-heading: #14273a;
}

.club-admin-theme :deep(.club-admin-dashboard),
.club-admin-theme :deep(.admin-club-management),
.club-admin-theme :deep(.admin-resource-approval),
.club-admin-theme :deep(.resource-definition),
.club-admin-theme :deep(.audit-log-management),
.club-admin-theme :deep(.realtime-dashboard),
.club-admin-theme :deep(.todo-center),
.club-admin-theme :deep(.recruit-management),
.club-admin-theme :deep(.member-management),
.club-admin-theme :deep(.notice-management),
.club-admin-theme :deep(.prohibited-word-management),
.club-admin-theme :deep(.activity-management),
.club-admin-theme :deep(.resource-management),
.club-admin-theme :deep(.finance-management),
.club-admin-theme :deep(.club-settings) {
  max-width: 1260px;
  margin: 0 auto;
  padding: 0 !important;
  color: var(--panel-heading);
}

.club-admin-theme :deep(.admin-dashboard) {
  max-width: 1260px;
  margin: 0 auto;
  padding: 20px !important;
  border: 1px solid var(--panel-border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--panel-shadow);
  color: var(--panel-heading);
}

.club-admin-theme :deep(.recruit-management > .el-table),
.club-admin-theme :deep(.member-management > .el-table),
.club-admin-theme :deep(.notice-management > .el-table),
.club-admin-theme :deep(.activity-management > .el-table),
.club-admin-theme :deep(.resource-management > .el-table),
.club-admin-theme :deep(.finance-management > .el-table),
.club-admin-theme :deep(.admin-club-management > .el-table),
.club-admin-theme :deep(.admin-resource-approval > .el-table),
.club-admin-theme :deep(.resource-definition > .el-table),
.club-admin-theme :deep(.audit-log-management > .el-table) {
  border: 1px solid var(--panel-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: var(--panel-shadow);
  overflow: hidden;
}

.club-admin-theme :deep(.club-admin-dashboard h2),
.club-admin-theme :deep(.admin-dashboard h2),
.club-admin-theme :deep(.admin-club-management h2),
.club-admin-theme :deep(.admin-resource-approval h2),
.club-admin-theme :deep(.resource-definition h2),
.club-admin-theme :deep(.audit-log-management h2),
.club-admin-theme :deep(.realtime-dashboard h2),
.club-admin-theme :deep(.recruit-management h2),
.club-admin-theme :deep(.member-management h2),
.club-admin-theme :deep(.notice-management h2),
.club-admin-theme :deep(.activity-management h2),
.club-admin-theme :deep(.resource-management h2),
.club-admin-theme :deep(.finance-management h2) {
  margin: 0 0 6px;
  font-size: clamp(1.35rem, 2vw, 1.7rem);
  color: #0f2e4a;
  letter-spacing: 0.01em;
}

.club-admin-theme :deep(.welcome-text) {
  margin: 0 0 18px;
  color: var(--panel-muted);
}

.club-admin-theme :deep(.header) {
  margin: 10px 0 16px !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.club-admin-theme :deep(.el-card) {
  border: 1px solid var(--panel-border) !important;
  border-radius: 14px !important;
  box-shadow: 0 10px 28px rgba(16, 44, 74, 0.08) !important;
  background: rgba(255, 255, 255, 0.9) !important;
}

.club-admin-theme :deep(.club-dashboard-card) {
  border: 1px solid rgba(23, 79, 128, 0.14) !important;
}

.club-admin-theme :deep(.active-card) {
  border-color: #1d5f9f !important;
  background: linear-gradient(180deg, rgba(38, 118, 191, 0.08), rgba(57, 161, 138, 0.08)) !important;
}

.club-admin-theme :deep(.el-table) {
  --el-table-border-color: rgba(17, 64, 106, 0.14);
  --el-table-header-bg-color: #edf3fb;
  --el-table-header-text-color: #13324c;
  --el-table-row-hover-bg-color: #f5f9ff;
  border-radius: 12px;
  overflow: hidden;
}

.club-admin-theme :deep(.el-table th.el-table__cell) {
  font-weight: 700;
  padding-top: 11px;
  padding-bottom: 11px;
}

.club-admin-theme :deep(.el-table td.el-table__cell) {
  padding-top: 10px;
  padding-bottom: 10px;
}

.club-admin-theme :deep(.el-tabs__item.is-active) {
  color: #165998;
}

.club-admin-theme :deep(.el-tabs__active-bar) {
  background-color: #165998;
}

.club-admin-theme :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(19, 73, 120, 0.14);
}

.club-admin-theme :deep(.el-page-header) {
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid var(--panel-border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 8px 20px rgba(18, 50, 82, 0.08);
}

.club-admin-theme :deep(.el-input__wrapper),
.club-admin-theme :deep(.el-textarea__inner),
.club-admin-theme :deep(.el-select__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(16, 63, 105, 0.14) inset;
}

.club-admin-theme :deep(.el-input__wrapper.is-focus),
.club-admin-theme :deep(.el-textarea__inner:focus),
.club-admin-theme :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #1a67ad inset, 0 0 0 3px rgba(38, 118, 191, 0.12);
}

.club-admin-theme :deep(.el-button--primary) {
  background: linear-gradient(135deg, #175d9e, #1f76b6) !important;
  border-color: #175d9e !important;
}

.club-admin-theme :deep(.el-button--success) {
  background: linear-gradient(135deg, #177f6e, #23a089) !important;
  border-color: #177f6e !important;
}

.club-admin-theme :deep(.el-button--danger) {
  background: linear-gradient(135deg, #b43f45, #c9575d) !important;
  border-color: #b43f45 !important;
}

.club-admin-theme :deep(.el-button:not(.el-button--primary):not(.el-button--success):not(.el-button--danger)) {
  border-color: rgba(16, 63, 105, 0.2) !important;
  color: #204766 !important;
  background: rgba(255, 255, 255, 0.9) !important;
}

.club-admin-theme :deep(.el-button:not(.el-button--primary):not(.el-button--success):not(.el-button--danger):hover) {
  border-color: rgba(26, 103, 173, 0.3) !important;
  color: #145287 !important;
  background: #ffffff !important;
}

.logout-button {
  border-color: rgba(15, 76, 129, 0.18);
  color: #45627c;
  background: rgba(255, 255, 255, 0.86);
}

.logout-button:hover,
.logout-button:focus {
  color: #1a67ad;
  border-color: rgba(26, 103, 173, 0.32);
  background: rgba(255, 255, 255, 0.96);
}

.club-admin-theme :deep(.action-buttons) {
  display: flex;
  align-items: center;
  gap: 8px !important;
}

.club-admin-theme :deep(.logout-button) {
  color: rgba(231, 241, 255, 0.86);
  border-color: rgba(231, 241, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
}

.club-admin-theme :deep(.logout-button:hover),
.club-admin-theme :deep(.logout-button:focus) {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.14);
}

.club-admin-theme :deep(.el-tag--primary) {
  background: #e7f0ff;
  border-color: #bdd4ff;
  color: #1a5693;
}

.club-admin-theme :deep(.el-tag--success) {
  background: #e6f6f1;
  border-color: #b6e5d8;
  color: #157761;
}

.club-admin-theme :deep(.el-tag--warning) {
  background: #fff5e6;
  border-color: #ffe1ae;
  color: #9c6112;
}

.club-admin-theme :deep(.el-tag--danger) {
  background: #fdeced;
  border-color: #f8c6ca;
  color: #9d3037;
}

.club-admin-theme :deep(.el-tag--info) {
  background: #f0f5fb;
  border-color: #d3dfec;
  color: #50677f;
}

.club-admin-theme :deep(.el-dialog) {
  border-radius: 14px;
  overflow: hidden;
}

.club-admin-theme :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(18, 67, 111, 0.12);
  background: #f3f8ff;
}

.club-admin-theme :deep(.el-dialog__body) {
  background: #f8fbff;
}

.club-admin-theme :deep(.el-form-item__label) {
  font-weight: 600;
  color: #183a59;
}

.club-admin-theme :deep(.stats-value) {
  color: #123c61;
}

.club-admin-theme :deep(.balance-card) {
  margin: 0;
  background: linear-gradient(135deg, #0f4f88, #2a7ab7) !important;
  color: #f2f8ff !important;
  border: 1px solid rgba(242, 248, 255, 0.22) !important;
  box-shadow: 0 14px 26px rgba(11, 43, 74, 0.24) !important;
}

.club-admin-theme :deep(.balance-card .label),
.club-admin-theme :deep(.balance-card .amount) {
  color: inherit !important;
}

.system-admin-theme .admin-sidebar {
  background: var(--sidebar-bg);
  border-right: 1px solid var(--sidebar-border);
  box-shadow: 10px 0 24px rgba(13, 37, 61, 0.08);
}

.system-admin-theme .sidebar-header {
  border-bottom: 1px solid var(--sidebar-border);
}

.system-admin-theme .logo-icon,
.system-admin-theme .logo-text {
  color: var(--text-main);
}

.system-admin-theme :deep(.admin-menu .el-menu-item),
.system-admin-theme :deep(.admin-menu .el-sub-menu__title) {
  color: var(--menu-color) !important;
}

.system-admin-theme :deep(.admin-menu .el-menu-item:hover),
.system-admin-theme :deep(.admin-menu .el-sub-menu__title:hover) {
  background: var(--menu-hover-bg) !important;
  color: var(--text-main) !important;
}

.system-admin-theme :deep(.admin-menu .el-menu-item.is-active) {
  background: var(--menu-active-bg) !important;
  color: var(--menu-active-color) !important;
}

.system-admin-theme :deep(.admin-menu .el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--menu-active-color) !important;
}

.system-admin-theme :deep(.admin-menu .el-menu-item-group__title) {
  color: var(--menu-subtitle) !important;
}

.system-admin-theme :deep(.logout-button) {
  color: #3f5f7d;
  border-color: rgba(18, 79, 129, 0.22);
  background: rgba(255, 255, 255, 0.9);
}

.system-admin-theme :deep(.logout-button:hover),
.system-admin-theme :deep(.logout-button:focus) {
  color: #15558e;
  border-color: rgba(21, 85, 142, 0.34);
  background: rgba(255, 255, 255, 0.98);
}

.club-admin-theme :deep(.page-head) {
  margin-bottom: 16px !important;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.club-admin-theme :deep(.subtext) {
  margin: 6px 0 0 !important;
  color: var(--text-muted) !important;
  line-height: 1.6;
}

.club-admin-theme :deep(.actions) {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.club-admin-theme :deep(.table-panel),
.club-admin-theme :deep(.todo-overview-card),
.club-admin-theme :deep(.stats-section),
.club-admin-theme :deep(.chart-card),
.club-admin-theme :deep(.settings-card) {
  border: 1px solid var(--panel-border) !important;
  border-radius: 16px !important;
  background: var(--panel-bg) !important;
  box-shadow: var(--panel-shadow) !important;
}

.club-admin-theme :deep(.page-head h2) {
  margin: 0 !important;
  font-size: clamp(1.32rem, 1.8vw, 1.66rem) !important;
  color: #0f2e4a !important;
  letter-spacing: 0.01em;
}

.club-admin-theme :deep(.head-right) {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.club-admin-theme :deep(.table-panel) {
  padding: 14px !important;
}

.club-admin-theme :deep(.action-buttons),
.club-admin-theme :deep(.row-actions),
.club-admin-theme :deep(.review-actions) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.club-admin-theme :deep(.action-buttons .el-button),
.club-admin-theme :deep(.row-actions .el-button),
.club-admin-theme :deep(.review-actions .el-button) {
  margin-left: 0 !important;
}

.club-admin-theme :deep(.el-button:focus-visible),
.club-admin-theme :deep(.el-menu-item:focus-visible),
.club-admin-theme :deep(.el-sub-menu__title:focus-visible),
.club-admin-theme :deep(.todo-preview-item:focus-visible),
.club-admin-theme :deep(.club-dashboard-card:focus-visible) {
  outline: none;
  box-shadow: var(--focus-ring);
}

/* ClubAdmin light-gray operations board refresh */
.club-admin-theme {
  --layout-bg: #f5f7fa;
  --sidebar-bg: #f3f4f6;
  --panel-bg: #ffffff;
  --panel-border: #e5e7eb;
  --panel-shadow: 0 2px 8px rgba(17, 24, 39, 0.06);
  --text-main: #111827;
  --text-muted: #6b7280;
  --accent: #3b82f6;
  --accent-soft: rgba(59, 130, 246, 0.1);
  --menu-color: #374151;
  --menu-hover-bg: #eef2ff;
  --menu-active-bg: #eaf2ff;
  --menu-active-color: #2563eb;
  --menu-subtitle: #6b7280;
  --focus-ring: 0 0 0 3px rgba(59, 130, 246, 0.18);
  background: var(--layout-bg) !important;
}

.club-admin-theme .admin-main {
  padding: 24px 28px;
  background: var(--layout-bg);
}

.club-admin-theme .main-inner {
  max-width: 1260px;
  min-height: calc(100vh - 48px);
  margin: 0 auto;
}

.club-admin-theme .admin-sidebar {
  background: var(--sidebar-bg) !important;
  border-right: 1px solid var(--panel-border) !important;
  box-shadow: none !important;
}

.club-admin-theme .sidebar-header {
  border-bottom: 1px solid var(--panel-border) !important;
}

.club-admin-theme .logo-icon,
.club-admin-theme .logo-text {
  color: var(--text-main) !important;
}

.club-admin-theme :deep(.admin-menu) {
  padding: 10px 10px;
}

.club-admin-theme :deep(.admin-menu .el-menu-item),
.club-admin-theme :deep(.admin-menu .el-sub-menu__title) {
  height: 38px;
  line-height: 38px;
  border-radius: 10px;
  margin: 3px 0;
  color: var(--menu-color) !important;
  background: transparent !important;
  font-weight: 600;
}

.club-admin-theme :deep(.admin-menu .el-menu-item .el-icon),
.club-admin-theme :deep(.admin-menu .el-sub-menu__title .el-icon) {
  margin-right: 8px;
  color: currentColor;
}

.club-admin-theme :deep(.admin-menu .el-menu-item:hover),
.club-admin-theme :deep(.admin-menu .el-sub-menu__title:hover) {
  background: var(--menu-hover-bg) !important;
  color: #1f2937 !important;
}

.club-admin-theme :deep(.admin-menu .el-menu-item.is-active) {
  background: var(--menu-active-bg) !important;
  color: var(--menu-active-color) !important;
}

.club-admin-theme :deep(.admin-menu .el-menu-item.is-active)::before {
  display: none;
}

.club-admin-theme :deep(.admin-menu .el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--menu-active-color) !important;
}

.club-admin-theme :deep(.admin-menu .el-menu-item-group__title) {
  color: var(--menu-subtitle) !important;
  text-transform: none !important;
  letter-spacing: 0.02em;
}

.club-admin-theme :deep(.el-card),
.club-admin-theme :deep(.table-panel),
.club-admin-theme :deep(.todo-overview-card),
.club-admin-theme :deep(.stats-section),
.club-admin-theme :deep(.chart-card),
.club-admin-theme :deep(.settings-card) {
  border: 1px solid var(--panel-border) !important;
  border-radius: 12px !important;
  background: var(--panel-bg) !important;
  box-shadow: var(--panel-shadow) !important;
}

.club-admin-theme :deep(.club-dashboard-card),
.club-admin-theme :deep(.todo-preview-item) {
  border: 1px solid var(--panel-border) !important;
  border-radius: 12px !important;
  background: #fff !important;
  box-shadow: none !important;
}

.club-admin-theme :deep(.club-dashboard-card:hover),
.club-admin-theme :deep(.todo-preview-item:hover) {
  transform: none !important;
  border-color: #d1d5db !important;
  box-shadow: 0 2px 8px rgba(17, 24, 39, 0.08) !important;
}

.club-admin-theme :deep(.active-card) {
  border-color: #93c5fd !important;
  background: #eff6ff !important;
}

.club-admin-theme :deep(.el-table) {
  --el-table-border-color: #e5e7eb;
  --el-table-header-bg-color: #f9fafb;
  --el-table-header-text-color: #374151;
  --el-table-row-hover-bg-color: #f5f8ff;
}

.club-admin-theme :deep(.el-table th.el-table__cell) {
  font-weight: 600;
}

.club-admin-theme :deep(.el-input__wrapper),
.club-admin-theme :deep(.el-textarea__inner),
.club-admin-theme :deep(.el-select__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d1d5db inset !important;
  background: #fff !important;
}

.club-admin-theme :deep(.el-input__wrapper.is-focus),
.club-admin-theme :deep(.el-textarea__inner:focus),
.club-admin-theme :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #60a5fa inset, 0 0 0 3px rgba(59, 130, 246, 0.16) !important;
}

.club-admin-theme :deep(.el-button) {
  text-transform: none !important;
  letter-spacing: normal !important;
  border-radius: 8px !important;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease !important;
}

.club-admin-theme :deep(.el-button:hover) {
  transform: none !important;
  box-shadow: none !important;
}

.club-admin-theme :deep(.el-button--primary) {
  background: #3b82f6 !important;
  border-color: #3b82f6 !important;
  color: #fff !important;
}

.club-admin-theme :deep(.el-button--primary:hover) {
  background: #2563eb !important;
  border-color: #2563eb !important;
}

.club-admin-theme :deep(.el-button--success) {
  background: #10b981 !important;
  border-color: #10b981 !important;
}

.club-admin-theme :deep(.el-button--danger) {
  background: #ef4444 !important;
  border-color: #ef4444 !important;
}

.club-admin-theme :deep(.el-button--warning) {
  background: #f59e0b !important;
  border-color: #f59e0b !important;
}

.club-admin-theme :deep(.el-button:not(.el-button--primary):not(.el-button--success):not(.el-button--danger):not(.el-button--warning)) {
  background: #ffffff !important;
  border-color: #d1d5db !important;
  color: #374151 !important;
}

.club-admin-theme :deep(.el-button:not(.el-button--primary):not(.el-button--success):not(.el-button--danger):not(.el-button--warning):hover) {
  background: #f9fafb !important;
  border-color: #9ca3af !important;
  color: #1f2937 !important;
}

.club-admin-theme :deep(.logout-button) {
  color: #374151 !important;
  border-color: #d1d5db !important;
  background: #fff !important;
}

.club-admin-theme :deep(.logout-button:hover),
.club-admin-theme :deep(.logout-button:focus) {
  color: #1f2937 !important;
  border-color: #9ca3af !important;
  background: #f9fafb !important;
}

.club-admin-theme :deep(.el-tag) {
  text-transform: none !important;
  letter-spacing: normal !important;
  border-radius: 8px !important;
}

.club-admin-theme :deep(.el-tag--primary) {
  background: #eff6ff !important;
  border: 1px solid #bfdbfe !important;
  color: #1d4ed8 !important;
}

.club-admin-theme :deep(.el-tag--success) {
  background: #ecfdf5 !important;
  border: 1px solid #a7f3d0 !important;
  color: #047857 !important;
}

.club-admin-theme :deep(.el-tag--warning) {
  background: #fffbeb !important;
  border: 1px solid #fde68a !important;
  color: #b45309 !important;
}

.club-admin-theme :deep(.el-tag--danger) {
  background: #fef2f2 !important;
  border: 1px solid #fecaca !important;
  color: #b91c1c !important;
}

.club-admin-theme :deep(.el-tag--info) {
  background: #f3f4f6 !important;
  border: 1px solid #e5e7eb !important;
  color: #4b5563 !important;
}

.club-admin-theme :deep(.el-dialog) {
  border-radius: 12px !important;
  border: 1px solid var(--panel-border);
}

.club-admin-theme :deep(.el-dialog__header) {
  border-bottom: 1px solid var(--panel-border) !important;
  background: #f9fafb !important;
}

.club-admin-theme :deep(.el-dialog__body) {
  background: #fff !important;
}

.club-admin-theme :deep(.balance-card) {
  border: 1px solid var(--panel-border) !important;
  background: #fff !important;
  color: var(--text-main) !important;
  box-shadow: var(--panel-shadow) !important;
}

@media (prefers-reduced-motion: reduce) {
  .club-admin-theme :deep(.el-button),
  .club-admin-theme :deep(.el-card),
  .club-admin-theme :deep(.todo-preview-item),
  .club-admin-theme :deep(.club-dashboard-card),
  .club-admin-theme :deep(.el-menu-item),
  .club-admin-theme :deep(.el-sub-menu__title) {
    transition: none !important;
  }

  .club-admin-theme :deep(.el-card:hover),
  .club-admin-theme :deep(.todo-preview-item:hover),
  .club-admin-theme :deep(.club-dashboard-card:hover),
  .club-admin-theme :deep(.el-menu-item:hover),
  .club-admin-theme :deep(.el-sub-menu__title:hover) {
    transform: none !important;
  }
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
