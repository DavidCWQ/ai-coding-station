import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    layout?: string
    auth?: boolean
    access?: 'NOT_LOGIN' | 'USER' | 'ADMIN'
    hideInMenu?: boolean
  }
}
