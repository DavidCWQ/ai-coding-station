<script setup lang="ts">
import { computed } from 'vue'
import { ExclamationCircleFilled } from '@ant-design/icons-vue'

const props = defineProps<{
  open: boolean
  /** 删除逻辑；成功 resolve，失败 reject（Modal 会保持打开） */
  onConfirm?: () => Promise<void>
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

const innerOpen = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v),
})

const handleOk = async () => {
  if (props.onConfirm) {
    await props.onConfirm()
  }
}
</script>

<template>
  <a-modal
    v-model:open="innerOpen"
    :closable="false"
    :get-container="false"
    ok-text="删除"
    cancel-text="取消"
    ok-type="danger"
    class="agent-delete-modal"
    :width="416"
    @ok="handleOk"
  >
    <template #title>
      <span class="agent-delete-modal__title">
        <ExclamationCircleFilled class="agent-delete-modal__icon" />
        确认删除会话？
      </span>
    </template>
    <p class="agent-delete-modal__content">删除后不可恢复</p>
  </a-modal>
</template>

<style scoped>
.agent-delete-modal__title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.agent-delete-modal__icon {
  color: #faad14;
  font-size: 22px;
}

.agent-delete-modal__content {
  margin: 8px 0 0;
  padding-left: 32px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
  line-height: 1.6;
}

.agent-delete-modal :deep(.ant-modal-header) {
  padding: 20px 24px 8px;
}

.agent-delete-modal :deep(.ant-modal-body) {
  padding: 0 24px 8px;
}

.agent-delete-modal :deep(.ant-modal-footer) {
  padding: 12px 24px 20px;
}
</style>
