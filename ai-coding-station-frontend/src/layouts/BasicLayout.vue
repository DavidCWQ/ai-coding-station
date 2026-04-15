<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()

const showFooter = computed(() => route.meta?.showFooter === true)
const fullWidth = computed(() => route.meta?.fullWidth === true)
const noContentCard = computed(() => route.meta?.noContentCard === true)
</script>

<template>
  <a-layout class="basic-layout">
    <GlobalHeader />
    <a-layout-content class="basic-layout__content" :class="{ 'basic-layout__content--full': fullWidth }">
      <div class="basic-layout__content-inner" :class="{ 'basic-layout__content-inner--plain': noContentCard }">
        <router-view />
      </div>
    </a-layout-content>
    <GlobalFooter v-if="showFooter" />
  </a-layout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: #f5f5f5;
}

.basic-layout__content {
  padding: 24px 16px;
}

.basic-layout__content-inner {
  margin: 0 auto;
  max-width: 1200px;
  min-height: calc(100vh - 64px - 120px - 48px);
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
}

.basic-layout__content--full {
  padding: 12px;
}

.basic-layout__content-inner--plain {
  max-width: none;
  padding: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  min-height: calc(100vh - 64px - 24px);
}

@media (max-width: 768px) {
  .basic-layout__content {
    padding: 16px 12px;
  }

  .basic-layout__content-inner {
    padding: 16px;
    border-radius: 10px;
  }
}
</style>

