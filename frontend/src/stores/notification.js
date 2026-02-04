import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from '@/api/axios'
import { useAuthStore } from './auth'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const authStore = useAuthStore()

  const fetchUnreadCount = async () => {
    if (!authStore.token) {
        unreadCount.value = 0
        return
    }
    try {
      const res = await axios.get('/notifications/unread-count')
      // 后端直接返回数字，或者包装在 Result 中
      // NotificationController: return Result.success(notificationService.getUnreadCount(user.getId()));
      // axios 拦截器已经解包了 Result.data (如果 code === 200)
      unreadCount.value = res
    } catch (error) {
      console.error('Failed to fetch unread notifications count:', error)
    }
  }

  const decrementCount = (amount = 1) => {
      if (unreadCount.value >= amount) {
          unreadCount.value -= amount
      } else {
          unreadCount.value = 0
      }
  }

  const clearCount = () => {
      unreadCount.value = 0
  }

  return {
    unreadCount,
    fetchUnreadCount,
    decrementCount,
    clearCount
  }
})
