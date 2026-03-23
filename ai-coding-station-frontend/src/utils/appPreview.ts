/**
 * 生成代码静态预览（与 WebMvcConfig `/api/static/**` → outputDir 一致）
 * 显式 index.html，避免部分浏览器对目录 URL 处理不一致
 */
export function buildStaticPreviewUrl(codeGenType: string, appId: string): string {
  const safeType = (codeGenType || 'html').trim() || 'html'
  const path = `/static/${safeType}_${appId}/index.html`
  const base = import.meta.env.VITE_APP_API_BASE_URL ?? '/api'
  if (base.startsWith('http://') || base.startsWith('https://')) {
    const u = new URL(base)
    const prefix = u.pathname.endsWith('/') ? u.pathname.slice(0, -1) : u.pathname
    return `${u.origin}${prefix}${path}`
  }
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  const prefix = base.endsWith('/') ? base.slice(0, -1) : base
  return `${origin}${prefix}${path}`
}

function looksLikeJsonError(res: Response): boolean {
  const ct = (res.headers.get('content-type') ?? '').toLowerCase()
  return ct.includes('application/json') || ct.includes('text/json')
}

/**
 * 流式生成结束后磁盘写入可能略晚于 SSE 结束；若直接给 iframe 赋值，易加载到错误 JSON。
 * 轮询 GET 直至拿到 HTML 或非 JSON 成功响应，避免把接口错误页嵌进 iframe。
 */
export async function waitForStaticPreviewReady(
  codeGenType: string,
  appId: string,
  options?: { maxAttempts?: number; intervalMs?: number; signal?: AbortSignal },
): Promise<{ ok: true; url: string } | { ok: false }> {
  const maxAttempts = options?.maxAttempts ?? 12
  const intervalMs = options?.intervalMs ?? 400
  const url = buildStaticPreviewUrl(codeGenType, appId)

  for (let i = 0; i < maxAttempts; i++) {
    if (options?.signal?.aborted) {
      return { ok: false }
    }
    try {
      const res = await fetch(url, {
        method: 'GET',
        credentials: 'include',
        cache: 'no-store',
        signal: options?.signal,
      })
      if (res.ok && !looksLikeJsonError(res)) {
        return { ok: true, url }
      }
    } catch {
      /* 网络或服务未就绪，重试 */
    }
    await new Promise((r) => setTimeout(r, intervalMs))
  }
  return { ok: false }
}
