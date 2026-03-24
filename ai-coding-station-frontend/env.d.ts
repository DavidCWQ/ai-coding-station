import 'vue-router'

interface ImportMetaEnv {
  readonly VITE_APP_API_BASE_URL?: string
  readonly VITE_APP_PREVIEW_BASE_URL?: string
  readonly VITE_APP_DEPLOY_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    layout?: string
    auth?: boolean
    access?: 'NOT_LOGIN' | 'USER' | 'ADMIN'
    hideInMenu?: boolean
  }
}
