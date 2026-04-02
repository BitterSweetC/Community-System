<template>
  <div class="member-management">
    <div class="page-head">
      <div>
        <h2>成员管理</h2>
        <p class="subtext">管理成员角色，并查看积分档案。</p>
      </div>
      <el-button type="success" @click="exportMembers">导出成员名单</el-button>
    </div>

    <div class="table-panel">
      <el-table :data="members" class="table-shell" v-loading="loading">
        <el-table-column prop="user.username" label="学号" min-width="140" />
        <el-table-column prop="user.realName" label="姓名" min-width="130" />
        <el-table-column prop="roleCode" label="角色" width="130">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.roleCode)">{{ getRoleLabel(row.roleCode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="joinAt" label="加入时间" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.joinAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280" align="center" class-name="action-column">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="openArchive(row)">
                档案
              </el-button>
              <el-button
                v-if="row.roleCode !== 'PRESIDENT'"
                type="primary"
                link
                size="small"
                @click="openRoleDialog(row)"
              >
                修改角色
              </el-button>
              <el-popconfirm
                v-if="row.roleCode !== 'PRESIDENT'"
                title="确定要移除该成员吗？"
                @confirm="removeMember(row.user.id)"
              >
                <template #reference>
                  <el-button type="danger" size="small">移除</el-button>
                </template>
              </el-popconfirm>
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
        @current-change="loadMembers"
      />
    </div>

    <el-dialog v-model="roleDialogVisible" title="修改成员角色" width="460px">
      <el-form label-width="104px">
        <el-form-item label="成员">
          <el-input v-model="currentMemberName" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRole" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="MANAGER" />
            <el-option label="成员" value="MEMBER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="roleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateMemberRole" :loading="updating">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-drawer
      v-model="archiveDrawerVisible"
      title="成员档案"
      size="720px"
      destroy-on-close
    >
      <div v-loading="archiveLoading" class="archive-wrap">
        <div class="archive-head">
          <div>
            <div class="archive-name">{{ archiveSummary.realName || archiveSummary.username || '-' }}</div>
            <div class="archive-meta">
              {{ archiveSummary.username || '-' }} · {{ getRoleLabel(archiveSummary.roleCode) }}
            </div>
          </div>
          <el-tag :type="archiveSummary.status === 'ACTIVE' ? 'success' : 'info'">
            {{ getStatusLabel(archiveSummary.status) }}
          </el-tag>
        </div>

        <div class="summary-grid">
          <div class="summary-card">
            <div class="summary-label">当前积分</div>
            <div class="summary-value">{{ archiveSummary.totalPoints ?? 0 }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">本月积分变动</div>
            <div class="summary-value">{{ archiveSummary.monthlyPoints ?? 0 }}</div>
          </div>
        </div>

        <el-descriptions :column="2" border class="archive-desc">
          <el-descriptions-item label="加入时间">
            {{ formatDate(archiveSummary.joinAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="成员状态">
            {{ getStatusLabel(archiveSummary.status) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-tabs class="archive-tabs">
          <el-tab-pane label="积分流水">
            <el-table :data="pointRecords" size="small" empty-text="暂无积分流水">
              <el-table-column label="变动" width="100">
                <template #default="{ row }">
                  <span :class="row.deltaPoints >= 0 ? 'positive-text' : 'negative-text'">
                    {{ row.deltaPoints > 0 ? `+${row.deltaPoints}` : row.deltaPoints }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="balanceAfter" label="结余" width="90" />
              <el-table-column label="来源" min-width="140">
                <template #default="{ row }">
                  {{ getSourceLabel(row.sourceType) }}
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作人" width="120">
                <template #default="{ row }">
                  {{ row.operatorName || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="时间" min-width="170">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const clubId = route.params.clubId

const members = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const roleDialogVisible = ref(false)
const updating = ref(false)
const selectedRole = ref('')
const currentMember = ref(null)
const currentMemberName = ref('')

const archiveDrawerVisible = ref(false)
const archiveLoading = ref(false)
const archiveSummary = ref({})
const pointRecords = ref([])

const loadMembers = async () => {
  loading.value = true
  try {
    const res = await axios.get(`/clubs/${clubId}/members`, {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value
      }
    })
    members.value = res?.list || []
    total.value = Number(res?.total || 0)

    if (currentPage.value > 1 && members.value.length === 0 && total.value > 0) {
      currentPage.value -= 1
      await loadMembers()
    }
  } catch (error) {
    ElMessage.error(error.message || '加载成员列表失败')
  } finally {
    loading.value = false
  }
}

const openRoleDialog = (member) => {
  currentMember.value = member
  currentMemberName.value = member.user.realName || member.user.username
  selectedRole.value = member.roleCode
  roleDialogVisible.value = true
}

const updateMemberRole = async () => {
  if (!currentMember.value || !selectedRole.value) return

  updating.value = true
  try {
    await axios.put(`/clubs/${clubId}/members/${currentMember.value.user.id}/role`, null, {
      params: { role: selectedRole.value }
    })
    ElMessage.success('角色更新成功')
    roleDialogVisible.value = false
    await loadMembers()
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    updating.value = false
  }
}

const openArchive = async (member) => {
  archiveDrawerVisible.value = true
  archiveLoading.value = true
  currentMember.value = member
  try {
    const [summary, points] = await Promise.all([
      axios.get(`/clubs/${clubId}/members/${member.user.id}/archive`),
      axios.get(`/clubs/${clubId}/members/${member.user.id}/point-records`)
    ])
    archiveSummary.value = summary || {}
    pointRecords.value = points || []
  } catch (error) {
    archiveSummary.value = {}
    pointRecords.value = []
    ElMessage.error(error.message || '加载成员档案失败')
  } finally {
    archiveLoading.value = false
  }
}

const exportMembers = async () => {
  try {
    const res = await axios.get(`/clubs/${clubId}/members/export`, {
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `社团成员_${clubId}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
}

const removeMember = async (userId) => {
  try {
    await axios.delete(`/clubs/${clubId}/members/${userId}`)
    ElMessage.success('移除成功')
    await loadMembers()
  } catch (error) {
    ElMessage.error(error.message || '移除失败')
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadMembers()
}

const getRoleType = (role) => {
  switch (role) {
    case 'PRESIDENT':
      return 'danger'
    case 'VICE_PRESIDENT':
      return 'warning'
    case 'MANAGER':
      return 'primary'
    default:
      return 'info'
  }
}

const getRoleLabel = (role) => {
  switch (role) {
    case 'PRESIDENT':
      return '会长'
    case 'VICE_PRESIDENT':
      return '副会长'
    case 'MANAGER':
      return '管理员'
    case 'MEMBER':
      return '成员'
    default:
      return role || '-'
  }
}

const getStatusLabel = (status) => {
  switch (status) {
    case 'ACTIVE':
      return '在会'
    case 'INACTIVE':
      return '未激活'
    case 'LEFT':
      return '已退出'
    default:
      return status || '-'
  }
}

const getSourceLabel = (sourceType) => {
  switch (sourceType) {
    case 'ACTIVITY_ATTEND':
      return '活动结算'
    case 'MANUAL_ADD':
      return '手动增加'
    case 'MANUAL_DEDUCT':
      return '手动扣减'
    default:
      return sourceType || '-'
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

onMounted(loadMembers)
</script>

<style scoped>
.member-management {
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

.table-shell {
  width: 100%;
}

.table-panel {
  padding: 12px;
  border: 1px solid rgba(14, 55, 94, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 10px 24px rgba(17, 46, 77, 0.08);
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
  gap: 8px;
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

.archive-wrap {
  padding-right: 8px;
}

.archive-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.archive-name {
  font-size: 18px;
  font-weight: 600;
  color: #183b56;
}

.archive-meta {
  margin-top: 4px;
  color: #61788f;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  padding: 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, #f7fbff 0%, #eff6ff 100%);
  border: 1px solid rgba(14, 55, 94, 0.08);
}

.summary-label {
  color: #60748c;
  font-size: 13px;
}

.summary-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #183b56;
}

.archive-desc {
  margin-bottom: 16px;
}

.archive-tabs {
  margin-top: 8px;
}

.positive-text {
  color: #1f9d55;
  font-weight: 600;
}

.negative-text {
  color: #d14343;
  font-weight: 600;
}
</style>
