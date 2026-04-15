import { nextTick, ref, type ComputedRef, type Ref } from 'vue'
import { message } from 'ant-design-vue'
import { streamAgentChat } from '@/hooks/useSSEChat'
import type { AgentChatRow } from '@/types/agent'
import { getErrorMessage } from '@/utils/error'

export function useAgentStream(options: {
  agentCode: ComputedRef<string>
  messages: Ref<AgentChatRow[]>
  inputText: Ref<string>
  listEl: Ref<HTMLElement | null>
  ensureSessionForSend: () => Promise<string>
  loadSessions: () => Promise<void>
  loadHistory: (mode: 'initial' | 'more') => Promise<void>
  isNotLoginError: (e: unknown) => boolean
  jumpToLogin: () => Promise<void>
}) {
  const {
    agentCode,
    messages,
    inputText,
    listEl,
    ensureSessionForSend,
    loadSessions,
    loadHistory,
    isNotLoginError,
    jumpToLogin,
  } = options

  const chatLoading = ref(false)
  let abortCtl: AbortController | null = null

  const scrollEnd = async () => {
    await nextTick()
    const el = listEl.value
    if (el) el.scrollTop = el.scrollHeight
  }

  const appendAssistantStreaming = (key: string) => {
    messages.value.push({ key, role: 'assistant', content: '', streaming: true })
  }

  const finishAssistant = () => {
    const last = messages.value[messages.value.length - 1]
    if (last?.role === 'assistant') last.streaming = false
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

  const sendUser = async (ensureLoginForSend: () => Promise<boolean>) => {
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

  const onAttachmentClick = () => {
    message.info('附件上传功能即将上线')
  }

  const abortInFlight = () => {
    abortCtl?.abort()
  }

  return {
    chatLoading,
    scrollEnd,
    runStream,
    sendUser,
    onAttachmentClick,
    abortInFlight,
  }
}
