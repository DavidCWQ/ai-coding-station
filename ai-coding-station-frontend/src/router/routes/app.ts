import type { RouteRecordRaw } from 'vue-router'

import { ACCESS_ENUM } from '@/access/accessEnum'

export const appRoutes: RouteRecordRaw[] = [
  {
    path: '/app',
    name: 'app-home',
    component: () => import('@/views/app/AppHomePage.vue'),
    meta: { layout: 'basic', title: '应用工坊', access: ACCESS_ENUM.NOT_LOGIN },
  },
  {
    path: '/app/manage',
    name: 'app-manage',
    component: () => import('@/views/app/AppManagePage.vue'),
    meta: { layout: 'basic', title: '应用管理', access: ACCESS_ENUM.ADMIN },
  },
  {
    path: '/app/edit/:id',
    name: 'app-edit',
    component: () => import('@/views/app/AppEditPage.vue'),
    meta: { layout: 'basic', title: '编辑应用', access: ACCESS_ENUM.USER },
  },
  {
    path: '/app/:appId',
    name: 'app-detail',
    component: () => import('@/views/app/AppDetailPage.vue'),
    meta: { layout: 'blank', title: '应用对话', access: ACCESS_ENUM.USER },
  },
]
