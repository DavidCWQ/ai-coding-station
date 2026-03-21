import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { getLoginUser, userLogout } from '@/api/userController'

export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO | null>(null)

  const isLoggedIn = computed(() => !!loginUser.value?.id)

  let fetched = false

  const fetchLoginUser = async (options?: {
    force?: boolean
  }): Promise<API.LoginUserVO | null> => {
    const force = options?.force === true
    if (!force && fetched && loginUser.value) {
      return loginUser.value
    }

    try {
      const res = await getLoginUser()
      const user = res.data?.data ?? null
      setLoginUser(user)
      fetched = true
      return user
    } catch {
      setLoginUser(null)
      fetched = false
      return null
    }
  }

  const setLoginUser = (user: API.LoginUserVO | null) => {
    loginUser.value = user ?? null
  }

  const logout = async (): Promise<boolean> => {
    let ok = false
    try {
      await userLogout()
      ok = true
    } catch {
      // 即使后端 logout 失败，也保证前端状态清空
    }
    loginUser.value = null
    return ok
  }

  return { loginUser, isLoggedIn, fetchLoginUser, setLoginUser, logout }
})

