import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue')
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
              path: 'applications',
              name: 'my-applications',
              component: () => import('@/views/student/MyApplicationsView.vue')
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
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.token) {
    next('/login')
  } else {
    // Role check logic can be added here
    next()
  }
})

export default router
