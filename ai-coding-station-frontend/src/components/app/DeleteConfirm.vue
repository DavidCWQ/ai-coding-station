<script setup lang="ts">
import { computed } from 'vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  open: boolean
  loading?: boolean
  title?: string
  content?: string
}>()

const emit = defineEmits<{
  'update:open': [v: boolean]
  confirm: []
}>()

const modalOpen = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v),
})

const onCancel = () => {
  modalOpen.value = false
}

const onConfirm = async () => {
  emit('confirm')
}
</script>

<template>
  <a-modal
    v-model:open="modalOpen"
    :confirm-loading="loading"
    ok-text="删除"
    cancel-text="取消"
    @ok="onConfirm"
    @cancel="onCancel"
  >
    <template #title>
      <span class="delete-confirm__title">
        <ExclamationCircleOutlined class="delete-confirm__icon" />
        <span>{{ title || '确认删除该应用？' }}</span>
      </span>
    </template>
    <p>{{ content || '删除后无法恢复，请谨慎操作。' }}</p>
  </a-modal>
</template>

<style scoped>
.delete-confirm__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.delete-confirm__icon {
  font-size: 24px;
  color: #faad14;
}
</style>
