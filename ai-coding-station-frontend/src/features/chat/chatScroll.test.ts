import { describe, expect, it } from 'vitest'

import { isChatPinned } from './chatScroll'

describe('isChatPinned', () => {
  it('treats a missing list as pinned so the first message can land at the end', () => {
    expect(isChatPinned(null)).toBe(true)
  })

  it('stays pinned when the reader is within the bottom gap', () => {
    expect(isChatPinned({ scrollHeight: 1000, scrollTop: 760, clientHeight: 200 })).toBe(true)
  })

  it('lets go once the reader has moved up to look at earlier text', () => {
    expect(isChatPinned({ scrollHeight: 1000, scrollTop: 100, clientHeight: 200 })).toBe(false)
  })
})
