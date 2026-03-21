import type { RouteRecordRaw } from 'vue-router'

import { ACCESS_ENUM } from '@/access/accessEnum'

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin/userManage',
    name: 'admin-userManage',
    component: () => import('@/views/admin/userManage.vue'),
    meta: { layout: 'basic', title: '用户管理', access: ACCESS_ENUM.ADMIN },
  },
]

