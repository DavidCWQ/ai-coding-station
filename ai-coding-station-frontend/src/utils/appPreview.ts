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
