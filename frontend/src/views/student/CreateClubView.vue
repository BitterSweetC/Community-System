<template>
  <div class="create-club-container">
    <el-card class="application-card">
      <template #header>
        <div class="card-header">
          <h2>社团创建申请</h2>
          <p class="subtitle">填写以下信息，开启你的社团之旅</p>
        </div>
      </template>

      <el-steps :active="activeStep" finish-status="success" align-center class="mb-4">
        <el-step title="填写基本信息" />
        <el-step title="提交审核" />
        <el-step title="等待批准" />
      </el-steps>

      <div v-if="activeStep === 0">
        <el-alert
          title="申请须知"
          type="info"
          description="请确保您填写的社团名称和简介符合学校规定。申请提交后将由管理员审核，审核通过后您将自动成为社长。"
          show-icon
          class="mb-4"
          :closable="false"
        />
        
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large">
          <el-row :gutter="20">
            <el-col :span="12">
               <el-form-item label="社团名称" prop="name">
                <el-input v-model="form.name" placeholder="请输入社团名称（如：摄影协会）" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
               <el-form-item label="社团分类" prop="category">
                <el-select v-model="form.category" placeholder="请选择分类" class="w-full">
                  <el-option label="学术科技" value="学术科技" />
                  <el-option label="文化艺术" value="文化艺术" />
                  <el-option label="体育竞技" value="体育竞技" />
                  <el-option label="志愿公益" value="志愿公益" />
                  <el-option label="创新创业" value="创新创业" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="社团Logo链接" prop="logoUrl">
            <el-input v-model="form.logoUrl" placeholder="请输入图片URL（选填）">
               <template #prepend>Http://</template>
            </el-input>
          </el-form-item>

          <el-form-item label="社团简介 / 申请理由" prop="description">
            <el-input 
              type="textarea" 
              v-model="form.description" 
              :rows="6" 
              placeholder="请详细描述社团的宗旨、主要活动计划以及申请成立的理由..." 
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="submit" :loading="loading" class="submit-btn">提交申请</el-button>
            <el-button @click="$router.back()">取消</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div v-else-if="activeStep === 1" class="success-state">
        <el-result
          icon="success"
          title="申请已提交"
          sub-title="您的社团创建申请已提交成功，请耐心等待管理员审核。"
        >
          <template #extra>
            <el-button type="primary" @click="$router.push('/user/applications')">查看我的申请</el-button>
            <el-button @click="$router.push('/home')">返回首页</el-button>
          </template>
        </el-result>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const activeStep = ref(0)

const form = reactive({
  name: '',
  category: '',
  description: '',
  logoUrl: ''
})

const rules = {
  name: [
    { required: true, message: '请输入社团名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择社团分类', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入社团简介', trigger: 'blur' },
    { min: 10, message: '简介不能少于10个字符', trigger: 'blur' }
  ]
}

const submit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await axios.post('/clubs', form)
        activeStep.value = 1
        ElMessage.success('申请提交成功')
      } catch (error) {
        ElMessage.error('提交失败: ' + error.message)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.create-club-container {
  max-width: 800px;
  margin: 0 auto;
}

.application-card {
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}

.card-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: var(--color-text);
}

.subtitle {
  color: var(--color-text-light);
  margin-top: 5px;
  font-size: 0.9rem;
}

.mb-4 {
  margin-bottom: 2rem;
}

.w-full {
  width: 100%;
}

.submit-btn {
  width: 150px;
}

.success-state {
  padding: 40px 0;
}
</style>
