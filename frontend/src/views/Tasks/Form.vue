<template>
  <div class="task-form-page">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">{{ isEdit ? '编辑任务' : '创建任务' }}</h1>
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
              <el-form-item label="任务名称" prop="taskName">
                <el-input v-model="form.taskName" placeholder="请输入任务名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="任务类型" prop="taskType">
                <el-select v-model="form.taskType" style="width: 100%">
                  <el-option label="常规调度" value="常规调度" />
                  <el-option label="紧急调度" value="紧急调度" />
                  <el-option label="维护调度" value="维护调度" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="优先级" prop="priority">
                <el-select v-model="form.priority" style="width: 100%">
                  <el-option label="低" :value="1" />
                  <el-option label="中" :value="2" />
                  <el-option label="高" :value="3" />
                  <el-option label="紧急" :value="4" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="开始时间" prop="startTime">
                <el-date-picker
                  v-model="form.startTime"
                  type="datetime"
                  placeholder="选择开始时间"
                  style="width: 100%"
                  value-format="YYYY-MM-DD HH:mm:ss"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="起始位置" prop="startLocation">
                <el-input v-model="form.startLocation" placeholder="请输入起始位置" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="目标位置" prop="endLocation">
                <el-input v-model="form.endLocation" placeholder="请输入目标位置" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="任务描述" prop="description">
            <el-input 
              v-model="form.description" 
              type="textarea" 
              :rows="4"
              placeholder="请输入任务描述"
            />
          </el-form-item>
          
          <el-form-item label="备注" prop="remark">
            <el-input 
              v-model="form.remark" 
              type="textarea" 
              :rows="3"
              placeholder="请输入备注"
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
import { getTaskApi, createTaskApi, updateTaskApi } from '@/api/tasks'
import type { DispatchTask } from '@/api/types'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const isEdit = ref(false)

const form = reactive<Partial<DispatchTask>>({
  taskName: '',
  taskType: '常规调度',
  priority: 2,
  description: '',
  startLocation: '',
  endLocation: '',
  startTime: '',
  endTime: undefined,
  remark: ''
})

const rules: FormRules = {
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' }
  ],
  taskType: [
    { required: true, message: '请选择任务类型', trigger: 'change' }
  ],
  priority: [
    { required: true, message: '请选择优先级', trigger: 'change' }
  ],
  startLocation: [
    { required: true, message: '请输入起始位置', trigger: 'blur' }
  ],
  endLocation: [
    { required: true, message: '请输入目标位置', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' }
  ]
}

// 加载任务数据
const loadTaskData = async () => {
  const id = route.params.id as string
  if (id && id !== 'add') {
    isEdit.value = true
    try {
      loading.value = true
      const response = await getTaskApi(Number(id))
      if (response.data.code === 200) {
        Object.assign(form, response.data.data)
        if (form.startTime) {
          form.startTime = dayjs(form.startTime).format('YYYY-MM-DD HH:mm:ss')
        }
        if (form.endTime) {
          form.endTime = dayjs(form.endTime).format('YYYY-MM-DD HH:mm:ss')
        }
      }
    } catch (error: any) {
      ElMessage.error(error?.response?.data?.message || '加载任务数据失败')
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
    
    // 准备提交数据，确保时间格式正确
    const submitData: any = {
      ...form,
      startTime: form.startTime ? dayjs(form.startTime).format('YYYY-MM-DD HH:mm:ss') : null,
      endTime: form.endTime ? dayjs(form.endTime).format('YYYY-MM-DD HH:mm:ss') : null
    }
    
    // 移除空值字段
    Object.keys(submitData).forEach(key => {
      if (submitData[key] === '' || submitData[key] === undefined) {
        delete submitData[key]
      }
    })
    
    if (isEdit.value) {
      await updateTaskApi(Number(route.params.id), submitData)
      ElMessage.success('任务更新成功')
    } else {
      await createTaskApi(submitData)
      ElMessage.success('任务创建成功')
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
  loadTaskData()
})
</script>

<style scoped lang="scss">
.task-form-page {
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

