<template>
  <div class="activity-management">
    <div class="page-head">
      <div>
        <h2>活动管理</h2>
        <p class="subtext">管理活动发布、资源申请，以及积分结算。</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">发布活动</el-button>
    </div>

    <div class="table-panel">
      <el-table :data="activities" class="table-shell" v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="开始时间" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="奖励" min-width="120">
          <template #default="{ row }">
            {{ `${row.rewardPoints || 0} 积分` }}
          </template>
        </el-table-column>
        <el-table-column label="结算状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getSettlementTagType(row.settlementStatus)">
              {{ getSettlementLabel(row.settlementStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="地点" width="150" show-overflow-tooltip />
        <el-table-column label="操作" min-width="220" align="center" class-name="action-column">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button type="primary" size="small" class="row-btn" @click="editActivity(row)">
                编辑
              </el-button>
              <el-dropdown
                trigger="click"
                @command="(command) => handleActivityCommand(command, row)"
              >
                <el-button size="small" class="row-btn more-btn">
                  更多
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="export">导出签到</el-dropdown-item>
                    <el-dropdown-item command="resource">申请资源</el-dropdown-item>
                    <el-dropdown-item
                      command="settle"
                      :disabled="row.status !== 'ENDED' || settlingId === row.id"
                    >
                      {{ row.settlementStatus === 'SETTLED' ? '重新结算' : '结算奖励' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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
          <el-input v-model="form.coverUrl" placeholder="可选，支持外链或上传后回填" />
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
          <div class="upload-tip">建议比例 16:9，图片大小不超过 2MB。</div>
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="form.maxParticipants" :min="1" />
        </el-form-item>
        <el-form-item label="签到码">
          <el-input v-model="form.checkinCode" maxlength="20" show-word-limit placeholder="可选" />
        </el-form-item>
        <el-form-item label="签到要求">
          <el-switch v-model="form.needAttendance" />
          <span class="inline-tip">开启后，仅签到成员参与积分结算。</span>
        </el-form-item>
        <el-form-item label="奖励积分">
          <el-input-number v-model="form.rewardPoints" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeDialog">取消</el-button>
          <el-button type="primary" @click="submitActivity">
            {{ isEdit ? '保存修改' : '发布' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="resourceDialogVisible" title="申请活动资源" width="580px">
      <el-form :model="resourceForm" label-width="92px">
        <el-form-item label="资源">
          <el-select
            v-model="resourceForm.resource.id"
            placeholder="请选择资源"
            @change="handleResourceChange"
            style="width: 100%"
          >
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
          <div class="form-tip">时间已锁定为活动时间。</div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Plus } from '@element-plus/icons-vue'

const route = useRoute()
const clubId = route.params.clubId

const activities = ref([])
const resources = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentActivityId = ref(null)
const settlingId = ref(null)

const resourceDialogVisible = ref(false)
const resourceTimeRange = ref([])
const resourceForm = ref({
  club: { id: null },
  activity: { id: null },
  resource: { id: null },
  quantity: 1,
  description: ''
})

const form = ref(createDefaultForm())

const selectedResource = computed(() => {
  return resources.value.find((item) => item.id === resourceForm.value.resource.id)
})

function createDefaultForm() {
  return {
    title: '',
    description: '',
    type: 'Offline',
    startTime: '',
    endTime: '',
    location: '',
    maxParticipants: 50,
    checkinCode: '',
    coverUrl: '',
    needAttendance: false,
    rewardPoints: 0,
    clubId: clubId ? Number(clubId) : null
  }
}

const loadActivities = async () => {
  loading.value = true
  try {
    activities.value = await axios.get(`/activities/club/${clubId}`)
  } catch (error) {
    ElMessage.error(error.message || '获取活动列表失败')
  } finally {
    loading.value = false
  }
}

const loadResources = async () => {
  try {
    resources.value = await axios.get('/resources/list')
  } catch (error) {
    ElMessage.error(error.message || '获取资源列表失败')
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  currentActivityId.value = null
  form.value = createDefaultForm()
  dialogVisible.value = true
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
    checkinCode: activity.checkinCode || '',
    coverUrl: activity.coverUrl,
    needAttendance: Boolean(activity.needAttendance),
    rewardPoints: Number(activity.rewardPoints ?? 0),
    clubId: clubId ? Number(clubId) : null
  }
  dialogVisible.value = true
}

const closeDialog = () => {
  dialogVisible.value = false
  form.value = createDefaultForm()
}

const submitActivity = async () => {
  if (!form.value.title || !form.value.startTime || !form.value.endTime) {
    ElMessage.warning('请填写完整活动信息')
    return
  }

  try {
    const payload = {
      ...form.value,
      clubId: clubId ? Number(clubId) : form.value.clubId
    }

    if (isEdit.value) {
      await axios.put(`/activities/${currentActivityId.value}`, payload)
      ElMessage.success('活动修改成功')
    } else {
      await axios.post('/activities', payload)
      ElMessage.success('活动发布成功')
    }

    closeDialog()
    loadActivities()
  } catch (error) {
    ElMessage.error(error.message || (isEdit.value ? '活动修改失败' : '活动发布失败'))
  }
}

const settleRewards = async (activity) => {
  settlingId.value = activity.id
  try {
    const res = await axios.post(`/activities/${activity.id}/settle-rewards`)
    ElMessage.success(`结算完成，${res.beneficiaryCount || 0} 人，发放 ${res.grantedPointsTotal || 0} 积分`)
    loadActivities()
  } catch (error) {
    ElMessage.error(error.message || '奖励结算失败')
  } finally {
    settlingId.value = null
  }
}

const handleActivityCommand = async (command, activity) => {
  switch (command) {
    case 'export':
      await exportCheckIns(activity.id)
      break
    case 'resource':
      showResourceDialog(activity)
      break
    case 'settle':
      if (activity.status === 'ENDED' && settlingId.value !== activity.id) {
        await settleRewards(activity)
      }
      break
    case 'delete':
      try {
        await ElMessageBox.confirm('确定删除该活动吗？', '提示', { type: 'warning' })
        await deleteActivity(activity.id)
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('操作失败')
        }
      }
      break
    default:
      break
  }
}

const deleteActivity = async (id) => {
  try {
    await axios.delete(`/activities/${id}`)
    ElMessage.success('活动删除成功')
    loadActivities()
  } catch (error) {
    ElMessage.error(error.message || '活动删除失败')
  }
}

const beforeCoverUpload = (rawFile) => {
  const isImage = rawFile.type === 'image/jpeg' || rawFile.type === 'image/png'
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('请上传 JPG 或 PNG 图片')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

const uploadCover = async ({ file }) => {
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
    ElMessage.error(error.message || '封面上传失败')
  }
}

const showResourceDialog = (activity) => {
  resourceForm.value = {
    club: { id: Number(clubId) },
    activity: { id: activity.id },
    resource: { id: null },
    quantity: 1,
    description: `活动资源申请：${activity.title}`
  }
  resourceTimeRange.value = [activity.startTime, activity.endTime]
  loadResources()
  resourceDialogVisible.value = true
}

const handleResourceChange = () => {
  if (selectedResource.value?.type === 'VENUE') {
    resourceForm.value.quantity = 1
  }
}

const submitResourceApply = async () => {
  if (!resourceForm.value.resource.id) {
    ElMessage.warning('请选择资源')
    return
  }
  if (!resourceTimeRange.value || resourceTimeRange.value.length !== 2) {
    ElMessage.warning('活动时间缺失')
    return
  }

  try {
    await axios.post('/resources/applications', {
      ...resourceForm.value,
      clubId: Number(clubId),
      resourceId: resourceForm.value.resource.id,
      activityId: resourceForm.value.activity.id,
      startTime: resourceTimeRange.value[0],
      endTime: resourceTimeRange.value[1]
    })
    ElMessage.success('资源申请提交成功')
    resourceDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '资源申请提交失败')
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
    ElMessage.error(error.message || '签到导出失败')
  }
}

const getResourceTypeLabel = (type) => {
  if (type === 'VENUE') return '场地'
  if (type === 'MATERIAL') return '物资'
  return type
}

const getSettlementLabel = (status) => {
  switch (status) {
    case 'SETTLED':
      return '已结算'
    case 'PENDING':
      return '待结算'
    default:
      return status || '-'
  }
}

const getSettlementTagType = (status) => {
  return status === 'SETTLED' ? 'success' : 'warning'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
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
  gap: 6px;
  flex-wrap: nowrap;
}

.row-actions :deep(.el-button) {
  margin-left: 0 !important;
}

:deep(.action-column .cell) {
  white-space: normal !important;
  overflow: visible;
  line-height: 1.35;
  padding-top: 6px;
  padding-bottom: 6px;
}

.row-btn {
  min-width: 0;
}

.more-btn {
  border-color: rgba(14, 55, 94, 0.16);
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

.upload-tip,
.form-tip,
.inline-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #7188a1;
}

.inline-tip {
  margin-left: 10px;
}
</style>
