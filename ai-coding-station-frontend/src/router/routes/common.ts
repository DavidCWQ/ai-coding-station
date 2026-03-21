import type { RouteRecordRaw } from 'vue-router'

import { ACCESS_ENUM } from '@/access/accessEnum'

export const commonRoutes: RouteRecordRaw[] = [
  {
    path: '/noAuth',
    name: 'no-auth',
    component: () => import('@/views/system/403.vue'),
    meta: { layout: 'blank', access: ACCESS_ENUM.NOT_LOGIN, title: '无权限' },
  },
  {
    // 404 must be the last route
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/system/404.vue'),
    meta: { layout: 'blank' },
  },
]

