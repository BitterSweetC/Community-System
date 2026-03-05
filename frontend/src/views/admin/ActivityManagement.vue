<template>
  <div class="activity-management">
    <div class="page-head">
      <div>
        <h2>活动管理</h2>
        <p class="subtext">管理活动生命周期、签到导出与资源申请。</p>
      </div>
      <el-button type="primary" @click="dialogVisible = true">发布活动</el-button>
    </div>

    <div class="table-panel">
      <el-table :data="activities" class="table-shell" v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="startTime" label="开始时间" min-width="170">
        <template #default="scope">
          {{ formatDate(scope.row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="endTime" label="结束时间" min-width="170">
        <template #default="scope">
          {{ formatDate(scope.row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="location" label="地点" width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="370" align="center">
        <template #default="scope">
          <div class="row-actions">
            <el-button type="primary" size="small" class="row-btn" @click="editActivity(scope.row)">编辑</el-button>
            <el-button type="success" size="small" class="row-btn" @click="exportCheckIns(scope.row.id)">导出签到</el-button>
            <el-button type="primary" size="small" class="row-btn" plain @click="showResourceDialog(scope.row)">申请资源</el-button>
            <el-popconfirm title="确定删除该活动吗？" @confirm="deleteActivity(scope.row.id)">
              <template #reference>
                <el-button type="danger" size="small" class="row-btn">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑活动' : '发布活动'" width="760px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="活动标题">
          <el-input v-model="form.title" placeholder="请输入活动标题" />
        </el-form-item>
        <el-form-item label="封面链接">
          <el-input v-model="form.coverUrl" placeholder="请输入封面链接" />
        </el-form-item>
        <el-form-item label="活动描述">
          <el-input v-model="form.description" type="textarea" rows="4" placeholder="请输入活动描述" />
        </el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="form.type" placeholder="请选择活动类型" style="width: 100%">
            <el-option label="线下活动" value="Offline" />
            <el-option label="线上活动" value="Online" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="活动地点">
          <el-input v-model="form.location" placeholder="请输入活动地点" />
        </el-form-item>
        <el-form-item label="封面图片">
          <el-upload
            class="cover-uploader"
            action="#"
            :show-file-list="false"
            :http-request="uploadCover"
            :before-upload="beforeCoverUpload"
          >
            <img v-if="form.coverUrl" :src="form.coverUrl" class="cover-preview" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议比例 16:9，支持常见图片格式，图片小于 2 兆字节</div>
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="form.maxParticipants" :min="1" />
        </el-form-item>
        <el-form-item label="签到码">
          <el-input v-model="form.checkinCode" placeholder="可选" maxlength="20" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false; resetForm()">取消</el-button>
          <el-button type="primary" @click="submitActivity">{{ isEdit ? '保存修改' : '发布' }}</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="resourceDialogVisible" title="申请活动资源" width="580px">
      <el-form :model="resourceForm" label-width="92px">
        <el-form-item label="资源">
          <el-select v-model="resourceForm.resource.id" placeholder="请选择资源" @change="handleResourceChange" style="width: 100%">
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
            v-model="resourceForm.quantity"
            :min="1"
            :max="selectedResource ? (selectedResource.type === 'VENUE' ? 1 : selectedResource.totalQuantity) : 999"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="resourceTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            disabled
            style="width: 100%"
          />
          <div class="form-tip">时间已锁定为活动时间</div>
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
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const route = useRoute()
const clubId = route.params.clubId
const activities = ref([])
const resources = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentActivityId = ref(null)
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
  return resources.value.find((r) => r.id === resourceForm.value.resource.id)
})

const handleResourceChange = () => {
  if (selectedResource.value && selectedResource.value.type === 'VENUE') {
    resourceForm.value.quantity = 1
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

const form = ref({
  title: '',
  description: '',
  type: 'Offline',
  startTime: '',
  endTime: '',
  location: '',
  maxParticipants: 50,
  checkinCode: '',
  coverUrl: '',
  clubId: clubId ? Number(clubId) : null
})

const beforeCoverUpload = (rawFile) => {
  const isImage = rawFile.type === 'image/jpeg' || rawFile.type === 'image/png'
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('上传图片格式不正确，请使用常见图片格式')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2 兆字节')
    return false
  }
  return true
}

const uploadCover = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await axios.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    form.value.coverUrl = res
    ElMessage.success('封面上传成功')
  } catch (error) {
    console.error('封面上传失败:', error)
    ElMessage.error('上传失败，请稍后重试')
  }
}

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
    if (clubId) payload.clubId = Number(clubId)

    if (isEdit.value) {
      await axios.put(`/activities/${currentActivityId.value}`, payload)
      ElMessage.success('修改成功')
    } else {
      await axios.post('/activities', payload)
      ElMessage.success('发布成功')
    }

    dialogVisible.value = false
    resetForm()
    loadActivities()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || (isEdit.value ? '修改失败' : '发布失败'))
  }
}

const editActivity = (activity) => {
  isEdit.value = true
  currentActivityId.value = activity.id
  form.value = {
    title: activity.title,
    description: activity.description,
    type: activity.type,
    startTime: activity.startTime,
    endTime: activity.endTime,
    location: activity.location,
    maxParticipants: activity.maxParticipants,
    checkinCode: activity.checkinCode,
    coverUrl: activity.coverUrl,
    clubId: clubId ? Number(clubId) : null
  }
  dialogVisible.value = true
}

const resetForm = () => {
  isEdit.value = false
  currentActivityId.value = null
  form.value = {
    title: '',
    description: '',
    type: 'Offline',
    startTime: '',
    endTime: '',
    location: '',
    maxParticipants: 50,
    checkinCode: '',
    coverUrl: '',
    clubId: clubId ? Number(clubId) : null
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
    resource: { id: null },
    quantity: 1,
    description: `为活动申请：${activity.title}`
  }
  resourceTimeRange.value = [activity.startTime, activity.endTime]
  loadResources()
  resourceDialogVisible.value = true
}

const submitResourceApply = async () => {
  if (!resourceForm.value.resource.id) {
    ElMessage.warning('请选择资源')
    return
  }
  if (!resourceTimeRange.value || resourceTimeRange.value.length !== 2) {
    ElMessage.warning('时间信息缺失')
    return
  }

  try {
    const payload = {
      ...resourceForm.value,
      clubId: Number(clubId),
      resourceId: resourceForm.value.resource.id,
      activityId: resourceForm.value.activity.id,
      startTime: resourceTimeRange.value[0],
      endTime: resourceTimeRange.value[1]
    }
    await axios.post('/resources/applications', payload)
    ElMessage.success('申请提交成功')
    resourceDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '申请提交失败')
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
    link.setAttribute('download', `活动签到_${activityId}.xlsx`)
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

onMounted(() => {
  loadActivities()
  loadResources()
})
</script>

<style scoped>
.activity-management {
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

.row-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.row-btn {
  min-width: 0;
}

:deep(.row-btn.el-button--small) {
  padding: 6px 8px;
}

.cover-uploader {
  width: 180px;
  height: 102px;
  border-radius: 10px;
  border: 1px dashed #aebfd3;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #f6faff;
}

.cover-uploader-icon {
  font-size: 26px;
  color: #6c839a;
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #7188a1;
}

.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #7188a1;
}
</style>
