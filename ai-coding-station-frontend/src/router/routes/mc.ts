import type { RouteRecordRaw } from 'vue-router'

import { ACCESS_ENUM } from '@/access/accessEnum'

export const mcRoutes: RouteRecordRaw[] = [
  {
    path: '/mc',
    name: 'mc-visitor',
    component: () => import('@/views/mc/McRealmPage.vue'),
    meta: {
      layout: 'basic',
      title: '基岩世界',
      access: ACCESS_ENUM.USER,
      fullWidth: true,
      noContentCard: true,
    },
  },
  {
    path: '/admin/mc',
    name: 'admin-mc',
    component: () => import('@/views/mc/McRealmPage.vue'),
    meta: {
      layout: 'basic',
      title: '基岩服',
      access: ACCESS_ENUM.ADMIN,
      fullWidth: true,
      noContentCard: true,
    },
  },
]
