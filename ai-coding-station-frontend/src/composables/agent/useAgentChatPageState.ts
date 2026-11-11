import {
  computed,
  getCurrentInstance,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { findAgentCard } from '@/constants/agents'
import { getErrorMessage } from '@/utils/error'
import { agentCreateSession } from '@/api/agentController'
import { voteMessage } from '@/api/feedbackController'
import { createAgentShare, revokeAgentShare } from '@/api/shareController'
import type { ChatAction } from '@/components/chat/ChatMessage.vue'
import { groupKeyOf, groupTurns } from '@/features/chat/chatGroups'
import { conversationFilename, renderConversationMarkdown } from '@/features/chat/conversationMarkdown'
import type { AgentChatRow } from '@/types/agent'
import { triggerBlobDownload } from '@/utils/fileDownload'
import { useAgentHistory } from './useAgentHistory'
import { useAgentSession } from './useAgentSession'
import { useAgentStream } from './useAgentStream'

export function useAgentChatPageState() {
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
  const inputText = ref('')
  const messages = ref<AgentChatRow[]>([])
  const listEl = ref<HTMLElement | null>(null)

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

  const session = useAgentSession({
    agentCode,
    isLoggedIn,
    route,
    router,
    messages,
  })

  const history = useAgentHistory({
    agentCode,
    activeSessionId: session.activeSessionId,
    messages,
    listEl,
    isNotLoginError,
  })

  session.bindHistoryReload(history.loadHistoryInitial)

  const stream = useAgentStream({
    agentCode,
    messages,
    inputText,
    listEl,
    ensureSessionForSend: session.ensureSessionForSend,
    loadSessions: session.loadSessions,
    loadHistory: history.loadHistory,
    isNotLoginError,
    jumpToLogin,
  })

  const createNewConversation = async () => {
    if (stream.chatLoading.value) return
    if (!(await ensureLoginForSend())) return
    try {
      const created = await agentCreateSession({ agentCode: agentCode.value })
      const sid = created.data?.data
      if (sid == null) {
        message.error('创建会话失败')
        return
      }
      session.activeSessionId.value = String(sid)
      history.resetPaging()
      messages.value = []
      // 先同步路由，loadSessions 内会按 query.sessionId 解析选中项；否则会沿用旧 sessionId 选中第二条等错误
      await session.syncRouteSession(session.activeSessionId.value)
      await session.loadSessions()
      message.success('已新建对话')
    } catch (e) {
      message.error(getErrorMessage(e))
    }
  }

  const selectSession = async (sid: string) => {
    if (session.activeSessionId.value === sid) return
    session.activeSessionId.value = sid
    history.resetPaging()
    await session.syncRouteSession(sid)
    await history.loadHistory('initial')
    await stream.pinAndScroll()
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
      if (agentMeta.value.adminOnly && loginUserStore.loginUser?.userRole !== 'admin') {
        message.error('无权限使用该智能体')
        await router.replace('/agents')
        return
      }
      messages.value = []
      history.resetPaging()
      history.historyInited.value = true
      await session.loadSessions()
      if (session.activeSessionId.value) {
        await history.loadHistory('initial')
        await stream.pinAndScroll()
      }
    } finally {
      pageLoading.value = false
    }
  }

  watch(
    () => agentCode.value,
    async () => {
      session.activeSessionId.value = null
      session.sessions.value = []
      messages.value = []
      await initPage()
    },
  )

  watch(
    () => route.value?.query?.sessionId,
    async (sid) => {
      const s = String(sid ?? '')
      if (!/^\d+$/.test(s)) return
      if (session.activeSessionId.value === s) return
      session.activeSessionId.value = s
      await history.loadHistory('initial')
      await stream.pinAndScroll()
    },
  )

  onMounted(() => {
    void initPage()
  })

  onBeforeUnmount(() => {
    stream.abortInFlight()
  })

  const sendUser = () => stream.sendUser(ensureLoginForSend)

  const selecting = ref(false)
  const selectedGroupKeys = ref<string[]>([])
  const shareOpen = ref(false)
  const shareToken = ref('')
  const shareTitle = ref('')

  const groups = computed(() => groupTurns(messages.value))
  const selectableGroups = computed(() => groups.value.filter((group) => group.selectable))
  const selectedCount = computed(() => selectedGroupKeys.value.length)
  const allGroupsSelected = computed(() =>
    selectableGroups.value.length > 0
    && selectableGroups.value.every((group) => selectedGroupKeys.value.includes(group.key)),
  )
  const shareUrl = computed(() => (shareToken.value ? `${window.location.origin}/s/${shareToken.value}` : ''))
  const shareBlurb = computed(() => `来自「${agentMeta.value?.title || '智能体'}」的一段对话 ${shareUrl.value}`.trim())

  watch(
    () => session.activeSessionId.value,
    () => {
      selecting.value = false
      selectedGroupKeys.value = []
    },
  )

  const previousUserIndex = (from: number) => {
    for (let i = from; i >= 0; i -= 1) {
      if (messages.value[i]?.role === 'user') return i
    }
    return -1
  }

  const rewriteFrom = async (userIndex: number, text: string) => {
    const user = messages.value[userIndex]
    const sid = session.activeSessionId.value
    if (!user?.id || !sid) {
      message.warning('这条消息还没保存完成，请稍后再试')
      return
    }
    if (stream.chatLoading.value) return
    const next = text.trim()
    if (!next) return
    selecting.value = false
    user.content = next
    messages.value.splice(userIndex + 1)
    await stream.runStream(next, sid, user.id)
  }

  const onRetry = async (index: number) => {
    const userIndex = previousUserIndex(index)
    const user = userIndex >= 0 ? messages.value[userIndex] : undefined
    if (!user) {
      message.warning('找不到对应的发言')
      return
    }
    await rewriteFrom(userIndex, user.content)
  }

  const onEditMessage = (index: number, text: string) => rewriteFrom(index, text)

  const onVote = async (row: AgentChatRow, payload: { vote: 1 | -1 | 0; reason?: string }) => {
    if (!row.id) {
      message.warning('这条消息还没保存完成，请稍后再试')
      return
    }
    const previous = row.vote
    row.vote = payload.vote === 0 ? null : payload.vote
    try {
      await voteMessage({
        source: 'agent',
        messageId: row.id,
        vote: payload.vote,
        reason: payload.reason,
      })
    } catch {
      row.vote = previous
    }
  }

  const actionsFor = (row: AgentChatRow): ChatAction[] => {
    if (selecting.value || row.streaming || !row.content.trim()) return []
    if (!isLoggedIn.value) return row.role === 'assistant' ? ['copy'] : ['copy']
    if (row.role === 'user') return ['copy', 'edit']
    return ['copy', 'retry', 'like', 'dislike', 'share']
  }

  const startShare = (messageKey: string) => {
    const groupKey = groupKeyOf(messages.value, messageKey)
    if (!groupKey) return
    selectedGroupKeys.value = [groupKey]
    selecting.value = true
  }

  const toggleGroup = (key: string) => {
    const next = new Set(selectedGroupKeys.value)
    if (next.has(key)) next.delete(key)
    else next.add(key)
    selectedGroupKeys.value = [...next]
  }

  const toggleAllGroups = () => {
    selectedGroupKeys.value = allGroupsSelected.value
      ? []
      : selectableGroups.value.map((group) => group.key)
  }

  const selectedRows = () => {
    const keys = new Set(selectedGroupKeys.value)
    return groups.value.filter((group) => keys.has(group.key)).flatMap((group) => group.messages)
  }

  const cancelSharePick = () => {
    selecting.value = false
    selectedGroupKeys.value = []
  }

  const downloadSelected = () => {
    const rows = selectedRows()
    if (!rows.length) return
    const title = agentMeta.value?.title || '对话'
    const now = new Date()
    const markdown = renderConversationMarkdown({
      title,
      source: `智能体「${title}」`,
      exportedAt: now,
      includeReasoning: true,
      turns: rows.map((row) => ({ role: row.role, content: row.content, reasoning: row.reasoning })),
    })
    triggerBlobDownload(new Blob([markdown], { type: 'text/markdown;charset=utf-8' }), conversationFilename(title, now))
  }

  const createShareLink = async () => {
    const sid = session.activeSessionId.value
    const rows = selectedRows()
    if (!sid || !rows.length) return
    if (rows.some((row) => !row.id)) {
      message.warning('有消息还没保存完成，请稍后再分享')
      return
    }
    try {
      const res = await createAgentShare(sid, rows.map((row) => row.id as string))
      const token = res.data?.data?.token
      if (!token) return
      shareToken.value = token
      shareTitle.value = res.data?.data?.title || agentMeta.value?.title || '对话分享'
      shareOpen.value = true
      selecting.value = false
    } catch {
      // 请求拦截器已经提示失败原因
    }
  }

  const revokeShare = async () => {
    if (!shareToken.value) return
    try {
      await revokeAgentShare(shareToken.value)
      shareOpen.value = false
      message.success('已停止分享')
    } catch {
      // 请求拦截器已经提示失败原因
    }
  }

  return {
    router,
    route,
    agentCode,
    agentMeta,
    loginUser,
    isLoggedIn,
    displayUserName,
    pageLoading,
    inputText,
    messages,
    listEl,
    jumpToLogin,
    sessions: session.sessions,
    sessionsLoading: session.sessionsLoading,
    activeSessionId: session.activeSessionId,
    renameModalOpen: session.renameModalOpen,
    renameTitle: session.renameTitle,
    deleteModalOpen: session.deleteModalOpen,
    runDeleteSession: session.runDeleteSession,
    submitRename: session.submitRename,
    onSessionAction: session.onSessionAction,
    hasMoreHistory: history.hasMoreHistory,
    historyLoading: history.historyLoading,
    loadHistory: history.loadHistory,
    chatLoading: stream.chatLoading,
    showJump: stream.showJump,
    onListScroll: stream.onListScroll,
    onUserWheel: stream.onUserWheel,
    onTouchStart: stream.onTouchStart,
    onTouchMove: stream.onTouchMove,
    pinAndScroll: stream.pinAndScroll,
    sendUser,
    onAttachmentClick: stream.onAttachmentClick,
    createNewConversation,
    selectSession,
    selecting,
    groups,
    selectedGroupKeys,
    selectedCount,
    allGroupsSelected,
    shareOpen,
    shareUrl,
    shareTitle,
    shareBlurb,
    actionsFor,
    onRetry,
    onEditMessage,
    onVote,
    startShare,
    toggleGroup,
    toggleAllGroups,
    cancelSharePick,
    downloadSelected,
    createShareLink,
    revokeShare,
  }
}
