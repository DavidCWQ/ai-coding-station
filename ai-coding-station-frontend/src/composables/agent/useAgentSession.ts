import { ref, type ComputedRef, type Ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  agentCreateSession,
  agentDeleteSession,
  agentListSessions,
  agentUpdateSessionTitle,
} from '@/api/agentController'
import type { AgentChatRow, AgentSessionRow } from '@/types/agent'
import { apiLongId, idFromData } from '@/utils/id'
import { getErrorMessage } from '@/utils/error'

export function sessionLabel(item: AgentSessionRow): string {
  return item.title?.trim() || '新对话'
}

export function sessionRecencyMs(row: AgentSessionRow): number {
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

export function useAgentSession(options: {
  agentCode: ComputedRef<string>
  isLoggedIn: ComputedRef<boolean>
  route: ComputedRef<any>
  router: any
  messages: Ref<AgentChatRow[]>
}) {
  const { agentCode, isLoggedIn, route, router, messages } = options

  const sessions = ref<AgentSessionRow[]>([])
  const sessionsLoading = ref(false)
  const activeSessionId = ref<string | null>(null)

  const renameModalOpen = ref(false)
  const renamingSessionId = ref<string | null>(null)
  const renameTitle = ref('')
  const deleteModalOpen = ref(false)
  const pendingDeleteItem = ref<AgentSessionRow | null>(null)

  let reloadHistoryInitial: (() => Promise<void>) | null = null

  const bindHistoryReload = (fn: () => Promise<void>) => {
    reloadHistoryInitial = fn
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

  const ensureSessionForSend = async (): Promise<string> => {
    if (activeSessionId.value) return activeSessionId.value
    const created = await agentCreateSession({ agentCode: agentCode.value })
    const sid = created.data?.data
    if (sid == null) {
      throw new Error('创建会话失败')
    }
    const finalSid = String(sid)
    activeSessionId.value = finalSid
    await syncRouteSession(finalSid)
    await loadSessions()
    return finalSid
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
      if (activeSessionId.value && reloadHistoryInitial) {
        await reloadHistoryInitial()
      }
      pendingDeleteItem.value = null
    } catch (e) {
      message.error(getErrorMessage(e, '删除失败，请检查后端服务或数据库连接'))
      throw e
    }
  }

  return {
    sessions,
    sessionsLoading,
    activeSessionId,
    renameModalOpen,
    renamingSessionId,
    renameTitle,
    deleteModalOpen,
    pendingDeleteItem,
    bindHistoryReload,
    syncRouteSession,
    loadSessions,
    ensureSessionForSend,
    submitRename,
    onSessionAction,
    runDeleteSession,
  }
}
