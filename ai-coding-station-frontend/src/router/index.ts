import { createRouter, createWebHistory } from 'vue-router'

import { homeRoutes } from './routes/home'
import { userRoutes } from './routes/user'
import { adminRoutes } from './routes/admin'
import { aiRoutes } from './routes/ai'
import { commonRoutes } from './routes/common'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [...homeRoutes, ...userRoutes, ...adminRoutes, ...aiRoutes, ...commonRoutes],
})

export default router
