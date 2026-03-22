import { createRouter, createWebHistory } from 'vue-router'

import { homeRoutes } from './routes/home'
import { userRoutes } from './routes/user'
import { adminRoutes } from './routes/admin'
import { appRoutes } from './routes/app'
import { aiRoutes } from './routes/ai'
import { commonRoutes } from './routes/common'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [...homeRoutes, ...userRoutes, ...adminRoutes, ...appRoutes, ...aiRoutes, ...commonRoutes],
})

export default router
