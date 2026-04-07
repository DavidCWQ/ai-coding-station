<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  PaperClipOutlined,
  PlusOutlined,
  SendOutlined,
} from '@ant-design/icons-vue'

import UserAvatar from '@/components/UserAvatar.vue'
import ChatMessage from '@/components/app/ChatMessage.vue'
import type { AgentSessionRow } from '@/types/agentSession'
import AgentSessionList from '@/components/agent/AgentSessionList.vue'
import DeleteSessionModal from '@/components/agent/DeleteSessionModal.vue'
import RenameSessionModal from '@/components/agent/RenameSessionModal.vue'
import {
  agentCreateSession,
  agentDeleteSession,
  agentListHistory,
  agentListSessions,
  agentUpdateSessionTitle,
} from '@/api/agentController'
import { useLoginUserStore } from '@/stores/loginUser'
import { streamAgentChat } from '@/hooks/useSSEChat'
import { findAgentCard } from '@/constants/agents'
import { apiLongId, idFromData } from '@/utils/id'
import { getErrorMessage } from '@/utils/error'

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

const agentCode = computed(() => String(route.value?.params?.agentCode ?? '').trim())
const agentMeta = computed(() => findAgentCard(agentCode.value))
const loginUser = computed(() => loginUserStore.loginUser)
const isLoggedIn = computed(() => loginUserStore.isLoggedIn)
const displayUserName = computed(() => {
  const raw = String(loginUser.value?.userName || loginUser.value?.userAccount || '').trim()
  if (!raw) return ''
  return raw.charAt(0).toUpperCase() + raw.slice(1)
})

const pageLoading = ref(true)
const chatLoading = ref(false)
const sessionsLoading = ref(false)
const inputText = ref('')
const renameModalOpen = ref(false)
const renamingSessionId = ref<string | null>(null)
const renameTitle = ref('')
const messages = ref<ChatRow[]>([])
const listEl = ref<HTMLElement | null>(null)

const historyLoading = ref(false)
const historyInited = ref(false)
const hasMoreHistory = ref(false)
const activeSessionId = ref<string | null>(null)
const historyCursor = ref<{ beforeMessageId?: string; beforeCreateTime?: string }>({})
const sessions = ref<AgentSessionRow[]>([])
const deleteModalOpen = ref(false)
const pendingDeleteItem = ref<AgentSessionRow | null>(null)

let abortCtl: AbortController | null = null

const sessionLabel = (item: AgentSessionRow) => item.title?.trim() || '新对话'

/** 列表排序：越「新」越靠上（有消息看最后消息时间，否则看更新时间、创建时间） */
const sessionRecencyMs = (row: AgentSessionRow): number => {
  for (const k of ['lastMsgTime', 'updateTime', 'createTime'] as const) {
    const v = row[k]
    if (v) {
      const t = new Date(v).getTime()
      if (!Number.isNaN(t)) return t
    }
  }
  try {
    return Number(BigInt(row.id))
  } catch {
    return 0
  }
}

const isNotLoginError = (e: unknown): boolean => {
  const msg = getErrorMessage(e, '')
  return msg.includes('未登录') || msg.includes('401')
}

const jumpToLogin = async () => {
  await router.push({
    path: '/user/login',
    query: { redirect: route.value?.fullPath || '/agents' },
  })
}

const ensureLoginForSend = async () => {
  if (isLoggedIn.value) {
    return true
  }
  message.warning('请先登录后再发送消息')
  await jumpToLogin()
  return false
}

const scrollEnd = async () => {
  await nextTick()
  const el = listEl.value
  if (el) el.scrollTop = el.scrollHeight
}

const isAscendingByCreateTime = (rows: Array<{ createTime?: string }>) => {
  const first = rows[0]?.createTime
  const last = rows[rows.length - 1]?.createTime
  if (!first || !last) return true
  return new Date(first).valueOf() <= new Date(last).valueOf()
}

const mergeHistoryToMessages = (incoming: API.AgentChatMessageVO[], mode: 'prepend' | 'append') => {
  const list = Array.isArray(incoming) ? incoming : []
  if (list.length === 0) return

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
  messages.value = mode === 'prepend' ? [...mapped, ...messages.value] : [...messages.value, ...mapped]
}

const syncRouteSession = async (sid: string | null) => {
  const q = route.value?.query ?? {}
  const nextSid = sid ? idFromData(sid) : undefined
  if ((q.sessionId ?? undefined) === nextSid) return
  await router.replace({
    path: route.value.path,
    query: { ...q, sessionId: nextSid },
  })
}

const loadSessions = async () => {
  if (!isLoggedIn.value || !agentCode.value) {
    sessions.value = []
    activeSessionId.value = null
    return
  }
  sessionsLoading.value = true
  try {
    const res = await agentListSessions({
      agentCode: agentCode.value,
      pageNum: 1,
      pageSize: 20,
    })
    const records = res.data?.data?.records ?? []
    const mapped = records
      .filter((x) => x.id != null)
      .map((x) => ({
        id: String(x.id),
        title: String(x.title ?? '').trim(),
        lastMsgTime: x.lastMsgTime,
        createTime: x.createTime,
        updateTime: x.updateTime,
      }))
    mapped.sort((a, b) => {
      const d = sessionRecencyMs(b) - sessionRecencyMs(a)
      if (d !== 0) return d
      try {
        if (BigInt(b.id) > BigInt(a.id)) return 1
        if (BigInt(b.id) < BigInt(a.id)) return -1
      } catch {
        /* ignore */
      }
      return 0
    })
    sessions.value = mapped

    const q = String(route.value?.query?.sessionId ?? '')
    const exists = sessions.value.find((x) => x.id === q)
    activeSessionId.value = exists?.id ?? sessions.value[0]?.id ?? null
    await syncRouteSession(activeSessionId.value)
  } finally {
    sessionsLoading.value = false
  }
}

const loadHistory = async (mode: 'initial' | 'more') => {
  if (historyLoading.value) return
  if (!agentCode.value || !activeSessionId.value) return
  if (mode === 'more' && !hasMoreHistory.value) return

  historyLoading.value = true
  const el = listEl.value
  const beforeTop = el?.scrollTop ?? 0
  const beforeHeight = el?.scrollHeight ?? 0
  try {
    const body: API.AgentHistoryQueryRequest = {
      agentCode: agentCode.value,
      sessionId: apiLongId(activeSessionId.value),
      pageSize: 10,
      beforeMessageId:
        mode === 'more' && historyCursor.value.beforeMessageId
          ? apiLongId(historyCursor.value.beforeMessageId)
          : undefined,
      beforeCreateTime: mode === 'more' ? historyCursor.value.beforeCreateTime : undefined,
    }
    const res = await agentListHistory(body)
    const list = res.data?.data ?? []

    if (mode === 'initial') {
      messages.value = []
      historyCursor.value = {}
      hasMoreHistory.value = false
    }
    mergeHistoryToMessages(list, 'prepend')
    const first = messages.value[0]
    if (first?.id) {
      historyCursor.value = {
        beforeMessageId: first.id,
        beforeCreateTime: first.createTime,
      }
    }
    hasMoreHistory.value = Array.isArray(list) && list.length >= 10
  } catch (e) {
    if (isNotLoginError(e)) {
      return
    }
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

const appendAssistantStreaming = (key: string) => {
  messages.value.push({ key, role: 'assistant', content: '', streaming: true })
}

const finishAssistant = () => {
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'assistant') last.streaming = false
}

const ensureSessionForSend = async (): Promise<string> => {
  if (activeSessionId.value) return activeSessionId.value
  const created = await agentCreateSession({ agentCode: agentCode.value })
  const sid = created.data?.data
  if (sid == null) {
    throw new Error('创建会话失败')
  }
  const finalSid = String(sid)
  activeSessionId.value = finalSid
  await loadSessions()
  return finalSid
}

const runStream = async (text: string, sessionId: string) => {
  abortCtl?.abort()
  abortCtl = new AbortController()
  chatLoading.value = true
  const assistantKey = `local-ai-${Date.now()}-${Math.random().toString(16).slice(2)}`
  appendAssistantStreaming(assistantKey)
  await scrollEnd()
  const idx = messages.value.length - 1
  try {
    await streamAgentChat(
      agentCode.value,
      sessionId,
      text,
      (chunk) => {
        const row = messages.value[idx]
        if (row && row.role === 'assistant') row.content += chunk
      },
      { signal: abortCtl.signal },
    )
    finishAssistant()
    await loadSessions()
    await loadHistory('initial')
  } catch (e) {
    if ((e as Error).name === 'AbortError') {
      finishAssistant()
      return
    }
    if (isNotLoginError(e)) {
      await jumpToLogin()
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

const sendUser = async () => {
  const t = inputText.value.trim()
  if (!t || chatLoading.value) return
  if (!(await ensureLoginForSend())) return

  const userKey = `local-user-${Date.now()}-${Math.random().toString(16).slice(2)}`
  messages.value.push({ key: userKey, role: 'user', content: t })
  inputText.value = ''
  await scrollEnd()
  try {
    const sid = await ensureSessionForSend()
    await runStream(t, sid)
  } catch (e) {
    message.error(getErrorMessage(e, '无法创建会话'))
  }
}

const createNewConversation = async () => {
  if (chatLoading.value) return
  if (!(await ensureLoginForSend())) return
  try {
    const created = await agentCreateSession({ agentCode: agentCode.value })
    const sid = created.data?.data
    if (sid == null) {
      message.error('创建会话失败')
      return
    }
    activeSessionId.value = String(sid)
    historyCursor.value = {}
    hasMoreHistory.value = false
    messages.value = []
    await loadSessions()
    await syncRouteSession(activeSessionId.value)
    message.success('已新建对话')
  } catch (e) {
    message.error(getErrorMessage(e))
  }
}

const selectSession = async (sid: string) => {
  if (activeSessionId.value === sid) return
  activeSessionId.value = sid
  historyCursor.value = {}
  hasMoreHistory.value = false
  await syncRouteSession(sid)
  await loadHistory('initial')
}

const onAttachmentClick = () => {
  message.info('附件上传功能即将上线')
}

const openRenameModal = (item: AgentSessionRow) => {
  renamingSessionId.value = item.id
  renameTitle.value = sessionLabel(item)
  renameModalOpen.value = true
}

const submitRename = async (titleInput?: string) => {
  const sid = renamingSessionId.value
  if (typeof titleInput === 'string') {
    renameTitle.value = titleInput
  }
  const title = renameTitle.value.trim()
  if (!sid) {
    renameModalOpen.value = false
    return
  }
  if (!title) {
    message.warning('标题不能为空')
    return
  }
  const current = sessions.value.find((x) => x.id === sid)
  if (current && title === sessionLabel(current)) {
    renameModalOpen.value = false
    return
  }
  try {
    await agentUpdateSessionTitle({
      sessionId: apiLongId(sid),
      title,
    })
    message.success('重命名成功')
    renameModalOpen.value = false
    await loadSessions()
  } catch (e) {
    message.error(getErrorMessage(e, '重命名失败'))
  }
}

const onSessionAction = (action: 'rename' | 'delete', item: AgentSessionRow) => {
  if (!item?.id) return
  if (action === 'rename') {
    openRenameModal(item)
    return
  }
  pendingDeleteItem.value = item
  deleteModalOpen.value = true
}

const runDeleteSession = async () => {
  const item = pendingDeleteItem.value
  if (!item?.id) return
  try {
    await agentDeleteSession({ id: apiLongId(item.id) })
    message.success('删除成功')
    if (activeSessionId.value === item.id) {
      activeSessionId.value = null
      messages.value = []
    }
    await loadSessions()
    if (activeSessionId.value) {
      await loadHistory('initial')
    }
    pendingDeleteItem.value = null
  } catch (e) {
    message.error(getErrorMessage(e, '删除失败，请检查后端服务或数据库连接'))
    throw e
  }
}

const initPage = async () => {
  pageLoading.value = true
  try {
    await loginUserStore.fetchLoginUser()
    if (!agentMeta.value) {
      message.error('未知智能体')
      await router.replace('/agents')
      return
    }
    messages.value = []
    historyCursor.value = {}
    hasMoreHistory.value = false
    historyInited.value = true
    await loadSessions()
    if (activeSessionId.value) {
      await loadHistory('initial')
    }
  } finally {
    pageLoading.value = false
  }
}

watch(
  () => agentCode.value,
  async () => {
    activeSessionId.value = null
    sessions.value = []
    messages.value = []
    await initPage()
  },
)

watch(
  () => route.value?.query?.sessionId,
  async (sid) => {
    const s = String(sid ?? '')
    if (!/^\d+$/.test(s)) return
    if (activeSessionId.value === s) return
    activeSessionId.value = s
    await loadHistory('initial')
  },
)

onMounted(() => {
  void initPage()
})

onBeforeUnmount(() => {
  abortCtl?.abort()
})
</script>

<template>
  <div class="agent-chat">
    <a-spin :spinning="pageLoading">
      <div class="agent-layout">
        <aside class="agent-sidebar">
          <div class="agent-sidebar__header">
            <h2 class="agent-sidebar__title">{{ agentMeta?.title ?? '智能体' }}</h2>
            <p v-if="agentMeta" class="agent-sidebar__desc">{{ agentMeta.description }}</p>
            <a-button type="primary" block @click="createNewConversation">
              <template #icon><PlusOutlined /></template>
              新对话
            </a-button>
          </div>

          <AgentSessionList
            :sessions="sessions"
            :sessions-loading="sessionsLoading"
            :active-session-id="activeSessionId"
            :is-logged-in="isLoggedIn"
            @select="selectSession"
            @action="onSessionAction"
          />

          <div class="agent-sidebar__user">
            <template v-if="isLoggedIn">
              <UserAvatar :src="loginUser?.userAvatar" :name="loginUser?.userName" :account="loginUser?.userAccount" />
              <div class="agent-sidebar__user-text">
                <div class="agent-sidebar__user-name">{{ displayUserName }}</div>
                <div class="agent-sidebar__user-sub">已登录</div>
              </div>
            </template>
            <template v-else>
              <a-button type="default" block @click="jumpToLogin">登录后继续</a-button>
            </template>
          </div>
        </aside>

        <section class="agent-main">
          <div ref="listEl" class="agent-main__messages">
            <div class="agent-main__messages-inner">
              <div class="agent-main__more">
                <a-button
                  v-if="hasMoreHistory && isLoggedIn"
                  size="small"
                  :loading="historyLoading"
                  @click="loadHistory('more')"
                >
                  加载更多
                </a-button>
              </div>
              <ChatMessage
                v-for="m in messages"
                :key="m.key"
                :role="m.role"
                :content="m.content"
                :streaming="m.streaming"
              />
              <a-empty
                v-if="messages.length === 0"
                class="agent-main__empty"
                :description="isLoggedIn ? '暂无消息，开始提问吧' : '登录后可开启智能体对话'"
              />
            </div>
          </div>

          <div class="agent-main__composer">
            <div class="agent-main__composer-inner">
              <div class="agent-main__input-box">
                <a-textarea
                  v-model:value="inputText"
                  :rows="3"
                  :disabled="chatLoading"
                  class="agent-main__textarea"
                  placeholder="输入消息，Enter 发送，Shift+Enter 换行"
                  @press-enter="
                    (e: KeyboardEvent) => {
                      if (!e.shiftKey) {
                        e.preventDefault()
                        sendUser()
                      }
                    }
                  "
                />
                <div class="agent-main__actions">
                  <a-tooltip title="附件">
                    <button
                      class="agent-icon-btn"
                      type="button"
                      :disabled="chatLoading"
                      @click="onAttachmentClick"
                    >
                      <PaperClipOutlined />
                    </button>
                  </a-tooltip>
                  <a-tooltip title="发送">
                    <button
                      class="agent-icon-btn agent-icon-btn--send"
                      type="button"
                      :disabled="chatLoading"
                      @click="sendUser"
                    >
                      <SendOutlined />
                    </button>
                  </a-tooltip>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </a-spin>

    <RenameSessionModal
      v-model:open="renameModalOpen"
      :initial-title="renameTitle"
      @submit="submitRename"
    />
    <DeleteSessionModal v-model:open="deleteModalOpen" :on-confirm="runDeleteSession" />
  </div>
</template>

<style scoped>
.agent-chat {
  width: 100%;
  padding: 0;
}

.agent-layout {
  min-height: calc(100vh - 112px);
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  border: 1px solid rgba(5, 5, 5, 0.06);
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
}

.agent-sidebar {
  border-right: 1px solid rgba(5, 5, 5, 0.06);
  background: #fafafa;
  display: flex;
  flex-direction: column;
}

.agent-sidebar__header {
  padding: 16px;
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
}

.agent-sidebar__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.agent-sidebar__desc {
  margin: 8px 0 12px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.56);
}

.agent-sidebar__user {
  border-top: 1px solid rgba(5, 5, 5, 0.06);
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-sidebar__user-text {
  min-width: 0;
}

.agent-sidebar__user-name {
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.agent-sidebar__user-sub {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.agent-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
}

.agent-main__messages {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px 20px 8px;
  background: #fcfcfc;
}

.agent-main__messages-inner {
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.agent-main__more {
  min-height: 24px;
  display: flex;
  justify-content: center;
}

.agent-main__empty {
  margin: 56px 0;
}

.agent-main__composer {
  border-top: 1px solid rgba(5, 5, 5, 0.06);
  padding: 12px 20px 16px;
  background: #fff;
}

.agent-main__composer-inner {
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
}

.agent-main__input-box {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(5, 5, 5, 0.12);
  background: #fff;
}

.agent-main__textarea {
  padding-bottom: 46px;
}

.agent-main__input-box :deep(.ant-input) {
  border: none !important;
  box-shadow: none !important;
  border-radius: 16px !important;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
  resize: none !important;
}

.agent-main__input-box :deep(.ant-input::-webkit-scrollbar) {
  width: 8px;
  height: 8px;
}

.agent-main__input-box :deep(.ant-input::-webkit-scrollbar-track) {
  background: transparent;
  margin: 0 0 10px;
}

.agent-main__input-box :deep(.ant-input::-webkit-scrollbar-thumb) {
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.2);
}

.agent-main__input-box :deep(.ant-input::-webkit-scrollbar-thumb:hover) {
  background: rgba(0, 0, 0, 0.3);
}

.agent-main__input-box :deep(.ant-input::-webkit-scrollbar-button),
.agent-main__input-box :deep(.ant-input::-webkit-scrollbar-corner),
.agent-main__input-box :deep(.ant-input::-webkit-resizer) {
  display: none;
  width: 0;
  height: 0;
}

.agent-main__actions {
  position: absolute;
  right: 8px;
  bottom: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  background: #fff;
  padding-left: 8px;
}

.agent-icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: rgba(0, 0, 0, 0.45);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.agent-icon-btn:hover {
  background: rgba(0, 0, 0, 0.08);
  color: rgba(0, 0, 0, 0.85);
}

.agent-icon-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.agent-icon-btn--send {
  color: #1677ff;
}

.agent-icon-btn--send:hover {
  background: rgba(22, 119, 255, 0.14);
  color: #0958d9;
}

@media (max-width: 960px) {
  .agent-layout {
    grid-template-columns: 1fr;
  }

  .agent-sidebar {
    border-right: none;
    border-bottom: 1px solid rgba(5, 5, 5, 0.06);
  }
}
</style>
