import { nextTick, ref, type Ref } from 'vue'

export const CHAT_STICK_GAP = 48

export function isChatPinned(
  el: { scrollHeight: number; scrollTop: number; clientHeight: number } | null,
  gap = CHAT_STICK_GAP,
) {
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight <= gap
}

export function useStickToBottom(listEl: Ref<HTMLElement | null>) {
  const pinned = ref(true)
  let ignoreScroll = 0

  const onListScroll = () => {
    if (ignoreScroll > 0) return
    pinned.value = isChatPinned(listEl.value)
  }

  const scrollToEnd = async () => {
    await nextTick()
    const el = listEl.value
    if (!el) return
    ignoreScroll += 1
    el.scrollTop = el.scrollHeight
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        ignoreScroll = Math.max(0, ignoreScroll - 1)
      })
    })
  }

  const scrollIfPinned = async () => {
    if (!pinned.value) return
    await scrollToEnd()
  }

  const pinAndScroll = async () => {
    pinned.value = true
    await scrollToEnd()
  }

  return { pinned, onListScroll, scrollIfPinned, pinAndScroll }
}
