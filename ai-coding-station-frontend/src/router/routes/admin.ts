import type { RouteRecordRaw } from 'vue-router'

import { ACCESS_ENUM } from '@/access/accessEnum'

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin/userManage',
    name: 'admin-userManage',
    component: () => import('@/views/admin/userManage.vue'),
    meta: { layout: 'basic', title: '用户管理', access: ACCESS_ENUM.ADMIN },
  },
  {
    path: '/admin/stats',
    name: 'admin-stats',
    component: () => import('@/views/admin/visitStats.vue'),
    meta: { layout: 'basic', title: '访问统计', access: ACCESS_ENUM.ADMIN },
  },
]

