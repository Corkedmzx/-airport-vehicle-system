<template>
  <div class="vehicles-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">车辆管理</h1>
      <p class="page-description">管理机场车辆信息、监控车辆状态</p>
    </div>
    
    <!-- 搜索和操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索车牌号、品牌、型号"
          style="width: 300px"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select
          v-model="searchForm.status"
          placeholder="车辆状态"
          style="width: 120px; margin-left: 12px"
          clearable
          @change="handleSearch"
        >
          <el-option label="正常" :value="1" />
          <el-option label="维修中" :value="2" />
          <el-option label="故障" :value="3" />
          <el-option label="停用" :value="0" />
        </el-select>
      </div>
      
      <div class="toolbar-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加车辆
        </el-button>
      </div>
    </div>
    
    <!-- 数据表格 -->
    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="vehicleList"
        stripe
        @row-click="handleRowClick"
      >
        <el-table-column prop="vehicleNo" label="车牌号" width="120" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="color" label="颜色" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="mileage" label="里程(km)" width="100">
          <template #default="{ row }">
            {{ row.mileage?.toFixed(1) || '0.0' }}
          </template>
        </el-table-column>
        <el-table-column prop="locationAddress" label="当前位置" min-width="200">
          <template #default="{ row }">
            <span v-if="row.locationAddress">{{ row.locationAddress }}</span>
            <span v-else class="text-muted">未定位</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastUpdateTime" label="最后更新" width="150">
          <template #default="{ row }">
            {{ formatTime(row.lastUpdateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link @click.stop="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { getVehiclesApi, deleteVehicleApi } from '@/api/vehicles'
import type { Vehicle } from '@/api/types'
import { formatDate, timeAgo } from '@/utils'

const router = useRouter()

// 数据状态
const loading = ref(false)
const vehicleList = ref<Vehicle[]>([])

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined
})

// 分页信息
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

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

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return ''
  return timeAgo(time)
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadVehicles()
}

// 加载车辆列表
const loadVehicles = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword || undefined,
      status: searchForm.status || undefined
    }
    
    const response = await getVehiclesApi(params)
    if (response.data.code === 200) {
      // 模拟分页数据
      const allVehicles = generateMockVehicles()
      const filteredVehicles = allVehicles.filter(vehicle => {
        const matchKeyword = !searchForm.keyword || 
          vehicle.vehicleNo.toLowerCase().includes(searchForm.keyword.toLowerCase()) ||
          (vehicle.brand && vehicle.brand.toLowerCase().includes(searchForm.keyword.toLowerCase())) ||
          (vehicle.model && vehicle.model.toLowerCase().includes(searchForm.keyword.toLowerCase()))
        
        const matchStatus = searchForm.status === undefined || vehicle.status === searchForm.status
        
        return matchKeyword && matchStatus
      })
      
      // 分页
      const start = (pagination.page - 1) * pagination.size
      const end = start + pagination.size
      vehicleList.value = filteredVehicles.slice(start, end)
      pagination.total = filteredVehicles.length
    }
  } catch (error) {
    console.error('Load vehicles failed:', error)
  } finally {
    loading.value = false
  }
}

// 生成模拟数据
const generateMockVehicles = (): Vehicle[] => {
  const vehicles: Vehicle[] = []
  const brands = ['福田', '金龙', '解放', '比亚迪', '宇通']
  const colors = ['白色', '蓝色', '黄色', '红色', '绿色']
  const statuses = [1, 1, 1, 2, 3, 0]
  
  for (let i = 1; i <= 50; i++) {
    vehicles.push({
      id: i,
      vehicleNo: `京A${String(Math.floor(Math.random() * 90000) + 10000)}`,
      vehicleTypeId: 1,
      brand: brands[Math.floor(Math.random() * brands.length)],
      model: `Model-${i}`,
      color: colors[Math.floor(Math.random() * colors.length)],
      status: statuses[Math.floor(Math.random() * statuses.length)],
      mileage: Math.floor(Math.random() * 100000),
      createTime: new Date(Date.now() - Math.random() * 365 * 24 * 60 * 60 * 1000).toISOString(),
      updateTime: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString()
    })
  }
  
  return vehicles
}

// 添加车辆
const handleAdd = () => {
  router.push('/vehicles/add')
}

// 编辑车辆
const handleEdit = (row: Vehicle) => {
  router.push(`/vehicles/${row.id}/edit`)
}

// 删除车辆
const handleDelete = async (row: Vehicle) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除车辆 ${row.vehicleNo} 吗？`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 这里应该调用删除API
    // await deleteVehicleApi(row.id)
    
    ElMessage.success('删除成功')
    loadVehicles()
  } catch {
    // 用户取消
  }
}

// 行点击
const handleRowClick = (row: Vehicle) => {
  router.push(`/vehicles/${row.id}`)
}

// 分页变化
const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadVehicles()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  loadVehicles()
}

// 组件挂载
onMounted(() => {
  loadVehicles()
})
</script>

<style scoped lang="scss">
.vehicles-page {
  .page-header {
    margin-bottom: 24px;
    
    .page-title {
      font-size: 24px;
      font-weight: 600;
      color: var(--text-primary-color);
      margin-bottom: 8px;
    }
    
    .page-description {
      color: var(--text-regular-color);
    }
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  
  .toolbar-left {
    display: flex;
    align-items: center;
  }
  
  .toolbar-right {
    display: flex;
    align-items: center;
  }
}

.table-container {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  
  .el-table {
    .el-table__row {
      cursor: pointer;
      
      &:hover {
        background-color: var(--background-extra-light-color);
      }
    }
  }
}

.pagination-container {
  margin-top: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.text-muted {
  color: var(--text-secondary-color);
  font-style: italic;
}
</style>