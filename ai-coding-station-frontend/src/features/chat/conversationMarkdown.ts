export type MarkdownTurn = {
  role: 'user' | 'assistant' | 'system' | string
  content: string
  reasoning?: string
}

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

export function formatExportedAt(at: Date): string {
  return `${at.getFullYear()}-${pad(at.getMonth() + 1)}-${pad(at.getDate())} ${pad(at.getHours())}:${pad(at.getMinutes())}`
}

export function conversationFilename(name: string, at: Date): string {
  let base = (name || '').trim() || '对话'
  base = base.replace(/[\\/:*?"<>|\r\n]/g, '_')
  if (base.length > 40) base = base.slice(0, 40)
  const stamp = `${at.getFullYear()}${pad(at.getMonth() + 1)}${pad(at.getDate())}-${pad(at.getHours())}${pad(at.getMinutes())}`
  return `${base}-对话-${stamp}.md`
}

function heading(role: string): string {
  if (role === 'user') return '你'
  if (role === 'system') return '系统'
  return '助手'
}

function quoteReasoning(reasoning: string): string {
  const lines = reasoning.split(/\r?\n/)
  return ['> 思考', '>', ...lines.map((line) => `> ${line}`)].join('\n')
}

/** 与后端 ConversationMarkdown 同一版式。 */
export function renderConversationMarkdown(options: {
  title: string
  source: string
  exportedAt: Date
  turns: MarkdownTurn[]
  includeReasoning: boolean
  truncated?: boolean
}): string {
  const title = (options.title || '').trim() || '对话'
  const lines = [
    `# ${title}`,
    '',
    `- 导出于 ${formatExportedAt(options.exportedAt)}`,
    `- 来源：${options.source || ''}`,
  ]
  if (options.truncated) lines.push('- 更早的消息没有写入这份文件')
  lines.push('', '---')
  for (const turn of options.turns) {
    const content = (turn.content || '').trim()
    const reasoning = (turn.reasoning || '').trim()
    const hasThought = options.includeReasoning && !!reasoning
    if (!content && !hasThought) continue
    lines.push('', `### ${heading(turn.role)}`, '')
    if (content) lines.push(content)
    if (hasThought) lines.push('', quoteReasoning(reasoning))
  }
  return `${lines.join('\n')}\n`
}
