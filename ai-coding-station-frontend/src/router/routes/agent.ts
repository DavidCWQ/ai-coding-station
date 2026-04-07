import { ACCESS_ENUM } from '@/access/accessEnum'

export const agentRoutes = [
  {
    path: '/agents',
    name: 'agent-list',
    component: () => import('@/views/agent/AgentListPage.vue'),
    meta: { layout: 'basic', title: '智能体', access: ACCESS_ENUM.NOT_LOGIN },
  },
  {
    path: '/agents/:agentCode/chat',
    name: 'agent-chat',
    component: () => import('@/views/agent/AgentChatPage.vue'),
    meta: {
      layout: 'basic',
      title: '智能体对话',
      access: ACCESS_ENUM.NOT_LOGIN,
      hideFooter: true,
      fullWidth: true,
      noContentCard: true,
    },
  },
]
