<template>
  <div class="audit-log-management">
    <div class="header">
      <h2>审计日志</h2>
      <el-form :inline="true" :model="searchForm" class="demo-form-inline">
        <el-form-item label="操作行为">
          <el-input v-model="searchForm.action" placeholder="输入操作行为" clearable />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-input v-model="searchForm.resourceType" placeholder="输入资源类型" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table class="audit-table" :data="logs" style="width: 100%" height="640" v-loading="loading">
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="真实姓名" width="120" />
      <el-table-column prop="action" label="操作行为" width="150">
         <template #default="scope">
           <el-tag>{{ scope.row.action }}</el-tag>
         </template>
      </el-table-column>
      <el-table-column prop="resourceType" label="资源类型" width="150" />
      <el-table-column prop="resourceId" label="资源编号" width="120" />
      <el-table-column prop="detail" label="详情" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP地址" width="140" />
      <el-table-column prop="createdAt" label="时间" width="180">
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
const pageSize = ref(15)
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
    ElMessage.error('加载日志失败')
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
.audit-log-management {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.audit-table {
  margin-top: 8px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
