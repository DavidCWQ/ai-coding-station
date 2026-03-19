<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { MenuProps } from 'ant-design-vue'

import { appMenu, type AppMenuItem } from '@/config/menu'

const router = useRouter()
const route = useRoute()

const isAuthed = ref(false)
const mobileOpen = ref(false)
const isMobile = ref(false)

const selectedKeys = computed(() => {
  const match = appMenu.find((m) => m.path === route.path)
  return match ? [match.key] : []
})

const menuItems = computed<MenuProps['items']>(() =>
  appMenu.map((m: AppMenuItem) => ({
    key: m.key,
    label: m.label,
  })),
)

const updateIsMobile = () => {
  isMobile.value = window.matchMedia('(max-width: 768px)').matches
}

const onMenuClick: MenuProps['onClick'] = (info) => {
  const target = appMenu.find((m) => m.key === String(info.key))
  if (!target) return
  router.push(target.path)
  mobileOpen.value = false
}

const onLogin = () => {
  message.info('未接入登录系统：这里可对接你的鉴权逻辑')
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
        <template v-if="isAuthed">已登录</template>
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

