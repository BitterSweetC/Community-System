<template>
  <div class="resource-management">
    <div class="page-head">
      <div>
        <h2>资源管理</h2>
        <p class="subtext">查看资源申请记录并提交新申请。</p>
      </div>
      <el-button type="primary" @click="showApplyDialog">申请资源</el-button>
    </div>

    <div class="table-panel">
      <el-table :data="applications" class="table-shell" v-loading="loading">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column label="资源" min-width="170">
          <template #default="scope">
            <div class="resource-cell">
              <div class="resource-name">{{ scope.row.resource ? scope.row.resource.name : '-' }}</div>
              <div class="resource-meta">
                {{ scope.row.resource ? (scope.row.resource.type === 'VENUE' ? scope.row.resource.location : '物资') : '' }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="scope">
            <el-tag>{{ scope.row.resource ? getResourceTypeLabel(scope.row.resource.type) : '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联活动" min-width="150">
          <template #default="scope">
            {{ scope.row.activity ? scope.row.activity.title : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="使用时间" min-width="260">
          <template #default="scope">
            {{ formatTime(scope.row.startTime) }} - {{ formatTime(scope.row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="备注" min-width="150" />
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

    <el-dialog v-model="dialogVisible" title="申请资源" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="资源">
          <el-select v-model="form.resourceId" placeholder="请选择资源" @change="handleResourceChange" style="width: 100%">
            <el-option
              v-for="item in resources"
              :key="item.id"
              :label="item.name + (item.type === 'VENUE' ? ` (${item.location})` : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" v-if="selectedResource">
          <el-tag>{{ getResourceTypeLabel(selectedResource.type) }}</el-tag>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number
            v-model="form.quantity"
            :min="1"
            :max="selectedResource ? (selectedResource.type === 'VENUE' ? 1 : selectedResource.totalQuantity) : 999"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitApply">提交</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const clubId = route.params.clubId
const applications = ref([])
const resources = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const timeRange = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const form = ref({
  clubId,
  resourceId: null,
  quantity: 1,
  description: ''
})

const selectedResource = computed(() => {
  return resources.value.find((r) => r.id === form.value.resourceId)
})

const handleResourceChange = () => {
  if (selectedResource.value && selectedResource.value.type === 'VENUE') {
    form.value.quantity = 1
  }
}

const getResourceTypeLabel = (type) => {
  return type === 'VENUE' ? '场地' : type === 'MATERIAL' ? '物资' : type
}

const loadResources = async () => {
  try {
    const res = await axios.get('/resources/list')
    resources.value = res
  } catch (error) {
    ElMessage.error('获取资源列表失败')
  }
}

const load = async () => {
  loading.value = true
  try {
    const res = await axios.get(`/resources/clubs/${clubId}/applications`, {
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
    ElMessage.error('获取申请列表失败')
  } finally {
    loading.value = false
  }
}

const showApplyDialog = () => {
  form.value = {
    clubId,
    resourceId: null,
    quantity: 1,
    description: ''
  }
  timeRange.value = []
  dialogVisible.value = true
}

const submitApply = async () => {
  if (!form.value.resourceId || !timeRange.value || timeRange.value.length !== 2) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    const payload = { ...form.value }
    payload.startTime = timeRange.value[0]
    payload.endTime = timeRange.value[1]

    await axios.post('/resources/applications', payload)
    ElMessage.success('申请提交成功')
    dialogVisible.value = false
    currentPage.value = 1
    await load()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '申请提交失败')
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  load()
}

const getStatusType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status) => {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString()
}

onMounted(() => {
  load()
  loadResources()
})
</script>

<style scoped>
.resource-management {
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

.resource-cell {
  display: grid;
  gap: 2px;
}

.resource-name {
  font-weight: 700;
}

.resource-meta {
  font-size: 12px;
  color: #667c95;
}
</style>
