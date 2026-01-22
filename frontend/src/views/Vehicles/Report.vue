<template>
  <div class="vehicle-report-page">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">车辆问题报告</h1>
        <div class="page-actions">
          <el-button @click="$router.back()">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="loading">
            提交报告
          </el-button>
        </div>
      </div>
      
      <el-card>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="120px"
        >
          <el-form-item label="车辆信息">
            <el-input :value="vehicleInfo" disabled />
          </el-form-item>
          
          <el-form-item label="报告类型" prop="reportType">
            <el-select v-model="form.reportType" style="width: 100%">
              <el-option label="故障" value="fault" />
              <el-option label="维修需求" value="maintenance" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="严重程度" prop="severity">
            <el-select v-model="form.severity" style="width: 100%">
              <el-option label="低" value="low" />
              <el-option label="中" value="medium" />
              <el-option label="高" value="high" />
              <el-option label="紧急" value="urgent" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="报告标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入报告标题" />
          </el-form-item>
          
          <el-form-item label="问题描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="6"
              placeholder="请详细描述车辆问题..."
            />
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getVehicleApi } from '@/api/vehicles'
import { createVehicleReportApi } from '@/api/vehicles'
import type { Vehicle } from '@/api/types'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const vehicleInfo = ref('')

const form = reactive({
  vehicleId: 0,
  reportType: 'fault',
  severity: 'medium',
  title: '',
  description: ''
})

const rules: FormRules = {
  reportType: [
    { required: true, message: '请选择报告类型', trigger: 'change' }
  ],
  severity: [
    { required: true, message: '请选择严重程度', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入报告标题', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入问题描述', trigger: 'blur' }
  ]
}

// 加载车辆数据
const loadVehicleData = async () => {
  const vehicleId = route.params.id as string
  if (vehicleId) {
    try {
      loading.value = true
      const response = await getVehicleApi(Number(vehicleId))
      if (response.data.code === 200) {
        const vehicle: Vehicle = response.data.data
        form.vehicleId = vehicle.id
        vehicleInfo.value = `${vehicle.vehicleNo} - ${vehicle.brand || ''} ${vehicle.model || ''}`
      }
    } catch (error: any) {
      ElMessage.error(error?.response?.data?.message || '加载车辆数据失败')
      router.back()
    } finally {
      loading.value = false
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    loading.value = true
    
    await createVehicleReportApi(form)
    ElMessage.success('报告提交成功，已通知相关人员')
    
    router.back()
  } catch (error: any) {
    if (error !== false) {
      ElMessage.error(error?.response?.data?.message || '提交失败')
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadVehicleData()
})
</script>

<style scoped lang="scss">
.vehicle-report-page {
  .page-container {
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      
      .page-title {
        font-size: 24px;
        font-weight: 600;
        color: var(--text-primary-color);
      }
    }
  }
}
</style>
