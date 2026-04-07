<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  CloudDownloadOutlined,
  CloudUploadOutlined,
  EditOutlined,
  ExportOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons-vue'

import BackButton from '@/components/common/backButton.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import ChatMessage from '@/components/chat/ChatMessage.vue'
import IframePreview from '@/components/app-editor/IframePreview.vue'
import SelectedElementDetail from '@/components/app-editor/SelectedElementDetail.vue'
import AppDetail from '@/components/app/AppDetail.vue'
import DeleteConfirm from '@/components/app/DeleteConfirm.vue'
import SuccessfulDeploy from '@/components/app/SuccessfulDeploy.vue'
import { adminDeleteApp } from '@/api/appAdminController'
import { deleteApp, deployApp, downloadAppCode, getApp } from '@/api/appController'
import { addMessage, createSession, listHistory, listSessions } from '@/api/chatController'
import { streamChatGenCode } from '@/hooks/useSSEChat'
import { useLoginUserStore } from '@/stores/loginUser'
import { apiLongId, idFromData } from '@/utils/id'
import { getErrorMessage } from '@/utils/error'
import { waitForStaticPreviewReady } from '@/features/app-editor/utils/appPreview'
import {
  parseFilenameFromContentDisposition,
  sanitizeDownloadFilename,
  triggerBlobDownload,
} from '@/utils/fileDownload'
import { buildAugmentedChatMessage } from '@/features/app-editor/utils/elementExtractor'
import type { VisualEditorSelectedElement } from '@/features/app-editor/utils/visualEditor'

type ChatRow = {
  key: string
  id?: string
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
  createTime?: string
}

const vm = getCurrentInstance()
const router = vm?.proxy?.$router as any
const route = computed(() => vm?.proxy?.$route as any)
const loginUserStore = useLoginUserStore()

const appId = computed(() => String(route.value?.params?.appId ?? ''))

const appVo = ref<API.AppVO | null>(null)
const pageLoading = ref(true)
const chatLoading = ref(false)
const inputText = ref('')
const messages = ref<ChatRow[]>([])
const listEl = ref<HTMLElement | null>(null)
const previewUrl = ref('')
const previewFailed = ref(false)
const deployLoading = ref(false)
const downloadLoading = ref(false)
const deploySuccessOpen = ref(false)
const deploySuccessUrl = ref('')
const infoVisible = ref(false)
const deleteConfirmOpen = ref(false)
const deleteLoading = ref(false)

/** 预览 iframe 内点选元素（可视化编辑） */
const visualEditMode = ref(false)
const selectedElement = ref<VisualEditorSelectedElement | null>(null)

const historyLoading = ref(false)
const historyInited = ref(false)
const hasMoreHistory = ref(false)
const sessionId = ref<string | null>(null)
const historyCursor = ref<{ beforeMessageId?: string; beforeCreateTime?: string }>({})

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
const showPreviewPanel = computed(() => historyInited.value && messages.value.length >= 2)
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

const withCacheBust = (url: string): string => {
  if (!url) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}_t=${Date.now()}`
}

const tryRestorePreview = async (silent = true) => {
  const app = appVo.value
  if (!app || !showPreviewPanel.value) {
    previewUrl.value = ''
    previewFailed.value = false
    return
  }
  try {
    previewFailed.value = false
    previewUrl.value = ''
    const cg = app.codeGenType || 'html'
    const ready = await waitForStaticPreviewReady(cg, idFromData(app.id), {
      maxAttempts: silent ? 4 : 30,
      intervalMs: silent ? 300 : 600,
    })
    if (ready.ok) {
      previewUrl.value = withCacheBust(ready.url)
    } else if (!silent) {
      previewFailed.value = true
      message.warning('预览暂不可用：文件尚未就绪或生成失败，可稍后重试或查看服务端日志')
    }
  } catch {
    if (!silent) {
      previewFailed.value = true
    }
  }
}

const isAscendingByCreateTime = (rows: Array<{ createTime?: string }>) => {
  const first = rows[0]?.createTime
  const last = rows[rows.length - 1]?.createTime
  if (!first || !last) return true
  return dayjs(first).valueOf() <= dayjs(last).valueOf()
}

const mergeHistoryToMessages = (incoming: API.ChatHistoryVO[], mode: 'prepend' | 'append') => {
  const list = Array.isArray(incoming) ? incoming : []
  if (list.length === 0) return

  // 服务端承诺时间正序；为稳妥起见做一次保护
  const normalized = isAscendingByCreateTime(list) ? list : [...list].reverse()

  const existingIds = new Set<string>()
  for (const m of messages.value) {
    if (typeof m.id === 'string' && m.id) existingIds.add(m.id)
  }

  const mapped: ChatRow[] = normalized
    .filter((x) => x.id != null && !existingIds.has(String(x.id)))
    .map((x) => ({
      key: String(x.id),
      id: String(x.id),
      role: x.messageType === 'ai' ? 'assistant' : 'user',
      content: x.message ?? '',
      createTime: x.createTime,
      streaming: false,
    }))

  if (mapped.length === 0) return

  if (mode === 'prepend') {
    messages.value = [...mapped, ...messages.value]
  } else {
    messages.value = [...messages.value, ...mapped]
  }
}

const ensureSession = async () => {
  if (!canChat.value) {
    sessionId.value = null
    return
  }
  if (sessionId.value != null) return
  const idNum = apiLongId(appId.value)
  const res = await listSessions({
    appId: idNum,
    pageNum: 1,
    pageSize: 1,
  })
  const records = res.data?.data?.records ?? []
  const first = records[0]
  if (first?.id != null) {
    sessionId.value = String(first.id)
    return
  }
  const created = await createSession({ appId: idNum })
  const sid = created.data?.data
  if (sid == null) throw new Error('创建会话失败')
  sessionId.value = String(sid)
}

const loadHistory = async (mode: 'initial' | 'more') => {
  if (historyLoading.value) return
  if (!canChat.value) {
    historyInited.value = true
    hasMoreHistory.value = false
    return
  }
  if (mode === 'more' && !hasMoreHistory.value) return

  historyLoading.value = true
  const el = listEl.value
  const beforeTop = el?.scrollTop ?? 0
  const beforeHeight = el?.scrollHeight ?? 0

  try {
    await ensureSession()
    if (!sessionId.value) return

    const body: API.ChatHistoryQueryRequest = {
      appId: apiLongId(appId.value),
      sessionId: apiLongId(sessionId.value),
      pageSize: 10,
      beforeMessageId: mode === 'more' && historyCursor.value.beforeMessageId
        ? apiLongId(historyCursor.value.beforeMessageId)
        : undefined,
      beforeCreateTime: mode === 'more' ? historyCursor.value.beforeCreateTime : undefined,
    }
    const res = await listHistory(body)
    const list = res.data?.data ?? []

    if (mode === 'initial') {
      messages.value = []
      historyCursor.value = {}
      hasMoreHistory.value = false
    }

    // 服务端返回的是“更老的一页”（时间正序），所以这里做 prepend
    mergeHistoryToMessages(list, 'prepend')

    // 更新游标：取当前列表最老一条（列表头部）
    const first = messages.value[0]
    if (first?.id) {
      historyCursor.value = {
        beforeMessageId: first.id,
        beforeCreateTime: first.createTime,
      }
    }

    // 是否还有更多：返回条数 == pageSize 则认为可能还有
    hasMoreHistory.value = Array.isArray(list) && list.length >= 10
  } catch (e) {
    message.error(getErrorMessage(e, '加载历史消息失败'))
  } finally {
    historyLoading.value = false
    historyInited.value = true
    if (mode === 'more' && el) {
      await nextTick()
      const afterHeight = el.scrollHeight
      el.scrollTop = beforeTop + Math.max(0, afterHeight - beforeHeight)
    }
  }
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
}

const appendAssistantStreaming = (key: string) => {
  messages.value.push({ key, role: 'assistant', content: '', streaming: true })
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
  const assistantKey = `local-ai-${Date.now()}-${Math.random().toString(16).slice(2)}`
  appendAssistantStreaming(assistantKey)
  await scrollEnd()
  const idx = messages.value.length - 1
  try {
    if (!sessionId.value) {
      throw new Error('会话不存在，请刷新后重试')
    }
    await streamChatGenCode(
      appId.value,
      sessionId.value,
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

    // 持久化 AI 消息（忽略空内容）
    try {
      if (canChat.value && sessionId.value && messages.value[idx]?.content?.trim()) {
        const saved = await addMessage({
          appId: apiLongId(appId.value),
          sessionId: apiLongId(sessionId.value),
          message: messages.value[idx].content,
          messageType: 'ai',
        })
        const hid = saved.data?.data
        if (hid != null) {
          messages.value[idx].id = String(hid)
          messages.value[idx].key = String(hid)
        }
      }
    } catch {
      // 持久化失败不影响聊天主流程
    }

    const app = appVo.value
    if (app && showPreviewPanel.value) {
      const cg = app.codeGenType || 'html'
      const ready = await waitForStaticPreviewReady(cg, idFromData(app.id), {
        signal: abortCtl.signal,
      })
      if (ready.ok) {
        previewUrl.value = withCacheBust(ready.url)
        previewFailed.value = false
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
  await ensureSession()

  const selectedSnapshot = selectedElement.value
  const streamMessage =
    visualEditMode.value && selectedSnapshot
      ? buildAugmentedChatMessage(t, selectedSnapshot)
      : t

  const userKey = `local-user-${Date.now()}-${Math.random().toString(16).slice(2)}`
  messages.value.push({ key: userKey, role: 'user', content: t })
  inputText.value = ''
  await scrollEnd()

  // 持久化用户消息（失败不阻断对话）— 仅保存用户输入原文，与气泡展示一致
  try {
    if (sessionId.value) {
      const saved = await addMessage({
        appId: apiLongId(appId.value),
        sessionId: apiLongId(sessionId.value),
        message: t,
        messageType: 'user',
      })
      const hid = saved.data?.data
      if (hid != null) {
        const last = messages.value[messages.value.length - 1]
        if (last && last.key === userKey) {
          last.id = String(hid)
          last.key = String(hid)
        }
      }
    }
  } catch {
    // ignore
  }

  // 发送后清除选中并退出可视化编辑，恢复 iframe 正常交互
  visualEditMode.value = false
  selectedElement.value = null

  await runStream(streamMessage)
}

const cancelStream = () => {
  if (!chatLoading.value || !abortCtl) return
  cancelledByUser = true
  abortCtl.abort()
}

const maybeRunInitial = async () => {
  if (didInitial || !appVo.value || !canChat.value) return
  if (messages.value.length > 0) return
  const init = appVo.value.initPrompt?.trim()
  if (!init) return
  didInitial = true
  await ensureSession()
  const userKey = `local-user-${Date.now()}-${Math.random().toString(16).slice(2)}`
  messages.value.push({ key: userKey, role: 'user', content: init })
  await scrollEnd()

  try {
    if (sessionId.value) {
      const saved = await addMessage({
        appId: apiLongId(appId.value),
        sessionId: apiLongId(sessionId.value),
        message: init,
        messageType: 'user',
      })
      const hid = saved.data?.data
      if (hid != null) {
        const last = messages.value[messages.value.length - 1]
        if (last && last.key === userKey) {
          last.id = String(hid)
          last.key = String(hid)
        }
      }
    }
  } catch {
    // ignore
  }

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

const onDownloadCode = async () => {
  if (!appId.value) return
  try {
    downloadLoading.value = true
    const res = await downloadAppCode(
      { appId: apiLongId(appId.value) },
      { responseType: 'blob' },
    )
    const blob = res.data as unknown as Blob
    const ct = String(res.headers['content-type'] ?? '').toLowerCase()
    if (ct.includes('application/json')) {
      const text = await blob.text()
      try {
        const j = JSON.parse(text) as { message?: string; code?: number }
        message.error(j.message || '下载失败')
      } catch {
        message.error('下载失败')
      }
      return
    }
    const cd = res.headers['content-disposition'] as string | undefined
    const fromHeader = parseFilenameFromContentDisposition(cd)
    const fallback = `${appVo.value?.appName?.trim() || `app-${appId.value}`}.zip`
    const filename = sanitizeDownloadFilename(fromHeader || fallback)
    if (!blob.size) {
      message.warning('下载内容为空')
      return
    }
    triggerBlobDownload(blob, filename)
    message.success('已开始下载')
  } catch (e) {
    message.error(getErrorMessage(e))
  } finally {
    downloadLoading.value = false
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
  const id = idFromData(appVo.value.id)
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

const toggleVisualEditMode = () => {
  if (visualEditMode.value) {
    visualEditMode.value = false
    selectedElement.value = null
  } else {
    visualEditMode.value = true
  }
}

const clearSelectedElement = () => {
  selectedElement.value = null
}

const onVisualInjectFailed = () => {
  visualEditMode.value = false
}

watch(appId, async () => {
  didInitial = false
  messages.value = []
  previewUrl.value = ''
  previewFailed.value = false
  visualEditMode.value = false
  selectedElement.value = null
  historyInited.value = false
  hasMoreHistory.value = false
  historyCursor.value = {}
  sessionId.value = null
  await loadApp()
  await loadHistory('initial')
  await maybeRunInitial()
  await tryRestorePreview(true)
})

onMounted(async () => {
  await loadApp()
  await loadHistory('initial')
  await maybeRunInitial()
  await tryRestorePreview(true)
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
          <a-button :loading="downloadLoading" @click="onDownloadCode">
            <template #icon><CloudDownloadOutlined /></template>
            下载
          </a-button>
          <a-button type="primary" :loading="deployLoading" @click="onDeploy">
            <template #icon><CloudUploadOutlined /></template>
            部署
          </a-button>
        </header>

        <div class="app-detail__main">
          <div class="app-detail__chat">
            <div ref="listEl" class="app-detail__messages">
              <div class="app-detail__more">
                <a-button
                  v-if="hasMoreHistory"
                  size="small"
                  :loading="historyLoading"
                  @click="loadHistory('more')"
                >
                  加载更多
                </a-button>
                <a-spin v-else-if="!historyInited" size="small" />
              </div>
              <ChatMessage
                v-for="m in messages"
                :key="m.key"
                :role="m.role"
                :content="m.content"
                :streaming="m.streaming"
              />
              <a-empty
                v-if="historyInited && messages.length === 0"
                class="app-detail__empty"
                :description="canChat ? '暂无历史消息，快来发送第一条吧' : '暂无可查看的历史消息'"
              />
            </div>
            <a-tooltip :title="canChat ? '' : '亲，无法在别人的作品下对话哦~'">
              <div>
                <SelectedElementDetail
                  v-if="selectedElement && canChat"
                  :element="selectedElement"
                  @close="clearSelectedElement"
                />
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
          <div v-if="showPreviewPanel" class="app-detail__preview">
            <div class="app-detail__preview-title">
              <span>生成预览</span>
              <div class="app-detail__preview-actions">
                <button
                  type="button"
                  class="app-detail__preview-open"
                  :class="{ 'app-detail__preview-edit--exit': visualEditMode }"
                  :disabled="!previewUrl"
                  @click="toggleVisualEditMode"
                >
                  <EditOutlined />
                  <span>{{ visualEditMode ? '退出编辑' : '进入编辑' }}</span>
                </button>
                <button type="button" class="app-detail__preview-open" @click="openPreviewInNewWindow">
                  <ExportOutlined />
                  <span>新窗口打开</span>
                </button>
              </div>
            </div>
            <IframePreview
              v-if="previewUrl"
              class="app-detail__iframe"
              :src="previewUrl"
              :visual-edit-active="visualEditMode"
              @select="selectedElement = $event"
              @inject-failed="onVisualInjectFailed"
            />
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

.app-detail__more {
  display: flex;
  justify-content: center;
  padding: 4px 0 12px;
}

.app-detail__empty {
  margin: 24px 0;
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

.app-detail__preview-actions {
  display: inline-flex;
  align-items: center;
  gap: 12px;
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

.app-detail__preview-open:disabled {
  color: rgba(0, 0, 0, 0.25);
  cursor: not-allowed;
}

.app-detail__preview-edit--exit:not(:disabled) {
  color: #ff4d4f;
}

.app-detail__iframe {
  flex: 1;
  min-height: 0;
  width: 100%;
  border: none;
  background: #fff;
}

.app-detail__preview :deep(.ant-empty) {
  margin: auto;
}

</style>
