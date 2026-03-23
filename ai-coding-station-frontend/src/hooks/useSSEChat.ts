import { assertValidAppId } from '@/utils/appId'

function buildChatGenCodeUrl(appId: string, message: string): string {
  assertValidAppId(appId)
  const qs = `appId=${encodeURIComponent(appId)}&message=${encodeURIComponent(message)}`
  const base = import.meta.env.VITE_APP_API_BASE_URL ?? '/api'
  // 处理完整 URL：http://localhost:8142/api
  if (base.startsWith('http://') || base.startsWith('https://')) {
    const u = new URL(base)
    const prefix = u.pathname.endsWith('/') ? u.pathname.slice(0, -1) : u.pathname
    return `${u.origin}${prefix}/app/chat/genCode?${qs}`
  }
  // 处理相对路径：/api
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  const prefix = base.endsWith('/') ? base.slice(0, -1) : base
  return `${origin}${prefix}/app/chat/genCode?${qs}`
}

/**
 * 调用 GET /app/chat/genCode (SSE)，解析 Spring 下发的 data: {"d":"..."}
 */
export async function streamChatGenCode(
  appId: string,
  message: string,
  onDelta: (chunk: string) => void,   // 每次收到数据块的回调
  options?: { signal?: AbortSignal }, // 用于取消请求
): Promise<void> {
  const url = buildChatGenCodeUrl(appId, message)

  const res = await fetch(url, {
    method: 'GET',
    credentials: 'include',           // 携带 Cookie（保持登录状态）
    signal: options?.signal,
    headers: { Accept: 'text/event-stream' },
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `请求失败 ${res.status}`)
  }

  const reader = res.body?.getReader()
  if (!reader) {
    throw new Error('无法读取响应流')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  const processBlock = (block: string) => {
    const lines = block.split(/\r?\n/)
    for (const line of lines) {
      if (!line.startsWith('data:')) continue
      const raw = line.slice(5).trim()
      if (!raw || raw === '[DONE]') continue
      try {
        const j = JSON.parse(raw) as { d?: string }
        if (typeof j.d === 'string' && j.d.length > 0) {
          onDelta(j.d)
        }
      } catch {
        onDelta(raw)
      }
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split(/\n\n/)
    buffer = parts.pop() ?? ''
    for (const p of parts) {
      processBlock(p)
    }
  }
  if (buffer.trim()) {
    processBlock(buffer)
  }
}
