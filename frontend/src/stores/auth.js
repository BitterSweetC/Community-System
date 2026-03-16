import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'

function parseStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch (_) {
    localStorage.removeItem('user')
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(parseStoredUser())

  function setToken(nextToken) {
    token.value = nextToken ? 'authenticated' : ''
    if (token.value) {
      localStorage.setItem('token', token.value)
    } else {
      localStorage.removeItem('token')
    }
  }

  function setUser(newUser) {
    user.value = newUser
    if (newUser) {
      localStorage.setItem('user', JSON.stringify(newUser))
      setToken('authenticated')
    } else {
      localStorage.removeItem('user')
      setToken('')
    }
  }

  async function logout() {
    try {
      await api.post('/auth/logout')
    } catch (_) {
      // Ignore logout API failures and still clear local session.
    }
    user.value = null
    setToken('')
    localStorage.removeItem('user')
  }

  async function refreshUser() {
    try {
      const me = await api.get('/users/me')
      if (me) {
        setUser(me)
      }
    } catch (error) {
      console.error('Failed to refresh user info:', error)
    }
  }

  return { token, user, setToken, setUser, logout, refreshUser }
})
