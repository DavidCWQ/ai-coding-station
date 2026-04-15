<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { InfoCircleFilled } from '@ant-design/icons-vue'

const props = defineProps<{
  open: boolean
  initialTitle: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  submit: [title: string]
}>()

const localTitle = ref('')

watch(
  () => [props.open, props.initialTitle] as const,
  ([open, initialTitle]) => {
    if (open) {
      localTitle.value = String(initialTitle || '')
    }
  },
  { immediate: true },
)

const innerOpen = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
})

const onSubmit = () => {
  emit('submit', localTitle.value)
}

const onCancel = () => {
  innerOpen.value = false
}
</script>

<template>
  <a-modal
    v-model:open="innerOpen"
    :closable="false"
    :get-container="false"
    wrap-class-name="agent-rename-modal-wrap"
    class="agent-rename-modal"
    :width="440"
  >
    <template #title>
      <span class="agent-rename-modal__title">
        <InfoCircleFilled class="agent-rename-modal__icon" />
        重命名会话
      </span>
    </template>
    <div class="agent-rename-modal__body">
      <a-input
        v-model:value="localTitle"
        :maxlength="80"
        placeholder="请输入会话标题"
        @press-enter="onSubmit"
      />
    </div>
    <template #footer>
      <div class="agent-rename-modal__footer">
        <div class="agent-rename-modal__footer-inner">
          <a-button @click="onCancel">取消</a-button>
          <a-button type="primary" @click="onSubmit">保存</a-button>
        </div>
      </div>
    </template>
  </a-modal>
</template>

<style scoped>
.agent-rename-modal__title {
  display: inline-flex;
  align-items: center;
  font-weight: bold;
  margin: 0 0 -10px;
  gap: 10px;
}

.agent-rename-modal__icon {
  color: #1677ff;
  font-size: 22px;
}

.agent-rename-modal__body {
  margin: 12px 12px 8px 32px;
}

.agent-rename-modal :deep(.ant-modal-header) {
  padding: 20px 24px 0;
}

.agent-rename-modal :deep(.ant-modal-body) {
  padding: 0 24px 4px;
}

.agent-rename-modal__footer {
  padding: 4px 0 2px;
}

/* 右缘与输入框对齐 */
.agent-rename-modal__footer-inner {
  display: flex;
  justify-content: flex-end;
  margin: 0 13px 0 56px;
}
</style>

<!-- Modal 常挂在 body，scoped :deep 难以命中外层 .ant-modal-footer；用 wrap 类清掉默认 padding -->
<style>
.agent-rename-modal-wrap .ant-modal-footer {
  padding: 0 !important;
  margin: 0;
  border-top: none;
}
</style>

