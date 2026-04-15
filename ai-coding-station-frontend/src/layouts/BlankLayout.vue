<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()
const showHeader = computed(() => route.meta?.showHeader === true)
</script>

<template>
  <div class="blank-layout">
    <GlobalHeader v-if="showHeader" />
    <div class="blank-layout__main">
      <router-view />
    </div>
    <GlobalFooter />
  </div>
</template>

<style scoped>
.blank-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-background);
}

.blank-layout__main {
  flex: 1 0 auto;
}
</style>
