import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', () => {
  // token 不再存储真实 JWT（已改为 HttpOnly Cookie）
  // 保留 token ref 供现有模板中的 v-if/路由守卫使用，值为 'authenticated' 或 ''
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  function setToken() {
    // HttpOnly Cookie 由后端写入，前端无需操作真实 token
    // 保留此方法避免改动调用方（LoginView、LoginDialog）
  }

  function setUser(newUser) {
    user.value = newUser
    if (newUser) {
      localStorage.setItem('user', JSON.stringify(newUser))
      // 标记"已认证"状态（非真实 JWT）
      token.value = 'authenticated'
      localStorage.setItem('token', 'authenticated')
    } else {
      localStorage.removeItem('user')
      token.value = ''
      localStorage.removeItem('token')
    }
  }

  async function logout() {
    try {
      await api.post('/auth/logout')
    } catch (_) {
      // 即使接口失败也清除本地状态
    }
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, setToken, setUser, logout }
})
