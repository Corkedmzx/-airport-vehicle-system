<template>
  <div class="vehicle-detail-page">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">车辆详情</h1>
        <div class="page-actions">
          <el-button @click="$router.back()">返回</el-button>
          <el-button type="primary" @click="handleEdit">编辑车辆</el-button>
        </div>
      </div>
      
      <div v-loading="loading" class="detail-content">
        <el-row :gutter="20">
          <!-- 基本信息 -->
          <el-col :span="24">
            <el-card>
              <template #header>
                <span>基本信息</span>
              </template>
              <el-descriptions :column="3" border v-if="vehicle">
                <el-descriptions-item label="车牌号">{{ vehicle.vehicleNo }}</el-descriptions-item>
                <el-descriptions-item label="车辆状态">
                  <el-tag :type="getStatusType(vehicle.status)">
                    {{ getStatusText(vehicle.status) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="品牌">{{ vehicle.brand || '-' }}</el-descriptions-item>
                <el-descriptions-item label="型号">{{ vehicle.model || '-' }}</el-descriptions-item>
                <el-descriptions-item label="颜色">{{ vehicle.color || '-' }}</el-descriptions-item>
                <el-descriptions-item label="车辆类型ID">{{ vehicle.vehicleTypeId }}</el-descriptions-item>
                <el-descriptions-item label="发动机号">{{ vehicle.engineNo || '-' }}</el-descriptions-item>
                <el-descriptions-item label="车架号">{{ vehicle.vin || '-' }}</el-descriptions-item>
                <el-descriptions-item label="购买日期">{{ formatDate(vehicle.purchaseDate) }}</el-descriptions-item>
                <el-descriptions-item label="里程数">{{ vehicle.mileage?.toFixed(2) || '0.00' }} km</el-descriptions-item>
                <el-descriptions-item label="油箱容量">{{ vehicle.fuelCapacity || '-' }} L</el-descriptions-item>
                <el-descriptions-item label="当前油量">{{ vehicle.currentFuel || '-' }} L</el-descriptions-item>
                <el-descriptions-item label="GPS设备ID">{{ vehicle.gpsDeviceId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="当前位置" :span="3">{{ vehicle.locationAddress || '未定位' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDateTime(vehicle.createTime) }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ formatDateTime(vehicle.updateTime) }}</el-descriptions-item>
                <el-descriptions-item label="最后位置更新">{{ formatDateTime(vehicle.lastUpdateTime) || '-' }}</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getVehicleApi } from '@/api/vehicles'
import type { Vehicle } from '@/api/types'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const vehicle = ref<Vehicle | null>(null)

// 获取状态类型
const getStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    1: 'success',  // 正常
    2: 'warning',  // 维修中
    3: 'danger',   // 故障
    0: 'info'      // 停用
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    1: '正常',
    2: '维修中',
    3: '故障',
    0: '停用'
  }
  return textMap[status] || '未知'
}

// 格式化日期
const formatDate = (date?: string) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD')
}

// 格式化日期时间
const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm:ss')
}

// 编辑车辆
const handleEdit = () => {
  if (vehicle.value) {
    router.push(`/vehicles/${vehicle.value.id}/edit`)
  }
}

// 加载车辆详情
const loadVehicleDetail = async () => {
  const id = route.params.id as string
  if (!id) {
    ElMessage.error('车辆ID不存在')
    router.back()
    return
  }
  
  try {
    loading.value = true
    const response = await getVehicleApi(Number(id))
    if (response.data.code === 200) {
      vehicle.value = response.data.data
    } else {
      ElMessage.error(response.data.message || '加载车辆详情失败')
      router.back()
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '加载车辆详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadVehicleDetail()
})
</script>

<style scoped lang="scss">
.vehicle-detail-page {
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
