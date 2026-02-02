<template>
  <div class="admin-layout">
    <el-container>
      <el-aside width="200px" style="background-color: #545c64">
        <el-menu
          router
          active-text-color="#ffd04b"
          background-color="#545c64"
          text-color="#fff"
        >
          <el-menu-item index="/admin" v-if="isAdmin">仪表盘</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/clubs">社团管理</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/notices">公告管理</el-menu-item>
          
          <el-menu-item index="/clubadmin" v-if="isClubAdmin">仪表盘</el-menu-item>
          
          <template v-if="isClubAdmin">
             <el-sub-menu v-for="club in myClubs" :key="club.id" :index="'club-' + club.id">
               <template #title>
                 <span>{{ club.name }}</span>
               </template>
               <el-menu-item :index="`/clubadmin/recruit/${club.id}`">招新管理</el-menu-item>
               <el-menu-item :index="`/clubadmin/notices/${club.id}`">公告管理</el-menu-item>
               <el-menu-item :index="`/clubadmin/activities/${club.id}`">活动管理</el-menu-item>
             </el-sub-menu>
          </template>

          <el-menu-item @click="logout">退出登录</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { computed, ref, onMounted } from 'vue'
import axios from '@/api/axios'

const authStore = useAuthStore()
const router = useRouter()
const myClubs = ref([])

const isAdmin = computed(() => {
    const roles = authStore.user?.roles || []
    return roles.some(r => (typeof r === 'string' ? r : r.code) === 'ADMIN')
})

const isClubAdmin = computed(() => {
    const roles = authStore.user?.roles || []
    return roles.some(r => (typeof r === 'string' ? r : r.code) === 'CLUB_ADMIN')
})

const loadMyClubs = async () => {
    if (!isClubAdmin.value) return
    try {
        const res = await axios.get('/clubs/my')
        if (res.list) myClubs.value = res.list
        else if (Array.isArray(res)) myClubs.value = res
        else myClubs.value = res.content || []
    } catch (error) {
        console.error('Failed to load my clubs', error)
    }
}

const logout = () => {
  authStore.logout()
  router.push('/login')
}

onMounted(() => {
    loadMyClubs()
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  display: flex;
}
</style>
