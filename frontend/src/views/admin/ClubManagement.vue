<template>
  <div class="admin-club-management">
    <div class="header">
      <h2>社团管理</h2>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="所有社团" name="all"></el-tab-pane>
        <el-tab-pane label="待审批" name="pending"></el-tab-pane>
      </el-tabs>
    </div>

    <el-table :data="clubs" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" width="200" />
      <el-table-column prop="category" label="分类" width="150" />
      <el-table-column prop="foundedYear" label="成立年份" width="120" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button 
            v-if="scope.row.status === 'PENDING'" 
            type="success" 
            size="small" 
            @click="approve(scope.row.id)"
          >
            通过审批
          </el-button>
          <el-button 
            type="primary" 
            size="small" 
            @click="$router.push(`/clubs/${scope.row.id}`)"
          >
            社团详情
          </el-button>
          <el-popconfirm 
            title="确定要删除该社团吗？此操作不可恢复。"
            @confirm="remove(scope.row.id)"
          >
            <template #reference>
              <el-button type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="activeTab === 'all' && total > 0">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const activeTab = ref('all')
const clubs = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const load = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'pending') {
      const res = await axios.get('/admin/clubs/pending')
      clubs.value = res
      total.value = res.length
    } else {
      const params = {
        page: currentPage.value - 1,
        size: pageSize.value
      }
      const res = await axios.get('/clubs', { params })
      if (res.list) {
        clubs.value = res.list
        total.value = res.total
      } else if (res.content) {
        clubs.value = res.content
        total.value = res.totalElements
      } else {
        clubs.value = res
        total.value = res.length
      }
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  load()
}

const approve = async (id) => {
  try {
    await axios.post(`/admin/clubs/${id}/approve`)
    ElMessage.success('审批通过')
    load()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const remove = async (id) => {
  try {
    await axios.delete(`/admin/clubs/${id}`)
    ElMessage.success('删除成功')
    load()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const getStatusType = (status) => {
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'PENDING': return 'warning'
    case 'INACTIVE': return 'info'
    default: return ''
  }
}

onMounted(load)
</script>

<style scoped>
.header {
  margin-bottom: 20px;
}
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
