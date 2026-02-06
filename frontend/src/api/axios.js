import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const instance = axios.create({
  baseURL: '/api',
  timeout: 5000
})

instance.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response) => {
    // Check if response is blob
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const res = response.data
    if (res.code === 200) {
      return res.data
    } else {
      // Handle business error
      console.error(res.message)
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  (error) => {
    console.error('API Error:', error)
    
    // Extract backend error message if available
    if (error.response && error.response.data && error.response.data.message) {
        error.message = error.response.data.message
    }

    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
        const authStore = useAuthStore()
        authStore.logout()
        // Avoid infinite loop if already on login page
        if (router.currentRoute.value.path !== '/login') {
            router.push({
                path: '/login',
                query: { redirect: router.currentRoute.value.fullPath }
            })
        }
    }
    return Promise.reject(error)
  }
)

export default instance
