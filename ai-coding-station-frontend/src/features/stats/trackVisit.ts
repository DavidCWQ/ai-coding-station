import { getApiUrl } from '@/config/env'

export type VisitScopeName = 'SITE' | 'APP'

export type VisitTarget = {
  scope: VisitScopeName
  appId?: string
  path: string
}

const SITE_ROUTES = new Set(['home', 'about', 'app-home'])

/**
 * 首页、关于、应用工坊记入全站。应用详情只记入该应用，不记入全站。
 */
export function resolveVisitTarget(to: {
  name?: string | symbol | null
  path: string
  params: Record<string, unknown>
}): VisitTarget | null {
  const name = typeof to.name === 'string' ? to.name : ''
  if (SITE_ROUTES.has(name)) {
    return { scope: 'SITE', path: to.path || '/' }
  }
  if (name !== 'app-detail') {
    return null
  }
  const raw = to.params.appId
  const appId = Array.isArray(raw) ? raw[0] : raw
  if (typeof appId !== 'string' || !/^\d+$/.test(appId)) {
    return null
  }
  return { scope: 'APP', appId, path: to.path || `/app/${appId}` }
}

let active: VisitTarget | null = null
let timer = 0
let listening = false

function postHit(kind: 'VIEW' | 'HEARTBEAT', target: VisitTarget) {
  const body = JSON.stringify({
    kind,
    scope: target.scope,
    appId: target.appId,
    path: target.path,
  })
  void fetch(getApiUrl('/stats/hit'), {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body,
  }).catch(() => {
    // 统计失败不打断浏览
  })
}

function onVisibility() {
  if (document.visibilityState === 'visible' && active) {
    postHit('HEARTBEAT', active)
  }
}

/**
 * 路由切换时上报一次浏览，并在页面可见时每分钟刷新在线状态。
 */
export function trackRoute(to: {
  name?: string | symbol | null
  path: string
  params: Record<string, unknown>
}) {
  active = resolveVisitTarget(to)
  if (!listening && typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', onVisibility)
    listening = true
  }
  if (timer) {
    window.clearInterval(timer)
    timer = 0
  }
  if (!active) {
    return
  }
  postHit('VIEW', active)
  timer = window.setInterval(() => {
    if (!active || document.visibilityState !== 'visible') {
      return
    }
    postHit('HEARTBEAT', active)
  }, 60_000)
}
