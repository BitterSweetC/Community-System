<template>
  <div class="notice-management">
    <h2>公告管理</h2>
    
    <div class="actions" style="margin-bottom: 20px">
      <el-button type="primary" @click="dialogVisible = true">发布公告</el-button>
    </div>

    <el-table :data="notices" style="width: 100%" v-loading="loading">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="publishedAt" label="发布时间">
        <template #default="scope">
          {{ formatDate(scope.row.publishedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-popconfirm 
            title="确定要删除这条公告吗？"
            @confirm="deleteNotice(scope.row.id)"
          >
            <template #reference>
              <el-button type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create Notice Dialog -->
    <el-dialog v-model="dialogVisible" title="发布新公告" width="50%">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input 
            v-model="form.content" 
            type="textarea" 
            rows="6" 
            placeholder="请输入公告内容" 
          />
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="form.scope" placeholder="选择发布范围">
            <el-option label="公开 (Public)" value="PUBLIC" />
            <el-option label="仅社团成员 (Internal)" value="INTERNAL" />
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
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const clubId = route.params.clubId // If managing specific club
const notices = ref([])
const loading = ref(false)
const dialogVisible = ref(false)

const form = ref({
  title: '',
  content: '',
  scope: 'PUBLIC',
  club: clubId ? { id: clubId } : null
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
    if (clubId) payload.club = { id: clubId }
    
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
