import type { RouteRecordRaw } from 'vue-router'

export const userRoutes: RouteRecordRaw[] = [
  {
    path: '/user/login',
    name: 'user-login',
    component: () => import('@/views/user/login.vue'),
    meta: { title: '用户登录', auth: false, layout: 'blank' },
  },
  {
    path: '/user/register',
    name: 'user-register',
    component: () => import('@/views/user/register.vue'),
    meta: { title: '用户注册', auth: false, layout: 'blank' },
  },
  {
    path: '/user/profile',
    name: 'user-profile',
    component: () => import('@/views/user/profile.vue'),
    meta: { layout: 'basic' },
  },
]

