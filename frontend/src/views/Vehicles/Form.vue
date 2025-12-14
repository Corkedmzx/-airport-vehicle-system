<template>
  <div class="vehicle-form-page">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">{{ isEdit ? '编辑车辆' : '添加车辆' }}</h1>
        <div class="page-actions">
          <el-button @click="$router.back()">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="loading">
            保存
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
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="车牌号" prop="vehicleNo">
                <el-input 
                  v-model="form.vehicleNo" 
                  placeholder="请输入车牌号"
                  :disabled="isEdit"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="车辆类型ID" prop="vehicleTypeId">
                <el-input-number 
                  v-model="form.vehicleTypeId" 
                  :min="1"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="品牌" prop="brand">
                <el-input v-model="form.brand" placeholder="请输入品牌" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="型号" prop="model">
                <el-input v-model="form.model" placeholder="请输入型号" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="颜色" prop="color">
                <el-input v-model="form.color" placeholder="请输入颜色" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态" prop="status">
                <el-select v-model="form.status" style="width: 100%">
                  <el-option label="正常" :value="1" />
                  <el-option label="维修中" :value="2" />
                  <el-option label="故障" :value="3" />
                  <el-option label="停用" :value="0" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="发动机号" prop="engineNo">
                <el-input v-model="form.engineNo" placeholder="请输入发动机号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="车架号" prop="vin">
                <el-input v-model="form.vin" placeholder="请输入车架号" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="购买日期" prop="purchaseDate">
                <el-date-picker
                  v-model="form.purchaseDate"
                  type="date"
                  placeholder="选择购买日期"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="里程数(km)" prop="mileage">
                <el-input-number 
                  v-model="form.mileage" 
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="油箱容量(L)" prop="fuelCapacity">
                <el-input-number 
                  v-model="form.fuelCapacity" 
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="当前油量(L)" prop="currentFuel">
                <el-input-number 
                  v-model="form.currentFuel" 
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="GPS设备ID" prop="gpsDeviceId">
                <el-input v-model="form.gpsDeviceId" placeholder="请输入GPS设备ID" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-divider content-position="left">当前位置信息</el-divider>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="经度" prop="locationLongitude">
                <el-input-number 
                  v-model="form.locationLongitude" 
                  :min="-180" 
                  :max="180"
                  :precision="6"
                  style="width: 100%"
                  placeholder="请输入经度"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="纬度" prop="locationLatitude">
                <el-input-number 
                  v-model="form.locationLatitude" 
                  :min="-90" 
                  :max="90"
                  :precision="6"
                  style="width: 100%"
                  placeholder="请输入纬度"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="位置地址" prop="locationAddress">
                <el-input 
                  v-model="form.locationAddress" 
                  placeholder="请输入位置地址"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getVehicleApi, createVehicleApi, updateVehicleApi } from '@/api/vehicles'
import type { Vehicle } from '@/api/types'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const isEdit = ref(false)

const form = reactive<Partial<Vehicle>>({
  vehicleNo: '',
  vehicleTypeId: 1,
  brand: '',
  model: '',
  color: '',
  engineNo: '',
  vin: '',
  purchaseDate: '',
  mileage: 0,
  fuelCapacity: undefined,
  currentFuel: undefined,
  gpsDeviceId: '',
  status: 1,
  locationLongitude: undefined,
  locationLatitude: undefined,
  locationAddress: ''
})

const rules: FormRules = {
  vehicleNo: [
    { required: true, message: '请输入车牌号', trigger: 'blur' }
  ],
  vehicleTypeId: [
    { required: true, message: '请选择车辆类型', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 加载车辆数据
const loadVehicleData = async () => {
  const id = route.params.id as string
  if (id && id !== 'add') {
    isEdit.value = true
    try {
      loading.value = true
      const response = await getVehicleApi(Number(id))
      if (response.data.code === 200) {
        Object.assign(form, response.data.data)
        if (form.purchaseDate) {
          form.purchaseDate = form.purchaseDate.split('T')[0]
        }
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
    
    if (isEdit.value) {
      await updateVehicleApi(Number(route.params.id), form)
      ElMessage.success('车辆更新成功')
    } else {
      await createVehicleApi(form)
      ElMessage.success('车辆创建成功')
    }
    
    router.back()
  } catch (error: any) {
    if (error !== false) {
      ElMessage.error(error?.response?.data?.message || '保存失败')
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
.vehicle-form-page {
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

