<template>
  <div class="audit-log-management">
    <div class="header">
      <h2>Audit Logs</h2>
      <el-form :inline="true" :model="searchForm" class="demo-form-inline">
        <el-form-item label="Action">
          <el-input v-model="searchForm.action" placeholder="Action" clearable />
        </el-form-item>
        <el-form-item label="Resource Type">
          <el-input v-model="searchForm.resourceType" placeholder="Resource Type" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">Search</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="logs" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="Username" width="120" />
      <el-table-column prop="realName" label="Real Name" width="120" />
      <el-table-column prop="action" label="Action" width="150">
         <template #default="scope">
           <el-tag>{{ scope.row.action }}</el-tag>
         </template>
      </el-table-column>
      <el-table-column prop="resourceType" label="Resource Type" width="150" />
      <el-table-column prop="resourceId" label="Resource ID" width="120" />
      <el-table-column prop="detail" label="Detail" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="createdAt" label="Time" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAuditLogs } from '@/api/audit'
import { ElMessage } from 'element-plus'

const logs = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = ref({
  action: '',
  resourceType: ''
})

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const load = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      action: searchForm.value.action || undefined,
      resourceType: searchForm.value.resourceType || undefined
    }
    const res = await getAuditLogs(params)
    logs.value = res.list
    total.value = res.total
  } catch (error) {
    ElMessage.error('Failed to load logs')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  load()
}

onMounted(load)
</script>

<style scoped>
.header {
  margin-bottom: 20px;
}
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
