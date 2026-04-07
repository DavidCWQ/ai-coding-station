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
}

export const AGENT_CARDS: AgentCardMeta[] = [
  {
    code: 'code_assistant',
    title: '编程助手',
    description: '代码解读、基础知识与工程实践建议',
    color: 'linear-gradient(135deg, #eef4ff 0%, #e5edff 100%)',
  },
  {
    code: 'tax_assistant',
    title: '财税助手',
    description: '政策口径与计算思路参考（非执业意见）',
    color: 'linear-gradient(135deg, #eefbf7 0%, #e3f8ef 100%)',
  },
  {
    code: 'life_advisor',
    title: '生活导师',
    description: '生活导师：情绪支持与哲学思辨（非医疗诊断）',
    color: 'linear-gradient(135deg, #fff2f8 0%, #ffeaf1 100%)',
  },
]

export function findAgentCard(code: string): AgentCardMeta | undefined {
  const c = String(code || '').trim()
  return AGENT_CARDS.find((a) => a.code === c)
}
