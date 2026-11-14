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
  LeftOutlined,
  RightOutlined,
} from '@ant-design/icons-vue'

import BackButton from '@/components/common/backButton.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import ChatJump from '@/components/chat/ChatJump.vue'
import ChatMessage, { type ChatAction } from '@/components/chat/ChatMessage.vue'
import LineIcon from '@/components/icons/LineIcon.vue'
import IframePreview from '@/components/app-editor/IframePreview.vue'
import SelectedElementDetail from '@/components/app-editor/SelectedElementDetail.vue'
import AppDetail from '@/components/app/AppDetail.vue'
import DeleteConfirm from '@/components/app/DeleteConfirm.vue'
import SuccessfulDeploy from '@/components/app/SuccessfulDeploy.vue'
import DeployHint from '@/components/share/DeployHint.vue'
import ShareSheet from '@/components/share/ShareSheet.vue'
import { adminDeleteApp } from '@/api/appAdminController'
import { uploadAppImage, replaceAppImage } from '@/api/appAssetController'
import { deleteApp, deployApp, downloadAppCode, getApp } from '@/api/appController'
import { continueCodeVersion, listCodeVersions, pinCodeVersion } from '@/api/codeVersionController'
import { addMessage, createSession, exportHistory, listHistory, listSessions, rewriteHistory } from '@/api/chatController'
import { voteMessage } from '@/api/feedbackController'
import { streamChatGenCode } from '@/hooks/useSSEChat'
import { useLoginUserStore } from '@/stores/loginUser'
import { apiLongId, idFromData } from '@/utils/id'
import { getErrorMessage } from '@/utils/error'
import { getVersionPreviewIndexUrl, replyUpdatesPreview, waitForStaticPreviewReady } from '@/features/app-editor/utils/appPreview'
import {
  parseFilenameFromContentDisposition,
  sanitizeDownloadFilename,
  triggerBlobDownload,
} from '@/utils/fileDownload'
import { buildAugmentedChatMessage } from '@/features/app-editor/utils/elementExtractor'
import { useStickToBottom } from '@/features/chat/chatScroll'
import type { VisualEditorSelectedElement } from '@/features/app-editor/utils/visualEditor'

type ChatRow = {
  key: string
  id?: string
  role: 'user' | 'assistant'
  content: string
  reasoning?: string
  streaming?: boolean
  createTime?: string
  vote?: 1 | -1 | null
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
const versionIds = ref<string[]>([])
const versionIndex = ref(0)
const deployLoading = ref(false)
const downloadLoading = ref(false)
const deploySuccessOpen = ref(false)
const deploySuccessUrl = ref('')
const shareOpen = ref(false)
const deployHintOpen = ref(false)
const infoVisible = ref(false)
const deleteConfirmOpen = ref(false)
const deleteLoading = ref(false)

/** 预览 iframe 内点选元素（可视化编辑） */
const visualEditMode = ref(false)
const mobilePane = ref<'chat' | 'preview'>('chat')
const selectedElement = ref<VisualEditorSelectedElement | null>(null)
const uploadedImagePath = ref('')
const uploadedImageName = ref('')
const imageUploading = ref(false)
const imageReplacing = ref(false)

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
const canViewHistory = computed(() => {
  if (isOwner.value || isAdmin.value) return true
  const p = Number(appVo.value?.priority ?? 0)
  return Number.isFinite(p) && p >= 99
})
const showPreviewPanel = computed(() => historyInited.value && messages.value.length >= 2)
const currentVersionId = computed(() => versionIds.value[versionIndex.value] ?? '')
const canPrevVersion = computed(() => !chatLoading.value && versionIndex.value > 0)
const canNextVersion = computed(() => !chatLoading.value && versionIndex.value < versionIds.value.length - 1)
const prevVersionTip = computed(() => (
  !chatLoading.value && versionIndex.value <= 0 ? '已是最早版本' : '上一版'
))
const nextVersionTip = computed(() => (
  !chatLoading.value && versionIndex.value >= versionIds.value.length - 1 ? '已是最新版本' : '下一版'
))
const selectedIsImage = computed(() => selectedElement.value?.tag === 'img')
const canReplaceImage = computed(() => canChat.value
  && selectedIsImage.value
  && !!uploadedImagePath.value
  && !imageUploading.value
  && !imageReplacing.value)
const formattedCreateTime = computed(() => {
  const t = appVo.value?.createTime
  if (!t) return '—'
  const d = dayjs(t)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : t.replace('T', ' ')
})

const { showJump, onListScroll, onUserWheel, onTouchStart, onTouchMove, scrollIfPinned, pinAndScroll } = useStickToBottom(listEl)

const withCacheBust = (url: string): string => {
  if (!url) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}_t=${Date.now()}`
}

const showCurrentVersion = () => {
  const app = appVo.value
  const id = currentVersionId.value
  if (!app || !id) return false
  previewUrl.value = withCacheBust(
    getVersionPreviewIndexUrl(app.codeGenType || 'html', idFromData(app.id), id),
  )
  previewFailed.value = false
  return true
}

const refreshVersions = async () => {
  if (!sessionId.value || !appVo.value) return false
  try {
    const res = await listCodeVersions({
      appId: apiLongId(appId.value),
      sessionId: apiLongId(sessionId.value),
    })
    const ids = (res.data?.data ?? []).map((id) => String(id)).filter((id) => /^\d+$/.test(id))
    versionIds.value = ids
    versionIndex.value = Math.max(0, ids.length - 1)
    if (ids.length === 0) {
      previewUrl.value = ''
      return false
    }
    return showCurrentVersion()
  } catch {
    return false
  }
}

const stepVersion = (delta: number) => {
  const next = versionIndex.value + delta
  if (chatLoading.value || next < 0 || next >= versionIds.value.length) return
  versionIndex.value = next
  selectedElement.value = null
  showCurrentVersion()
}

const tryRestorePreview = async (silent = true) => {
  const app = appVo.value
  if (!app || !showPreviewPanel.value) {
    previewUrl.value = ''
    previewFailed.value = false
    return
  }
  if (sessionId.value && (await refreshVersions())) {
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
      reasoning: x.reasoning || undefined,
      createTime: x.createTime,
      streaming: false,
      vote: x.vote === 1 || x.vote === -1 ? x.vote : null,
    }))

  if (mapped.length === 0) return

  if (mode === 'prepend') {
    messages.value = [...mapped, ...messages.value]
  } else {
    messages.value = [...messages.value, ...mapped]
  }
}

const ensureSession = async () => {
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
  if (!canChat.value) {
    sessionId.value = null
    return
  }
  const created = await createSession({ appId: idNum })
  const sid = created.data?.data
  if (sid == null) throw new Error('创建会话失败')
  sessionId.value = String(sid)
}

const loadHistory = async (mode: 'initial' | 'more') => {
  if (historyLoading.value) return
  if (!canViewHistory.value) {
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
    } else if (mode === 'initial') {
      await pinAndScroll()
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

const dropEmptyAssistant = (index: number) => {
  const row = messages.value[index]
  if (row?.role === 'assistant' && !row.content.trim() && !row.reasoning?.trim()) {
    messages.value.splice(index, 1)
  }
}

const runStream = async (text: string) => {
  abortCtl?.abort()
  abortCtl = new AbortController()
  cancelledByUser = false
  chatLoading.value = true
  const assistantKey = `local-ai-${Date.now()}-${Math.random().toString(16).slice(2)}`
  appendAssistantStreaming(assistantKey)
  await pinAndScroll()
  const idx = messages.value.length - 1
  try {
    if (!sessionId.value) {
      throw new Error('会话不存在，请刷新后重试')
    }
    await streamChatGenCode(
      appId.value,
      sessionId.value,
      text,
      (delta) => {
        const row = messages.value[idx]
        if (row && row.role === 'assistant') {
          if (delta.kind === 'reasoning') row.reasoning = (row.reasoning || '') + delta.text
          else row.content += delta.text
        }
        void scrollIfPinned()
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
          reasoning: messages.value[idx].reasoning || undefined,
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
    const assistantText = messages.value[idx]?.content ?? ''
    const savedId = messages.value[idx]?.id
    if (app && showPreviewPanel.value && replyUpdatesPreview(assistantText)) {
      const cg = app.codeGenType || 'html'
      const ready = await waitForStaticPreviewReady(cg, idFromData(app.id), {
        signal: abortCtl.signal,
      })
      if (ready.ok && savedId && sessionId.value && canChat.value) {
        try {
          await pinCodeVersion({
            appId: apiLongId(appId.value),
            sessionId: apiLongId(sessionId.value),
            messageId: apiLongId(savedId),
          })
          if (!versionIds.value.includes(savedId)) {
            versionIds.value = [...versionIds.value, savedId]
          }
          versionIndex.value = versionIds.value.length - 1
          showCurrentVersion()
        } catch {
          previewUrl.value = withCacheBust(ready.url)
          previewFailed.value = false
        }
      } else if (ready.ok) {
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
    dropEmptyAssistant(idx)
    message.error(getErrorMessage(e))
    finishAssistant()
  } finally {
    chatLoading.value = false
    abortCtl = null
    await scrollIfPinned()
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

  const cursor = currentVersionId.value
  const tip = versionIds.value[versionIds.value.length - 1]
  if (cursor && tip && cursor !== tip && sessionId.value) {
    try {
      await continueCodeVersion({
        appId: apiLongId(appId.value),
        sessionId: apiLongId(sessionId.value),
        messageId: apiLongId(cursor),
      })
    } catch (e) {
      message.error(getErrorMessage(e, '无法从这一版继续'))
      return
    }
    const anchorIndex = messages.value.findIndex((row) => row.id === cursor)
    if (anchorIndex >= 0) {
      messages.value.splice(anchorIndex + 1)
    } else {
      messages.value = []
      historyInited.value = false
      await loadHistory('initial')
    }
    const cut = versionIds.value.indexOf(cursor)
    if (cut >= 0) {
      versionIds.value = versionIds.value.slice(0, cut + 1)
      versionIndex.value = cut
    }
  }

  const selectedSnapshot = selectedElement.value
  const streamMessage =
    visualEditMode.value && selectedSnapshot
      ? buildAugmentedChatMessage(t, selectedSnapshot)
      : t

  const userKey = `local-user-${Date.now()}-${Math.random().toString(16).slice(2)}`
  messages.value.push({ key: userKey, role: 'user', content: t })
  inputText.value = ''
  await pinAndScroll()

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

let nameWatchTimer: ReturnType<typeof setInterval> | undefined

const stopNameWatch = () => {
  if (nameWatchTimer != null) {
    clearInterval(nameWatchTimer)
    nameWatchTimer = undefined
  }
}

const refreshAppName = async () => {
  if (!appVo.value) return
  try {
    const res = await getApp({ id: apiLongId(appId.value) })
    const name = res.data?.data?.appName?.trim()
    if (name && name !== appVo.value.appName) {
      appVo.value = { ...appVo.value, appName: name }
      stopNameWatch()
    }
  } catch {
    // 标题还没写好时不打断对话
  }
}

const watchGeneratedName = () => {
  stopNameWatch()
  let tries = 0
  nameWatchTimer = setInterval(() => {
    tries += 1
    if (tries > 12) {
      stopNameWatch()
      return
    }
    void refreshAppName()
  }, 3000)
}

const maybeRunInitial = async () => {
  if (didInitial || !appVo.value || !canChat.value) return
  if (messages.value.length > 0) return
  const init = appVo.value.initPrompt?.trim()
  if (!init) return
  didInitial = true
  watchGeneratedName()
  await ensureSession()
  const userKey = `local-user-${Date.now()}-${Math.random().toString(16).slice(2)}`
  messages.value.push({ key: userKey, role: 'user', content: init })
  await pinAndScroll()

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
    const res = await deployApp({
      appId: apiLongId(appId.value),
      messageId: currentVersionId.value ? apiLongId(currentVersionId.value) : undefined,
    })
    const url = res.data?.data
    if (url) {
      message.success('部署成功')
      deploySuccessUrl.value = url
      deploySuccessOpen.value = true
      if (appVo.value) appVo.value.deployUrl = url
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
      {
        appId: apiLongId(appId.value),
        messageId: currentVersionId.value ? apiLongId(currentVersionId.value) : undefined,
      },
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

const deployUrl = computed(() => (appVo.value?.deployUrl || '').trim())
const showShare = computed(() => !!deployUrl.value || isOwner.value)
const shareBlurb = computed(() => {
  const name = appVo.value?.appName?.trim() || '未命名作品'
  return `我在 AI 编程站做了一个页面：${name} ${deployUrl.value}`
})

const onShareApp = () => {
  if (deployUrl.value) {
    shareOpen.value = true
    return
  }
  if (isOwner.value) deployHintOpen.value = true
}

const onDeployFromHint = async () => {
  deployHintOpen.value = false
  await onDeploy()
}

const previousUserIndex = (from: number) => {
  for (let i = from; i >= 0; i -= 1) {
    if (messages.value[i]?.role === 'user') return i
  }
  return -1
}

const actionsFor = (row: ChatRow): ChatAction[] => {
  if (row.streaming || !row.content.trim()) return []
  if (row.role === 'user') return canChat.value ? ['copy', 'edit'] : ['copy']
  const actions: ChatAction[] = ['copy']
  if (canChat.value) actions.push('retry', 'like', 'dislike')
  actions.push('download')
  return actions
}

const rewriteFrom = async (userIndex: number, nextText: string) => {
  const user = messages.value[userIndex]
  if (!user?.id || !sessionId.value) {
    message.warning('这条消息还没保存完成，请稍后再试')
    return
  }
  if (chatLoading.value) return
  const text = nextText.trim()
  if (!text) return
  try {
    await rewriteHistory({
      appId: apiLongId(appId.value),
      sessionId: apiLongId(sessionId.value),
      userMessageId: apiLongId(user.id),
      message: text,
    })
  } catch {
    return
  }
  user.content = text
  messages.value.splice(userIndex + 1)
  await refreshVersions()
  await runStream(text)
}

const onRetry = async (index: number) => {
  const userIndex = previousUserIndex(index)
  if (userIndex < 0) {
    message.warning('找不到对应的发言')
    return
  }
  const user = messages.value[userIndex]
  if (!user) return
  await rewriteFrom(userIndex, user.content)
}

const onEditMessage = async (index: number, text: string) => {
  await rewriteFrom(index, text)
}

const onVote = async (row: ChatRow, payload: { vote: 1 | -1 | 0; reason?: string }) => {
  if (!row.id) {
    message.warning('这条消息还没保存完成，请稍后再试')
    return
  }
  const previous = row.vote
  row.vote = payload.vote === 0 ? null : payload.vote
  try {
    await voteMessage({
      source: 'app',
      messageId: row.id,
      vote: payload.vote,
      reason: payload.reason,
    })
  } catch {
    row.vote = previous
  }
}

const onExportMessage = async (row: ChatRow) => {
  if (!row.id || !sessionId.value) {
    message.warning('这条消息还没保存完成，请稍后再试')
    return
  }
  try {
    const res = await exportHistory(
      {
        appId: apiLongId(appId.value),
        sessionId: apiLongId(sessionId.value),
        untilMessageId: apiLongId(row.id),
      },
      { responseType: 'blob' },
    )
    const blob = res.data as unknown as Blob
    const ct = String(res.headers['content-type'] ?? '').toLowerCase()
    if (ct.includes('application/json')) {
      const text = await blob.text()
      try {
        const body = JSON.parse(text) as { message?: string }
        message.error(body.message || '下载失败')
      } catch {
        message.error('下载失败')
      }
      return
    }
    const fromHeader = parseFilenameFromContentDisposition(res.headers['content-disposition'] as string | undefined)
    const fallback = `${appVo.value?.appName?.trim() || '对话'}.md`
    triggerBlobDownload(blob, sanitizeDownloadFilename(fromHeader || fallback))
  } catch (e) {
    message.error(getErrorMessage(e, '下载失败'))
  }
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

const clearUploadedImage = () => {
  uploadedImagePath.value = ''
  uploadedImageName.value = ''
}

const onChooseImage = async (file: File) => {
  if (!canChat.value) {
    message.warning('亲，无法在别人的作品下上传图片哦~')
    return
  }
  if (!selectedIsImage.value) {
    message.warning('请先在预览区选中一张图片，再上传附件')
    return
  }
  try {
    imageUploading.value = true
    const sid = sessionId.value ? apiLongId(sessionId.value) : undefined
    const res = await uploadAppImage(apiLongId(appId.value), file, sid)
    const path = res.data?.data
    if (!path) {
      throw new Error('上传成功但未返回图片路径')
    }
    uploadedImagePath.value = path
    uploadedImageName.value = file.name
    message.success('图片上传成功，点击“替换图片”即可生效')
  } catch (e) {
    message.error(getErrorMessage(e, '图片上传失败'))
  } finally {
    imageUploading.value = false
  }
}

const onReplaceImage = async () => {
  if (!selectedElement.value || !selectedIsImage.value) {
    message.warning('请先选中预览中的图片元素')
    return
  }
  if (!uploadedImagePath.value) {
    message.warning('请先上传图片附件')
    return
  }
  try {
    imageReplacing.value = true
    await replaceAppImage({
      appId: apiLongId(appId.value),
      messageId: currentVersionId.value ? apiLongId(currentVersionId.value) : undefined,
      newImagePath: uploadedImagePath.value,
      targetXpath: selectedElement.value.xpath,
      targetTag: selectedElement.value.tag,
      targetId: selectedElement.value.id,
      targetClassList: selectedElement.value.classList,
      targetTagPath: selectedElement.value.tagPath,
    })
    message.success('图片替换成功')
    if (previewUrl.value) {
      previewUrl.value = withCacheBust(previewUrl.value.split('?')[0] || previewUrl.value)
    } else {
      await tryRestorePreview(false)
    }
  } catch (e) {
    message.error(getErrorMessage(e, '图片替换失败'))
  } finally {
    imageReplacing.value = false
  }
}

const onVisualInjectFailed = () => {
  visualEditMode.value = false
}

const resetPageScroll = () => {
  window.scrollTo(0, 0)
  document.documentElement.scrollTop = 0
  document.body.scrollTop = 0
}

watch(appId, async () => {
  stopNameWatch()
  resetPageScroll()
  didInitial = false
  messages.value = []
  previewUrl.value = ''
  previewFailed.value = false
  versionIds.value = []
  versionIndex.value = 0
  visualEditMode.value = false
  mobilePane.value = 'chat'
  selectedElement.value = null
  clearUploadedImage()
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
  resetPageScroll()
  await loadApp()
  await loadHistory('initial')
  await maybeRunInitial()
  await tryRestorePreview(true)
})

onBeforeUnmount(() => {
  stopNameWatch()
  abortCtl?.abort()
})
</script>

<template>
  <div class="app-detail" :class="{ 'is-preview': mobilePane === 'preview' }">
    <a-spin :spinning="pageLoading">
      <template v-if="appVo">
        <header class="app-detail__header">
          <BackButton @click="router.push('/app')" />
          <span class="app-detail__name">{{ appVo.appName || '应用' }}</span>
          <a-button class="app-detail__tool" aria-label="详情" @click="infoVisible = true">
            <template #icon><InfoCircleOutlined /></template>
            <span class="app-detail__tool-label">详情</span>
          </a-button>
          <a-button class="app-detail__tool" aria-label="下载" :loading="downloadLoading" @click="onDownloadCode">
            <template #icon><CloudDownloadOutlined /></template>
            <span class="app-detail__tool-label">下载</span>
          </a-button>
          <a-button class="app-detail__tool" type="primary" aria-label="部署" :loading="deployLoading" @click="onDeploy">
            <template #icon><CloudUploadOutlined /></template>
            <span class="app-detail__tool-label">部署</span>
          </a-button>
        </header>
        <div v-if="showPreviewPanel" class="app-detail__switch" role="tablist" aria-label="对话或预览">
          <button
            type="button"
            role="tab"
            class="app-detail__switch-btn"
            :class="{ 'is-on': mobilePane === 'chat' }"
            :aria-selected="mobilePane === 'chat'"
            @click="mobilePane = 'chat'"
          >
            对话
          </button>
          <button
            type="button"
            role="tab"
            class="app-detail__switch-btn"
            :class="{ 'is-on': mobilePane === 'preview' }"
            :aria-selected="mobilePane === 'preview'"
            @click="mobilePane = 'preview'"
          >
            预览
          </button>
        </div>

        <div class="app-detail__main">
          <div class="app-detail__chat">
            <div class="app-detail__thread">
            <div
              ref="listEl"
              class="app-detail__messages"
              data-chat-scroll
              @scroll="onListScroll"
              @wheel="onUserWheel"
              @touchstart="onTouchStart"
              @touchmove="onTouchMove"
            >
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
                v-for="(m, index) in messages"
                :key="m.key"
                :role="m.role"
                :content="m.content"
                :reasoning="m.reasoning"
                :streaming="m.streaming"
                :actions="actionsFor(m)"
                :vote="m.vote"
                :pending="chatLoading"
                :drop-later="index < messages.length - 1"
                @retry="onRetry(index)"
                @edit="onEditMessage(index, $event)"
                @vote="onVote(m, $event)"
                @download="onExportMessage(m)"
              />
              <a-empty
                v-if="historyInited && messages.length === 0"
                class="app-detail__empty"
                :description="canChat ? '暂无历史消息，快来发送第一条吧' : (canViewHistory ? '暂无历史消息' : '暂无可查看的历史消息')"
              />
            </div>
            <ChatJump :show="showJump" @jump="pinAndScroll" />
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
                  :image-uploading="imageUploading"
                  :replace-loading="imageReplacing"
                  :can-replace-image="canReplaceImage"
                  :selected-image-name="uploadedImageName"
                  @submit="sendUser(inputText)"
                  @cancel="cancelStream"
                  @choose-image="onChooseImage"
                  @replace-image="onReplaceImage"
                  @clear-image="clearUploadedImage"
                />
              </div>
            </a-tooltip>
          </div>
          <div v-if="showPreviewPanel" class="app-detail__preview">
            <div class="app-detail__preview-title">
              <div class="app-detail__preview-lead">
                <span>生成预览</span>
                <div class="app-detail__preview-versions">
                  <a-tooltip :title="prevVersionTip">
                    <button
                      type="button"
                      class="app-detail__version-arrow"
                      :class="{ 'is-inactive': !canPrevVersion }"
                      :aria-label="prevVersionTip"
                      :aria-disabled="!canPrevVersion"
                      @click="stepVersion(-1)"
                    >
                      <LeftOutlined />
                    </button>
                  </a-tooltip>
                  <a-tooltip :title="nextVersionTip">
                    <button
                      type="button"
                      class="app-detail__version-arrow"
                      :class="{ 'is-inactive': !canNextVersion }"
                      :aria-label="nextVersionTip"
                      :aria-disabled="!canNextVersion"
                      @click="stepVersion(1)"
                    >
                      <RightOutlined />
                    </button>
                  </a-tooltip>
                </div>
              </div>
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
                <button v-if="showShare" type="button" class="app-detail__preview-open app-detail__preview-share" @click="onShareApp">
                  <LineIcon name="share" />
                  <span>分享</span>
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
    <DeployHint v-model:open="deployHintOpen" :loading="deployLoading" @deploy="onDeployFromHint" />
    <ShareSheet
      v-model:open="shareOpen"
      kind="app"
      :title="appVo?.appName || '未命名作品'"
      :url="deployUrl"
      :blurb="shareBlurb"
      :cover="appVo?.cover"
    />
  </div>
</template>

<style scoped>
.app-detail {
  position: fixed;
  inset: 0;
  z-index: 1;
  height: 100vh;
  height: 100dvh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-detail :deep(.ant-spin-nested-loading),
.app-detail :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.app-detail__header {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
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
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
  gap: 0;
  overflow: hidden;
}

.app-detail__tool-label {
  margin-inline-start: 6px;
}

.app-detail__switch {
  display: none;
}

@media (max-width: 960px) {
  .app-detail__header {
    gap: 4px;
    padding: 8px 10px 8px 4px;
  }

  .app-detail__header :deep(.back-button__inner > span) {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0 0 0 0);
  }

  .app-detail__name {
    font-size: 15px;
  }

  .app-detail__tool {
    padding-inline: 8px;
  }

  .app-detail__tool-label {
    display: none;
  }

  .app-detail__switch {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 4px;
    flex-shrink: 0;
    margin: 8px 12px 0;
    padding: 4px;
    border-radius: 12px;
    background: rgba(0, 0, 0, 0.04);
  }

  .app-detail__switch-btn {
    min-height: 36px;
    border: none;
    border-radius: 9px;
    background: transparent;
    color: rgba(0, 0, 0, 0.55);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
  }

  .app-detail__switch-btn.is-on {
    background: #fff;
    color: rgba(0, 0, 0, 0.88);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  }

  .app-detail__main {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(0, 1fr);
  }

  .app-detail.is-preview .app-detail__chat,
  .app-detail:not(.is-preview) .app-detail__preview {
    display: none;
  }

  .app-detail__messages {
    padding: 12px;
  }

  .app-detail__chat :deep(.chat-input) {
    padding-bottom: calc(10px + env(safe-area-inset-bottom));
  }

  .app-detail__preview-title {
    flex-wrap: wrap;
    gap: 8px;
    padding: 8px 12px;
  }

  .app-detail__preview-actions {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    width: 100%;
    gap: 4px;
  }

  .app-detail__preview-open {
    justify-content: center;
    min-height: 40px;
    border-radius: 8px;
    background: rgba(22, 119, 255, 0.06);
  }
}

.app-detail__chat {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid rgba(5, 5, 5, 0.06);
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.app-detail__thread {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.app-detail__messages {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  overflow-anchor: none;
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
  gap: 12px;
}

.app-detail__preview-lead {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.app-detail__preview-versions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.app-detail__version-arrow {
  border: none;
  background: transparent;
  color: rgba(0, 0, 0, 0.65);
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
  border-radius: 4px;
}

.app-detail__version-arrow:not(.is-inactive):hover {
  background: rgba(0, 0, 0, 0.04);
  color: #1677ff;
}

.app-detail__version-arrow.is-inactive {
  color: rgba(0, 0, 0, 0.25);
  cursor: default;
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

.app-detail__preview-share :deep(svg) {
  width: 14px;
  height: 14px;
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

@media (max-width: 960px) {
  .app-detail__preview-title {
    flex-wrap: wrap;
    gap: 8px;
    padding: 8px 12px;
  }

  .app-detail__preview-actions {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    width: 100%;
    gap: 4px;
  }

  .app-detail__preview-open {
    justify-content: center;
    min-height: 40px;
    border-radius: 8px;
    background: rgba(22, 119, 255, 0.06);
  }
}

</style>
