<template>
  <div class="audit-log-management">
    <div class="page-head">
      <div>
        <h2>审计日志</h2>
        <p class="subtext">筛选并追踪系统关键操作记录。</p>
      </div>
      <el-form :inline="true" :model="searchForm" class="filter-form">
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

    <div class="table-panel">
      <el-table class="table-shell" :data="logs" height="640" v-loading="loading">
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
    </div>

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
  padding: 4px 0 8px;
}

.page-head {
  margin-bottom: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.subtext {
  margin: 6px 0 0;
  color: #60748c;
}

.filter-form {
  display: flex;
  align-items: center;
  gap: 8px 0;
  flex-wrap: wrap;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.table-panel {
  padding: 12px;
  border: 1px solid rgba(14, 55, 94, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 10px 24px rgba(17, 46, 77, 0.08);
}

.table-shell {
  width: 100%;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
