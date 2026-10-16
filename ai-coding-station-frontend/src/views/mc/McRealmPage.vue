<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import McRealmView from '@/components/mc/McRealmView.vue'

const route = useRoute()
const admin = computed(() => route.path.startsWith('/admin/mc'))
</script>

<template>
  <McRealmView :admin="admin" />
</template>
