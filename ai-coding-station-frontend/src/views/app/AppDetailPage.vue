<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

import ChatInput from '@/components/app/ChatInput.vue'
import ChatMessage from '@/components/app/ChatMessage.vue'
import { deployApp, getApp } from '@/api/appController'
import { streamChatGenCode } from '@/hooks/useSSEChat'
import { apiLongId, appIdFromData } from '@/utils/appId'
import { buildStaticPreviewUrl } from '@/utils/appPreview'

type ChatRow = {
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}

const route = useRoute()
const router = useRouter()

const appId = computed(() => String(route.params.appId ?? ''))

const appVo = ref<API.AppVO | null>(null)
const pageLoading = ref(true)
const chatLoading = ref(false)
const inputText = ref('')
const messages = ref<ChatRow[]>([])
const listEl = ref<HTMLElement | null>(null)
const previewUrl = ref('')
const deployLoading = ref(false)

let abortCtl: AbortController | null = null
let didInitial = false

const scrollEnd = async () => {
  await nextTick()
  const el = listEl.value
  if (el) el.scrollTop = el.scrollHeight
}

const getErr = (err: unknown): string => {
  if (typeof err === 'object' && err !== null) {
    const m = (err as Record<string, unknown>).message
    if (typeof m === 'string') return m
  }
  return '操作失败'
}

const loadApp = async () => {
  pageLoading.value = true
  try {
    const res = await getApp({ id: apiLongId(appId.value) })
    appVo.value = res.data?.data ?? null
    await maybeRunInitial()
  } catch (e) {
    message.error(getErr(e))
    appVo.value = null
  } finally {
    pageLoading.value = false
  }
}

const appendAssistantStreaming = () => {
  messages.value.push({ role: 'assistant', content: '', streaming: true })
}

const finishAssistant = () => {
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'assistant') {
    last.streaming = false
  }
}

const runStream = async (text: string) => {
  abortCtl?.abort()
  abortCtl = new AbortController()
  chatLoading.value = true
  appendAssistantStreaming()
  await scrollEnd()
  const idx = messages.value.length - 1
  try {
    await streamChatGenCode(
      appId.value,
      text,
      (chunk) => {
        const row = messages.value[idx]
        if (row && row.role === 'assistant') {
          row.content += chunk
        }
      },
      { signal: abortCtl.signal },
    )
    finishAssistant()
    const app = appVo.value
    if (app) {
      const cg = app.codeGenType || 'html'
      previewUrl.value = buildStaticPreviewUrl(cg, appIdFromData(app.id))
    }
  } catch (e) {
    if ((e as Error).name === 'AbortError') return
    message.error(getErr(e))
    finishAssistant()
  } finally {
    chatLoading.value = false
    await scrollEnd()
  }
}

const sendUser = async (text: string) => {
  const t = text.trim()
  if (!t || chatLoading.value) return
  messages.value.push({ role: 'user', content: t })
  inputText.value = ''
  await scrollEnd()
  await runStream(t)
}

const maybeRunInitial = async () => {
  if (didInitial || !appVo.value) return
  const init = appVo.value.initPrompt?.trim()
  if (!init) return
  didInitial = true
  messages.value.push({ role: 'user', content: init })
  await scrollEnd()
  await runStream(init)
}

const onDeploy = async () => {
  try {
    deployLoading.value = true
    const res = await deployApp({ appId: apiLongId(appId.value) })
    const url = res.data?.data
    if (url) {
      message.success('部署成功')
      window.open(url, '_blank', 'noopener,noreferrer')
    }
  } catch (e) {
    message.error(getErr(e))
  } finally {
    deployLoading.value = false
  }
}

watch(appId, async () => {
  didInitial = false
  messages.value = []
  previewUrl.value = ''
  await loadApp()
})

onMounted(async () => {
  await loadApp()
})

onBeforeUnmount(() => {
  abortCtl?.abort()
})
</script>

<template>
  <div class="app-detail">
    <a-spin :spinning="pageLoading">
      <template v-if="appVo">
        <header class="app-detail__header">
          <a-button type="text" @click="router.push('/app')">← 返回</a-button>
          <span class="app-detail__name">{{ appVo.appName || '应用' }}</span>
          <a-button type="primary" :loading="deployLoading" @click="onDeploy">
            部署
          </a-button>
        </header>

        <div class="app-detail__main">
          <div class="app-detail__chat">
            <div ref="listEl" class="app-detail__messages">
              <ChatMessage
                v-for="(m, i) in messages"
                :key="i"
                :role="m.role"
                :content="m.content"
                :streaming="m.streaming"
              />
            </div>
            <ChatInput
              v-model="inputText"
              :loading="chatLoading"
              @submit="sendUser(inputText)"
            />
          </div>
          <div class="app-detail__preview">
            <div class="app-detail__preview-title">生成预览</div>
            <iframe v-if="previewUrl" class="app-detail__iframe" :src="previewUrl" title="preview" />
            <a-empty v-else description="生成完成后将在此展示预览" />
          </div>
        </div>
      </template>
      <a-empty v-else-if="!pageLoading" description="应用不存在或无权访问" />
    </a-spin>
  </div>
</template>

<style scoped>
.app-detail {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.app-detail__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
  position: sticky;
  top: 0;
  z-index: 10;
}

.app-detail__name {
  flex: 1;
  font-weight: 600;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-detail__main {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
  gap: 0;
  min-height: calc(100vh - 57px);
}

@media (max-width: 960px) {
  .app-detail__main {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(320px, 45vh) minmax(400px, 55vh);
  }
}

.app-detail__chat {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid rgba(5, 5, 5, 0.06);
  min-height: 0;
}

.app-detail__messages {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.app-detail__chat :deep(.chat-input) {
  border-radius: 0;
  border-left: none;
  border-right: none;
  border-bottom: none;
}

.app-detail__preview {
  background: #fafafa;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.app-detail__preview-title {
  padding: 10px 16px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
  background: #fff;
}

.app-detail__iframe {
  flex: 1;
  width: 100%;
  border: none;
  background: #fff;
}

.app-detail__preview :deep(.ant-empty) {
  margin: auto;
}
</style>
