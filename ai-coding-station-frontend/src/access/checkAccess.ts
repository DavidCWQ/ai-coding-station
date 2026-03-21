import type { AccessEnum } from './accessEnum'
import { ACCESS_ENUM } from './accessEnum'

/**
 * - NOT_LOGIN： 始终通过
 * - USER：      必须已登录（有 id）
 * - ADMIN：     必须 userRole === 'admin'
 */
export function checkAccess(
  loginUser: API.LoginUserVO | null,
  needAccess: AccessEnum,
): boolean {
  if (needAccess === ACCESS_ENUM.NOT_LOGIN) {
    return true
  }
  if (needAccess === ACCESS_ENUM.USER) {
    return !!loginUser?.id
  }
  if (needAccess === ACCESS_ENUM.ADMIN) {
    return loginUser?.userRole === 'admin'
  }
  return true
}
