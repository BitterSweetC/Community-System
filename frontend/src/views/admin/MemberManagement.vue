<template>
  <div class="member-management">
    <div class="page-head">
      <div>
        <h2>成员管理</h2>
        <p class="subtext">管理成员角色与状态，并导出成员名单。</p>
      </div>
      <el-button type="success" @click="exportMembers">导出成员名单</el-button>
    </div>

    <div class="table-panel">
      <el-table :data="members" class="table-shell" v-loading="loading">
      <el-table-column prop="user.username" label="学号" min-width="140" />
      <el-table-column prop="user.realName" label="姓名" min-width="130" />
      <el-table-column prop="roleCode" label="角色" width="130">
        <template #default="scope">
          <el-tag :type="getRoleType(scope.row.roleCode)">{{ getRoleLabel(scope.row.roleCode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">{{ getStatusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="joinAt" label="加入时间" min-width="170">
        <template #default="scope">
          {{ formatDate(scope.row.joinAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" align="center">
        <template #default="scope">
          <div class="action-buttons">
            <el-button
              v-if="scope.row.roleCode !== 'PRESIDENT'"
              type="primary"
              link
              size="small"
              @click="openRoleDialog(scope.row)"
            >
              修改角色
            </el-button>
            <el-popconfirm
              v-if="scope.row.roleCode !== 'PRESIDENT'"
              title="确定要移除该成员吗？"
              @confirm="removeMember(scope.row.user.id)"
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
const roleDialogVisible = ref(false)
const updating = ref(false)
const selectedRole = ref('')
const currentMember = ref(null)
const currentMemberName = ref('')

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
    loadMembers()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '更新失败')
  } finally {
    updating.value = false
  }
}

const loadMembers = async () => {
  loading.value = true
  try {
    const res = await axios.get(`/clubs/${clubId}/members`)
    members.value = res
  } catch (error) {
    ElMessage.error('加载成员列表失败')
  } finally {
    loading.value = false
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
    ElMessage.error('导出失败')
  }
}

const removeMember = async (userId) => {
  try {
    await axios.delete(`/clubs/${clubId}/members/${userId}`)
    ElMessage.success('移除成功')
    loadMembers()
  } catch (error) {
    ElMessage.error('移除失败')
  }
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
      return '在职'
    case 'INACTIVE':
      return '未激活'
    case 'LEFT':
      return '已退出'
    default:
      return status || '-'
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

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}
</style>
