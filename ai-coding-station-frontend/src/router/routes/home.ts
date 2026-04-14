import type { RouteRecordRaw } from 'vue-router'

export const homeRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/home/index.vue'),
    meta: { layout: 'basic', title: '首页' },
  },
  {
    path: '/about',
    name: 'about',
    component: () => import('@/views/home/about.vue'),
    meta: { layout: 'basic', title: '关于' },
  },
]

