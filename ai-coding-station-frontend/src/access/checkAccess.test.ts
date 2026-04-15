import { describe, expect, it } from 'vitest'
import { ACCESS_ENUM } from './accessEnum'
import { checkAccess } from './checkAccess'

describe('checkAccess', () => {
  it('always passes for NOT_LOGIN', () => {
    expect(checkAccess(null, ACCESS_ENUM.NOT_LOGIN)).toBe(true)
  })

  it('requires user id for USER', () => {
    expect(checkAccess(null, ACCESS_ENUM.USER)).toBe(false)
    expect(checkAccess({ id: 1 } as API.LoginUserVO, ACCESS_ENUM.USER)).toBe(true)
  })

  it('requires admin role for ADMIN', () => {
    expect(checkAccess({ id: 1, userRole: 'user' } as API.LoginUserVO, ACCESS_ENUM.ADMIN)).toBe(false)
    expect(checkAccess({ id: 1, userRole: 'admin' } as API.LoginUserVO, ACCESS_ENUM.ADMIN)).toBe(true)
  })
})
