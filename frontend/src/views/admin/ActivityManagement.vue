<template>
  <div class="activity-management">
    <h2>活动管理</h2>
    
    <div class="actions" style="margin-bottom: 20px">
      <el-button type="primary" @click="dialogVisible = true">发布活动</el-button>
    </div>

    <el-table :data="activities" style="width: 100%" v-loading="loading">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="startTime" label="开始时间">
        <template #default="scope">
          {{ formatDate(scope.row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="endTime" label="结束时间">
        <template #default="scope">
          {{ formatDate(scope.row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="location" label="地点" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-popconfirm 
            title="确定要删除这个活动吗？"
            @confirm="deleteActivity(scope.row.id)"
          >
            <template #reference>
              <el-button type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create Activity Dialog -->
    <el-dialog v-model="dialogVisible" title="发布新活动" width="50%">
      <el-form :model="form" label-width="100px">
        <el-form-item label="活动标题">
          <el-input v-model="form.title" placeholder="请输入活动标题" />
        </el-form-item>
        <el-form-item label="活动描述">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            rows="4" 
            placeholder="请输入活动描述" 
          />
        </el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="form.type" placeholder="请选择活动类型">
            <el-option label="线下活动" value="Offline" />
            <el-option label="线上活动" value="Online" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="活动地点">
          <el-input v-model="form.location" placeholder="请输入活动地点" />
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="form.maxParticipants" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitActivity">发布</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const clubId = route.params.clubId // If managing specific club
const activities = ref([])
const loading = ref(false)
const dialogVisible = ref(false)

const form = ref({
  title: '',
  description: '',
  type: 'Offline',
  startTime: '',
  endTime: '',
  location: '',
  maxParticipants: 50,
  clubId: clubId ? Number(clubId) : null
})

const loadActivities = async () => {
  loading.value = true
  try {
    const params = {}
    if (clubId) params.clubId = clubId
    const res = await axios.get('/activities', { params })
    if (res.list) activities.value = res.list
    else activities.value = res
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const submitActivity = async () => {
  if (!form.value.title || !form.value.startTime || !form.value.endTime) {
    ElMessage.warning('请填写完整信息')
    return
  }
  
  try {
    const payload = { ...form.value }
    // Ensure clubId is correct type
    if (clubId) payload.clubId = Number(clubId)
    
    await axios.post('/activities', payload)
    ElMessage.success('发布成功')
    dialogVisible.value = false
    // Reset form
    form.value = {
      title: '',
      description: '',
      type: 'Offline',
      startTime: '',
      endTime: '',
      location: '',
      maxParticipants: 50,
      clubId: clubId ? Number(clubId) : null
    }
    loadActivities()
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

const deleteActivity = async (id) => {
  try {
    await axios.delete(`/activities/${id}`)
    ElMessage.success('删除成功')
    loadActivities()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

onMounted(loadActivities)
</script>
