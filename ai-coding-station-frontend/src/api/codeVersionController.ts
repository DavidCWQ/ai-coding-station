import request from '@/request'

export type CodeVersionBody = {
  appId: number
  sessionId: number
  messageId?: number
}

type VersionListBody = {
  code?: number
  data?: Array<string | number>
  message?: string
}

/** 列出会话里可预览的版本，缺的快照会在服务端补上。 */
export async function listCodeVersions(body: { appId: number; sessionId: number }) {
  return request<VersionListBody>('/app/code/version/list', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

/** 把刚写完的工作目录固定成这一条消息的版本。 */
export async function pinCodeVersion(body: CodeVersionBody) {
  return request<{ code?: number; data?: boolean; message?: string }>('/app/code/version/pin', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

/** 从当前显示的版本继续改，并丢掉它之后的对话。 */
export async function continueCodeVersion(body: CodeVersionBody) {
  return request<{ code?: number; data?: boolean; message?: string }>('/app/code/version/continue', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}
