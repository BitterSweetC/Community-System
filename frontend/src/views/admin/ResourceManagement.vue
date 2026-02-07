<template>
  <div class="resource-management">
    <div class="header">
      <h2>资源管理</h2>
      <el-button type="primary" @click="showApplyDialog">申请资源</el-button>
    </div>

    <el-table :data="applications" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="资源" min-width="150">
        <template #default="scope">
          <div>
            <div style="font-weight: bold">{{ scope.row.resource ? scope.row.resource.name : '-' }}</div>
            <div style="font-size: 12px; color: #666">
              {{ scope.row.resource ? (scope.row.resource.type === 'VENUE' ? scope.row.resource.location : '物资') : '' }}
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="scope">
          <el-tag>{{ scope.row.resource ? scope.row.resource.type : '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关联活动" width="150">
        <template #default="scope">
          {{ scope.row.activity ? scope.row.activity.title : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column label="使用时间" width="300">
        <template #default="scope">
          {{ formatTime(scope.row.startTime) }} - {{ formatTime(scope.row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="备注" />
    </el-table>

    <!-- Apply Dialog -->
    <el-dialog v-model="dialogVisible" title="申请资源" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="资源">
          <el-select v-model="form.resource.id" placeholder="请选择资源" @change="handleResourceChange">
            <el-option
              v-for="item in resources"
              :key="item.id"
              :label="item.name + (item.type === 'VENUE' ? ` (${item.location})` : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" v-if="selectedResource">
          <el-tag>{{ selectedResource.type }}</el-tag>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="form.quantity" :min="1" :max="selectedResource ? (selectedResource.type === 'VENUE' ? 1 : selectedResource.totalQuantity) : 999" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
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
import { ref, onMounted, computed } from 'vue'
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

const form = ref({
  club: { id: clubId },
  resource: { id: null },
  quantity: 1,
  description: ''
})

const selectedResource = computed(() => {
  return resources.value.find(r => r.id === form.value.resource.id)
})

const handleResourceChange = () => {
  if (selectedResource.value && selectedResource.value.type === 'VENUE') {
    form.value.quantity = 1
  }
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
    const res = await axios.get(`/resources/clubs/${clubId}/applications`)
    applications.value = res
  } catch (error) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

const showApplyDialog = () => {
  form.value = {
    club: { id: clubId },
    resource: { id: null },
    quantity: 1,
    description: ''
  }
  timeRange.value = []
  dialogVisible.value = true
}

const submitApply = async () => {
  if (!form.value.resource.id || !timeRange.value || timeRange.value.length !== 2) {
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
    load()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '申请提交失败')
  }
}

const getStatusType = (status) => {
  const map = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return map[status] || 'info'
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
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
