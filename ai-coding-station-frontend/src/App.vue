<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'

import BasicLayout from '@/layouts/BasicLayout.vue'
import BlankLayout from '@/layouts/BlankLayout.vue'

import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()

const loginUserStore = useLoginUserStore()

onMounted(async () => {
  // 避免重复请求：已有用户态则不再拉取
  if (!loginUserStore.loginUser?.id) {
    await loginUserStore.fetchLoginUser()
  }
})

const layoutComponent = computed(() => {
  const layout = (route.meta?.layout as string | undefined) ?? 'basic'
  return layout === 'blank' ? BlankLayout : BasicLayout
})
</script>

<template>
  <component :is="layoutComponent" />
</template>
