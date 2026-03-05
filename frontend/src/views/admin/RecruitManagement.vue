<template>
  <div class="recruit-management">
    <div class="page-head">
      <div>
        <h2>招新管理</h2>
        <p class="subtext">管理招新批次并审核报名申请。</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="recruit-tabs">
      <el-tab-pane label="批次管理" name="batches">
        <div class="pane-inner">
          <div class="actions">
            <el-button type="primary" @click="createBatchDialog = true">新建批次</el-button>
          </div>

          <div class="table-panel">
            <el-table :data="batches" class="table-shell">
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="startTime" label="开始时间" width="180" />
            <el-table-column prop="endTime" label="结束时间" width="180" />
            <el-table-column prop="quota" label="名额" width="90" />
            <el-table-column label="操作" align="center" width="140">
              <template #default="scope">
                <el-button type="primary" size="small" @click="loadApplications(scope.row.id)">查看申请</el-button>
              </template>
            </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="申请审核" name="applications">
        <div class="pane-inner">
          <div class="actions" v-if="applications.length > 0">
            <el-button type="success" @click="exportApplications">导出名单</el-button>
          </div>

          <div class="table-panel">
            <el-table :data="applications" class="table-shell">
            <el-table-column prop="user.username" label="用户" min-width="160" />
            <el-table-column prop="firstReviewStatus" label="初审状态" width="140">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.firstReviewStatus)">
                  {{ getStatusLabel(scope.row.firstReviewStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="finalReviewStatus" label="复审状态" width="140">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.finalReviewStatus)">
                  {{ getStatusLabel(scope.row.finalReviewStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="260" align="center">
              <template #default="scope">
                <div v-if="scope.row.firstReviewStatus === 'PENDING'" class="review-actions">
                  <el-button size="small" type="success" @click="openReview(scope.row.id, 'first', true)">通过</el-button>
                  <el-button size="small" type="danger" @click="openReview(scope.row.id, 'first', false)">驳回</el-button>
                </div>
                <div
                  v-else-if="scope.row.firstReviewStatus === 'PASSED' && scope.row.finalReviewStatus === 'PENDING'"
                  class="review-actions"
                >
                  <el-button size="small" type="success" @click="openReview(scope.row.id, 'final', true)">通过</el-button>
                  <el-button size="small" type="danger" @click="openReview(scope.row.id, 'final', false)">驳回</el-button>
                </div>
              </template>
            </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="createBatchDialog" title="新建招新批次" width="520px">
      <el-form :model="batchForm" label-width="92px">
        <el-form-item label="标题">
          <el-input v-model="batchForm.title" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="batchForm.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="batchForm.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="名额">
          <el-input-number v-model="batchForm.quota" :min="1" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createBatchDialog = false">取消</el-button>
        <el-button type="primary" @click="submitBatch">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialog.visible" :title="reviewDialog.pass ? '通过审核' : '驳回申请'" width="420px">
      <el-form>
        <el-form-item label="审核意见">
          <el-input v-model="reviewDialog.comment" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialog.visible = false">取消</el-button>
        <el-button :type="reviewDialog.pass ? 'success' : 'danger'" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const clubId = route.params.clubId
const activeTab = ref('batches')
const batches = ref([])
const applications = ref([])
const createBatchDialog = ref(false)
const currentBatchId = ref(null)

const batchForm = ref({
  title: '',
  startTime: '',
  endTime: '',
  quota: null,
  club: { id: clubId }
})

const reviewDialog = ref({
  visible: false,
  applicationId: null,
  stage: '',
  pass: true,
  comment: ''
})

const getStatusType = (status) => {
  switch (status) {
    case 'PASSED':
      return 'success'
    case 'PENDING':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

const getStatusLabel = (status) => {
  switch (status) {
    case 'PASSED':
      return '已通过'
    case 'PENDING':
      return '待审核'
    case 'REJECTED':
      return '已驳回'
    default:
      return status
  }
}

const loadBatches = async () => {
  try {
    batches.value = await axios.get(`/recruit/batches?clubId=${clubId}`)
  } catch {
    ElMessage.error('加载批次失败')
  }
}

const submitBatch = async () => {
  if (!batchForm.value.title || !batchForm.value.startTime || !batchForm.value.endTime) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await axios.post('/recruit/batches', batchForm.value)
    ElMessage.success('批次创建成功')
    createBatchDialog.value = false
    batchForm.value = { title: '', startTime: '', endTime: '', quota: null, club: { id: clubId } }
    loadBatches()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  }
}

const loadApplications = async (batchId) => {
  currentBatchId.value = batchId
  try {
    applications.value = await axios.get(`/recruit/applications?batchId=${batchId}`)
    activeTab.value = 'applications'
  } catch {
    ElMessage.error('加载申请列表失败')
  }
}

const exportApplications = async () => {
  if (!currentBatchId.value) {
    ElMessage.warning('请先选择一个招新批次')
    return
  }
  try {
    const res = await axios.get(`/recruit/batches/${currentBatchId.value}/applications/export`, {
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `招新申请_${currentBatchId.value}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch {
    ElMessage.error('导出失败')
  }
}

const openReview = (id, stage, pass) => {
  reviewDialog.value = { visible: true, applicationId: id, stage, pass, comment: '' }
}

const submitReview = async () => {
  const { applicationId, stage, pass, comment } = reviewDialog.value
  if (!comment.trim()) {
    ElMessage.warning('请填写审核意见')
    return
  }
  try {
    await axios.post(`/recruit/applications/${applicationId}/${stage}-review`, null, {
      params: { pass, comment }
    })
    ElMessage.success(pass ? '审核通过' : '已驳回')
    reviewDialog.value.visible = false
    const app = applications.value.find((a) => a.id === applicationId)
    if (app) {
      if (stage === 'first') app.firstReviewStatus = pass ? 'PASSED' : 'REJECTED'
      else app.finalReviewStatus = pass ? 'PASSED' : 'REJECTED'
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

onMounted(loadBatches)
</script>

<style scoped>
.recruit-management {
  padding: 4px 0 8px;
}

.page-head {
  margin-bottom: 12px;
}

.subtext {
  margin: 6px 0 0;
  color: #60748c;
}

.recruit-tabs {
  display: grid;
  gap: 10px;
}

.pane-inner {
  display: grid;
  gap: 12px;
}

.actions {
  margin: 0;
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

.review-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
