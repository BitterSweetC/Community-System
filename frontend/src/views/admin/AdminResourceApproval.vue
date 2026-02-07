<template>
  <div class="admin-resource-approval">
    <h2>资源审批</h2>
    <el-table :data="applications" style="width: 100%" v-loading="loading">
      <el-table-column prop="club.name" label="申请社团" width="150" />
      <el-table-column label="类型" width="100">
        <template #default="scope">
          <el-tag>{{ scope.row.resource ? scope.row.resource.type : '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="资源名称">
        <template #default="scope">
          {{ scope.row.resource ? scope.row.resource.name : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="关联活动" width="150">
        <template #default="scope">
          {{ scope.row.activity ? scope.row.activity.title : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column label="使用时间" width="280">
        <template #default="scope">
          {{ formatTime(scope.row.startTime) }} - {{ formatTime(scope.row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="备注" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button type="success" size="small" @click="approve(scope.row.id)">通过</el-button>
          <el-button type="danger" size="small" @click="reject(scope.row.id)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const applications = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await axios.get('/resources/applications/pending')
    applications.value = res
  } catch (error) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

const approve = async (id) => {
  try {
    await axios.post(`/resources/applications/${id}/approve`)
    ElMessage.success('操作成功')
    load()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const reject = async (id) => {
  try {
    await axios.post(`/resources/applications/${id}/reject`)
    ElMessage.success('操作成功')
    load()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

onMounted(load)
</script>
