export function getErrorMessage(err: unknown, fallback = '操作失败'): string {
  if (typeof err === 'object' && err !== null) {
    const m = (err as Record<string, unknown>).message
    if (typeof m === 'string' && m.trim()) return m
  }
  return fallback
}
