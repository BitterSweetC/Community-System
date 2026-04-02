<template>
  <div class="admin-club-management">
    <div class="page-head">
      <div>
        <h2>社团管理</h2>
        <p class="subtext">查看社团状态并处理审核、解散和财务审计。</p>
      </div>
    </div>

    <div class="table-panel">
      <div class="tab-bar">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="所有社团" name="all"></el-tab-pane>
          <el-tab-pane label="待审核" name="pending"></el-tab-pane>
          <el-tab-pane label="解散申请" name="dissolving"></el-tab-pane>
        </el-tabs>
      </div>

    <el-table :data="clubs" class="table-shell" v-loading="loading">
      <el-table-column prop="id" label="编号" width="125" />
      <el-table-column prop="name" label="名称" width="250" />
      <el-table-column prop="category" label="分类" width="180" />
      <el-table-column prop="foundedYear" label="成立年份" width="180" />
      <el-table-column prop="status" label="状态" width="180">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" header-align="center" min-width="240" class-name="action-column">
        <template #default="scope">
          <div class="action-buttons">
            <el-button
              v-if="getPrimaryAction(scope.row)"
              :type="getPrimaryAction(scope.row).type"
              size="small"
              @click="handlePrimaryAction(scope.row)"
            >
              {{ getPrimaryAction(scope.row).label }}
            </el-button>
            <el-dropdown
              v-if="getMoreActions(scope.row).length"
              trigger="click"
              @command="(command) => handleMoreAction(command, scope.row)"
            >
              <el-button size="small" class="more-btn">
                更多
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="action in getMoreActions(scope.row)"
                    :key="action.command"
                    :command="action.command"
                    :divided="action.divided"
                  >
                    {{ action.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        v-model:current-page="currentPage"
        @size-change="handleSizeChange"
        @current-change="load"
      />
    </div>

    <!-- Club Detail Dialog -->
    <el-dialog v-model="detailDialogVisible" title="社团详情" width="600px">
      <el-descriptions v-if="currentClub" :column="1" border>
        <el-descriptions-item label="编号">{{ currentClub.id }}</el-descriptions-item>
        <el-descriptions-item label="社团名称">{{ currentClub.name }}</el-descriptions-item>
        <el-descriptions-item label="社团分类">{{ currentClub.category }}</el-descriptions-item>
        <el-descriptions-item label="成立年份">{{ currentClub.foundedYear }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="getStatusType(currentClub.status)">{{ getStatusText(currentClub.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentClub.status === 'DISSOLVING' || currentClub.status === 'DISSOLVED'" label="解散原因">
          <div style="white-space: pre-wrap;">{{ currentClub.dissolutionReason || '暂无' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="社团简介">
          <div style="white-space: pre-wrap;">{{ currentClub.description || '暂无简介' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(currentClub.createdAt || currentClub.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(currentClub.updatedAt || currentClub.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const activeTab = ref('all')
const clubs = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const normalizePageData = (res) => {
  if (res?.list) {
    return { list: res.list, total: Number(res.total || 0) }
  }
  if (res?.content) {
    return { list: res.content, total: Number(res.totalElements || 0) }
  }
  if (Array.isArray(res)) {
    return { list: res, total: res.length }
  }
  return { list: [], total: 0 }
}

const detailDialogVisible = ref(false)
const currentClub = ref(null)

const showDetail = async (club) => {
  try {
    const res = await axios.get(`/clubs/${club.id}`)
    currentClub.value = res
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}


const load = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }

    if (activeTab.value === 'pending') {
      const res = await axios.get('/admin/clubs/pending', { params })
      const pageData = normalizePageData(res)
      clubs.value = pageData.list
      total.value = pageData.total
    } else if (activeTab.value === 'dissolving') {
      const res = await axios.get('/admin/clubs/dissolving', { params })
      const pageData = normalizePageData(res)
      clubs.value = pageData.list
      total.value = pageData.total
    } else {
      const res = await axios.get('/clubs', { params })
      const pageData = normalizePageData(res)
      clubs.value = pageData.list
      total.value = pageData.total
    }

    if (currentPage.value > 1 && clubs.value.length === 0 && total.value > 0) {
      currentPage.value -= 1
      await load()
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const syncTabFromQuery = () => {
  const tab = route.query.tab
  activeTab.value = ['pending', 'dissolving', 'all'].includes(tab) ? tab : 'all'
}

const handleTabChange = () => {
  currentPage.value = 1
  pageSize.value = 10
  const query = activeTab.value === 'all' ? {} : { tab: activeTab.value }
  router.replace({ query })
  load()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  load()
}

const getPrimaryAction = (club) => {
  if (club.status === 'PENDING') {
    return { label: '通过审批', type: 'success', command: 'approve' }
  }
  if (club.status === 'DISSOLVING') {
    return { label: '批准解散', type: 'success', command: 'approve-dissolution' }
  }
  if (activeTab.value !== 'dissolving') {
    return { label: '社团详情', type: 'primary', command: 'detail' }
  }
  return null
}

const getMoreActions = (club) => {
  const actions = []

  if (club.status === 'DISSOLVING') {
    actions.push({ label: '拒绝解散', command: 'reject-dissolution' })
  }

  if (activeTab.value !== 'dissolving') {
    if (club.status !== 'ACTIVE' && club.status !== 'INACTIVE') {
      actions.push({ label: '社团详情', command: 'detail' })
    }
    actions.push({ label: '财务审计', command: 'finance' })
    actions.push({ label: '删除', command: 'delete', divided: true })
  }

  return actions
}

const handlePrimaryAction = async (club) => {
  const action = getPrimaryAction(club)
  if (!action) return

  if (action.command === 'approve') {
    await approve(club.id)
    return
  }
  if (action.command === 'approve-dissolution') {
    await approveDissolution(club.id)
    return
  }
  if (action.command === 'detail') {
    await showDetail(club)
  }
}

const handleMoreAction = async (command, club) => {
  switch (command) {
    case 'reject-dissolution':
      await rejectDissolution(club.id)
      break
    case 'detail':
      await showDetail(club)
      break
    case 'finance':
      await router.push(`/admin/finance/${club.id}`)
      break
    case 'delete':
      try {
        await ElMessageBox.confirm('确定要删除该社团吗？此操作不可恢复。', '提示', {
          type: 'warning'
        })
        await remove(club.id)
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('操作失败')
        }
      }
      break
    default:
      break
  }
}

const approve = async (id) => {
  try {
    await axios.post(`/admin/clubs/${id}/approve`)
    ElMessage.success('审批通过')
    load()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const approveDissolution = async (id) => {
  try {
    await axios.post(`/admin/clubs/${id}/approve-dissolution`)
    ElMessage.success('已批准解散申请')
    load()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const rejectDissolution = async (id) => {
  try {
    await axios.post(`/admin/clubs/${id}/reject-dissolution`)
    ElMessage.success('已拒绝解散申请')
    load()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const remove = async (id) => {
  try {
    await axios.delete(`/admin/clubs/${id}`)
    ElMessage.success('删除成功')
    load()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const getStatusType = (status) => {
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'PENDING': return 'warning'
    case 'DISSOLVING': return 'warning'
    case 'DISSOLVED': return 'info'
    case 'INACTIVE': return 'info'
    default: return ''
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'ACTIVE': return '活跃'
    case 'PENDING': return '待审核'
    case 'DISSOLVING': return '解散中'
    case 'DISSOLVED': return '已解散'
    case 'INACTIVE': return '未激活'
    default: return status
  }
}

onMounted(() => {
  syncTabFromQuery()
  load()
})
</script>

<style scoped>
.admin-club-management {
  padding: 4px 0 8px;
}

.page-head {
  margin-bottom: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.subtext {
  margin: 6px 0 0;
  color: #60748c;
}

.table-panel {
  padding: 12px;
  border: 1px solid rgba(14, 55, 94, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 10px 24px rgba(17, 46, 77, 0.08);
}

.tab-bar :deep(.el-tabs__header) {
  margin: 0 0 12px;
}

.tab-bar :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(14, 55, 94, 0.08);
}

.table-shell {
  width: 100%;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
}

.action-buttons :deep(.el-button) {
  margin-left: 0 !important;
}

.more-btn {
  border-color: rgba(14, 55, 94, 0.16);
}

:deep(.action-column .cell) {
  white-space: nowrap !important;
  overflow: visible;
  line-height: 1.35;
  padding-top: 6px;
  padding-bottom: 6px;
}

</style>
