<template>
  <div class="finance-management">
    <div class="page-head">
      <div>
        <h2>财务管理</h2>
        <p class="subtext">查看社团收支记录并审核待处理条目。</p>
        <div v-if="clubLabel" class="club-context">
          <span class="club-name">{{ clubLabel }}</span>
          <span class="club-hint">{{ isAdmin ? '当前审批社团' : '当前社团' }}</span>
        </div>
      </div>

      <div class="head-right">
        <div class="balance-card">
          <span class="label">当前余额</span>
          <span class="amount">{{ formatAmount(balance) }}</span>
        </div>
        <el-button v-if="isClubAdmin" type="primary" @click="showCreateDialog">记录收支</el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table :data="transactions" class="table-shell" v-loading="loading">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column v-if="showClubColumn" label="社团" min-width="180">
          <template #default="scope">
            <div class="club-cell">
              <span class="club-cell-name">{{ resolveClubName(scope.row) }}</span>
              <span class="club-cell-id">ID {{ scope.row.clubId || currentClubId || '-' }}</span>
            </div>
          </template>
        </el-table-column>
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
              {{ scope.row.type === 'INCOME' ? '+' : '-' }}{{ formatAmount(scope.row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="220" />
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
        <el-table-column
          v-if="isAdmin"
          label="操作"
          min-width="180"
          align="center"
          class-name="action-column"
        >
          <template #default="scope">
            <div v-if="scope.row.status === 'PENDING'" class="action-buttons">
              <el-button type="success" size="small" @click="handleApprove(scope.row.id)">通过</el-button>
              <el-button type="danger" size="small" @click="handleReject(scope.row.id)">驳回</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        @size-change="handleSizeChange"
        @current-change="loadData"
      />
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
          <el-input v-model="form.title" placeholder="请输入简短标题" />
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
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/api/axios'
import { useAuthStore } from '@/stores/auth'
import {
  approveTransaction,
  createTransaction,
  getClubBalance,
  getClubTransactions,
  rejectTransaction
} from '@/api/finance'
import { ElMessage } from 'element-plus'

const route = useRoute()
const authStore = useAuthStore()

const currentClubId = computed(() => {
  const rawClubId = route.params.clubId
  if (rawClubId === undefined || rawClubId === null || rawClubId === '') {
    return null
  }
  const parsed = Number(rawClubId)
  return Number.isFinite(parsed) ? parsed : null
})

const balance = ref(0)
const transactions = ref([])
const loading = ref(false)
const createDialogVisible = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const currentClub = ref(null)

const createEmptyForm = () => ({
  clubId: currentClubId.value,
  type: 'EXPENSE',
  amount: 0,
  title: '',
  description: '',
  proofUrl: ''
})

const form = ref(createEmptyForm())

const isAdmin = computed(() => {
  const roles = authStore.user?.roles || []
  return roles.some((role) => (typeof role === 'string' ? role : role.code) === 'ADMIN')
})

const isClubAdmin = computed(() => {
  const roles = authStore.user?.roles || []
  return roles.some((role) => (typeof role === 'string' ? role : role.code) === 'CLUB_ADMIN')
})

const showClubColumn = computed(() => isAdmin.value)

const clubLabel = computed(() => {
  if (currentClub.value?.name) {
    return currentClub.value.name
  }
  if (currentClubId.value) {
    return `社团 #${currentClubId.value}`
  }
  return ''
})

const loadData = async () => {
  if (!currentClubId.value) {
    balance.value = 0
    total.value = 0
    transactions.value = []
    currentClub.value = null
    return
  }

  loading.value = true
  try {
    const [clubRes, balanceRes, transactionsRes] = await Promise.all([
      axios.get(`/clubs/${currentClubId.value}`).catch(() => null),
      getClubBalance(currentClubId.value),
      getClubTransactions(currentClubId.value, {
        page: currentPage.value - 1,
        size: pageSize.value
      })
    ])

    currentClub.value = clubRes
    balance.value = balanceRes ?? 0
    total.value = Number(transactionsRes?.total || 0)
    transactions.value = (transactionsRes?.list || []).map((item) => ({
      ...item,
      clubName: item.clubName || clubRes?.name || ''
    }))

    if (currentPage.value > 1 && transactions.value.length === 0 && total.value > 0) {
      currentPage.value -= 1
      await loadData()
    }
  } catch (error) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

const resetPageState = () => {
  currentPage.value = 1
  balance.value = 0
  total.value = 0
  transactions.value = []
  currentClub.value = null
  form.value = createEmptyForm()
}

const showCreateDialog = () => {
  form.value = createEmptyForm()
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
    currentPage.value = 1
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  }
}

const handleApprove = async (id) => {
  try {
    await approveTransaction(id)
    ElMessage.success('已通过')
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleReject = async (id) => {
  try {
    await rejectTransaction(id)
    ElMessage.success('已驳回')
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadData()
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
  if (!dateStr) {
    return '-'
  }
  return new Date(dateStr).toLocaleString()
}

const formatAmount = (value) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed.toFixed(2) : String(value)
}

const resolveClubName = (row) => {
  if (row?.clubName) {
    return row.clubName
  }
  if (currentClub.value?.name) {
    return currentClub.value.name
  }
  if (row?.clubId) {
    return `社团 #${row.clubId}`
  }
  if (currentClubId.value) {
    return `社团 #${currentClubId.value}`
  }
  return '-'
}

watch(
  currentClubId,
  (newClubId, oldClubId) => {
    if (newClubId === oldClubId && oldClubId !== undefined) {
      return
    }
    resetPageState()
    if (newClubId) {
      loadData()
    }
  },
  { immediate: true }
)
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

.club-context {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.club-name {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(30, 102, 170, 0.1);
  color: #1b557f;
  font-weight: 700;
}

.club-hint {
  font-size: 13px;
  color: #6c8095;
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

.club-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.4;
}

.club-cell-name {
  font-weight: 700;
  color: #14314a;
}

.club-cell-id {
  font-size: 12px;
  color: #6b7d8e;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
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
  flex-wrap: wrap;
}

.action-buttons :deep(.el-button) {
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
