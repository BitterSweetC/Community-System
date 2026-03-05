<template>
  <div class="resource-definition">
    <div class="header">
      <h2>资源定义管理</h2>
      <el-button type="primary" @click="showAddDialog">新增资源</el-button>
    </div>
    <el-table class="resource-table" :data="resources" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="编号" width="70" />
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="type" label="类型" min-width="140">
        <template #default="scope">
          <el-tag>{{ getTypeLabel(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="location" label="地点" min-width="150"/>
      <el-table-column prop="capacity" label="容量" min-width="100" />
      <el-table-column prop="totalQuantity" label="总数量" min-width="110" />
      <el-table-column prop="status" label="状态" min-width="110">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'AVAILABLE' ? 'success' : 'danger'">
            {{ getStatusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="220" align="center" class-name="action-column">
        <template #default="scope">
          <div class="action-buttons">
            <el-button type="primary" size="small" @click="edit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="remove(scope.row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑资源' : '新增资源'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type">
            <el-option label="场地" value="VENUE" />
            <el-option label="物资" value="MATERIAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="地点" v-if="form.type === 'VENUE'">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="容量" v-if="form.type === 'VENUE'">
          <el-input-number v-model="form.capacity" :min="1" />
        </el-form-item>
        <el-form-item label="总数量" v-if="form.type === 'MATERIAL'">
          <el-input-number v-model="form.totalQuantity" :min="1" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="维护中" value="MAINTENANCE" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submit">提交</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const resources = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = ref({
  id: null,
  name: '',
  type: 'VENUE',
  location: '',
  capacity: null,
  totalQuantity: null,
  status: 'AVAILABLE',
  description: ''
})

const load = async () => {
  loading.value = true
  try {
    const res = await axios.get('/resources/admin/list')
    resources.value = res
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  form.value = {
    id: null,
    name: '',
    type: 'VENUE',
    location: '',
    capacity: null,
    totalQuantity: null,
    status: 'AVAILABLE',
    description: ''
  }
  dialogVisible.value = true
}

const edit = (row) => {
  form.value = { ...row }
  dialogVisible.value = true
}

const getTypeLabel = (type) => {
  return type === 'VENUE' ? '场地' : type === 'MATERIAL' ? '物资' : type
}

const getStatusLabel = (status) => {
  return status === 'AVAILABLE' ? '可用' : status === 'MAINTENANCE' ? '维护中' : status
}

const submit = async () => {
  try {
    if (form.value.id) {
      await axios.put('/resources/admin', form.value)
    } else {
      await axios.post('/resources/admin', form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除吗？', '提示', { type: 'warning' })
    await axios.delete(`/resources/admin/${id}`)
    ElMessage.success('删除成功')
    load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 6px;
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
