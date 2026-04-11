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
import type { AgentChatRow } from '@/types/agent'
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
    },
  )

  onMounted(() => {
    void initPage()
  })

  onBeforeUnmount(() => {
    stream.abortInFlight()
  })

  const sendUser = () => stream.sendUser(ensureLoginForSend)

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
    sendUser,
    onAttachmentClick: stream.onAttachmentClick,
    createNewConversation,
    selectSession,
  }
}
