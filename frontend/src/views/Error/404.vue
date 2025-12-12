<template>
  <div class="not-found-page">
    <div class="not-found-container">
      <div class="not-found-icon">
        <el-icon :size="120" color="#909399">
          <WarningFilled />
        </el-icon>
      </div>
      <div class="not-found-content">
        <h1 class="not-found-title">404</h1>
        <h2 class="not-found-subtitle">页面未找到</h2>
        <p class="not-found-description">
          抱歉，您访问的页面不存在或已被移除。
        </p>
        <div class="not-found-actions">
          <el-button type="primary" @click="goHome">
            返回首页
          </el-button>
          <el-button @click="goBack">
            返回上页
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.go(-1)
}
</script>

<style scoped lang="scss">
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.not-found-container {
  text-align: center;
  background: white;
  border-radius: 16px;
  padding: 60px 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 100%;
  
  .not-found-icon {
    margin-bottom: 32px;
  }
  
  .not-found-title {
    font-size: 72px;
    font-weight: 700;
    color: var(--primary-color);
    margin-bottom: 16px;
    line-height: 1;
  }
  
  .not-found-subtitle {
    font-size: 24px;
    font-weight: 600;
    color: var(--text-primary-color);
    margin-bottom: 16px;
  }
  
  .not-found-description {
    font-size: 16px;
    color: var(--text-regular-color);
    margin-bottom: 32px;
    line-height: 1.6;
  }
  
  .not-found-actions {
    display: flex;
    justify-content: center;
    gap: 16px;
  }
}

@media (max-width: 480px) {
  .not-found-container {
    padding: 40px 24px;
    
    .not-found-title {
      font-size: 56px;
    }
    
    .not-found-subtitle {
      font-size: 20px;
    }
    
    .not-found-actions {
      flex-direction: column;
      align-items: center;
      
      .el-button {
        width: 200px;
      }
    }
  }
}
</style>