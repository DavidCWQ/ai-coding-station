import type { RouteRecordRaw } from 'vue-router'

export const commonRoutes: RouteRecordRaw[] = [
  {
    // 404 must be the last route
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/system/404.vue'),
    meta: { layout: 'blank' },
  },
]

