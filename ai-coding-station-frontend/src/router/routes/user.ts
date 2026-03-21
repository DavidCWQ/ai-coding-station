import type { RouteRecordRaw } from 'vue-router'

import { ACCESS_ENUM } from '@/access/accessEnum'

export const userRoutes: RouteRecordRaw[] = [
  {
    path: '/user/login',
    name: 'user-login',
    component: () => import('@/views/user/login.vue'),
    meta: { title: '用户登录', auth: false, layout: 'blank', access: ACCESS_ENUM.NOT_LOGIN },
  },
  {
    path: '/user/register',
    name: 'user-register',
    component: () => import('@/views/user/register.vue'),
    meta: { title: '用户注册', auth: false, layout: 'blank', access: ACCESS_ENUM.NOT_LOGIN },
  },
  {
    path: '/user/profile',
    name: 'UserProfile',
    component: () => import('@/views/user/profile.vue'),
    meta: { title: '个人主页', layout: 'basic', access: ACCESS_ENUM.USER },
  },
]

