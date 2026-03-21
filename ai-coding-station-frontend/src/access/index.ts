import router from '@/router'
import { useLoginUserStore } from '@/stores/loginUser'

import { ACCESS_ENUM } from './accessEnum'
import { checkAccess } from './checkAccess'
import type { AccessEnum } from './accessEnum'

router.beforeEach(async (to) => {
  const store = useLoginUserStore()

  if (!store.loginUser || !store.loginUser.userRole) {
    await store.fetchLoginUser()
  }

  const need = to.meta.access as AccessEnum | undefined

  if (need === undefined || need === ACCESS_ENUM.NOT_LOGIN) {
    return true
  }

  if (checkAccess(store.loginUser, need)) {
    return true
  }

  if (!store.loginUser?.id) {
    return {
      path: '/user/login',
      query: { redirect: to.fullPath },
    }
  }

  return { path: '/noAuth' }
})
