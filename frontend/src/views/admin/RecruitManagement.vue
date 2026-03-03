<template>
  <div>
    <h2>招新管理</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="批次管理" name="batches">
        <el-button type="primary" @click="createBatchDialog = true">新建批次</el-button>
        <el-table :data="batches" style="margin-top: 20px">
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="startTime" label="开始时间" width="180" />
          <el-table-column prop="endTime" label="结束时间" width="180" />
          <el-table-column prop="quota" label="名额" width="80" />
          <el-table-column label="操作">
            <template #default="scope">
              <el-button @click="loadApplications(scope.row.id)">查看申请</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="申请审核" name="applications">
        <div style="margin-bottom: 20px" v-if="applications.length > 0">
          <el-button type="success" @click="exportApplications">导出报名表</el-button>
        </div>
        <el-table :data="applications">
          <el-table-column prop="user.username" label="用户" />
          <el-table-column prop="firstReviewStatus" label="初审状态" />
          <el-table-column prop="finalReviewStatus" label="复审状态" />
          <el-table-column label="操作" width="260">
            <template #default="scope">
              <div v-if="scope.row.firstReviewStatus === 'PENDING'">
                <el-button size="small" type="success" @click="openReview(scope.row.id, 'first', true)">初审通过</el-button>
                <el-button size="small" type="danger" @click="openReview(scope.row.id, 'first', false)">驳回</el-button>
              </div>
              <div v-else-if="scope.row.firstReviewStatus === 'PASSED' && scope.row.finalReviewStatus === 'PENDING'">
                <el-button size="small" type="success" @click="openReview(scope.row.id, 'final', true)">复审通过</el-button>
                <el-button size="small" type="danger" @click="openReview(scope.row.id, 'final', false)">驳回</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- Create Batch Dialog -->
    <el-dialog v-model="createBatchDialog" title="新建招新批次">
      <el-form :model="batchForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="batchForm.title" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="batchForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="batchForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="招募名额">
          <el-input-number v-model="batchForm.quota" :min="1" placeholder="不填则不限名额" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createBatchDialog = false">取消</el-button>
        <el-button type="primary" @click="submitBatch">提交</el-button>
      </template>
    </el-dialog>

    <!-- Review Comment Dialog -->
    <el-dialog v-model="reviewDialog.visible" :title="reviewDialog.pass ? '通过审核' : '驳回申请'" width="400px">
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
import { ref, onMounted } from 'vue'
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
    link.setAttribute('download', `Recruitment_Applications_${currentBatchId.value}.xlsx`)
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
    const app = applications.value.find(a => a.id === applicationId)
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
