import { describe, expect, it } from 'vitest'

import { codeBlockIsLong, codeLineCount, CODE_BLOCK_VISIBLE_LINES } from './codeBlock'

describe('codeBlockIsLong', () => {
  it('keeps a block of 24 lines fully open', () => {
    const source = Array.from({ length: CODE_BLOCK_VISIBLE_LINES }, (_, i) => `line ${i}`).join('\n')
    expect(codeLineCount(source)).toBe(24)
    expect(codeBlockIsLong(source)).toBe(false)
  })

  it('collapses a block once it passes 24 lines, ignoring one trailing newline', () => {
    const source = `${Array.from({ length: 25 }, (_, i) => `line ${i}`).join('\n')}\n`
    expect(codeLineCount(source)).toBe(25)
    expect(codeBlockIsLong(source)).toBe(true)
  })
})
