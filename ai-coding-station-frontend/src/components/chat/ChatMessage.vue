<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'

import LineIcon from '@/components/icons/LineIcon.vue'
import { confirmRewrite } from '@/features/chat/confirmRewrite'
import { codeBlockIsLong } from '@/features/chat/codeBlock'
import { copyText } from '@/utils/copyText'

hljs.registerLanguage('html', xml)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)

function escapeHtml(input: string): string {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function encodeAttr(input: string): string {
  return escapeHtml(encodeURIComponent(input))
}

function codeLangDisplayName(lang: string): string {
  const t = (lang || '').trim().toLowerCase()
  return t || 'text'
}

/** 工具栏左侧 </> lang + 下方 pre/code。过长时默认限高，由「展开」打开全文。 */
function renderCodeBlock(input: string, codeInnerHtml: string, lang: string): string {
  const name = escapeHtml(codeLangDisplayName(lang))
  const long = codeBlockIsLong(input)
  const wrapClass = long ? 'msg__code-wrap msg__code-wrap--collapsed' : 'msg__code-wrap'
  const fold = long
    ? `<button class="msg__fold-btn" type="button" aria-label="展开" aria-expanded="false">展开</button>`
    : ''
  const edge = long
    ? `<div class="msg__fold-edge"><button class="msg__fold-edge-btn" type="button" aria-label="展开" aria-expanded="false">` +
      `<svg class="msg__chevron msg__chevron--down" viewBox="0 0 16 16" aria-hidden="true"><path d="M3.2 6.2 8 11l4.8-4.8" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>` +
      `<svg class="msg__chevron msg__chevron--up" viewBox="0 0 16 16" aria-hidden="true"><path d="M3.2 9.8 8 5l4.8 4.8" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>` +
      `</button></div>`
    : ''
  return (
    `<div class="${wrapClass}">` +
    `<div class="msg__code-toolbar">` +
    `<span class="msg__code-lang"><span class="msg__code-lang-icon">&lt;/&gt;</span> ${name}</span>` +
    `<span class="msg__code-actions">${fold}` +
    `<button class="msg__copy-btn" data-code="${encodeAttr(input)}" type="button">复制</button>` +
    `</span></div>` +
    `<pre class="msg__code hljs" tabindex="0" role="region" aria-label="代码"><code>${codeInnerHtml}</code></pre>` +
    `${edge}` +
    `</div>`
  )
}

function createMarkdown(allowHtml: boolean) {
  const instance = new MarkdownIt({
    html: allowHtml,
    linkify: true,
    typographer: true,
    highlight(code: string, lang: string): string {
      const input = (code || '').trimEnd()
      const language = (lang || '').toLowerCase()
      if (language && hljs.getLanguage(language)) {
        try {
          const out = hljs.highlight(input, { language }).value
          return renderCodeBlock(input, out, lang)
        } catch {
          // fallback to escaped block
        }
      }
      const escaped = escapeHtml(input)
      return renderCodeBlock(input, escaped, lang)
    },
  })
  instance.renderer.rules.fence = (tokens, idx, options) => {
    const token = tokens[idx]
    const info = (token?.info ?? '').trim()
    const lang = info.split(/\s+/g)[0] || ''
    const content = token?.content ?? ''
    const rendered = options.highlight?.(content, lang, '') || ''
    if (rendered) return `${rendered}\n`
    const input = content.trimEnd()
    return `${renderCodeBlock(input, escapeHtml(input), lang)}\n`
  }
  return instance
}

const md = createMarkdown(true)
const mdSafe = createMarkdown(false)

export type ChatAction = 'copy' | 'retry' | 'like' | 'dislike' | 'download' | 'share' | 'edit'

const props = withDefaults(defineProps<{
  role: 'user' | 'assistant'
  content: string
  reasoning?: string
  streaming?: boolean
  actions?: ChatAction[]
  vote?: 1 | -1 | null
  pending?: boolean
  dropLater?: boolean
  trustHtml?: boolean
}>(), {
  actions: () => [],
  vote: null,
  pending: false,
  dropLater: false,
  trustHtml: true,
})

const emit = defineEmits<{
  retry: []
  edit: [text: string]
  vote: [payload: { vote: 1 | -1 | 0; reason?: string }]
  download: []
  share: []
}>()

const rootEl = ref<HTMLElement | null>(null)
const reasonBody = ref<HTMLElement | null>(null)
const reasonOpen = ref(false)
const reasonTouched = ref(false)
const reasonSeconds = ref(0)
let reasonStarted = 0
let reasonTimer: ReturnType<typeof setInterval> | undefined

const safeUserHtml = computed(() => md.utils.escapeHtml(props.content || ''))
const aiHtml = computed(() => (props.trustHtml ? md : mdSafe).render(props.content || ''))
const hasReasoning = computed(
  () => props.role === 'assistant' && !!(props.reasoning || '').trim(),
)
const waiting = computed(
  () => props.role === 'assistant' && !!props.streaming && !(props.content || '').trim() && !hasReasoning.value,
)
const reasoningLive = computed(
  () => !!props.streaming && !(props.content || '').trim() && hasReasoning.value,
)
const reasonLabel = computed(() => {
  if (reasoningLive.value) return '正在思考'
  if (reasonSeconds.value > 0) return `已思考 ${reasonSeconds.value} 秒`
  return '已思考'
})

const stopReasonTimer = () => {
  if (reasonTimer != null) {
    clearInterval(reasonTimer)
    reasonTimer = undefined
  }
}

const toggleReason = () => {
  reasonTouched.value = true
  reasonOpen.value = !reasonOpen.value
}

watch(
  () => props.reasoning,
  async (text) => {
    if (props.streaming && text && !reasonStarted) {
      reasonStarted = Date.now()
      reasonSeconds.value = 1
      reasonTimer = setInterval(() => {
        reasonSeconds.value = Math.max(1, Math.round((Date.now() - reasonStarted) / 1000))
      }, 500)
      if (!reasonTouched.value) reasonOpen.value = true
    }
    if (!reasonOpen.value || !props.streaming) return
    await nextTick()
    if (reasonBody.value) reasonBody.value.scrollTop = reasonBody.value.scrollHeight
  },
)

watch(
  () => props.content,
  (text) => {
    if (!(text || '').trim() || !hasReasoning.value) return
    stopReasonTimer()
    if (!reasonTouched.value) reasonOpen.value = false
  },
)

watch(
  () => props.streaming,
  (streaming) => {
    if (streaming) return
    stopReasonTimer()
    if (hasReasoning.value && !reasonTouched.value) reasonOpen.value = false
  },
)

onBeforeUnmount(stopReasonTimer)

const editing = ref(false)
const draft = ref('')
const editEl = ref<HTMLTextAreaElement | null>(null)
const copied = ref(false)
const dislikeNoteOpen = ref(false)
const reasonText = ref('')
let copyTimer: ReturnType<typeof setTimeout> | undefined

const showActions = computed(
  () => !editing.value && !props.streaming && !!props.content.trim() && props.actions.length > 0,
)
const has = (name: ChatAction) => props.actions.includes(name)

const actionLabel: Record<ChatAction, string> = {
  copy: '复制',
  retry: '重新生成',
  like: '有帮助',
  dislike: '没帮助',
  download: '下载对话',
  share: '分享',
  edit: '修改',
}

const flashCopied = () => {
  copied.value = true
  if (copyTimer) clearTimeout(copyTimer)
  copyTimer = setTimeout(() => {
    copied.value = false
  }, 1500)
}

const onCopyMessage = async () => {
  const ok = await copyText(props.content || '')
  if (!ok) {
    message.warning('复制失败，请重试')
    return
  }
  flashCopied()
  message.success('已复制')
}

const runGuarded = async (action: () => void) => {
  if (props.pending) return
  if (props.dropLater) {
    const ok = await confirmRewrite()
    if (!ok) return
  }
  action()
}

const onRetry = () => {
  void runGuarded(() => emit('retry'))
}

const startEdit = async () => {
  if (props.pending) return
  draft.value = props.content
  editing.value = true
  await nextTick()
  editEl.value?.focus()
}

const cancelEdit = () => {
  editing.value = false
  draft.value = ''
}

const submitEdit = () => {
  const text = draft.value.trim()
  if (!text || props.pending) return
  void runGuarded(() => {
    editing.value = false
    emit('edit', text)
  })
}

const onEditKey = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    event.preventDefault()
    cancelEdit()
    return
  }
  if (event.key === 'Enter' && (event.metaKey || event.ctrlKey)) {
    event.preventDefault()
    submitEdit()
  }
}

const onLike = () => {
  if (props.pending) return
  dislikeNoteOpen.value = false
  emit('vote', { vote: props.vote === 1 ? 0 : 1 })
}

const closeDislikeNote = () => {
  dislikeNoteOpen.value = false
}

const onDocPointerDown = (event: Event) => {
  const target = event.target
  if (!(target instanceof Node)) return
  if (rootEl.value?.contains(target)) return
  closeDislikeNote()
}

watch(dislikeNoteOpen, (open) => {
  document.removeEventListener('pointerdown', onDocPointerDown, true)
  if (open) document.addEventListener('pointerdown', onDocPointerDown, true)
})

const onDislike = () => {
  if (props.pending) return
  if (props.vote === -1 && !dislikeNoteOpen.value) {
    emit('vote', { vote: 0 })
    return
  }
  if (!dislikeNoteOpen.value) {
    reasonText.value = ''
    dislikeNoteOpen.value = true
    if (props.vote !== -1) emit('vote', { vote: -1 })
    return
  }
  closeDislikeNote()
}

const submitReason = () => {
  const reason = reasonText.value.trim()
  dislikeNoteOpen.value = false
  if (!reason) return
  emit('vote', { vote: -1, reason })
}

onBeforeUnmount(() => {
  if (copyTimer) clearTimeout(copyTimer)
  document.removeEventListener('pointerdown', onDocPointerDown, true)
})

watch(
  () => props.content,
  async () => {
    if (props.role !== 'assistant' || !props.streaming) return
    await nextTick()
    rootEl.value?.querySelectorAll<HTMLElement>('.msg__code').forEach((pre) => {
      const scroller = pre.closest('[data-chat-scroll]') as HTMLElement | null
      const kept = scroller?.scrollTop ?? 0
      pre.scrollTop = pre.scrollHeight
      // 代码块自己跟着新行走，不要把外层对话一起拽到底。
      if (scroller) {
        scroller.dataset.chatScrollHold = '1'
        scroller.scrollTop = kept
        requestAnimationFrame(() => {
          delete scroller.dataset.chatScrollHold
        })
      }
    })
  },
)

const setCodeFold = (wrap: Element, collapsed: boolean) => {
  wrap.classList.toggle('msg__code-wrap--collapsed', collapsed)
  const label = collapsed ? '展开' : '收起'
  const expanded = collapsed ? 'false' : 'true'
  wrap.querySelectorAll<HTMLElement>('.msg__fold-btn, .msg__fold-edge-btn').forEach((el) => {
    if (el.classList.contains('msg__fold-btn')) el.textContent = label
    el.setAttribute('aria-label', label)
    el.setAttribute('aria-expanded', expanded)
  })
}

const onContentClick = async (e: MouseEvent) => {
  const t = e.target as HTMLElement | null
  const fold = t?.closest('.msg__fold-btn, .msg__fold-edge-btn') as HTMLElement | null
  if (fold) {
    const wrap = fold.closest('.msg__code-wrap')
    if (!wrap) return
    setCodeFold(wrap, !wrap.classList.contains('msg__code-wrap--collapsed'))
    return
  }
  const btn = t?.closest('.msg__copy-btn') as HTMLElement | null
  if (!btn) return
  const raw = btn.getAttribute('data-code') || ''
  if (!raw) return
  try {
    await navigator.clipboard.writeText(decodeURIComponent(raw))
    message.success('已复制到剪贴板')
  } catch {
    message.warning('复制失败，请重试')
  }
}

</script>

<template>
  <!-- 时间线上的一块。以后的工具调用是同级的 data-chat-block="tool"，不放进助手正文。 -->
  <article ref="rootEl" class="msg" :class="role === 'user' ? 'msg--user' : 'msg--ai'" :data-chat-block="role">
    <div v-if="editing" class="msg__edit">
      <textarea
        ref="editEl"
        v-model="draft"
        rows="3"
        class="msg__edit-input"
        @keydown="onEditKey"
      />
      <div class="msg__edit-bar">
        <button type="button" class="msg__edit-cancel" @click="cancelEdit">取消</button>
        <button type="button" class="msg__edit-send" :disabled="!draft.trim() || pending" @click="submitEdit">发送</button>
      </div>
    </div>
    <div v-else class="msg__body">
      <div class="msg__content" @click="onContentClick">
        <div v-if="hasReasoning" class="msg__reason">
          <button
            type="button"
            class="msg__reason-toggle"
            :aria-expanded="reasonOpen"
            @click="toggleReason"
          >
            <span v-if="reasoningLive" class="msg__spinner" aria-hidden="true" />
            <span>{{ reasonLabel }}</span>
            <svg
              class="msg__reason-chevron"
              :class="{ 'msg__reason-chevron--open': reasonOpen }"
              viewBox="0 0 16 16"
              aria-hidden="true"
            >
              <path
                d="M6 3.5 10.5 8 6 12.5"
                fill="none"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
          <div v-show="reasonOpen" ref="reasonBody" class="msg__reason-body">{{ reasoning }}</div>
        </div>
        <div v-else-if="waiting" class="msg__thinking" role="status">
          <span class="msg__spinner" aria-hidden="true" />
          <span>正在思考中……</span>
        </div>
        <div
          v-if="role === 'assistant' && content.trim()"
          class="msg__markdown"
          v-html="aiHtml"
        />
        <pre v-else-if="role === 'user'" class="msg__text" v-html="safeUserHtml" />
      </div>
    </div>
    <div v-if="showActions" class="msg__actions">
      <a-tooltip v-if="has('copy')" :title="actionLabel.copy">
        <button type="button" class="msg__action" :aria-label="actionLabel.copy" @click="onCopyMessage">
          <LineIcon :name="copied ? 'check' : 'copy'" />
        </button>
      </a-tooltip>
      <a-tooltip v-if="has('edit')" :title="actionLabel.edit">
        <button type="button" class="msg__action" :disabled="pending" :aria-label="actionLabel.edit" @click="startEdit">
          <LineIcon name="edit" />
        </button>
      </a-tooltip>
      <a-tooltip v-if="has('retry')" :title="actionLabel.retry">
        <button type="button" class="msg__action" :disabled="pending" :aria-label="actionLabel.retry" @click="onRetry">
          <LineIcon name="retry" />
        </button>
      </a-tooltip>
      <a-tooltip v-if="has('like')" :title="actionLabel.like">
        <button
          type="button"
          class="msg__action"
          :class="{ 'msg__action--on': vote === 1 }"
          :disabled="pending"
          :aria-pressed="vote === 1"
          :aria-label="actionLabel.like"
          @click="onLike"
        >
          <LineIcon name="thumbUp" :filled="vote === 1" />
        </button>
      </a-tooltip>
      <span v-if="has('dislike')" class="msg__note-anchor">
        <a-tooltip :title="actionLabel.dislike">
          <button
            type="button"
            class="msg__action"
            :class="{ 'msg__action--down': vote === -1 }"
            :disabled="pending"
            :aria-pressed="vote === -1"
            :aria-label="actionLabel.dislike"
            @click.stop="onDislike"
          >
            <LineIcon name="thumbDown" :filled="vote === -1" />
          </button>
        </a-tooltip>
        <div v-if="dislikeNoteOpen" class="msg__note" @click.stop>
          <div class="msg__note-title">哪里不好？可以跳过</div>
          <textarea v-model="reasonText" maxlength="200" rows="3" class="msg__note-input" placeholder="选填，最多 200 字" />
          <div class="msg__note-bar">
            <button type="button" @click="closeDislikeNote">跳过</button>
            <button type="button" class="msg__note-send" @click="submitReason">提交</button>
          </div>
        </div>
      </span>
      <a-tooltip v-if="has('download')" :title="actionLabel.download">
        <button type="button" class="msg__action" :disabled="pending" :aria-label="actionLabel.download" @click="emit('download')">
          <LineIcon name="download" />
        </button>
      </a-tooltip>
      <a-tooltip v-if="has('share')" :title="actionLabel.share">
        <button type="button" class="msg__action" :disabled="pending" :aria-label="actionLabel.share" @click="emit('share')">
          <LineIcon name="share" />
        </button>
      </a-tooltip>
    </div>
  </article>
</template>

<style scoped>
.msg {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  margin-bottom: 18px;
}

.msg--user {
  align-items: flex-end;
}

.msg--ai {
  align-items: flex-start;
}

.msg:has(.msg__note) {
  position: relative;
  z-index: 3;
}

.msg__body {
  min-width: 0;
  max-width: 100%;
  line-height: 1.7;
  font-size: 15px;
  position: relative;
}

.msg--user .msg__body {
  max-width: min(75%, 420px);
  padding: 10px 14px;
  border-radius: 16px;
  border-bottom-right-radius: 4px;
  background: #1677ff;
  color: #fff;
  font-size: 14px;
  line-height: 1.55;
}

.msg--ai .msg__body {
  width: 100%;
  padding: 2px 0 4px;
  background: transparent;
  color: rgba(0, 0, 0, 0.88);
}

.msg__content {
  min-width: 0;
}

.msg__text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}

.msg__markdown {
  line-height: 1.75;
  word-break: break-word;
}

.msg__markdown :deep(p) {
  margin: 0 0 12px;
}

.msg__markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.msg__markdown :deep(ul),
.msg__markdown :deep(ol) {
  margin: 0 0 12px 1.2em;
  padding: 0;
}

.msg__markdown :deep(li + li) {
  margin-top: 4px;
}

.msg__markdown :deep(a) {
  color: #1677ff;
  text-decoration: underline;
}

.msg__markdown :deep(.msg__code-wrap) {
  position: relative;
  margin: 0 0 12px;
  max-width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  background: #fafafa;
}

.msg__markdown :deep(.msg__code-toolbar) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 6px 10px;
  background: #f6f7f9;
  border-bottom: 1px solid #e5e7eb;
}

.msg__markdown :deep(.msg__code-lang) {
  font-size: 12px;
  line-height: 1.4;
  color: rgba(0, 0, 0, 0.55);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
    monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
  flex: 1;
}

.msg__markdown :deep(.msg__code-lang-icon) {
  opacity: 0.85;
}

.msg__markdown :deep(.msg__code-actions) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.msg__markdown :deep(.msg__code) {
  margin: 0;
  padding: 10px 12px;
  box-sizing: border-box;
  border-radius: 0;
  background: #fafafa;
  color: #24292f;
  border: none;
  overflow: auto;
  max-width: 100%;
  white-space: pre;
  word-break: normal;
  overflow-wrap: normal;
  font-size: 13px;
  line-height: 1.5;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.28) transparent;
}

.msg__markdown :deep(.msg__code code) {
  white-space: pre;
  word-break: normal;
  overflow-wrap: normal;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
    monospace;
}

.msg__markdown :deep(.msg__code-wrap--collapsed .msg__code) {
  max-height: calc(1.5em * 24 + 20px);
}

.msg__markdown :deep(.msg__code::-webkit-scrollbar) {
  width: 8px;
  height: 8px;
}

.msg__markdown :deep(.msg__code::-webkit-scrollbar-thumb) {
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.28);
}

.msg__markdown :deep(.msg__fold-edge) {
  position: absolute;
  left: 50%;
  bottom: 0;
  z-index: 2;
  width: 120px;
  height: 32px;
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  align-items: flex-end;
}

.msg__markdown :deep(.msg__fold-edge-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 22px;
  margin-bottom: 6px;
  padding: 0;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 999px;
  background: #fff;
  color: rgba(0, 0, 0, 0.65);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.msg__markdown :deep(.msg__fold-edge:hover .msg__fold-edge-btn),
.msg__markdown :deep(.msg__fold-edge-btn:focus-visible) {
  opacity: 1;
}

.msg__markdown :deep(.msg__chevron) {
  width: 14px;
  height: 14px;
  display: block;
}

.msg__markdown :deep(.msg__chevron--up) {
  display: none;
}

.msg__markdown :deep(.msg__code-wrap:not(.msg__code-wrap--collapsed) .msg__chevron--down) {
  display: none;
}

.msg__markdown :deep(.msg__code-wrap:not(.msg__code-wrap--collapsed) .msg__chevron--up) {
  display: block;
}

.msg__markdown :deep(.msg__copy-btn),
.msg__markdown :deep(.msg__fold-btn) {
  flex-shrink: 0;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 6px;
  background: #ffffff;
  color: rgba(0, 0, 0, 0.65);
  padding: 2px 8px;
  font-size: 12px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.msg__markdown :deep(code:not(pre code)) {
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-size: 0.92em;
}

.msg__markdown :deep(.hljs-keyword),
.msg__markdown :deep(.hljs-selector-tag) {
  color: #cf222e;
}

.msg__markdown :deep(.hljs-string),
.msg__markdown :deep(.hljs-attribute) {
  color: #0a3069;
}

.msg__markdown :deep(.hljs-number),
.msg__markdown :deep(.hljs-literal) {
  color: #0550ae;
}

.msg__markdown :deep(.hljs-title),
.msg__markdown :deep(.hljs-function) {
  color: #8250df;
}

.msg__markdown :deep(.hljs-comment) {
  color: #6e7781;
  font-style: italic;
}

.msg__reason {
  margin: 0 0 12px;
}

.msg__reason-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
  line-height: 1.5;
  cursor: pointer;
}

.msg__reason-toggle:hover {
  color: rgba(0, 0, 0, 0.72);
}

.msg__reason-chevron {
  width: 14px;
  height: 14px;
  transition: transform 0.15s ease;
}

.msg__reason-chevron--open {
  transform: rotate(90deg);
}

.msg__reason-body {
  margin-top: 6px;
  padding: 2px 0 2px 12px;
  border-left: 2px solid rgba(0, 0, 0, 0.08);
  max-height: 168px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
  line-height: 1.65;
}

.msg__thinking {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
  line-height: 1.5;
}

.msg__spinner {
  width: 14px;
  height: 14px;
  box-sizing: border-box;
  border: 1.5px solid rgba(0, 0, 0, 0.12);
  border-top-color: rgba(0, 0, 0, 0.55);
  border-radius: 50%;
  animation: msg-spin 0.7s linear infinite;
  flex-shrink: 0;
}

@keyframes msg-spin {
  to {
    transform: rotate(360deg);
  }
}

.msg__actions {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 6px;
}

.msg__note-anchor {
  position: relative;
  display: inline-flex;
}

.msg__action {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: rgba(0, 0, 0, 0.45);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.msg__action:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.05);
  color: rgba(0, 0, 0, 0.88);
}

.msg__action:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.msg__action--on {
  color: #1677ff;
}

.msg__action--down {
  color: #595959;
}

.msg__edit {
  width: min(100%, 420px);
  border: 1px solid rgba(22, 119, 255, 0.35);
  border-radius: 14px;
  background: #fff;
  padding: 8px;
}

.msg__edit-input {
  width: 100%;
  border: 0;
  resize: vertical;
  outline: none;
  font: inherit;
  line-height: 1.55;
  color: rgba(0, 0, 0, 0.88);
}

.msg__edit-bar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.msg__edit-cancel,
.msg__edit-send,
.msg__note button {
  border: 0;
  border-radius: 8px;
  height: 28px;
  padding: 0 10px;
  cursor: pointer;
  font-size: 13px;
}

.msg__edit-cancel {
  background: transparent;
  color: rgba(0, 0, 0, 0.65);
}

.msg__edit-send,
.msg__note-send {
  background: #1677ff;
  color: #fff;
}

.msg__edit-send:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.msg__note {
  position: absolute;
  z-index: 20;
  bottom: calc(100% + 8px);
  left: 0;
  width: 240px;
  padding: 10px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.msg__note-title {
  margin-bottom: 8px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.msg__note-input {
  width: 100%;
  border: 1px solid rgba(5, 5, 5, 0.12);
  border-radius: 8px;
  padding: 6px 8px;
  resize: vertical;
  font: inherit;
}

.msg__note-bar {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
