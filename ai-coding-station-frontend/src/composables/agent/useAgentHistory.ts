import { nextTick, ref, watch, type ComputedRef, type Ref } from 'vue'
import { message } from 'ant-design-vue'
import { agentListHistory } from '@/api/agentController'
import type { AgentChatRow } from '@/types/agent'
import { apiLongId } from '@/utils/id'
import { getErrorMessage } from '@/utils/error'

const isAscendingByCreateTime = (rows: Array<{ createTime?: string }>) => {
  const first = rows[0]?.createTime
  const last = rows[rows.length - 1]?.createTime
  if (!first || !last) return true
  return new Date(first).valueOf() <= new Date(last).valueOf()
}

export function useAgentHistory(options: {
  agentCode: ComputedRef<string>
  activeSessionId: Ref<string | null>
  messages: Ref<AgentChatRow[]>
  listEl: Ref<HTMLElement | null>
  isNotLoginError: (e: unknown) => boolean
}) {
  const { agentCode, activeSessionId, messages, listEl, isNotLoginError } = options

  const historyLoading = ref(false)
  const historyInited = ref(false)
  const hasMoreHistory = ref(false)
  const historyCursor = ref<{ beforeMessageId?: string; beforeCreateTime?: string }>({})

  const mergeHistoryToMessages = (incoming: API.AgentChatMessageVO[], mode: 'prepend' | 'append') => {
    const list = Array.isArray(incoming) ? incoming : []
    if (list.length === 0) return

    const normalized = isAscendingByCreateTime(list) ? list : [...list].reverse()
    const existingIds = new Set<string>()
    for (const m of messages.value) {
      if (typeof m.id === 'string' && m.id) existingIds.add(m.id)
    }

    const mapped: AgentChatRow[] = normalized
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

  const resetPaging = () => {
    historyCursor.value = {}
    hasMoreHistory.value = false
  }

  // 删除当前会话且暂无下一选中项时不会走 loadHistory('initial')，需清掉「加载更多」状态
  watch(
    () => activeSessionId.value,
    (id) => {
      if (id == null) resetPaging()
    },
  )

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

  const loadHistoryInitial = () => loadHistory('initial')

  return {
    historyLoading,
    historyInited,
    hasMoreHistory,
    historyCursor,
    loadHistory,
    loadHistoryInitial,
    resetPaging,
  }
}
