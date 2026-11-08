<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'

import { codeBlockIsLong } from '@/features/chat/codeBlock'

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

const md = new MarkdownIt({
  html: true,
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

md.renderer.rules.fence = (tokens, idx, options) => {
  const token = tokens[idx]
  const info = (token?.info ?? '').trim()
  const lang = info.split(/\s+/g)[0] || ''
  const content = token?.content ?? ''
  const rendered = options.highlight?.(content, lang, '') || ''
  if (rendered) return `${rendered}\n`
  const input = content.trimEnd()
  return `${renderCodeBlock(input, escapeHtml(input), lang)}\n`
}

const props = defineProps<{
  role: 'user' | 'assistant'
  content: string
  reasoning?: string
  streaming?: boolean
}>()

const rootEl = ref<HTMLElement | null>(null)
const reasonBody = ref<HTMLElement | null>(null)
const reasonOpen = ref(false)
const reasonTouched = ref(false)
const reasonSeconds = ref(0)
let reasonStarted = 0
let reasonTimer: ReturnType<typeof setInterval> | undefined

const safeUserHtml = computed(() => md.utils.escapeHtml(props.content || ''))
const aiHtml = computed(() => md.render(props.content || ''))
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

watch(
  () => props.content,
  async () => {
    if (props.role !== 'assistant' || !props.streaming) return
    await nextTick()
    rootEl.value?.querySelectorAll<HTMLElement>('.msg__code').forEach((pre) => {
      pre.scrollTop = pre.scrollHeight
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
    <div class="msg__body">
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
  </article>
</template>

<style scoped>
.msg {
  display: flex;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  margin-bottom: 18px;
}

.msg--user {
  justify-content: flex-end;
}

.msg--ai {
  justify-content: flex-start;
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
</style>
