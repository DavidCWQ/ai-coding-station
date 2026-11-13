import request from '@/request'
import { apiLongId } from '@/utils/id'

export async function voteMessage(body: {
  source: 'app' | 'agent'
  messageId: string
  vote: 1 | -1 | 0
  reason?: string
}) {
  return request<{ code?: number; data?: boolean; message?: string }>('/feedback/vote', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: {
      source: body.source,
      messageId: apiLongId(body.messageId),
      vote: body.vote,
      reason: body.reason,
    },
  })
}
