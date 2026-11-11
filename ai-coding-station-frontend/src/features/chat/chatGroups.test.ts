import { describe, expect, it } from 'vitest'
import { groupKeyOf, groupTurns } from './chatGroups'

const row = (key: string, role: 'user' | 'assistant', content = '正文', streaming = false) => ({
  key,
  role,
  content,
  streaming,
})

describe('groupTurns', () => {
  it('pairs a user turn with the following assistant reply', () => {
    const groups = groupTurns([
      row('u1', 'user', '问一'),
      row('a1', 'assistant', '答一'),
      row('u2', 'user', '问二'),
      row('a2', 'assistant', '', true),
    ])
    expect(groups.map((group) => group.key)).toEqual(['u1', 'u2'])
    expect(groups[0]?.selectable).toBe(true)
    expect(groups[0]?.messages.map((item) => item.key)).toEqual(['u1', 'a1'])
    expect(groups[1]?.selectable).toBe(false)
  })

  it('finds the group that contains a message', () => {
    const messages = [row('u1', 'user'), row('a1', 'assistant'), row('u2', 'user'), row('a2', 'assistant')]
    expect(groupKeyOf(messages, 'a2')).toBe('u2')
  })
})
