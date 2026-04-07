import type { VisualEditorSelectedElement } from '@/features/app-editor/utils/visualEditor'

/**
 * 将选中元素上下文拼入发给模型的消息（与需求示例一致）。
 * 聊天列表仍展示用户原始输入，此处仅影响 genCode 请求体。
 */
export function buildAugmentedChatMessage(
  userInput: string,
  selected: VisualEditorSelectedElement,
): string {
  const lines: string[] = ['用户选中了页面中的一个元素：']
  const path =
    selected.tagPath && selected.tagPath.length > 0
      ? selected.tagPath.map((t) => `<${t}>`).join(' > ')
      : `<${selected.tag}>`
  lines.push(`- 标签路径: ${path}`)
  if (selected.id) lines.push(`- id: ${selected.id}`)
  if (selected.classList?.length) {
    lines.push(`- class: ${selected.classList.join(' ')}`)
  }
  if (selected.text) lines.push(`- 内容: ${JSON.stringify(selected.text)}`)
  if (selected.xpath) lines.push(`- xpath: ${selected.xpath}`)
  lines.push('')
  lines.push('请基于该元素进行修改：')
  lines.push(userInput.trim())
  return lines.join('\n')
}
