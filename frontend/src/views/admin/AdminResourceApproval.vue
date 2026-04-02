<template>
  <div class="admin-resource-approval">
    <div class="page-head">
      <div>
        <h2>资源审批</h2>
        <p class="subtext">集中处理社团提交的场地与物资申请。</p>
      </div>
    </div>

    <div class="table-panel">
      <el-table :data="applications" class="table-shell" v-loading="loading">
        <el-table-column label="申请社团" width="150">
          <template #default="scope">
            {{ scope.row.clubName || scope.row.club?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="scope">
            <el-tag>{{ scope.row.resource ? getResourceTypeLabel(scope.row.resource.type) : '-' }}</el-tag>
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
        <el-table-column label="操作" width="180" align="center" class-name="action-column">
          <template #default="scope">
            <div class="action-buttons">
              <el-button type="success" size="small" @click="approve(scope.row.id)">通过</el-button>
              <el-button type="danger" size="small" @click="reject(scope.row.id)">拒绝</el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const applications = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const load = async () => {
  loading.value = true
  try {
    const res = await axios.get('/resources/applications/pending', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value
      }
    })
    applications.value = res?.list || []
    total.value = Number(res?.total || 0)
    if (currentPage.value > 1 && applications.value.length === 0 && total.value > 0) {
      currentPage.value -= 1
      await load()
    }
  } catch (error) {
    ElMessage.error(error.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

const approve = async (id) => {
  try {
    await axios.post(`/resources/applications/${id}/approve`)
    ElMessage.success('操作成功')
    await load()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const reject = async (id) => {
  try {
    await axios.post(`/resources/applications/${id}/reject`)
    ElMessage.success('操作成功')
    await load()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  load()
}

const getResourceTypeLabel = (type) => {
  return type === 'VENUE' ? '场地' : type === 'MATERIAL' ? '物资' : type
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

onMounted(load)
</script>

<style scoped>
.admin-resource-approval {
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
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex-wrap: nowrap;
}

.action-buttons :deep(.el-button) {
  margin-left: 0 !important;
}

:deep(.action-column .cell) {
  white-space: nowrap !important;
  overflow: visible;
  line-height: 1.35;
  padding-top: 6px;
  padding-bottom: 6px;
}
</style>
