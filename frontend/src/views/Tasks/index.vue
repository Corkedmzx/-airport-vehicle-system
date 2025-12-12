<template>
  <div class="tasks-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">任务管理</h1>
      <p class="page-description">管理调度任务，监控任务执行状态</p>
    </div>
    
    <!-- 搜索和操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索任务名称、编号"
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
          placeholder="任务状态"
          style="width: 120px; margin-left: 12px"
          clearable
          @change="handleSearch"
        >
          <el-option label="待分配" :value="1" />
          <el-option label="已分配" :value="2" />
          <el-option label="执行中" :value="3" />
          <el-option label="已完成" :value="4" />
          <el-option label="已取消" :value="5" />
        </el-select>
      </div>
      
      <div class="toolbar-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          创建任务
        </el-button>
      </div>
    </div>
    
    <!-- 数据表格 -->
    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="taskList"
        stripe
        @row-click="handleRowClick"
      >
        <el-table-column prop="taskNo" label="任务编号" width="140" />
        <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="taskType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getTaskTypeText(row.taskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">
              {{ getPriorityText(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="路线" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.startLocation }} → {{ row.endLocation }}
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="100">
          <template #default="{ row }">
            <el-progress
              :percentage="row.progress"
              :status="getProgressStatus(row.status, row.progress)"
              :show-text="false"
              :stroke-width="6"
            />
            <span style="font-size: 12px; color: #666;">{{ row.progress }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="btn-group">
              <el-button type="primary" link @click.stop="viewTaskDetail(row)">
                查看详情
              </el-button>
              <el-button
                v-if="row.status === 1"
                type="primary"
                link
                @click.stop="handleAssign(row)"
              >
                分配
              </el-button>
              <el-button
                v-if="row.status === 2"
                type="success"
                link
                @click.stop="handleStart(row)"
              >
                开始
              </el-button>
              <el-button
                v-if="row.status === 3"
                type="warning"
                link
                @click.stop="handleComplete(row)"
              >
                完成
              </el-button>
              <el-button type="primary" link @click.stop="handleEdit(row)">
                编辑
              </el-button>
              <el-button type="danger" link @click.stop="handleDelete(row)">
                删除
              </el-button>
            </div>
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

const router = useRouter()
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { getTasksApi, assignTaskApi, startTaskApi, completeTaskApi, deleteTaskApi } from '@/api/tasks'
import type { DispatchTask } from '@/api/types'
import { timeAgo } from '@/utils'

const router = useRouter()

// 数据状态
const loading = ref(false)
const taskList = ref<DispatchTask[]>([])

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

// 获取任务类型文本
const getTaskTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'regular': '常规调度',
    'emergency': '紧急调度',
    'maintenance': '维护调度'
  }
  return typeMap[type] || type
}

// 获取优先级类型
const getPriorityType = (priority: number) => {
  const typeMap: Record<number, string> = {
    1: 'info',    // 低
    2: 'warning', // 中
    3: 'danger',  // 高
    4: 'danger'   // 紧急
  }
  return typeMap[priority] || 'info'
}

// 获取优先级文本
const getPriorityText = (priority: number) => {
  const textMap: Record<number, string> = {
    1: '低',
    2: '中',
    3: '高',
    4: '紧急'
  }
  return textMap[priority] || '中'
}

// 获取状态类型
const getStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    1: 'info',     // 待分配
    2: 'warning',  // 已分配
    3: 'primary',  // 执行中
    4: 'success',  // 已完成
    5: 'danger',   // 已取消
    6: 'danger'    // 异常
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    1: '待分配',
    2: '已分配',
    3: '执行中',
    4: '已完成',
    5: '已取消',
    6: '异常'
  }
  return textMap[status] || '未知'
}

// 获取进度状态
const getProgressStatus = (status: number, progress: number) => {
  if (status === 4) return 'success'
  if (status === 5 || status === 6) return 'exception'
  if (progress === 100) return 'success'
  return undefined
}

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return ''
  return timeAgo(time)
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadTasks()
}

// 加载任务列表
const loadTasks = async () => {
  try {
    loading.value = true
    const response = await getTasksApi()
    if (response.data.code === 200) {
      const allTasks = Array.isArray(response.data.data) ? response.data.data : []
      
      // 前端筛选
      let filteredTasks = allTasks
      if (searchForm.keyword) {
        const keyword = searchForm.keyword.toLowerCase()
        filteredTasks = filteredTasks.filter(task => 
          task.taskName.toLowerCase().includes(keyword) ||
          task.taskNo.toLowerCase().includes(keyword)
        )
      }
      
      if (searchForm.status !== undefined) {
        filteredTasks = filteredTasks.filter(task => task.status === searchForm.status)
      }
      
      // 分页
      const start = (pagination.page - 1) * pagination.size
      const end = start + pagination.size
      taskList.value = filteredTasks.slice(start, end)
      pagination.total = filteredTasks.length
    }
  } catch (error) {
    console.error('Load tasks failed:', error)
  } finally {
    loading.value = false
  }
}


// 添加任务
const handleAdd = () => {
  router.push('/tasks/add')
}

// 查看任务详情
const viewTaskDetail = (row: DispatchTask) => {
  router.push(`/tasks/${row.id}`)
}

// 编辑任务
const handleEdit = (row: DispatchTask) => {
  router.push(`/tasks/${row.id}/edit`)
}

// 删除任务
const handleDelete = async (row: DispatchTask) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务 ${row.taskName} 吗？`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteTaskApi(row.id)
    ElMessage.success('删除成功')
    loadTasks()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '删除失败')
    }
  }
}

// 分配任务
const handleAssign = async (row: DispatchTask) => {
  try {
    // 这里应该弹出对话框选择车辆
    ElMessage.info('请前往调度中心进行任务分配')
    router.push('/dispatch')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 开始任务
const handleStart = async (row: DispatchTask) => {
  try {
    await startTaskApi(row.id)
    ElMessage.success('任务已开始')
    loadTasks()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '操作失败')
  }
}

// 完成任务
const handleComplete = async (row: DispatchTask) => {
  try {
    await completeTaskApi(row.id)
    ElMessage.success('任务已完成')
    loadTasks()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '操作失败')
  }
}

// 行点击
const handleRowClick = (row: DispatchTask) => {
  router.push(`/tasks/${row.id}`)
}

// 分页变化
const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadTasks()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  loadTasks()
}

// 组件挂载
onMounted(() => {
  loadTasks()
})
</script>

<style scoped lang="scss">
.tasks-page {
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
    
    .el-progress {
      margin-right: 8px;
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

.btn-group {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
</style>