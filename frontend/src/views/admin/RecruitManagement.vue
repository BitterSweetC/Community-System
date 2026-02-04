<template>
  <div>
    <h2>招新管理</h2>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="批次管理" name="batches">
        <el-button type="primary" @click="createBatchDialog = true">新建批次</el-button>
        <el-table :data="batches" style="margin-top: 20px">
          <el-table-column prop="title" label="标题" />
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
           <el-table-column prop="firstReviewStatus" label="初审" />
           <el-table-column prop="finalReviewStatus" label="复审" />
           <el-table-column label="操作">
             <template #default="scope">
               <div v-if="scope.row.firstReviewStatus === 'PENDING'">
                 <el-button size="small" @click="review(scope.row.id, 'first', true)">初审通过</el-button>
                 <el-button size="small" type="danger" @click="review(scope.row.id, 'first', false)">驳回</el-button>
               </div>
               <div v-else-if="scope.row.firstReviewStatus === 'PASSED' && scope.row.finalReviewStatus === 'PENDING'">
                 <el-button size="small" @click="review(scope.row.id, 'final', true)">复审通过</el-button>
                 <el-button size="small" type="danger" @click="review(scope.row.id, 'final', false)">驳回</el-button>
               </div>
             </template>
           </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- Create Batch Dialog -->
    <el-dialog v-model="createBatchDialog" title="新建招新批次">
       <el-form :model="batchForm">
         <el-form-item label="标题">
           <el-input v-model="batchForm.title" />
         </el-form-item>
         <el-form-item label="开始时间">
           <el-date-picker v-model="batchForm.startTime" type="datetime" />
         </el-form-item>
         <el-form-item label="结束时间">
           <el-date-picker v-model="batchForm.endTime" type="datetime" />
         </el-form-item>
         <el-button type="primary" @click="submitBatch">提交</el-button>
       </el-form>
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
  club: { id: clubId }
})

const loadBatches = async () => {
  batches.value = await axios.get(`/recruit/batches?clubId=${clubId}`)
}

const submitBatch = async () => {
  await axios.post('/recruit/batches', batchForm.value)
  createBatchDialog.value = false
  loadBatches()
}

const loadApplications = async (batchId) => {
  currentBatchId.value = batchId
  applications.value = await axios.get(`/recruit/applications?batchId=${batchId}`)
  activeTab.value = 'applications'
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
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const review = async (id, stage, pass) => {
  const url = `/recruit/applications/${id}/${stage}-review`
  await axios.post(url, null, { params: { pass, comment: 'Reviewed' } })
  // Reload applications (hacky, ideally keep batchId in context)
  // For demo, we just remove it or reload if we knew the batchId
  const app = applications.value.find(a => a.id === id)
  if(app) {
     if(stage === 'first') app.firstReviewStatus = pass ? 'PASSED' : 'REJECTED'
     else app.finalReviewStatus = pass ? 'PASSED' : 'REJECTED'
  }
}

onMounted(loadBatches)
</script>
