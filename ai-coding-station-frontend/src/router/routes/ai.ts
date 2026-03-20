import type { RouteRecordRaw } from 'vue-router'

export const aiRoutes: RouteRecordRaw[] = [
  {
    path: '/ai/chat',
    name: 'ai-chat',
    component: () => import('@/views/ai/chat.vue'),
    meta: { layout: 'basic' },
  },
  {
    path: '/ai/coding',
    name: 'ai-coding',
    component: () => import('@/views/ai/coding.vue'),
    meta: { layout: 'basic' },
  },
  {
    path: '/ai/history',
    name: 'ai-history',
    component: () => import('@/views/ai/history.vue'),
    meta: { layout: 'basic' },
  },
]

