import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      // @ts-ignore: ignore missing module/type declaration for .vue files
      component: () => import('@/views/Login/index.vue'),
      meta: { title: '登录', requiresAuth: false }
    },
    {
      path: '/register',
      name: 'Register',
      // @ts-ignore: ignore missing module/type declaration for .vue files
      component: () => import('@/views/register/index.vue'),
      meta: { title: '注册', requiresAuth: false }
    },
    {
      path: '/forgot-password',
      name: 'ForgotPassword',
      // @ts-ignore: ignore missing module/type declaration for .vue files
      component: () => import('@/views/ForgotPassword/index.vue'),
      meta: { title: '账户找回', requiresAuth: false }
    },
    {
      path: '/',
      redirect: '/dashboard',
      // @ts-ignore: ignore missing module/type declaration for .vue files
      component: () => import('@/layout/index.vue'),
      meta: { title: '首页', requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Dashboard/index.vue'),
          meta: { title: '仪表盘' }
        },
        {
          path: 'vehicles',
          name: 'Vehicles',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Vehicles/index.vue'),
          meta: { title: '车辆管理' }
        },
        {
          path: 'vehicles/:id',
          name: 'VehicleDetail',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Vehicles/Detail.vue'),
          meta: { title: '车辆详情', hidden: true }
        },
        {
          path: 'vehicles/add',
          name: 'VehicleAdd',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Vehicles/Form.vue'),
          meta: { title: '添加车辆', hidden: true }
        },
        {
          path: 'vehicles/:id/edit',
          name: 'VehicleEdit',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Vehicles/Form.vue'),
          meta: { title: '编辑车辆', hidden: true }
        },
        {
          path: 'tasks',
          name: 'Tasks',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Tasks/index.vue'),
          meta: { title: '任务管理' }
        },
        {
          path: 'tasks/:id',
          name: 'TaskDetail',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Tasks/Detail.vue'),
          meta: { title: '任务详情', hidden: true }
        },
        {
          path: 'tasks/add',
          name: 'TaskAdd',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Tasks/Form.vue'),
          meta: { title: '创建任务', hidden: true }
        },
        {
          path: 'tasks/:id/edit',
          name: 'TaskEdit',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Tasks/Form.vue'),
          meta: { title: '编辑任务', hidden: true }
        },
        {
          path: 'statistics',
          name: 'Statistics',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Statistics/index.vue'),
          meta: { title: '统计分析' }
        },
        {
          path: 'monitoring',
          name: 'Monitoring',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Monitoring/index.vue'),
          meta: { title: '实时监控' }
        },
        {
          path: 'dispatch',
          name: 'Dispatch',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Dispatch/index.vue'),
          meta: { title: '调度中心' }
        },
        {
          path: 'alerts',
          name: 'Alerts',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Alerts/index.vue'),
          meta: { title: '告警管理' }
        },
        {
          path: 'map',
          name: 'Map',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Map/index.vue'),
          meta: { title: '地图监控' }
        },
        {
          path: 'users',
          name: 'Users',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Users/index.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'logs',
          name: 'Logs',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Logs/index.vue'),
          meta: { title: '系统日志' }
        },
        {
          path: 'settings',
          name: 'Settings',
          // @ts-ignore: ignore missing module/type declaration for .vue files
          component: () => import('@/views/Settings/index.vue'),
          meta: { title: '系统设置' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      // @ts-ignore: ignore missing module/type declaration for .vue files
      component: () => import('@/views/Error/404.vue'),
      meta: { title: '页面未找到' }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 机场车辆监控与调度系统`
  }
  
  // 检查登录状态
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router