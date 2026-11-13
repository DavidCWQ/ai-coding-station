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
  const showJump = ref(false)
  let ignoreScroll = false
  let touchStartY = 0
  let touchInCode = false

  const refreshJump = () => {
    const el = listEl.value
    showJump.value = !!el && el.scrollHeight - el.clientHeight > 8 && !isChatPinned(el, 8)
  }

  const onListScroll = () => {
    if (ignoreScroll || listEl.value?.dataset.chatScrollHold) return
    pinned.value = isChatPinned(listEl.value)
    refreshJump()
  }

  // 滚轮、触摸和方向键表示读者要离开底部。生成中的程序化滚动不能把它盖掉。
  const releasePin = () => {
    pinned.value = false
    refreshJump()
  }

  const onUserWheel = (event: WheelEvent) => {
    if (event.deltaY >= 0) return
    const target = event.target
    if (target instanceof Element) {
      const code = target.closest('.msg__code') as HTMLElement | null
      if (code && code.scrollTop > 0) return
    }
    releasePin()
  }

  const onTouchStart = (event: TouchEvent) => {
    const target = event.target
    touchInCode = target instanceof Element && !!target.closest('.msg__code')
    touchStartY = event.touches[0]?.clientY ?? 0
  }

  const onTouchMove = (event: TouchEvent) => {
    const y = event.touches[0]?.clientY ?? touchStartY
    if (!touchInCode && y - touchStartY > 12) releasePin()
    touchStartY = y
  }

  const scrollToEnd = async (force = false) => {
    await nextTick()
    if (!force && !pinned.value) return
    const el = listEl.value
    if (!el) return
    ignoreScroll = true
    el.scrollTop = el.scrollHeight
    requestAnimationFrame(() => {
      ignoreScroll = false
      refreshJump()
    })
  }

  const scrollIfPinned = async () => {
    if (!pinned.value) {
      refreshJump()
      return
    }
    await scrollToEnd()
  }

  const pinAndScroll = async () => {
    pinned.value = true
    await scrollToEnd(true)
  }

  return {
    pinned,
    showJump,
    onListScroll,
    onUserWheel,
    onTouchStart,
    onTouchMove,
    scrollIfPinned,
    pinAndScroll,
  }
}
