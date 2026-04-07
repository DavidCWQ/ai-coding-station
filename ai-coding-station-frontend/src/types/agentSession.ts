/** 智能体会话列表行（与后端 AgentChatSessionVO 字段对齐） */
export type AgentSessionRow = {
  id: string
  title: string
  lastMsgTime?: string
  /** 无消息时用于排序（越新越靠上） */
  createTime?: string
  updateTime?: string
}
