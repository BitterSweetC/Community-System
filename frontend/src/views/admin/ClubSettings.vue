<template>
  <div class="club-settings">
    <div class="page-head">
      <div>
        <h2>社团设置</h2>
        <p class="subtext">修改社团资料、分类、品牌信息和标签。</p>
      </div>
    </div>

    <el-page-header @back="$router.back()" title="返回" content="社团信息修改" />

    <el-card class="settings-card" v-loading="loading">
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="社团全称" prop="name">
              <el-input v-model="form.name" placeholder="请输入社团全称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="社团简称" prop="shortName">
              <el-input v-model="form.shortName" placeholder="用于展示的简称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="社团分类" prop="category">
              <el-select v-model="form.category" class="w-full" placeholder="请选择分类">
                <el-option label="学术科技" value="学术科技" />
                <el-option label="文化艺术" value="文化艺术" />
                <el-option label="体育竞技" value="体育竞技" />
                <el-option label="志愿公益" value="志愿公益" />
                <el-option label="创新创业" value="创新创业" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成立年份" prop="foundedYear">
              <el-input-number v-model="form.foundedYear" :min="1900" :max="2100" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="社团徽标" prop="logoUrl">
          <el-upload
            class="logo-uploader"
            action="#"
            :show-file-list="false"
            :http-request="uploadLogo"
            :before-upload="beforeLogoUpload"
          >
            <img v-if="form.logoUrl" :src="form.logoUrl" class="logo-preview" />
            <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议尺寸 200×200，支持常见图片格式，图片小于 2 兆字节</div>
        </el-form-item>

        <el-form-item label="社团简介" prop="description">
          <el-input type="textarea" v-model="form.description" :rows="5" />
        </el-form-item>

        <el-form-item label="标签" prop="tags">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入标签"
            class="w-full"
          >
            <el-option v-for="tag in availableTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" @click="submit" :loading="saving">保存修改</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const clubId = route.params.clubId
const loading = ref(false)
const saving = ref(false)
const formRef = ref(null)

const form = reactive({
  name: '',
  shortName: '',
  category: '',
  description: '',
  logoUrl: '',
  foundedYear: 2024,
  tags: []
})

const availableTags = ['技术', '运动', '艺术', '公益', '社交']

const beforeLogoUpload = (rawFile) => {
  const isImage = rawFile.type === 'image/jpeg' || rawFile.type === 'image/png'
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('上传图片格式不正确，请使用常见图片格式')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2 兆字节')
    return false
  }
  return true
}

const uploadLogo = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)

  try {
    const url = await axios.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    form.logoUrl = url
    ElMessage.success('徽标上传成功')
  } catch (error) {
    console.error('徽标上传失败:', error)
    ElMessage.error('上传失败，请稍后重试')
  }
}

const rules = {
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ required: true, message: '简介不能为空', trigger: 'blur' }]
}

const loadClubData = async () => {
  loading.value = true
  try {
    const res = await axios.get(`/clubs/${clubId}`)
    Object.assign(form, res)
    if (!form.tags) form.tags = []
  } catch (error) {
    ElMessage.error('获取社团信息失败')
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        await axios.put(`/clubs/${clubId}`, form)
        ElMessage.success('信息更新成功')
        router.back()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '更新失败')
      } finally {
        saving.value = false
      }
    }
  })
}

onMounted(loadClubData)
</script>

<style scoped>
.club-settings {
  padding: 4px 0 8px;
}

.page-head {
  margin-bottom: 10px;
}

.subtext {
  margin: 6px 0 0;
  color: #60748c;
}

.settings-card {
  margin-top: 14px;
  max-width: 920px;
}

.logo-uploader {
  border: 1px dashed #9fb4ca;
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 126px;
  height: 126px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f7fbff;
}

.logo-uploader:hover {
  border-color: #1f76b6;
}

.logo-uploader-icon {
  font-size: 28px;
  color: #6c8198;
  width: 126px;
  height: 126px;
  text-align: center;
  line-height: 126px;
}

.logo-preview {
  width: 126px;
  height: 126px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  font-size: 12px;
  color: #7188a1;
  margin-top: 8px;
}

.w-full {
  width: 100%;
}

.form-actions {
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 860px) {
  :deep(.settings-card .el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }

  .form-actions {
    justify-content: stretch;
  }
}
</style>
