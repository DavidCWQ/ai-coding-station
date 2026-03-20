<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import BasicLayout from '@/layouts/BasicLayout.vue'
import BlankLayout from '@/layouts/BlankLayout.vue'

const route = useRoute()

const layoutComponent = computed(() => {
  const layout = (route.meta?.layout as string | undefined) ?? 'basic'
  return layout === 'blank' ? BlankLayout : BasicLayout
})
</script>

<template>
  <component :is="layoutComponent" />
</template>
