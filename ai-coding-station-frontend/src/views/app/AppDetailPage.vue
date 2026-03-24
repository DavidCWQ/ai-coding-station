<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { CloudUploadOutlined, ExportOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'

import BackButton from '@/components/common/backButton.vue'
import ChatInput from '@/components/app/ChatInput.vue'
import ChatMessage from '@/components/app/ChatMessage.vue'
import AppDetail from '@/components/app/AppDetail.vue'
import DeleteConfirm from '@/components/app/DeleteConfirm.vue'
import SuccessfulDeploy from '@/components/app/SuccessfulDeploy.vue'
import { adminDeleteApp } from '@/api/appAdminController'
import { deleteApp, deployApp, getApp } from '@/api/appController'
import { streamChatGenCode } from '@/hooks/useSSEChat'
import { useLoginUserStore } from '@/stores/loginUser'
import { apiLongId, appIdFromData } from '@/utils/appId'
import { getErrorMessage } from '@/utils/error'
import { waitForStaticPreviewReady } from '@/utils/appPreview'

type ChatRow = {
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appId = computed(() => String(route.params.appId ?? ''))
const isViewOnlyEntry = computed(() => String(route.query.view ?? '') === '1')

const appVo = ref<API.AppVO | null>(null)
const pageLoading = ref(true)
const chatLoading = ref(false)
const inputText = ref('')
const messages = ref<ChatRow[]>([])
const listEl = ref<HTMLElement | null>(null)
const previewUrl = ref('')
const previewFailed = ref(false)
const deployLoading = ref(false)
const deploySuccessOpen = ref(false)
const deploySuccessUrl = ref('')
const infoVisible = ref(false)
const deleteConfirmOpen = ref(false)
const deleteLoading = ref(false)

let abortCtl: AbortController | null = null
let didInitial = false
let cancelledByUser = false

const isAdmin = computed(() => loginUserStore.loginUser?.userRole === 'admin')
const isOwner = computed(() => {
  const uid = loginUserStore.loginUser?.id
  const ownerId = appVo.value?.userId
  if (!uid || !ownerId) return false
  return String(uid) === String(ownerId)
})
const canOperate = computed(() => isOwner.value || isAdmin.value)
const canChat = computed(() => isOwner.value)
const formattedCreateTime = computed(() => {
  const t = appVo.value?.createTime
  if (!t) return '—'
  const d = dayjs(t)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : t.replace('T', ' ')
})

const scrollEnd = async () => {
  await nextTick()
  const el = listEl.value
  if (el) el.scrollTop = el.scrollHeight
}

const loadApp = async () => {
  pageLoading.value = true
  try {
    const res = await getApp({ id: apiLongId(appId.value) })
    appVo.value = res.data?.data ?? null
  } catch (e) {
    message.error(getErrorMessage(e))
    appVo.value = null
  } finally {
    pageLoading.value = false
  }
  if (appVo.value) {
    void maybeRunInitial()
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
  cancelledByUser = false
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
      previewFailed.value = false
      previewUrl.value = ''
      const cg = app.codeGenType || 'html'
      const ready = await waitForStaticPreviewReady(cg, appIdFromData(app.id), {
        signal: abortCtl.signal,
      })
      if (ready.ok) {
        previewUrl.value = ready.url
      } else if (!cancelledByUser) {
        previewFailed.value = true
        message.warning('预览暂不可用：文件尚未就绪或生成失败，可稍后重试或查看服务端日志')
      }
    }
  } catch (e) {
    if ((e as Error).name === 'AbortError') {
      finishAssistant()
      if (cancelledByUser) {
        message.info('已停止本次生成')
      }
      return
    }
    message.error(getErrorMessage(e))
    finishAssistant()
  } finally {
    chatLoading.value = false
    abortCtl = null
    await scrollEnd()
  }
}

const sendUser = async (text: string) => {
  if (!canChat.value) {
    message.warning('亲，无法在别人的作品下对话哦~')
    return
  }
  const t = text.trim()
  if (!t || chatLoading.value) return
  messages.value.push({ role: 'user', content: t })
  inputText.value = ''
  await scrollEnd()
  await runStream(t)
}

const cancelStream = () => {
  if (!chatLoading.value || !abortCtl) return
  cancelledByUser = true
  abortCtl.abort()
}

const maybeRunInitial = async () => {
  if (didInitial || !appVo.value || isViewOnlyEntry.value || !canChat.value) return
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
      deploySuccessUrl.value = url
      deploySuccessOpen.value = true
    }
  } catch (e) {
    message.error(getErrorMessage(e))
  } finally {
    deployLoading.value = false
  }
}

const onDelete = () => {
  if (!canOperate.value || !appVo.value) return
  deleteConfirmOpen.value = true
}

const openPreviewInNewWindow = () => {
  if (!previewUrl.value) {
    message.info('暂无可打开的预览')
    return
  }
  window.open(previewUrl.value, '_blank', 'noopener,noreferrer')
}

const confirmDelete = async () => {
  if (!appVo.value) return
  deleteLoading.value = true
  const id = appIdFromData(appVo.value.id)
  try {
    if (isAdmin.value) {
      await adminDeleteApp({ id: apiLongId(id) })
    } else {
      await deleteApp({ id: apiLongId(id) })
    }
    deleteConfirmOpen.value = false
    infoVisible.value = false
    message.success('删除成功')
    await router.push('/app')
  } catch (e) {
    message.error(getErrorMessage(e))
  } finally {
    deleteLoading.value = false
  }
}

watch(appId, async () => {
  didInitial = false
  messages.value = []
  previewUrl.value = ''
  previewFailed.value = false
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
          <BackButton @click="router.push('/app')" />
          <span class="app-detail__name">{{ appVo.appName || '应用' }}</span>
          <a-button @click="infoVisible = true">
            <template #icon><InfoCircleOutlined /></template>
            详情
          </a-button>
          <a-button type="primary" :loading="deployLoading" @click="onDeploy">
            <template #icon><CloudUploadOutlined /></template>
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
            <a-tooltip :title="canChat ? '' : '亲，无法在别人的作品下对话哦~'">
              <div>
                <ChatInput
                  v-model="inputText"
                  :loading="chatLoading"
                  :disabled="!canChat"
                  @submit="sendUser(inputText)"
                  @cancel="cancelStream"
                />
              </div>
            </a-tooltip>
          </div>
          <div class="app-detail__preview">
            <div class="app-detail__preview-title">
              <span>生成预览</span>
              <button type="button" class="app-detail__preview-open" @click="openPreviewInNewWindow">
                <ExportOutlined />
                <span>新窗口打开</span>
              </button>
            </div>
            <iframe v-if="previewUrl" class="app-detail__iframe" :src="previewUrl" title="preview" />
            <a-empty v-else-if="previewFailed">
              <template #description>
                <span>未能加载静态预览（文件未就绪或服务异常）。可稍后发送一条消息重新生成，或检查服务端代码输出目录与日志。</span>
              </template>
            </a-empty>
            <a-empty v-else description="生成完成后将在此展示预览" />
          </div>
        </div>
      </template>
      <a-empty v-else-if="!pageLoading" description="应用不存在或无权访问" />
    </a-spin>
    <AppDetail
      v-model:open="infoVisible"
      :app="appVo"
      :can-operate="canOperate"
      :create-time-text="formattedCreateTime"
      @edit="router.push(`/app/edit/${appId}`)"
      @delete="onDelete"
    />
    <DeleteConfirm
      v-model:open="deleteConfirmOpen"
      :loading="deleteLoading"
      @confirm="confirmDelete"
    />
    <SuccessfulDeploy v-model:open="deploySuccessOpen" :url="deploySuccessUrl" />
  </div>
</template>

<style scoped>
.app-detail {
  height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-detail :deep(.ant-spin-nested-loading),
.app-detail :deep(.ant-spin-container) {
  height: 100%;
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
  height: calc(100vh - 57px);
  overflow: hidden;
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
  overflow: hidden;
}

.app-detail__messages {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px;
}

.app-detail__chat :deep(.chat-input) {
  flex-shrink: 0;
  border-radius: 0;
  border-left: none;
  border-right: none;
  border-bottom: none;
  border-top: 1px solid rgba(5, 5, 5, 0.06);
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
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.app-detail__preview-open {
  border: none;
  background: transparent;
  color: #1677ff;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
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
