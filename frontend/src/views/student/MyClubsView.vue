<template>
  <div class="profile-page">
    <section class="profile-panel">
      <div class="panel-header">
        <div>
          <p class="panel-kicker">Account</p>
          <h1>我的社团</h1>
          <p>管理你加入的社团，快速进入详情、查看身份与成员规模。</p>
        </div>
        <span class="meta-pill">已加入：{{ clubs.length }} 个社团</span>
      </div>

      <el-card class="profile-card" shadow="never">
        <div v-if="loading" class="loading-wrap" v-loading="true"></div>

        <el-empty
          v-else-if="clubs.length === 0"
          description="你还没有加入任何社团"
          class="empty-wrap"
        />

        <div v-else class="clubs-grid">
          <el-card v-for="club in clubs" :key="club.id" class="club-card" shadow="hover">
            <div class="card-header">
              <el-avatar :size="66" :src="club.logoUrl" class="club-avatar">
                {{ (club.name || '社').charAt(0) }}
              </el-avatar>
              <el-tag :type="getRoleType(club.myRole)" class="role-badge">
                {{ roleLabel(club.myRole) }}
              </el-tag>
            </div>

            <div class="card-body">
              <h3>{{ club.name }}</h3>
              <p>{{ club.category || '未分类' }}</p>
              <div class="club-stats">
                <span class="stat-item">成员：{{ club.memberCount || 0 }}</span>
                <span class="stat-item">状态：{{ club.status === 'ACTIVE' ? '活跃中' : '筹备中' }}</span>
              </div>
            </div>

            <div class="card-footer">
              <el-button type="primary" plain size="small" @click="$router.push(`/home/clubs/${club.id}`)">
                查看详情
              </el-button>
              <el-popconfirm title="确定要退出该社团吗？" @confirm="leave(club.id)">
                <template #reference>
                  <el-button size="small" type="danger" plain :disabled="club.myRole === 'PRESIDENT'">
                    退出社团
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </el-card>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const clubs = ref([])
const loading = ref(false)

const roleLabel = (role) => {
  const map = {
    PRESIDENT: '社长',
    MANAGER: '管理员',
    MEMBER: '成员'
  }
  return map[role] || role || '成员'
}

const getRoleType = (role) => {
  const map = {
    PRESIDENT: 'danger',
    MANAGER: 'warning',
    MEMBER: 'success'
  }
  return map[role] || 'info'
}

const normalizeList = (payload) => {
  if (!payload) {
    return []
  }

  if (Array.isArray(payload)) {
    return payload
  }

  if (Array.isArray(payload.list)) {
    return payload.list
  }

  if (Array.isArray(payload.content)) {
    return payload.content
  }

  return []
}

const load = async () => {
  loading.value = true
  try {
    await authStore.refreshUser()

    const userRoles = authStore.user?.roles || []
    const hasClubAdmin = userRoles.some((r) => {
      const roleCode = typeof r === 'string' ? r : r.code
      return roleCode === 'CLUB_ADMIN'
    })

    if (window.location.pathname.startsWith('/clubadmin') && !hasClubAdmin) {
      window.location.href = '/home'
      return
    }

    const res = await axios.get('/clubs/my')
    clubs.value = normalizeList(res)
  } catch (error) {
    console.error(error)
    ElMessage.error('获取社团列表失败')
  } finally {
    loading.value = false
  }
}

const leave = async (clubId) => {
  try {
    await axios.delete(`/clubs/${clubId}/members/me`)
    ElMessage.success('已退出社团')
    clubs.value = clubs.value.filter((club) => club.id !== clubId)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '退出失败')
  }
}

onMounted(load)
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.profile-page {
  --ink: #142132;
  --muted: #5b6f86;
  --border: rgba(20, 33, 50, 0.12);
  --panel-bg: rgba(255, 255, 255, 0.64);
  --card-bg: rgba(255, 255, 255, 0.9);

  min-height: auto;
  padding: 8px 0 0;
  background: transparent;
  color: var(--ink);
}

.profile-panel {
  border-radius: 18px;
  border: 1px solid var(--border);
  background: var(--panel-bg);
  backdrop-filter: blur(10px);
  box-shadow: 0 14px 28px rgba(15, 28, 42, 0.08);
  padding: 18px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 14px;
}

.panel-kicker {
  margin: 0 0 8px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-weight: 700;
  color: var(--muted);
  font-size: 0.8rem;
}

.panel-header h1 {
  margin: 0;
  font-size: clamp(1.3rem, 2.6vw, 1.85rem);
}

.panel-header p {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.55;
}

.meta-pill {
  font-size: 0.86rem;
  border-radius: 999px;
  padding: 6px 12px;
  color: #355069;
  border: 1px solid rgba(15, 28, 42, 0.12);
  background: rgba(255, 255, 255, 0.66);
  white-space: nowrap;
}

.profile-card {
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--card-bg);
  box-shadow: none;
  overflow: hidden;
}

:deep(.profile-card .el-card__body) {
  padding: 18px;
}

.loading-wrap {
  height: 220px;
}

.empty-wrap {
  border: 1px solid var(--border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.65);
  padding: 30px 0;
}

.clubs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.club-card {
  border-radius: 12px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.8);
}

.card-header {
  padding: 18px 16px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(160deg, #eef3f3 0%, #e5ecee 100%);
}

.club-avatar {
  margin-bottom: 10px;
}

.role-badge {
  font-weight: 600;
}

.card-body {
  padding: 14px 16px;
  text-align: center;
}

.card-body h3 {
  margin: 0 0 6px;
  font-size: 1.06rem;
}

.card-body p {
  margin: 0 0 12px;
  color: var(--muted);
}

.club-stats {
  display: grid;
  gap: 6px;
}

.stat-item {
  color: #4e6073;
  font-size: 0.86rem;
}

.card-footer {
  padding: 12px 16px 16px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

@media (max-width: 860px) {
  .profile-page {
    padding-top: 4px;
  }

  .profile-panel {
    border-radius: 14px;
    padding: 14px;
  }

  .panel-header {
    flex-direction: column;
    align-items: stretch;
  }

  .meta-pill {
    width: fit-content;
  }

  .clubs-grid {
    grid-template-columns: 1fr;
  }
}
</style>
