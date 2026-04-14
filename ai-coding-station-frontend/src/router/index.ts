import { createRouter, createWebHistory } from 'vue-router'

import { SITE_DOCUMENT_TITLE } from '@/constants/siteFooter'

import { homeRoutes } from './routes/home'
import { userRoutes } from './routes/user'
import { adminRoutes } from './routes/admin'
import { appRoutes } from './routes/app'
import { aiRoutes } from './routes/ai'
import { agentRoutes } from './routes/agent'
import { commonRoutes } from './routes/common'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    ...homeRoutes,
    ...userRoutes,
    ...adminRoutes,
    ...appRoutes,
    ...agentRoutes,
    ...aiRoutes,
    ...commonRoutes,
  ],
})

router.afterEach((to) => {
  const page = to.meta?.title as string | undefined
  document.title = page ? `${page} - ${SITE_DOCUMENT_TITLE}` : SITE_DOCUMENT_TITLE
})

export default router
