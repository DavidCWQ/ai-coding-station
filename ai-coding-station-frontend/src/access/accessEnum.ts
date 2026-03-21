export const ACCESS_ENUM = {
  /** 无需登录 */
  NOT_LOGIN: 'NOT_LOGIN',
  /** 需要登录 */
  USER: 'USER',
  /** 需管理员 */
  ADMIN: 'ADMIN',
} as const

export type AccessEnum = (typeof ACCESS_ENUM)[keyof typeof ACCESS_ENUM]
