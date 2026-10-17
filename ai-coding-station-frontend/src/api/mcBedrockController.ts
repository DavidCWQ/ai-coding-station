import request from '@/request'
import { APP_API_BASE_URL } from '@/config/env'
import { parseFilenameFromContentDisposition, triggerBlobDownload } from '@/utils/fileDownload'

export type McViewerRole = 'ADMIN' | 'VISITOR'

export interface McWorldVO {
  folder: string
  displayName: string
  current: boolean
  sizeBytes: number | string
  lastModifiedEpochMs: number | string
  lastBackupEpochMs?: number | string
  hasIcon: boolean
  iconUrl?: string
}

export interface McSettingsVO {
  serverName: string
  levelName: string
  gamemode: string
  difficulty: string
  maxPlayers: number
  viewDistance: number
  tickDistance: number
  playerIdleTimeout: number
  allowList: boolean
  allowCheats: boolean
  onlineMode: boolean
}

export interface McGameruleVO {
  id: string
  label: string
  hint: string
  hot: boolean
  value: boolean | null
}

export interface McPlayerVO {
  name: string
  xuid: string
  permission: string
  allowlisted: boolean
}

export interface McBackupVO {
  filename: string
  sizeBytes: number | string
  createdEpochMs: number | string
}

export interface McConsoleVO {
  configured: boolean
  message?: string
  viewerRole: McViewerRole
  running: boolean
  runPhase?: 'SLEEPING' | 'STARTING' | 'RUNNING' | 'STOPPING' | 'BACKING_UP'
  serverName?: string
  joinHint?: string
  currentWorld?: McWorldVO
  worlds: McWorldVO[]
  settings?: McSettingsVO
  gamerules: McGameruleVO[]
  players: McPlayerVO[]
  backups: McBackupVO[]
  lastBackupEpochMs?: number | string
}

export interface McOpResultVO {
  opId: string
  action: string
  status: string
  message?: string
}

async function unwrap<T>(p: Promise<{ data: { data: T } }>): Promise<T> {
  const res = await p
  return res.data.data
}

export function getMcConsole() {
  return unwrap(request.get('/mc/console')) as Promise<McConsoleVO>
}

export function getMcAdminConsole() {
  return unwrap(request.get('/mc/admin/console')) as Promise<McConsoleVO>
}

export function postMcSettings(body: Partial<McSettingsVO>) {
  return unwrap(request.post('/mc/admin/settings', body)) as Promise<McOpResultVO>
}

export function postMcGamerules(rules: Record<string, boolean>) {
  return unwrap(request.post('/mc/admin/gamerules', { rules })) as Promise<McOpResultVO>
}

export function postMcPermission(body: { xuid: string; name?: string; permission: string }) {
  return unwrap(request.post('/mc/admin/permission', body)) as Promise<McOpResultVO>
}

export function postMcAllowlist(body: { xuid: string; name?: string; allowlisted: boolean }) {
  return unwrap(request.post('/mc/admin/allowlist', body)) as Promise<McOpResultVO>
}

export function postMcActivate(folder: string) {
  return unwrap(request.post('/mc/admin/world/activate', { folder })) as Promise<McOpResultVO>
}

export function postMcBackup() {
  return unwrap(request.post('/mc/admin/backup')) as Promise<McOpResultVO>
}

export function postMcUploadWorld(file: File) {
  const body = new FormData()
  body.append('file', file)
  return unwrap(request.post('/mc/admin/world/upload', body, { timeout: 600_000 })) as Promise<McOpResultVO>
}

export function postMcDeleteWorld(folder: string) {
  return unwrap(
    request.post('/mc/admin/world/delete', null, { params: { folder } }),
  ) as Promise<McOpResultVO>
}

export function postMcDeleteBackup(filename: string) {
  return unwrap(
    request.post('/mc/admin/backup/delete', null, { params: { filename } }),
  ) as Promise<McOpResultVO>
}

export function postMcStart() {
  return unwrap(request.post('/mc/admin/server/start')) as Promise<McOpResultVO>
}

export function postMcStop() {
  return unwrap(request.post('/mc/admin/server/stop')) as Promise<McOpResultVO>
}

export async function downloadMcWorld(folder: string) {
  const url = `${APP_API_BASE_URL}/mc/admin/world/download?folder=${encodeURIComponent(folder)}`
  const res = await fetch(url, { credentials: 'include' })
  if (!res.ok) {
    throw new Error('下载失败')
  }
  const blob = await res.blob()
  const name =
    parseFilenameFromContentDisposition(res.headers.get('content-disposition') || undefined) ||
    `${folder}.zip`
  triggerBlobDownload(blob, name)
}
