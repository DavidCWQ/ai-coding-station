import request from '@/request'
import { apiLongId } from '@/utils/id'

export type AgentShareCreated = {
  token?: string
  title?: string
}

export type AgentShareMessage = {
  role?: string
  content?: string
}

export type AgentShareView = {
  token?: string
  title?: string
  agentCode?: string
  agentTitle?: string
  messages?: AgentShareMessage[]
}

export async function createAgentShare(sessionId: string, messageIds: string[]) {
  return request<{ code?: number; data?: AgentShareCreated; message?: string }>('/agent/share/create', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: {
      sessionId: apiLongId(sessionId),
      messageIds: messageIds.map((id) => apiLongId(id)),
    },
  })
}

export async function revokeAgentShare(token: string) {
  return request<{ code?: number; data?: boolean; message?: string }>('/agent/share/revoke', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: { token },
  })
}

export async function viewAgentShare(token: string) {
  return request<{ code?: number; data?: AgentShareView; message?: string }>(`/agent/share/${encodeURIComponent(token)}`, {
    method: 'GET',
    quiet: true,
  })
}
