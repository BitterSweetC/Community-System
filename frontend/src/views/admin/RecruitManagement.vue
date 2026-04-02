<template>
  <div class="recruit-management">
    <div class="page-head">
      <div>
        <h2>招新管理</h2>
        <p class="subtext">管理招新批次并审核报名申请。</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="recruit-tabs" @tab-change="handleTabChange">
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
                  <el-button type="primary" size="small" @click="openApplications(scope.row.id)">查看申请</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-wrapper" v-if="batchesTotal > 0">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :total="batchesTotal"
              :page-size="batchesPageSize"
              :page-sizes="[10, 20, 50]"
              v-model:current-page="batchesPage"
              @size-change="handleBatchesSizeChange"
              @current-change="loadBatches"
            />
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
              <el-table-column label="操作" min-width="260" align="center" class-name="action-column">
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

          <div class="pagination-wrapper" v-if="applicationsTotal > 0">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :total="applicationsTotal"
              :page-size="applicationsPageSize"
              :page-sizes="[10, 20, 50]"
              v-model:current-page="applicationsPage"
              @size-change="handleApplicationsSizeChange"
              @current-change="handleApplicationsPageChange"
            />
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
import { useRoute, useRouter } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const clubId = route.params.clubId
const activeTab = ref('batches')
const batches = ref([])
const applications = ref([])
const createBatchDialog = ref(false)
const currentBatchId = ref(null)

const batchesPage = ref(1)
const batchesPageSize = ref(10)
const batchesTotal = ref(0)

const applicationsPage = ref(1)
const applicationsPageSize = ref(10)
const applicationsTotal = ref(0)

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

const normalizePageData = (res) => {
  if (res?.list) return { list: res.list, total: Number(res.total || 0) }
  if (Array.isArray(res)) return { list: res, total: res.length }
  return { list: [], total: 0 }
}

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

const syncRouteState = () => {
  const batchId = route.query.batchId
  const tab = route.query.tab

  if (batchId) {
    currentBatchId.value = Number(batchId)
    activeTab.value = 'applications'
    loadApplications(Number(batchId), false)
    return
  }

  activeTab.value = tab === 'applications' ? 'applications' : 'batches'
}

const loadBatches = async () => {
  try {
    const res = await axios.get('/recruit/batches/page', {
      params: {
        clubId,
        page: batchesPage.value - 1,
        size: batchesPageSize.value
      }
    })
    const pageData = normalizePageData(res)
    batches.value = pageData.list
    batchesTotal.value = pageData.total
    if (batchesPage.value > 1 && batches.value.length === 0 && batchesTotal.value > 0) {
      batchesPage.value -= 1
      await loadBatches()
    }
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
    batchesPage.value = 1
    await loadBatches()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  }
}

const loadApplications = async (batchId, syncQuery = true) => {
  if (!batchId) return
  currentBatchId.value = Number(batchId)
  try {
    const res = await axios.get('/recruit/applications/page', {
      params: {
        batchId: currentBatchId.value,
        page: applicationsPage.value - 1,
        size: applicationsPageSize.value
      }
    })
    const pageData = normalizePageData(res)
    applications.value = pageData.list
    applicationsTotal.value = pageData.total
    if (applicationsPage.value > 1 && applications.value.length === 0 && applicationsTotal.value > 0) {
      applicationsPage.value -= 1
      await loadApplications(currentBatchId.value, syncQuery)
      return
    }
    activeTab.value = 'applications'
    if (syncQuery) {
      router.replace({ query: { tab: 'applications', batchId: currentBatchId.value } })
    }
  } catch {
    ElMessage.error('加载申请列表失败')
  }
}

const openApplications = async (batchId) => {
  applicationsPage.value = 1
  await loadApplications(batchId, true)
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

const handleTabChange = (tabName) => {
  if (tabName === 'batches') {
    batchesPage.value = 1
    router.replace({ query: {} })
    return
  }

  if (currentBatchId.value) {
    router.replace({ query: { tab: 'applications', batchId: currentBatchId.value } })
    return
  }

  router.replace({ query: { tab: 'applications' } })
}

const handleBatchesSizeChange = (size) => {
  batchesPageSize.value = size
  batchesPage.value = 1
  loadBatches()
}

const handleApplicationsPageChange = () => {
  if (currentBatchId.value) {
    loadApplications(currentBatchId.value, false)
  }
}

const handleApplicationsSizeChange = (size) => {
  applicationsPageSize.value = size
  applicationsPage.value = 1
  handleApplicationsPageChange()
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
    if (currentBatchId.value) {
      await loadApplications(currentBatchId.value, false)
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

onMounted(async () => {
  await loadBatches()
  syncRouteState()
})
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

.pagination-wrapper {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.review-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.review-actions :deep(.el-button) {
  margin-left: 0 !important;
}

:deep(.action-column .cell) {
  white-space: normal !important;
  overflow: visible;
  line-height: 1.35;
  padding-top: 6px;
  padding-bottom: 6px;
}
</style>
