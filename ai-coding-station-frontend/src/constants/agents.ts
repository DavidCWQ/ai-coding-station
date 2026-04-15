/**
 * 内置智能体列表（与后端 {@link AgentCodeEnum} 编码一致）
 */
export type AgentCardMeta = {
  /** 路由与接口使用的编码 */
  code: string
  /** 展示名称 */
  title: string
  /** 简短说明 */
  description: string
  /** 卡片主题色 */
  color: string
  /** 为 true 时仅管理员在列表与对话页可见 */
  adminOnly?: boolean
}

export const AGENT_CARDS: AgentCardMeta[] = [
  {
    code: 'code_assistant',
    title: '编程助手',
    description: '代码解读、基础知识与工程实践建议',
    color: 'linear-gradient(135deg, #f4f5f6 0%, #e8eaec 100%)',
  },
  {
    code: 'tax_assistant',
    title: '财税助理',
    description: '政策口径与计算思路参考（非执业意见）',
    color: 'linear-gradient(135deg, #eef4ff 0%, #e5edff 100%)',
  },
  {
    code: 'life_advisor',
    title: '「问道」',
    description: '儒雅论道、含蓄启思与人生辨析（非医疗诊断）',
    color: 'linear-gradient(135deg, #eefbf7 0%, #e3f8ef 100%)',
  },
  {
    code: 'inspiration_echo',
    title: '灵感回声',
    description: '联结个人语料与当下问题（仅管理员）',
    color: 'linear-gradient(135deg, #f7f0ff 0%, #ede4ff 100%)',
    adminOnly: true,
  },
]

export function findAgentCard(code: string): AgentCardMeta | undefined {
  const c = String(code || '').trim()
  return AGENT_CARDS.find((a) => a.code === c)
}

/** 智能体列表页：按是否管理员过滤仅管理员可见的卡片 */
export function visibleAgentCards(isAdmin: boolean): AgentCardMeta[] {
  return AGENT_CARDS.filter((a) => !a.adminOnly || isAdmin)
}
