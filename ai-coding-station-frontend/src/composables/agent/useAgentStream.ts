import { ref, type ComputedRef, type Ref } from 'vue'
import { message } from 'ant-design-vue'
import { streamAgentChat } from '@/hooks/useSSEChat'
import type { AgentChatRow } from '@/types/agent'
import { useStickToBottom } from '@/features/chat/chatScroll'
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
  const { pinned, onListScroll, scrollIfPinned, pinAndScroll } = useStickToBottom(listEl)

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
    await pinAndScroll()
    const idx = messages.value.length - 1
    let reloadHistory = false
    try {
      await streamAgentChat(
        agentCode.value,
        sessionId,
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
      await loadSessions()
      reloadHistory = true
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
      const follow = pinned.value
      chatLoading.value = false
      abortCtl = null
      if (reloadHistory) await loadHistory('initial')
      if (follow) await pinAndScroll()
      else await scrollIfPinned()
    }
  }

  const sendUser = async (ensureLoginForSend: () => Promise<boolean>) => {
    const t = inputText.value.trim()
    if (!t || chatLoading.value) return
    if (!(await ensureLoginForSend())) return

    const userKey = `local-user-${Date.now()}-${Math.random().toString(16).slice(2)}`
    messages.value.push({ key: userKey, role: 'user', content: t })
    inputText.value = ''
    await pinAndScroll()
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
    onListScroll,
    pinAndScroll,
    runStream,
    sendUser,
    onAttachmentClick,
    abortInFlight,
  }
}
