import { describe, expect, it } from 'vitest'

import { resolveVisitTarget } from './trackVisit'

describe('resolveVisitTarget', () => {
  it('counts the homepage, about page, and workshop as site views', () => {
    expect(resolveVisitTarget({ name: 'home', path: '/', params: {} })).toEqual({
      scope: 'SITE',
      path: '/',
    })
    expect(resolveVisitTarget({ name: 'about', path: '/about', params: {} })?.scope).toBe('SITE')
    expect(resolveVisitTarget({ name: 'app-home', path: '/app', params: {} })?.scope).toBe('SITE')
  })

  it('counts an app detail view for that app only', () => {
    expect(
      resolveVisitTarget({
        name: 'app-detail',
        path: '/app/123',
        params: { appId: '123' },
      }),
    ).toEqual({ scope: 'APP', appId: '123', path: '/app/123' })
  })

  it('ignores admin, manage, and a detail route without a numeric id', () => {
    expect(resolveVisitTarget({ name: 'admin-stats', path: '/admin/stats', params: {} })).toBeNull()
    expect(resolveVisitTarget({ name: 'app-manage', path: '/app/manage', params: {} })).toBeNull()
    expect(resolveVisitTarget({ name: 'app-detail', path: '/app/new', params: { appId: 'new' } })).toBeNull()
  })
})
