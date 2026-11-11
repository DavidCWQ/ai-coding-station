import { describe, expect, it } from 'vitest'
import { conversationFilename, renderConversationMarkdown } from './conversationMarkdown'

describe('renderConversationMarkdown', () => {
  it('keeps code fences and quotes reasoning', () => {
    const md = renderConversationMarkdown({
      title: '待办清单',
      source: '应用「待办清单」',
      exportedAt: new Date(2026, 10, 11, 15, 4),
      includeReasoning: true,
      turns: [
        { role: 'user', content: '做一个待办' },
        { role: 'assistant', content: '```html\n<h1>待办</h1>\n```', reasoning: '先写标题\n再写列表' },
        { role: 'system', content: '   ' },
      ],
    })
    expect(md.startsWith('# 待办清单\n')).toBe(true)
    expect(md).toContain('- 导出于 2026-11-11 15:04')
    expect(md).toContain('```html\n<h1>待办</h1>\n```')
    expect(md).toContain('> 思考\n>\n> 先写标题\n> 再写列表')
    expect(md).not.toContain('### 系统')
  })

  it('can omit reasoning', () => {
    const md = renderConversationMarkdown({
      title: '对话',
      source: '智能体「编程助手」',
      exportedAt: new Date(2026, 0, 2, 3, 4),
      includeReasoning: false,
      truncated: true,
      turns: [{ role: 'assistant', content: '你好', reasoning: '秘密' }],
    })
    expect(md).toContain('更早的消息没有写入这份文件')
    expect(md).not.toContain('秘密')
  })
})

describe('conversationFilename', () => {
  it('strips unsafe characters', () => {
    expect(conversationFilename('a/b:c', new Date(2026, 10, 11, 9, 8))).toBe('a_b_c-对话-20261111-0908.md')
  })
})
