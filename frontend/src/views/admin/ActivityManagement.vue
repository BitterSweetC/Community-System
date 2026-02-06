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
      <el-table-column prop="location" label="地点" width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="300">
        <template #default="scope">
          <div style="display: flex; align-items: center; gap: 5px;">
            <el-button type="success" size="small" style="width: 80px" @click="exportCheckIns(scope.row.id)">导出签到</el-button>
            <el-button type="primary" size="small" style="width: 80px" @click="showResourceDialog(scope.row)">申请资源</el-button>
            <el-popconfirm 
              title="确定要删除这个活动吗？"
              @confirm="deleteActivity(scope.row.id)"
            >
              <template #reference>
                <el-button type="danger" size="small" style="width: 80px">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
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
        <el-form-item label="签到码">
           <el-input v-model="form.checkinCode" placeholder="可选：设置签到码" maxlength="20" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitActivity">发布</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Resource Dialog -->
    <el-dialog v-model="resourceDialogVisible" title="申请资源" width="500px">
      <el-form :model="resourceForm" label-width="80px">
        <el-form-item label="资源">
          <el-select v-model="resourceForm.resource.id" placeholder="请选择资源" @change="handleResourceChange">
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
          <el-input-number v-model="resourceForm.quantity" :min="1" :max="selectedResource ? (selectedResource.type === 'VENUE' ? 1 : selectedResource.totalQuantity) : 999" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="resourceTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            disabled
          />
          <div style="font-size: 12px; color: #999">时间已锁定为活动时间</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="resourceForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resourceDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitResourceApply">提交</el-button>
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
const clubId = route.params.clubId // If managing specific club
const activities = ref([])
const resources = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const resourceDialogVisible = ref(false)
const resourceTimeRange = ref([])
const resourceForm = ref({
  club: { id: null },
  activity: { id: null },
  resource: { id: null },
  quantity: 1,
  description: ''
})

const selectedResource = computed(() => {
  return resources.value.find(r => r.id === resourceForm.value.resource.id)
})

const handleResourceChange = () => {
  if (selectedResource.value && selectedResource.value.type === 'VENUE') {
    resourceForm.value.quantity = 1
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

const form = ref({
  title: '',
  description: '',
  type: 'Offline',
  startTime: '',
  endTime: '',
  location: '',
  maxParticipants: 50,
  checkinCode: '',
  clubId: clubId ? Number(clubId) : null
})

const loadActivities = async () => {
  loading.value = true
  try {
    const res = await axios.get(`/activities/club/${clubId}`)
    activities.value = res
  } catch (error) {
    ElMessage.error('获取活动列表失败')
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
      checkinCode: '',
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

const showResourceDialog = (activity) => {
  resourceForm.value = {
    club: { id: Number(clubId) },
    activity: { id: activity.id },
    type: 'VENUE',
    resourceName: '',
    quantity: 1,
    description: `为活动申请: ${activity.title}`
  }
  resourceTimeRange.value = [activity.startTime, activity.endTime]
  resourceDialogVisible.value = true
}

const submitResourceApply = async () => {
  if (!resourceForm.value.resourceName || !resourceTimeRange.value || resourceTimeRange.value.length !== 2) {
    ElMessage.warning('请填写完整信息')
    return
  }
  
  try {
    const payload = { ...resourceForm.value }
    payload.startTime = resourceTimeRange.value[0]
    payload.endTime = resourceTimeRange.value[1]
    
    await axios.post('/resources/applications', payload)
    ElMessage.success('申请提交成功')
    resourceDialogVisible.value = false
  } catch (error) {
    ElMessage.error('申请提交失败')
  }
}

const exportCheckIns = async (activityId) => {
  try {
    const res = await axios.get(`/activities/${activityId}/checkins/export`, {
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `Activity_CheckIns_${activityId}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

onMounted(loadActivities)
</script>
