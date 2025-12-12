<template>
  <div class="statistics-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">统计分析</h1>
      <p class="page-description">车辆和任务数据的统计分析</p>
    </div>
    
    <!-- 统计概览 -->
    <div class="overview-cards">
      <div class="overview-card">
        <div class="card-icon">
          <el-icon><Van /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ vehicleStats.totalCount }}</div>
          <div class="card-label">车辆总数</div>
          <div class="card-desc">正常运行 {{ vehicleStats.activeCount }} 辆</div>
        </div>
      </div>
      
      <div class="overview-card">
        <div class="card-icon">
          <el-icon><List /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ taskStats.totalCount }}</div>
          <div class="card-label">任务总数</div>
          <div class="card-desc">今日完成 {{ taskStats.completedCount }} 个</div>
        </div>
      </div>
      
      <div class="overview-card">
        <div class="card-icon">
          <el-icon><Odometer /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ completionRate }}%</div>
          <div class="card-label">任务完成率</div>
          <div class="card-desc">本月执行效率</div>
        </div>
      </div>
      
      <div class="overview-card">
        <div class="card-icon">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ activeTaskRate }}%</div>
          <div class="card-label">车辆使用率</div>
          <div class="card-desc">当前在线车辆</div>
        </div>
      </div>
    </div>
    
    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 车辆状态分布 -->
      <div class="chart-container">
        <div class="chart-header">
          <h3 class="chart-title">车辆状态分布</h3>
        </div>
        <div class="chart-content">
          <div ref="vehicleChartRef" class="chart"></div>
        </div>
      </div>
      
      <!-- 任务状态统计 -->
      <div class="chart-container">
        <div class="chart-header">
          <h3 class="chart-title">任务状态统计</h3>
        </div>
        <div class="chart-content">
          <div ref="taskChartRef" class="chart"></div>
        </div>
      </div>
      
      <!-- 任务趋势分析 -->
      <div class="chart-container chart-wide">
        <div class="chart-header">
          <h3 class="chart-title">任务执行趋势</h3>
          <div class="chart-controls">
            <el-radio-group v-model="trendPeriod" size="small" @change="loadTrendData">
              <el-radio-button label="week">本周</el-radio-button>
              <el-radio-button label="month">本月</el-radio-button>
              <el-radio-button label="quarter">本季度</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div class="chart-content">
          <div ref="trendChartRef" class="chart"></div>
        </div>
      </div>
    </div>
    
    <!-- 数据表格 -->
    <div class="data-tables">
      <!-- 车辆使用情况 -->
      <div class="table-section">
        <div class="table-header">
          <h3 class="table-title">车辆使用情况排行</h3>
          <el-button type="primary" link>查看详情</el-button>
        </div>
        <el-table :data="vehicleUsageList" stripe>
          <el-table-column prop="vehicleNo" label="车牌号" width="120" />
          <el-table-column prop="totalTasks" label="任务总数" width="100" />
          <el-table-column prop="completedTasks" label="完成任务" width="100" />
          <el-table-column prop="totalDistance" label="总里程(km)" width="120" />
          <el-table-column prop="usageRate" label="使用率" width="100">
            <template #default="{ row }">
              <el-progress
                :percentage="row.usageRate"
                :color="getUsageRateColor(row.usageRate)"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <!-- 任务效率统计 -->
      <div class="table-section">
        <div class="table-header">
          <h3 class="table-title">任务效率统计</h3>
          <el-button type="primary" link>查看详情</el-button>
        </div>
        <el-table :data="taskEfficiencyList" stripe>
          <el-table-column prop="taskType" label="任务类型" width="120" />
          <el-table-column prop="totalCount" label="任务数量" width="100" />
          <el-table-column prop="avgDuration" label="平均耗时(小时)" width="120" />
          <el-table-column prop="completionRate" label="完成率" width="100">
            <template #default="{ row }">
              <el-progress
                :percentage="row.completionRate"
                :color="getCompletionRateColor(row.completionRate)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="efficiency" label="效率指数" width="100">
            <template #default="{ row }">
              <span :class="getEfficiencyClass(row.efficiency)">{{ row.efficiency }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Van, List, Odometer, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getVehicleStatisticsApi } from '@/api/vehicles'
import { getTaskStatisticsApi } from '@/api/tasks'

// 图表引用
const vehicleChartRef = ref<HTMLDivElement>()
const taskChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()
const trendPeriod = ref('week')

// 数据状态
const vehicleStats = ref({
  totalCount: 0,
  activeCount: 0,
  maintenanceCount: 0,
  faultCount: 0,
  offlineCount: 0
})

const taskStats = ref({
  totalCount: 0,
  pendingCount: 0,
  assignedCount: 0,
  inProgressCount: 0,
  completedCount: 0,
  cancelledCount: 0,
  exceptionCount: 0,
  todayCount: 0
})

// 计算属性
const completionRate = computed(() => {
  if (taskStats.value.totalCount === 0) return 0
  return Math.round((taskStats.value.completedCount / taskStats.value.totalCount) * 100)
})

const activeTaskRate = computed(() => {
  if (vehicleStats.value.totalCount === 0) return 0
  return Math.round((vehicleStats.value.activeCount / vehicleStats.value.totalCount) * 100)
})

// 车辆使用情况数据
const vehicleUsageList = ref([
  {
    vehicleNo: '京A12345',
    totalTasks: 45,
    completedTasks: 42,
    totalDistance: 1250,
    usageRate: 93
  },
  {
    vehicleNo: '京B67890',
    totalTasks: 38,
    completedTasks: 35,
    totalDistance: 980,
    usageRate: 92
  },
  {
    vehicleNo: '京C11111',
    totalTasks: 52,
    completedTasks: 48,
    totalDistance: 1580,
    usageRate: 92
  }
])

// 任务效率数据
const taskEfficiencyList = ref([
  {
    taskType: '常规调度',
    totalCount: 120,
    avgDuration: 2.5,
    completionRate: 95,
    efficiency: '优秀'
  },
  {
    taskType: '紧急调度',
    totalCount: 25,
    avgDuration: 1.2,
    completionRate: 88,
    efficiency: '良好'
  },
  {
    taskType: '维护调度',
    totalCount: 15,
    avgDuration: 4.5,
    completionRate: 93,
    efficiency: '优秀'
  }
])

// 获取使用率颜色
const getUsageRateColor = (rate: number) => {
  if (rate >= 90) return '#67c23a'
  if (rate >= 80) return '#e6a23c'
  return '#f56c6c'
}

// 获取完成率颜色
const getCompletionRateColor = (rate: number) => {
  if (rate >= 95) return '#67c23a'
  if (rate >= 85) return '#e6a23c'
  return '#f56c6c'
}

// 获取效率样式
const getEfficiencyClass = (efficiency: string) => {
  if (efficiency === '优秀') return 'text-success'
  if (efficiency === '良好') return 'text-warning'
  return 'text-danger'
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const [vehicleRes, taskRes] = await Promise.all([
      getVehicleStatisticsApi(),
      getTaskStatisticsApi()
    ])
    
    if (vehicleRes.data.code === 200) {
      vehicleStats.value = vehicleRes.data.data
    }
    
    if (taskRes.data.code === 200) {
      taskStats.value = taskRes.data.data
    }
  } catch (error) {
    console.error('Load statistics failed:', error)
  }
}

// 加载趋势数据
const loadTrendData = () => {
  // 这里应该根据trendPeriod加载不同的趋势数据
  initTrendChart()
}

// 初始化车辆状态图表
const initVehicleChart = () => {
  if (!vehicleChartRef.value) return
  
  const chart = echarts.init(vehicleChartRef.value)
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        type: 'pie',
        radius: '60%',
        data: [
          { value: vehicleStats.value.activeCount, name: '正常运行' },
          { value: vehicleStats.value.maintenanceCount, name: '维修中' },
          { value: vehicleStats.value.faultCount, name: '故障' },
          { value: vehicleStats.value.offlineCount, name: '停用' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  
  chart.setOption(option)
}

// 初始化任务状态图表
const initTaskChart = () => {
  if (!taskChartRef.value) return
  
  const chart = echarts.init(taskChartRef.value)
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        type: 'pie',
        radius: '60%',
        data: [
          { value: taskStats.value.pendingCount, name: '待分配' },
          { value: taskStats.value.assignedCount, name: '已分配' },
          { value: taskStats.value.inProgressCount, name: '执行中' },
          { value: taskStats.value.completedCount, name: '已完成' },
          { value: taskStats.value.cancelledCount, name: '已取消' },
          { value: taskStats.value.exceptionCount, name: '异常' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  
  chart.setOption(option)
}

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) return
  
  const chart = echarts.init(trendChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['计划任务', '实际完成', '完成率']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        boundaryGap: false,
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      }
    ],
    yAxis: [
      {
        type: 'value'
      }
    ],
    series: [
      {
        name: '计划任务',
        type: 'line',
        stack: 'Total',
        areaStyle: {
          opacity: 0.3
        },
        emphasis: {
          focus: 'series'
        },
        data: [12, 13, 10, 13, 9, 23, 21]
      },
      {
        name: '实际完成',
        type: 'line',
        stack: 'Total',
        areaStyle: {
          opacity: 0.3
        },
        emphasis: {
          focus: 'series'
        },
        data: [10, 12, 8, 11, 8, 20, 19]
      },
      {
        name: '完成率',
        type: 'line',
        yAxisIndex: 0,
        data: [83, 92, 80, 85, 89, 87, 90]
      }
    ]
  }
  
  chart.setOption(option)
}

// 组件挂载
onMounted(async () => {
  await loadStatistics()
  
  // 延迟初始化图表，确保DOM渲染完成
  setTimeout(() => {
    initVehicleChart()
    initTaskChart()
    initTrendChart()
  }, 100)
})
</script>

<style scoped lang="scss">
.statistics-page {
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

.overview-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.overview-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
  }
  
  .card-icon {
    width: 60px;
    height: 60px;
    border-radius: 12px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: white;
  }
  
  .card-content {
    flex: 1;
    
    .card-value {
      font-size: 28px;
      font-weight: 700;
      color: var(--text-primary-color);
      margin-bottom: 4px;
    }
    
    .card-label {
      font-size: 14px;
      color: var(--text-regular-color);
      margin-bottom: 4px;
    }
    
    .card-desc {
      font-size: 12px;
      color: var(--text-secondary-color);
    }
  }
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.chart-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  
  &.chart-wide {
    grid-column: 1 / -1;
  }
  
  .chart-header {
    padding: 20px;
    border-bottom: 1px solid var(--border-lighter-color);
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .chart-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary-color);
    }
    
    .chart-controls {
      display: flex;
      align-items: center;
      gap: 16px;
    }
  }
  
  .chart-content {
    padding: 20px;
    
    .chart {
      width: 100%;
      height: 300px;
    }
  }
}

.data-tables {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.table-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  
  .table-header {
    padding: 20px;
    border-bottom: 1px solid var(--border-lighter-color);
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .table-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary-color);
    }
  }
}

.text-success {
  color: var(--success-color);
  font-weight: 600;
}

.text-warning {
  color: var(--warning-color);
  font-weight: 600;
}

.text-danger {
  color: var(--danger-color);
  font-weight: 600;
}

@media (max-width: 768px) {
  .overview-cards {
    grid-template-columns: 1fr;
  }
  
  .charts-grid {
    grid-template-columns: 1fr;
  }
  
  .data-tables {
    grid-template-columns: 1fr;
  }
}
</style>