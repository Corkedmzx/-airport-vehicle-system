<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

// 恢复登录状态
onMounted(() => {
  userStore.restoreLogin()
})
</script>

<style lang="scss">
// 全局样式变量 - 基于1920x1080标准分辨率（72dpi）
:root {
  --max-content-width: 1920px;
  --content-max-width: 1600px; // 内容区域最大宽度，留出边距
  --content-padding: 24px;
  --base-font-size: 14px; // 标准字体大小
  --base-line-height: 1.5;
  --spacing-unit: 8px; // 基础间距单位
}

// 响应式断点
$breakpoints: (
  'xs': 0,
  'sm': 576px,
  'md': 768px,
  'lg': 992px,
  'xl': 1200px,
  'xxl': 1600px,
  'full': 1920px
);

#app {
  width: 100%;
  min-height: 100vh;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  font-size: var(--base-font-size);
  line-height: var(--base-line-height);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  min-height: 100vh;
  overflow-x: hidden;
  font-size: var(--base-font-size);
  line-height: var(--base-line-height);
}

// 全局页面容器样式 - 响应式设计
.page-wrapper {
  width: 100%;
  max-width: var(--content-max-width);
  margin: 0 auto;
  padding: 0 var(--content-padding);
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  
  // 响应式调整
  @media (max-width: 1600px) {
    max-width: 100%;
    padding: 0 20px;
  }
  
  @media (max-width: 1200px) {
    padding: 0 16px;
  }
  
  @media (max-width: 768px) {
    padding: 0 12px;
  }
}

// 自定义滚动条
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

// 动画效果
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

// 卡片阴影效果
.card-shadow {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  transition: box-shadow 0.3s ease;
  
  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  }
}

// 文字省略
.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-ellipsis-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.text-ellipsis-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>