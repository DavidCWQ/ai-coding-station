import type { RouteRecordRaw } from 'vue-router'

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin/userManage',
    name: 'admin-userManage',
    component: () => import('@/views/admin/userManage.vue'),
    meta: { layout: 'basic' },
  },
]

