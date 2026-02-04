<template>
  <div class="member-management">
    <h2>成员管理</h2>
    
    <div class="actions" style="margin-bottom: 20px">
      <el-button type="success" @click="exportMembers">导出成员名单</el-button>
    </div>

    <el-table :data="members" style="width: 100%" v-loading="loading">
      <el-table-column prop="user.username" label="学号" />
      <el-table-column prop="user.realName" label="姓名" />
      <el-table-column prop="roleCode" label="角色">
        <template #default="scope">
          <el-tag :type="getRoleType(scope.row.roleCode)">{{ scope.row.roleCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
         <template #default="scope">
           <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">{{ scope.row.status }}</el-tag>
         </template>
      </el-table-column>
      <el-table-column prop="joinAt" label="加入时间">
        <template #default="scope">
          {{ formatDate(scope.row.joinAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-popconfirm 
            v-if="scope.row.roleCode !== 'PRESIDENT'"
            title="确定要移除该成员吗？"
            @confirm="removeMember(scope.row.user.id)"
          >
            <template #reference>
              <el-button type="danger" size="small">移除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const clubId = route.params.clubId
const members = ref([])
const loading = ref(false)

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
    link.setAttribute('download', `Club_Members_${clubId}.xlsx`)
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
    case 'PRESIDENT': return 'danger'
    case 'VICE_PRESIDENT': return 'warning'
    case 'MANAGER': return 'primary'
    default: return 'info'
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
  padding: 20px;
}
</style>
