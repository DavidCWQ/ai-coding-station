export type AppMenuItem = {
  key: string
  label: string
  path: string
}

export const appMenu: AppMenuItem[] = [
  { key: 'home', label: '首页', path: '/' },
  { key: 'about', label: '关于', path: '/about' },
  { key: 'app', label: '应用工坊', path: '/app' },
  { key: 'app-manage', label: '应用管理', path: '/app/manage' },
  { key: 'admin-user', label: '用户管理', path: '/admin/userManage' },
]

