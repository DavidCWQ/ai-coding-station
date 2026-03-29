/**
 * 仅含与 iframe 注入脚本共享的字面量，避免 visualEditor.ts ↔ bootstrapSource 循环依赖。
 */

export const VISUAL_EDITOR_MSG_ELEMENT_SELECTED = 'element-selected' as const

export const VISUAL_EDITOR_MSG_COMMAND = 'visual-editor-command' as const

export const VISUAL_EDITOR_DOM_FLAG = 'data-acs-visual-editor-boot' as const
