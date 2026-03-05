<template>
  <div class="finance-management">
    <div class="page-head">
      <div>
        <h2>财务管理</h2>
        <p class="subtext">查看社团收支记录并审核待处理条目。</p>
      </div>

      <div class="head-right">
        <div class="balance-card">
          <span class="label">当前余额</span>
          <span class="amount">{{ balance }}</span>
        </div>
        <el-button v-if="isClubAdmin" type="primary" @click="showCreateDialog">记录收支</el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table :data="transactions" class="table-shell" v-loading="loading">
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.type === 'INCOME' ? 'success' : 'danger'">
            {{ scope.row.type === 'INCOME' ? '收入' : '支出' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="130">
        <template #default="scope">
          <span :class="scope.row.type === 'INCOME' ? 'amount-positive' : 'amount-negative'">
            {{ scope.row.type === 'INCOME' ? '+' : '-' }}{{ scope.row.amount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="description" label="描述" min-width="180" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" min-width="170">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" v-if="isAdmin" width="160" align="center">
        <template #default="scope">
          <div v-if="scope.row.status === 'PENDING'" class="action-buttons">
            <el-button type="success" size="small" @click="handleApprove(scope.row.id)">通过</el-button>
            <el-button type="danger" size="small" @click="handleReject(scope.row.id)">驳回</el-button>
          </div>
        </template>
      </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="createDialogVisible" title="记录收支" width="560px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="收入" value="INCOME" />
            <el-option label="支出" value="EXPENSE" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="form.amount" :precision="2" :step="100" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="简短描述" />
        </el-form-item>
        <el-form-item label="详情描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="凭证链接">
          <el-input v-model="form.proofUrl" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitTransaction">提交</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { approveTransaction, createTransaction, getClubBalance, getClubTransactions, rejectTransaction } from '@/api/finance'
import { ElMessage } from 'element-plus'

const route = useRoute()
const authStore = useAuthStore()
const clubId = route.params.clubId

const balance = ref(0)
const transactions = ref([])
const loading = ref(false)
const createDialogVisible = ref(false)

const form = ref({
  club: { id: clubId },
  type: 'EXPENSE',
  amount: 0,
  title: '',
  description: '',
  proofUrl: ''
})

const isAdmin = computed(() => {
  const roles = authStore.user?.roles || []
  return roles.some((r) => (typeof r === 'string' ? r : r.code) === 'ADMIN')
})

const isClubAdmin = computed(() => {
  const roles = authStore.user?.roles || []
  return roles.some((r) => (typeof r === 'string' ? r : r.code) === 'CLUB_ADMIN')
})

const loadData = async () => {
  loading.value = true
  try {
    const [balanceRes, transactionsRes] = await Promise.all([getClubBalance(clubId), getClubTransactions(clubId)])
    balance.value = balanceRes
    transactions.value = transactionsRes
  } catch (error) {
    console.error(error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  form.value = {
    club: { id: clubId },
    type: 'EXPENSE',
    amount: 0,
    title: '',
    description: '',
    proofUrl: ''
  }
  createDialogVisible.value = true
}

const submitTransaction = async () => {
  if (!form.value.title || form.value.amount <= 0) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await createTransaction(form.value)
    ElMessage.success('提交成功')
    createDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const handleApprove = async (id) => {
  try {
    await approveTransaction(id)
    ElMessage.success('已通过')
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleReject = async (id) => {
  try {
    await rejectTransaction(id)
    ElMessage.success('已驳回')
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const getStatusType = (status) => {
  switch (status) {
    case 'APPROVED':
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
    case 'APPROVED':
      return '已通过'
    case 'PENDING':
      return '待审核'
    case 'REJECTED':
      return '已驳回'
    default:
      return status
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

onMounted(() => {
  if (clubId) {
    loadData()
  }
})
</script>

<style scoped>
.finance-management {
  padding: 4px 0 8px;
}

.page-head {
  margin-bottom: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.subtext {
  margin: 6px 0 0;
  color: #60748c;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
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

.balance-card {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  padding: 10px 14px;
  border-radius: 10px;
}

.balance-card .label {
  font-weight: 600;
}

.balance-card .amount {
  font-weight: 800;
  font-size: 20px;
}

.amount-positive {
  color: #178363;
  font-weight: 700;
}

.amount-negative {
  color: #ba3d43;
  font-weight: 700;
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}
</style>
