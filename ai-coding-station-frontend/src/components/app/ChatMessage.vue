<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'

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
        return `<div class="msg__code-wrap"><button class="msg__copy-btn" data-code="${encodeAttr(input)}" type="button">复制</button><pre class="msg__code hljs"><code>${out}</code></pre></div>`
      } catch {
        // fallback to escaped block
      }
    }
    const escaped = escapeHtml(input)
    return `<div class="msg__code-wrap"><button class="msg__copy-btn" data-code="${encodeAttr(input)}" type="button">复制</button><pre class="msg__code hljs"><code>${escaped}</code></pre></div>`
  },
})

const props = defineProps<{
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}>()

const safeUserHtml = computed(() => md.utils.escapeHtml(props.content || ''))
const aiHtml = computed(() => md.render(props.content || ''))

const onContentClick = async (e: MouseEvent) => {
  const t = e.target as HTMLElement | null
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
  <div class="msg" :class="role === 'user' ? 'msg--user' : 'msg--ai'">
    <div class="msg__bubble">
      <div class="msg__content-wrap">
        <div class="msg__content" @click="onContentClick">
          <div
            v-if="role === 'assistant'"
            class="msg__markdown"
            v-html="aiHtml"
          />
          <pre v-else class="msg__text" v-html="safeUserHtml" />
        </div>
      </div>
      <span v-if="streaming" class="msg__cursor" aria-hidden="true">▍</span>
    </div>
  </div>
</template>

<style scoped>
.msg {
  display: flex;
  margin-bottom: 12px;
}

.msg--user {
  justify-content: flex-end;
}

.msg--ai {
  justify-content: flex-start;
}

.msg__bubble {
  max-width: min(78%, 520px);
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.55;
  font-size: 14px;
  position: relative;
}

.msg--user .msg__bubble {
  background: #1677ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg--ai .msg__bubble {
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.88);
  border-bottom-left-radius: 4px;
}

.msg__content {
  max-height: min(86vh, 1200px);
  overflow: auto;
  margin: -4px -14px;
  padding: 4px 14px;
}

.msg__content-wrap {
  position: relative;
  padding: 4px 0;
}

.msg__text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}

.msg__markdown {
  line-height: 1.6;
  word-break: break-word;
}

.msg__markdown :deep(p) {
  margin: 0 0 10px;
}

.msg__markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.msg__markdown :deep(ul),
.msg__markdown :deep(ol) {
  margin: 0 0 10px 18px;
  padding: 0;
}

.msg__markdown :deep(li + li) {
  margin-top: 4px;
}

.msg__markdown :deep(a) {
  color: #1677ff;
  text-decoration: underline;
}

.msg__markdown :deep(.msg__code) {
  margin: 0 0 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #ffffff;
  color: #24292f;
  border: 1px solid #e5e7eb;
  overflow: auto;
  font-size: 13px;
  line-height: 1.5;
}

.msg__markdown :deep(.msg__code-wrap) {
  position: relative;
  margin-bottom: 10px;
}

.msg__markdown :deep(.msg__code-wrap .msg__code) {
  margin-bottom: 0;
}

.msg__markdown :deep(.msg__copy-btn) {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.92);
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
  font-size: 12px;
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

.msg__cursor {
  display: inline-block;
  animation: blink 1s step-end infinite;
  margin-left: 2px;
  color: rgba(0, 0, 0, 0.45);
}

.msg--user .msg__cursor {
  color: rgba(255, 255, 255, 0.8);
}

.msg--user .msg__content::-webkit-scrollbar,
.msg--ai .msg__content::-webkit-scrollbar {
  width: 8px;
}

.msg--user .msg__content::-webkit-scrollbar-thumb,
.msg--ai .msg__content::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.25);
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>
