export const CODE_BLOCK_VISIBLE_LINES = 24

export function codeLineCount(source: string) {
  const text = (source || '').replace(/\n$/, '')
  if (!text) return 0
  return text.split('\n').length
}

export function codeBlockIsLong(source: string) {
  return codeLineCount(source) > CODE_BLOCK_VISIBLE_LINES
}
