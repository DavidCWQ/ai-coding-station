/** 智能体会话列表行（与后端 AgentChatSessionVO 字段对齐） */
export type AgentSessionRow = {
  id: string
  title: string
  lastMsgTime?: string
  /** 无消息时用于排序（越新越靠上） */
  createTime?: string
  updateTime?: string
}

/** 智能体聊天区消息行（页面 / 历史合并用） */
export type AgentChatRow = {
  key: string
  id?: string
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
  createTime?: string
}
