/**
 * 环境变量统一配置与 URL 构造函数
 */
export const APP_DEPLOY_DOMAIN =
  (import.meta.env.VITE_APP_DEPLOY_BASE_URL || 'http://localhost:8088').replace(/\/+$/, '')

export const APP_API_BASE_URL =
  (import.meta.env.VITE_APP_API_BASE_URL || '/api').replace(/\/+$/, '')

export const APP_PREVIEW_BASE_URL =
  (import.meta.env.VITE_APP_PREVIEW_BASE_URL || APP_API_BASE_URL).replace(/\/+$/, '')

export const STATIC_BASE_URL = `${APP_PREVIEW_BASE_URL}/static`

function toAbsoluteUrl(base: string, path: string): string {
  const safePath = path.startsWith('/') ? path : `/${path}`
  if (base.startsWith('http://') || base.startsWith('https://')) {
    const u = new URL(base)
    const prefix = u.pathname.endsWith('/') ? u.pathname.slice(0, -1) : u.pathname
    return `${u.origin}${prefix}${safePath}`
  }
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  const prefix = base.endsWith('/') ? base.slice(0, -1) : base
  return `${origin}${prefix}${safePath}`
}

export function getApiUrl(path: string): string {
  return toAbsoluteUrl(APP_API_BASE_URL, path)
}

export function getDeployUrl(deployKey: string): string {
  const key = String(deployKey || '').trim()
  return `${APP_DEPLOY_DOMAIN}/${encodeURIComponent(key)}/`
}

export function getStaticPreviewUrl(codeGenType: string, appId: string): string {
  const safeType = (codeGenType || 'html').trim() || 'html'
  const standardType = safeType.replace(/-/g, '_')
  return toAbsoluteUrl(STATIC_BASE_URL, `/${standardType}_${appId}/`)
}

export function getStaticPreviewIndexUrl(codeGenType: string, appId: string): string {
  return `${getStaticPreviewUrl(codeGenType, appId)}index.html`
}
