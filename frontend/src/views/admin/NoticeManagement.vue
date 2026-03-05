<template>
  <div class="notice-management">
    <div class="page-head">
      <div>
        <h2>公告管理</h2>
        <p class="subtext">发布并管理社团公告。</p>
      </div>
      <el-button type="primary" @click="dialogVisible = true">发布公告</el-button>
    </div>

    <div class="table-panel">
      <el-table :data="notices" class="table-shell" v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="publishedAt" label="发布时间" min-width="180">
        <template #default="scope">
          {{ formatDate(scope.row.publishedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120">
        <template #default="scope">
          <el-popconfirm title="确定删除这条公告吗？" @confirm="deleteNotice(scope.row.id)">
            <template #reference>
              <el-button type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="发布新公告" width="640px">
      <el-form :model="form" label-width="86px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" rows="7" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="form.scope" placeholder="请选择发布范围" style="width: 100%">
            <el-option label="公开" value="PUBLIC" />
            <el-option label="仅内部" value="INTERNAL" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitNotice">发布</el-button>
        </span>
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
const notices = ref([])
const loading = ref(false)
const dialogVisible = ref(false)

const form = ref({
  title: '',
  content: '',
  scope: 'PUBLIC',
  clubId: clubId ? Number(clubId) : null
})

const loadNotices = async () => {
  loading.value = true
  try {
    const params = {}
    if (clubId) params.clubId = clubId
    const res = await axios.get('/notices', { params })
    if (res.list) notices.value = res.list
    else notices.value = res
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const submitNotice = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    const payload = { ...form.value }
    if (clubId) payload.clubId = clubId

    await axios.post('/notices', payload)
    ElMessage.success('发布成功')
    dialogVisible.value = false
    form.value.title = ''
    form.value.content = ''
    loadNotices()
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

const deleteNotice = async (id) => {
  try {
    await axios.delete(`/notices/${id}`)
    ElMessage.success('删除成功')
    loadNotices()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

onMounted(loadNotices)
</script>

<style scoped>
.notice-management {
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
</style>
