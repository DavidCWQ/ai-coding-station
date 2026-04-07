import { getStaticPreviewIndexUrl, getStaticPreviewUrl } from '@/config/env'

/**
 * 生成代码预览 (与后端 StaticResourceController `/api/static/**` 一致)
 */
function buildPreviewCandidates(codeGenType: string, appId: string): string[] {
  return [getStaticPreviewIndexUrl(codeGenType, appId), getStaticPreviewUrl(codeGenType, appId)]
}

function looksLikeJsonError(res: Response): boolean {
  const ct = (res.headers.get('content-type') ?? '').toLowerCase()
  return ct.includes('application/json') || ct.includes('text/json')
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
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
  const maxAttempts = options?.maxAttempts ?? 30
  const intervalMs = options?.intervalMs ?? 600
  const candidates = buildPreviewCandidates(codeGenType, appId)
  for (let i = 0; i < maxAttempts; i++) {
    if (options?.signal?.aborted) {
      return { ok: false }
    }
    for (const url of candidates) {
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
    }
    await sleep(intervalMs)
  }
  return { ok: false }
}
