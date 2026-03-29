import { createVisualEditorBootstrapScript } from '@/utils/visualEditorBootstrapSource'
import {
  VISUAL_EDITOR_DOM_FLAG,
  VISUAL_EDITOR_MSG_COMMAND,
  VISUAL_EDITOR_MSG_ELEMENT_SELECTED,
} from '@/utils/visualEditorConstants'

/** JSON 可序列化的 DOMRect */
export interface VisualEditorDOMRectJson {
  x: number
  y: number
  width: number
  height: number
  top: number
  left: number
  right: number
  bottom: number
}

/** 选中元素的纯数据描述（可 postMessage / JSON） */
export interface VisualEditorSelectedElement {
  tag: string
  /** 从 document 根到当前节点的标签链（小写），用于展示嵌套结构 */
  tagPath?: string[]
  id?: string
  classList?: string[]
  text?: string
  xpath?: string
  bounding?: VisualEditorDOMRectJson
}

export interface VisualEditorCommandMessage {
  type: typeof VISUAL_EDITOR_MSG_COMMAND
  active: boolean
}

export function getUrlOrigin(url: string): string | null {
  try {
    return new URL(url, typeof window !== 'undefined' ? window.location.href : undefined).origin
  } catch {
    return null
  }
}

/** 同源预览时父页面可访问 iframe document，用于注入脚本 */
export function canAccessIframeDocument(iframe: HTMLIFrameElement): boolean {
  try {
    return !!iframe.contentDocument
  } catch {
    return false
  }
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null
}

/** 校验 iframe → 父页面的 element-selected 消息 */
export function parseElementSelectedFromMessage(data: unknown): VisualEditorSelectedElement | null {
  if (!isRecord(data)) return null
  if (data.type !== VISUAL_EDITOR_MSG_ELEMENT_SELECTED) return null
  const payload = data.payload
  if (!isRecord(payload)) return null
  const tag = payload.tag
  if (typeof tag !== 'string' || !tag.trim()) return null
  const out: VisualEditorSelectedElement = { tag: tag.trim().toLowerCase() }
  if (typeof payload.id === 'string' && payload.id) out.id = payload.id
  if (Array.isArray(payload.classList)) {
    const cls = payload.classList.filter((x): x is string => typeof x === 'string' && x.length > 0)
    if (cls.length) out.classList = cls
  }
  if (typeof payload.text === 'string' && payload.text) out.text = payload.text
  if (typeof payload.xpath === 'string' && payload.xpath) out.xpath = payload.xpath
  if (Array.isArray(payload.tagPath)) {
    const tp = payload.tagPath
      .filter((x): x is string => typeof x === 'string' && x.trim().length > 0)
      .map((x) => x.trim().toLowerCase())
    if (tp.length) out.tagPath = tp
  }
  const b = payload.bounding
  if (isRecord(b)) {
    const finite = (k: string): number | null => {
      const n = b[k]
      return typeof n === 'number' && Number.isFinite(n) ? n : null
    }
    const x = finite('x')
    const y = finite('y')
    const width = finite('width')
    const height = finite('height')
    const top = finite('top')
    const left = finite('left')
    const right = finite('right')
    const bottom = finite('bottom')
    if (
      x != null &&
      y != null &&
      width != null &&
      height != null &&
      top != null &&
      left != null &&
      right != null &&
      bottom != null
    ) {
      out.bounding = { x, y, width, height, top, left, right, bottom }
    }
  }
  return out
}

/** 事件是否来自指定 iframe 且来源可信 */
export function isTrustedVisualEditorMessage(
  ev: MessageEvent,
  iframe: HTMLIFrameElement | null,
  allowedOrigin: string | null,
): boolean {
  if (!iframe || !allowedOrigin) return false
  if (ev.source !== iframe.contentWindow) return false
  if (typeof ev.origin !== 'string' || ev.origin !== allowedOrigin) return false
  return true
}

function commandPayload(active: boolean): VisualEditorCommandMessage {
  return { type: VISUAL_EDITOR_MSG_COMMAND, active }
}

/** 若尚未注入则在 iframe 内执行引导脚本 */
export function ensureVisualEditorBootstrapped(
  iframe: HTMLIFrameElement,
  parentOrigin: string,
): boolean {
  try {
    const doc = iframe.contentDocument
    if (!doc?.documentElement) return false
    if (doc.documentElement.getAttribute(VISUAL_EDITOR_DOM_FLAG) === '1') return true
    const code = createVisualEditorBootstrapScript(parentOrigin)
    const el = doc.createElement('script')
    el.textContent = code
    doc.documentElement.appendChild(el)
    el.remove()
    return doc.documentElement.getAttribute(VISUAL_EDITOR_DOM_FLAG) === '1'
  } catch {
    return false
  }
}

/** 通知 iframe 开启/关闭可视化选区（须已 bootstrap） */
export function postVisualEditorActive(
  iframe: HTMLIFrameElement,
  active: boolean,
  targetOrigin: string,
): void {
  try {
    iframe.contentWindow?.postMessage(commandPayload(active), targetOrigin)
  } catch {
    /* ignore */
  }
}
