<template>
  <div class="vehicle-detail-page">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">车辆详情</h1>
        <div class="page-actions">
          <el-button @click="$router.back()">返回</el-button>
          <el-button type="primary">编辑车辆</el-button>
        </div>
      </div>
      
      <div class="detail-content">
        <el-card>
          <template #header>
            <span>基本信息</span>
          </template>
          <div class="detail-info">
            <p>车辆详情页面开发中...</p>
            <p>这里将显示车辆的详细信息、位置信息、任务历史等</p>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 车辆详情页面
</script>

<style scoped lang="scss">
.vehicle-detail-page {
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