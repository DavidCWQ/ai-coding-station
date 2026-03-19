export type AppMenuItem = {
  key: string
  label: string
  path: string
}

export const appMenu: AppMenuItem[] = [
  { key: 'home', label: '首页', path: '/' },
  { key: 'about', label: '关于', path: '/about' },
]

