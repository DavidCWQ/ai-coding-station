<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { CheckCircleOutlined, CopyOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  open: boolean
  url: string
}>()

const emit = defineEmits<{
  'update:open': [v: boolean]
}>()

const modalOpen = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v),
})

const onClose = () => {
  modalOpen.value = false
}

const onVisit = () => {
  if (props.url) {
    window.open(props.url, '_blank', 'noopener,noreferrer')
  }
  modalOpen.value = false
}

const onCopy = async () => {
  if (!props.url) return
  try {
    await navigator.clipboard.writeText(props.url)
    message.success('链接已复制')
  } catch {
    message.warning('复制失败，请手动复制')
  }
}
</script>

<template>
  <a-modal
    v-model:open="modalOpen"
    title="部署成功"
    :footer="null"
    :centered="false"
    :width="'50vw'"
    wrap-class-name="app-deploy-success-modal"
    @cancel="onClose"
  >
    <div class="deploy-success-modal">
      <CheckCircleOutlined class="deploy-success-modal__ok-icon" />
      <div class="deploy-success-modal__title">网站部署成功!</div>
      <p class="deploy-success-modal__desc">你的网站已经成功部署，可以通过以下链接访问：</p>
      <div class="deploy-success-modal__link">
        <span class="deploy-success-modal__text">{{ url }}</span>
        <button class="ant-btn ant-btn-text ant-btn-icon-only" title="复制链接" @click="onCopy">
          <CopyOutlined />
        </button>
      </div>
      <div class="deploy-success-modal__footer">
        <a-button type="primary" class="deploy-success-modal__visit" @click="onVisit">访问</a-button>
        <a-button type="primary" ghost class="deploy-success-modal__close" @click="onClose">关闭</a-button>
      </div>
    </div>
  </a-modal>
</template>

<style>
.app-deploy-success-modal .ant-modal {
  top: 6%;
  min-width: 560px;
}

.app-deploy-success-modal .ant-modal-content {
  border-radius: 8px;
}

.app-deploy-success-modal .ant-modal-body {
  padding: 16px 16px 24px;
}

.app-deploy-success-modal .ant-modal-title {
  font-size: 18px;
  line-height: 1.2;
}

.deploy-success-modal {
  text-align: center;
}

.deploy-success-modal__ok-icon {
  font-size: 48px;
  color: #52c41a;
  display: block;
  margin: 10px auto 16px;
}

.deploy-success-modal__title {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
  margin-bottom: 14px;
}

.deploy-success-modal__desc {
  margin: 0 0 16px;
  font-size: 16px;
  color: rgba(0, 0, 0, 0.65);
}

.deploy-success-modal__link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid rgba(5, 5, 5, 0.1);
  border-radius: 7px;
  padding: 6px 8px;
  background: #fff;
}

.deploy-success-modal__text {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 20px;
  text-align: left;
}

.deploy-success-modal__link .ant-btn-icon-only {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
  color: rgba(0, 0, 0, 0.65);
  font-size: 15px;
  width: 22px;
  height: 22px;
  cursor: pointer;
  margin-right: 4px;
}

.deploy-success-modal__footer {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 9px;
}

.deploy-success-modal__visit,
.deploy-success-modal__close {
  min-width: 76px;
  height: 32px;
  font-size: 14px;
  border-radius: 6px;
}

.deploy-success-modal__close {
  min-width: 68px;
}
</style>
