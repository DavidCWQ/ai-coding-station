<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'

import {
  canAccessIframeDocument,
  ensureVisualEditorBootstrapped,
  getUrlOrigin,
  isTrustedVisualEditorMessage,
  parseElementSelectedFromMessage,
  postVisualEditorActive,
} from '@/utils/visualEditor'
import type { VisualEditorSelectedElement } from '@/utils/visualEditor'

const props = defineProps<{
  src: string
  /** 父页面「可视化编辑」开关 */
  visualEditActive: boolean
}>()

const emit = defineEmits<{
  select: [payload: VisualEditorSelectedElement]
  /** 同源且 iframe 已加载但仍无法注入时（极少见） */
  'inject-failed': []
}>()

const iframeRef = ref<HTMLIFrameElement | null>(null)
const allowedOrigin = ref<string | null>(null)
/** 避免在 about:blank 阶段注入脚本 */
const iframeLoadedForSrc = ref(false)

const updateAllowedOrigin = () => {
  allowedOrigin.value = props.src ? getUrlOrigin(props.src) : null
}

const onWindowMessage = (ev: MessageEvent) => {
  try {
    const iframe = iframeRef.value
    if (!isTrustedVisualEditorMessage(ev, iframe, allowedOrigin.value)) return
    const payload = parseElementSelectedFromMessage(ev.data)
    if (payload) emit('select', payload)
  } catch {
    /* 忽略异常消息 */
  }
}

/** 将当前开关同步到 iframe 内脚本（注入 + postMessage） */
const applyVisualEditToIframe = (): boolean => {
  const iframe = iframeRef.value
  if (!iframe || !props.src) return false
  const origin = getUrlOrigin(props.src)
  if (!origin) return false
  if (!canAccessIframeDocument(iframe)) return false

  const parentOrigin = window.location.origin

  if (props.visualEditActive) {
    try {
      if (!ensureVisualEditorBootstrapped(iframe, parentOrigin)) return false
      postVisualEditorActive(iframe, true, origin)
      return true
    } catch {
      return false
    }
  }

  postVisualEditorActive(iframe, false, origin)
  return true
}

const scheduleApply = () => {
  void nextTick(() => {
    if (!props.visualEditActive) {
      applyVisualEditToIframe()
      return
    }
    if (!iframeLoadedForSrc.value) {
      return
    }
    const ok = applyVisualEditToIframe()
    if (!ok && props.visualEditActive) {
      message.warning(
        '无法启用可视化编辑：预览页与当前站点不同源。请通过本站同源地址访问静态预览（如 Vite / Nginx 将 /api 代理到后端）。',
      )
      emit('inject-failed')
    }
  })
}

const onIframeLoad = () => {
  updateAllowedOrigin()
  iframeLoadedForSrc.value = true
  scheduleApply()
}

onMounted(() => {
  updateAllowedOrigin()
  window.addEventListener('message', onWindowMessage)
})

onBeforeUnmount(() => {
  window.removeEventListener('message', onWindowMessage)
  const iframe = iframeRef.value
  const origin = allowedOrigin.value
  if (iframe && origin && canAccessIframeDocument(iframe)) {
    postVisualEditorActive(iframe, false, origin)
  }
})

watch(
  () => props.src,
  () => {
    iframeLoadedForSrc.value = false
    updateAllowedOrigin()
    void nextTick(() => scheduleApply())
  },
)

watch(
  () => props.visualEditActive,
  () => {
    scheduleApply()
  },
)
</script>

<template>
  <iframe
    ref="iframeRef"
    class="iframe-preview"
    :src="src"
    title="preview"
    @load="onIframeLoad"
  />
</template>

<style scoped>
.iframe-preview {
  flex: 1;
  width: 100%;
  border: none;
  background: #fff;
}
</style>
