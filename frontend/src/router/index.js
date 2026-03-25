import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const CHUNK_RELOAD_KEY = 'community:chunk-reload-path'

function isChunkLoadError(error) {
  const message = error?.message || ''
  return [
    'Failed to fetch dynamically imported module',
    'Importing a module script failed',
    'Unable to preload CSS',
    'Loading chunk'
  ].some(fragment => message.includes(fragment))
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: () => import('@/views/LandingView.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue')
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('@/views/ForgotPasswordView.vue')
    },
    {
      path: '/screenshot-login',
      name: 'screenshot-login',
      component: () => import('@/views/tools/ScreenshotLoginView.vue')
    },
    {
      path: '/home',
      component: () => import('@/views/student/Layout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/student/HomeView.vue')
        },
        {
          path: 'clubs',
          name: 'club-search',
          component: () => import('@/views/student/ClubSearchView.vue')
        },
        {
          path: 'notices',
          name: 'notices',
          component: () => import('@/views/student/NoticeListView.vue')
        },
        {
          path: 'activities',
          name: 'activities',
          component: () => import('@/views/student/ActivityListView.vue')
        },
        {
          path: 'schedule',
          name: 'schedule',
          component: () => import('@/views/student/ScheduleView.vue')
        },
        {
          path: 'clubs/:id',
          name: 'club-detail',
          component: () => import('@/views/student/ClubDetailView.vue')
        }
      ]
    },
    {
      path: '/user',
      component: () => import('@/views/student/Layout.vue'), // Keep the main header/footer
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          component: () => import('@/views/UserCenter.vue'),
          children: [
            {
              path: 'profile',
              name: 'user-profile',
              component: () => import('@/views/student/UserProfileView.vue')
            },
            {
              path: 'avatar',
              name: 'user-avatar',
              component: () => import('@/views/student/UserAvatarView.vue')
            },
            {
              path: 'applications',
              name: 'my-applications',
              component: () => import('@/views/student/MyApplicationsView.vue')
            },
            {
              path: 'clubs',
              name: 'my-clubs',
              component: () => import('@/views/student/MyClubsView.vue')
            },
            {
              path: 'archive',
              name: 'my-archive',
              component: () => import('@/views/student/MyArchiveView.vue')
            },
            {
              path: 'activities',
              name: 'my-activities',
              component: () => import('@/views/student/MyActivitiesView.vue')
            },
            {
              path: 'notifications',
              name: 'notifications',
              component: () => import('@/views/student/NotificationsView.vue')
            },
            {
              path: 'create-club',
              name: 'create-club',
              component: () => import('@/views/student/CreateClubView.vue')
            }
          ]
        }
      ]
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/Layout.vue'),
      meta: { requiresAuth: true, role: 'ADMIN' }, // Simplified: ADMIN or CLUB_ADMIN
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue')
        },
        {
          path: 'realtime',
          name: 'admin-realtime',
          component: () => import('@/views/admin/RealtimeDashboard.vue')
        },
        {
          path: 'todos',
          name: 'admin-todos',
          component: () => import('@/views/admin/TodoCenterView.vue')
        },
        {
          path: 'clubs',
          name: 'admin-clubs',
          component: () => import('@/views/admin/ClubManagement.vue')
        },
        {
          path: 'recruit/:clubId',
          name: 'admin-recruit',
          component: () => import('@/views/admin/RecruitManagement.vue')
        },
        {
          path: 'notices/:clubId?', // Optional clubId for club-specific notices
          name: 'admin-notices',
          component: () => import('@/views/admin/NoticeManagement.vue')
        },
        {
          path: 'prohibited-words',
          name: 'admin-prohibited-words',
          component: () => import('@/views/admin/ProhibitedWordManagement.vue')
        },
        {
          path: 'activities/:clubId?',
          name: 'admin-activities',
          component: () => import('@/views/admin/ActivityManagement.vue')
        },
        {
          path: 'resources',
          name: 'admin-resources',
          component: () => import('@/views/admin/AdminResourceApproval.vue')
        },
        {
          path: 'resources/definitions',
          name: 'admin-resource-definitions',
          component: () => import('@/views/admin/AdminResourceDefinition.vue')
        },
        {
          path: 'audit',
          name: 'admin-audit',
          component: () => import('@/views/admin/AuditLogManagement.vue')
        },
        {
          path: 'finance/:clubId',
          name: 'admin-finance',
          component: () => import('@/views/admin/FinanceManagement.vue')
        }
      ]
    },
    {
      path: '/clubadmin',
      component: () => import('@/views/admin/Layout.vue'),
      meta: { requiresAuth: true, role: 'CLUB_ADMIN' },
      children: [
        {
          path: '',
          name: 'club-admin-dashboard',
          component: () => import('@/views/admin/ClubAdminDashboard.vue')
        },
        {
          path: 'todos',
          name: 'club-admin-todos',
          component: () => import('@/views/admin/TodoCenterView.vue')
        },
        {
          path: 'recruit/:clubId',
          name: 'club-admin-recruit',
          component: () => import('@/views/admin/RecruitManagement.vue')
        },
        {
          path: 'members/:clubId',
          name: 'club-admin-members',
          component: () => import('@/views/admin/MemberManagement.vue')
        },
        {
          path: 'notices/:clubId?',
          name: 'club-admin-notices',
          component: () => import('@/views/admin/NoticeManagement.vue')
        },
        {
          path: 'activities/:clubId?',
          name: 'club-admin-activities',
          component: () => import('@/views/admin/ActivityManagement.vue')
        },
        {
          path: 'resources/:clubId',
          name: 'club-admin-resources',
          component: () => import('@/views/admin/ResourceManagement.vue')
        },
        {
          path: 'settings/:clubId',
          name: 'club-admin-settings',
          component: () => import('@/views/admin/ClubSettings.vue')
        },
        {
          path: 'finance/:clubId',
          name: 'club-admin-finance',
          component: () => import('@/views/admin/FinanceManagement.vue')
        }
      ]
    }
  ]
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.token) {
    next('/login')
  } else {
    // 如果需要角色权限，先刷新用户信息确保角色是最新的
    if (to.meta.role && authStore.token) {
      await authStore.refreshUser()
    }

    // Role based authorization
    if (to.meta.role) {
      const userRoles = authStore.user?.roles || []
      const hasRole = userRoles.some(r => {
        const roleCode = typeof r === 'string' ? r : r.code
        return roleCode === to.meta.role
      })

      if (!hasRole) {
        // Redirect unauthorized access to appropriate page or home
        if (userRoles.some(r => (typeof r === 'string' ? r : r.code) === 'CLUB_ADMIN')) {
          if (to.path.startsWith('/admin')) {
            next('/clubadmin')
            return
          }
        }
        next('/home')
        return
      }
    }
    next()
  }
})

router.afterEach(to => {
  if (typeof window === 'undefined') {
    return
  }

  if (sessionStorage.getItem(CHUNK_RELOAD_KEY) === to.fullPath) {
    sessionStorage.removeItem(CHUNK_RELOAD_KEY)
  }
})

router.onError((error, to) => {
  if (typeof window === 'undefined' || !isChunkLoadError(error)) {
    return
  }

  const reloadPath = to?.fullPath || `${window.location.pathname}${window.location.search}${window.location.hash}`
  const previousReloadPath = sessionStorage.getItem(CHUNK_RELOAD_KEY)

  if (previousReloadPath === reloadPath) {
    sessionStorage.removeItem(CHUNK_RELOAD_KEY)
    return
  }

  sessionStorage.setItem(CHUNK_RELOAD_KEY, reloadPath)
  window.location.assign(reloadPath)
})

export default router
