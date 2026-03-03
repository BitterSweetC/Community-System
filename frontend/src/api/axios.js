import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const instance = axios.create({
  baseURL: '/api',
  timeout: 5000,
  withCredentials: true  // 随请求自动携带 HttpOnly Cookie
})

instance.interceptors.request.use(
  (config) => {
    // token 现由 HttpOnly Cookie 自动携带，无需手动设置 Authorization header
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const res = response.data
    if (res.code === 200) {
      return res.data
    } else {
      console.error(res.message)
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  (error) => {
    console.error('API Error:', error)

    if (error.response && error.response.data && error.response.data.message) {
      error.message = error.response.data.message
    }

    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      const url = error.config?.url || ''
      if (!url.includes('/auth/logout') && !url.includes('/auth/login')) {
        const authStore = useAuthStore()
        authStore.logout()
        if (router.currentRoute.value.path !== '/login') {
          router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
        }
      }
    }
    return Promise.reject(error)
  }
)

export default instance
