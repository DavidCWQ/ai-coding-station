export type GroupableMessage = {
  key: string
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}

export type ChatGroup<T extends GroupableMessage> = {
  key: string
  messages: T[]
  selectable: boolean
}

/** 一组是一条用户发言，加上紧跟着的助手回复，直到下一条用户发言。 */
export function groupTurns<T extends GroupableMessage>(messages: T[]): ChatGroup<T>[] {
  const groups: ChatGroup<T>[] = []
  let current: T[] | null = null
  for (const message of messages) {
    if (message.role === 'user' || !current) {
      current = [message]
      groups.push({ key: message.key, messages: current, selectable: false })
    } else {
      current.push(message)
    }
  }
  for (const group of groups) {
    const assistant = [...group.messages].reverse().find((item) => item.role === 'assistant')
    group.selectable = group.messages[0]?.role === 'user'
      && !!assistant
      && !assistant.streaming
      && !!assistant.content.trim()
  }
  return groups
}

export function groupKeyOf(messages: GroupableMessage[], messageKey: string): string | null {
  const group = groupTurns(messages).find((item) => item.messages.some((message) => message.key === messageKey))
  return group?.key ?? null
}
