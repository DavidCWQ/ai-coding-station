<script setup lang="ts">
import { ref, watch } from 'vue'
import { PaperClipOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  modelValue: string
  loading?: boolean
  disabled?: boolean
  placeholder?: string
  imageUploading?: boolean
  replaceLoading?: boolean
  canReplaceImage?: boolean
  selectedImageName?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [v: string]
  submit: []
  cancel: []
  'choose-image': [file: File]
  'replace-image': []
  'clear-image': []
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

const fileInput = ref<HTMLInputElement | null>(null)

const openFilePicker = () => {
  if (props.loading || props.disabled || props.imageUploading || props.replaceLoading) return
  fileInput.value?.click()
}

const onFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  emit('choose-image', file)
  target.value = ''
}

const onReplaceImage = () => {
  emit('replace-image')
}

const onClearImage = () => {
  emit('clear-image')
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
    <div v-if="selectedImageName" class="chat-input__attachment chat-input__attachment--top">
      <span class="chat-input__attachment-name">已选图片：{{ selectedImageName }}</span>
      <button
        type="button"
        class="chat-input__attachment-clear"
        :disabled="imageUploading || replaceLoading"
        @click="onClearImage"
      >
        <CloseCircleOutlined />
      </button>
    </div>
    <a-textarea
      :value="local"
      :rows="3"
      :placeholder="placeholder ?? '描述越详细，页面越具体，可以一步一步完善生成效果'"
      :disabled="loading || disabled"
      class="chat-input__area"
      @update:value="onInput"
      @press-enter="onPressEnter"
    />
    <div class="chat-input__bar">
      <div class="chat-input__hints">
        <span class="chat-input__hint">Enter 发送 · Shift+Enter 换行</span>
      </div>
      <a-space :size="8">
        <input
          ref="fileInput"
          type="file"
          accept="image/png,image/jpeg,image/jpg,image/webp,image/gif,image/svg+xml"
          style="display: none"
          @change="onFileChange"
        />
        <a-button
          :loading="imageUploading"
          :disabled="loading || disabled || replaceLoading"
          @click="openFilePicker"
        >
          <template #icon><PaperClipOutlined /></template>
          上传图片
        </a-button>
        <a-tooltip :title="!canReplaceImage ? '进入编辑后，选中图片替换' : ''">
          <a-button
            :loading="replaceLoading"
            :disabled="!canReplaceImage || loading || disabled || imageUploading"
            @click="onReplaceImage"
          >
            替换图片
          </a-button>
        </a-tooltip>
        <a-button v-if="loading" danger @click="onCancel">停止</a-button>
        <a-button type="primary" :loading="loading" :disabled="disabled" @click="onSubmit">发送</a-button>
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

.chat-input__attachment {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 12px;
  padding: 2px 8px;
  background: rgba(22, 119, 255, 0.08);
}

.chat-input__attachment--top {
  margin-bottom: 8px;
}

.chat-input__attachment-name {
  font-size: 12px;
  color: #1677ff;
}

.chat-input__attachment-clear {
  border: none;
  background: transparent;
  color: rgba(0, 0, 0, 0.45);
  padding: 0;
  line-height: 1;
  cursor: pointer;
}
</style>
