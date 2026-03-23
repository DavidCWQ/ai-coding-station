<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: string
  loading?: boolean
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [v: string]
  submit: []
  cancel: []
}>()

const local = ref(props.modelValue)

watch(
  () => props.modelValue,
  (v) => {
    local.value = v
  },
)

const onInput = (v: string) => {
  local.value = v
  emit('update:modelValue', v)
}

const onSubmit = () => {
  emit('submit')
}

const onCancel = () => {
  emit('cancel')
}

const onPressEnter = (e: Event) => {
  const ke = e as KeyboardEvent
  if (!ke.shiftKey) {
    ke.preventDefault()
    onSubmit()
  }
}
</script>

<template>
  <div class="chat-input">
    <a-textarea
      :value="local"
      :rows="3"
      :placeholder="placeholder ?? '描述越详细，页面越具体，可以一步一步完善生成效果'"
      :disabled="loading"
      class="chat-input__area"
      @update:value="onInput"
      @press-enter="onPressEnter"
    />
    <div class="chat-input__bar">
      <div class="chat-input__hints">
        <span class="chat-input__hint">Enter 发送 · Shift+Enter 换行</span>
      </div>
      <a-space :size="8">
        <a-button v-if="loading" danger @click="onCancel">停止</a-button>
        <a-button type="primary" :loading="loading" @click="onSubmit">发送</a-button>
      </a-space>
    </div>
  </div>
</template>

<style scoped>
.chat-input {
  border: 1px solid rgba(5, 5, 5, 0.12);
  border-radius: 12px;
  padding: 10px 12px;
  background: #fff;
}

.chat-input__area {
  border: none !important;
  box-shadow: none !important;
  padding: 4px 0 !important;
  resize: none;
}

.chat-input__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
}

.chat-input__hints {
  flex: 1;
  min-width: 0;
}

.chat-input__hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
