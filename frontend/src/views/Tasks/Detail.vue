<template>
  <div class="task-detail-page">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">任务详情</h1>
        <div class="page-actions">
          <el-button @click="$router.back()">返回</el-button>
          <el-button type="primary" @click="handleEdit">编辑任务</el-button>
        </div>
      </div>
      
      <div v-loading="loading" class="detail-content">
        <el-row :gutter="20">
          <!-- 基本信息 -->
          <el-col :span="24">
            <el-card>
              <template #header>
                <span>任务信息</span>
              </template>
              <el-descriptions :column="3" border v-if="task">
                <el-descriptions-item label="任务编号">{{ task.taskNo }}</el-descriptions-item>
                <el-descriptions-item label="任务名称">{{ task.taskName }}</el-descriptions-item>
                <el-descriptions-item label="任务类型">{{ task.taskType }}</el-descriptions-item>
                <el-descriptions-item label="优先级">
                  <el-tag :type="getPriorityType(task.priority)">
                    {{ getPriorityText(task.priority) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="任务状态">
                  <el-tag :type="getStatusType(task.status)">
                    {{ getStatusText(task.status) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="完成进度">
                  <el-progress :percentage="task.progress" :show-text="true" />
                </el-descriptions-item>
                <el-descriptions-item label="起始位置" :span="3">{{ task.startLocation }}</el-descriptions-item>
                <el-descriptions-item label="目标位置" :span="3">{{ task.endLocation }}</el-descriptions-item>
                <el-descriptions-item label="任务描述" :span="3">{{ task.description || '-' }}</el-descriptions-item>
                <el-descriptions-item label="开始时间">{{ formatDateTime(task.startTime) }}</el-descriptions-item>
                <el-descriptions-item label="预计结束时间">{{ formatDateTime(task.endTime) || '-' }}</el-descriptions-item>
                <el-descriptions-item label="实际开始时间">{{ formatDateTime(task.actualStartTime) || '-' }}</el-descriptions-item>
                <el-descriptions-item label="实际结束时间">{{ formatDateTime(task.actualEndTime) || '-' }}</el-descriptions-item>
                <el-descriptions-item label="分配车辆ID">{{ task.assignedVehicleId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="分配司机ID">{{ task.assignedDriverId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="备注" :span="3">{{ task.remark || '-' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDateTime(task.createTime) }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ formatDateTime(task.updateTime) }}</el-descriptions-item>
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
import { getTaskApi } from '@/api/tasks'
import type { DispatchTask } from '@/api/types'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const task = ref<DispatchTask | null>(null)

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

// 格式化日期时间
const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm:ss')
}

// 编辑任务
const handleEdit = () => {
  if (task.value) {
    router.push(`/tasks/${task.value.id}/edit`)
  }
}

// 加载任务详情
const loadTaskDetail = async () => {
  const id = route.params.id as string
  if (!id) {
    ElMessage.error('任务ID不存在')
    router.back()
    return
  }
  
  try {
    loading.value = true
    const response = await getTaskApi(Number(id))
    if (response.data.code === 200) {
      task.value = response.data.data
    } else {
      ElMessage.error(response.data.message || '加载任务详情失败')
      router.back()
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '加载任务详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTaskDetail()
})
</script>

<style scoped lang="scss">
.task-detail-page {
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
