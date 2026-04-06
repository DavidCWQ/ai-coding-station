<script setup lang="ts">
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<template>
  <div class="blank-layout">
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
