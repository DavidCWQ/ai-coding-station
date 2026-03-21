import type { RouteRecordRaw } from 'vue-router'

import { useLoginUserStore } from '@/stores/loginUser'

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
    name: 'UserProfile',
    component: () => import('@/views/user/profile.vue'),
    meta: { title: '个人主页', layout: 'basic' },
    beforeEnter: async (to) => {
      const store = useLoginUserStore()
      if (!store.loginUser?.id) {
        await store.fetchLoginUser()
      }
      if (!store.loginUser?.id) {
        return {
          path: '/user/login',
          query: { redirect: to.fullPath },
        }
      }
    },
  },
]

