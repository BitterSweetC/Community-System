<template>
  <div class="admin-club-management">
    <div class="header">
      <h2>社团管理</h2>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="所有社团" name="all"></el-tab-pane>
        <el-tab-pane label="待审核" name="pending"></el-tab-pane>
        <el-tab-pane label="解散申请" name="dissolving"></el-tab-pane>
      </el-tabs>
    </div>

    <el-table :data="clubs" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="编号" width="125" />
      <el-table-column prop="name" label="名称" width="250" />
      <el-table-column prop="category" label="分类" width="180" />
      <el-table-column prop="foundedYear" label="成立年份" width="180" />
      <el-table-column prop="status" label="状态" width="180">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="left" header-align="left" min-width="520" class-name="action-column" label-class-name="action-column-header">
        <template #default="scope">
          <div class="action-buttons">
            <el-button
              v-if="scope.row.status === 'PENDING'"
              type="success"
              size="small"
              @click="approve(scope.row.id)"
            >
              通过审批
            </el-button>
            <el-button
              v-if="scope.row.status === 'DISSOLVING'"
              type="success"
              size="small"
              @click="approveDissolution(scope.row.id)"
            >
              批准解散
            </el-button>
            <el-button
              v-if="scope.row.status === 'DISSOLVING'"
              type="warning"
              size="small"
              @click="rejectDissolution(scope.row.id)"
            >
              拒绝解散
            </el-button>
            <el-button
              v-if="activeTab !== 'dissolving'"
              type="primary"
              size="small"
              @click="showDetail(scope.row)"
            >
              社团详情
            </el-button>
            <el-button
              v-if="activeTab !== 'dissolving'"
              type="warning"
              size="small"
              @click="router.push(`/admin/finance/${scope.row.id}`)"
            >
              财务审计
            </el-button>
            <el-popconfirm
              v-if="activeTab !== 'dissolving'"
              title="确定要删除该社团吗？此操作不可恢复。"
              @confirm="remove(scope.row.id)"
            >
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="activeTab === 'all' && total > 0">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="currentPage"
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
        <el-descriptions-item label="创建时间">{{ formatDate(currentClub.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(currentClub.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const activeTab = ref('all')
const clubs = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

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
    if (activeTab.value === 'pending') {
      const res = await axios.get('/admin/clubs/pending')
      clubs.value = res
      total.value = res.length
    } else if (activeTab.value === 'dissolving') {
      const res = await axios.get('/admin/clubs/dissolving')
      clubs.value = Array.isArray(res) ? res : (res.list || res.content || [])
      total.value = clubs.value.length
    } else {
      const params = {
        page: currentPage.value - 1,
        size: pageSize.value
      }
      const res = await axios.get('/clubs', { params })
      if (res.list) {
        clubs.value = res.list
        total.value = res.total
      } else if (res.content) {
        clubs.value = res.content
        total.value = res.totalElements
      } else {
        clubs.value = res
        total.value = res.length
      }
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  load()
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

onMounted(load)
</script>

<style scoped>
.admin-club-management {
  padding: 20px;
}

.header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.el-table {
  margin-top: 20px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.action-buttons {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.action-buttons :deep(.el-button) {
  margin-left: 0 !important;
}

:deep(.action-column .cell) {
  white-space: normal !important;
  overflow: visible;
  line-height: 1.35;
  padding-top: 6px;
  padding-bottom: 6px;
}

:deep(.action-column-header .cell) {
  text-align: center !important;
  justify-content: center;
  margin-left: -180px;
}
</style>

