<script setup lang="ts">
import { computed, getCurrentInstance, h, onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { MenuProps } from 'ant-design-vue'
import { LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'

import UserAvatar from '@/components/UserAvatar.vue'
import { appMenu, type AppMenuItem } from '@/config/menu'
import { ACCESS_ENUM } from '@/access/accessEnum'
import { checkAccess } from '@/access/checkAccess'
import { useLoginUserStore } from '@/stores/loginUser'

const vm = getCurrentInstance()
const route = computed(() => vm?.proxy?.$route as any)
const router = vm?.proxy?.$router as any
const loginUserStore = useLoginUserStore()

const mobileOpen = ref(false)
const isMobile = ref(false)

const loginUser = computed(() => loginUserStore.loginUser)
const isLoggedIn = computed(() => loginUserStore.isLoggedIn)

const visibleMenuItems = computed(() => {
  const loginUser = loginUserStore.loginUser
  return appMenu.filter((item) => {
    const resolved = router.resolve(item.path)
    const leaf = resolved.matched[resolved.matched.length - 1]
    if (!leaf) return false
    if (leaf.meta.hideInMenu) return false
    const need = leaf.meta.access ?? ACCESS_ENUM.NOT_LOGIN
    return checkAccess(loginUser, need)
  })
})

const selectedKeys = computed(() => {
  const path = route.value.path
  const match = visibleMenuItems.value.find((m) => {
    if (m.path === path) return true
    if (m.path !== '/agents') return false
    return path.startsWith('/agents')
  })
  return match ? [match.key] : []
})

const menuItems = computed<MenuProps['items']>(() =>
  visibleMenuItems.value.map((m: AppMenuItem) => ({
    key: m.key,
    label: m.label,
  })),
)

const userMenuItems = computed<MenuProps['items']>(() => [
  {
    key: 'profile',
    icon: () => h(UserOutlined),
    label: '个人主页',
  },
  {
    key: 'logout',
    icon: () => h(LogoutOutlined),
    label: '退出登录',
  },
])

const updateIsMobile = () => {
  isMobile.value = window.matchMedia('(max-width: 768px)').matches
}

const onMenuClick: MenuProps['onClick'] = (info) => {
  const target = visibleMenuItems.value.find((m) => m.key === String(info.key))
  if (!target) return
  router.push(target.path)
  mobileOpen.value = false
}

const onLogin = () => {
  router.push('/user/login')
}

const onLogout = async () => {
  const ok = await loginUserStore.logout()
  if (ok) {
    message.success('已退出登录')
  } else {
    message.error('注销请求失败，已清除本地登录状态')
  }
  await router.push('/user/login')
}

const onUserMenuClick: MenuProps['onClick'] = async (info) => {
  const key = String(info.key)
  if (key === 'profile') {
    await router.push('/user/profile')
    return
  }
  if (key === 'logout') {
    await onLogout()
  }
}

onMounted(() => {
  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateIsMobile)
})
</script>

<template>
  <a-layout-header class="global-header">
    <div class="global-header__inner">
      <RouterLink to="/" class="global-header__brand" aria-label="Go Home">
        <img class="global-header__logo" src="@/assets/logo.svg" alt="logo" />
        <span class="global-header__title">AI Coding Station</span>
      </RouterLink>

      <div class="global-header__menu">
        <a-menu
          mode="horizontal"
          :selected-keys="selectedKeys"
          :items="menuItems"
          @click="onMenuClick"
        />
      </div>

      <div class="global-header__right">
        <a-dropdown v-if="isLoggedIn" :trigger="['click']" placement="bottomRight">
          <div class="global-header__user-trigger" role="button" tabindex="0">
            <UserAvatar
              :src="loginUser?.userAvatar"
              :name="loginUser?.userName"
              :account="loginUser?.userAccount"
            />
            <span class="global-header__username">
              {{ loginUser?.userName || loginUser?.userAccount }}
            </span>
          </div>
          <template #overlay>
            <a-menu :items="userMenuItems" @click="onUserMenuClick" />
          </template>
        </a-dropdown>
        <a-button v-else type="primary" @click="onLogin">登录</a-button>
        <a-button v-if="isMobile" type="text" class="global-header__mobile-btn" @click="mobileOpen = true">
          菜单
        </a-button>
      </div>
    </div>

    <a-drawer v-model:open="mobileOpen" placement="right" title="导航" :width="260">
      <a-menu
        mode="inline"
        :selected-keys="selectedKeys"
        :items="menuItems"
        @click="onMenuClick"
      />
    </a-drawer>
  </a-layout-header>
</template>

<style scoped>
.global-header {
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 0;
  background: #fff;
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
}

.global-header__inner {
  height: 64px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.global-header__brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 200px;
  color: rgba(0, 0, 0, 0.88);
}

.global-header__logo {
  width: 28px;
  height: 28px;
}

.global-header__title {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.global-header__menu {
  flex: 1;
  min-width: 0;
}

.global-header__right {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.global-header__user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 200px;
  padding: 0 8px;
  border-radius: 8px;
  cursor: pointer;
  outline: none;
}

.global-header__user-trigger:hover {
  background: rgba(0, 0, 0, 0.04);
}

.global-header__username {
  color: rgba(0, 0, 0, 0.88);
  font-size: 14px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-transform: capitalize;
}

.global-header__mobile-btn {
  display: none;
}

@media (max-width: 768px) {
  .global-header__menu {
    display: none;
  }

  .global-header__brand {
    min-width: 0;
  }

  .global-header__mobile-btn {
    display: inline-flex;
  }
}
</style>
