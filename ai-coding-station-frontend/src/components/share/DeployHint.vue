<script setup lang="ts">
import { computed } from 'vue'

import LineIcon from '@/components/icons/LineIcon.vue'

const props = defineProps<{
  open: boolean
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  deploy: []
}>()

const modalOpen = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
})
</script>

<template>
  <a-modal v-model:open="modalOpen" :footer="null" :width="400" centered title="先部署，再分享">
    <div class="deploy-hint">
      <div class="deploy-hint__icon">
        <LineIcon name="share" />
      </div>
      <p>需要先部署，才能把作品分享给别人。</p>
      <div class="deploy-hint__actions">
        <a-button @click="modalOpen = false">取消</a-button>
        <a-button type="primary" :loading="loading" @click="emit('deploy')">去部署</a-button>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.deploy-hint {
  text-align: center;
  padding-top: 4px;
}

.deploy-hint__icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: #e6f4ff;
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.deploy-hint__icon :deep(svg) {
  width: 22px;
  height: 22px;
}

.deploy-hint p {
  margin: 0 0 18px;
  color: rgba(0, 0, 0, 0.72);
  font-size: 14px;
}

.deploy-hint__actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
