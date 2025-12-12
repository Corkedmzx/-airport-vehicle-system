<template>
  <div class="task-detail-page">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">任务详情</h1>
        <div class="page-actions">
          <el-button @click="$router.back()">返回</el-button>
          <el-button type="primary">编辑任务</el-button>
        </div>
      </div>
      
      <div class="detail-content">
        <el-card>
          <template #header>
            <span>任务信息</span>
          </template>
          <div class="detail-info">
            <p>任务详情页面开发中...</p>
            <p>这里将显示任务的详细信息、执行进度、车辆分配等</p>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 任务详情页面
</script>

<style scoped lang="scss">
.task-detail-page {
  .page-container {
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
  }
}
</style>