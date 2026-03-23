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
      <div class="msg__content" @click="onContentClick">
        <div
          v-if="role === 'assistant'"
          class="msg__markdown"
          v-html="aiHtml"
        />
        <pre v-else class="msg__text" v-html="safeUserHtml" />
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
  max-width: min(92%, 640px);
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
  max-height: min(52vh, 560px);
  overflow: auto;
  margin: -10px -14px;
  padding: 10px 14px;
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
  background: #0f172a;
  color: #e2e8f0;
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
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.65);
  color: #e2e8f0;
  padding: 2px 8px;
  font-size: 12px;
  cursor: pointer;
}

.msg__markdown :deep(code:not(pre code)) {
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-size: 12px;
}

.msg__markdown :deep(.hljs-keyword),
.msg__markdown :deep(.hljs-selector-tag) {
  color: #c792ea;
}

.msg__markdown :deep(.hljs-string),
.msg__markdown :deep(.hljs-attribute) {
  color: #c3e88d;
}

.msg__markdown :deep(.hljs-number),
.msg__markdown :deep(.hljs-literal) {
  color: #f78c6c;
}

.msg__markdown :deep(.hljs-title),
.msg__markdown :deep(.hljs-function) {
  color: #82aaff;
}

.msg__markdown :deep(.hljs-comment) {
  color: #7f8c98;
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

.msg--user .msg__content::-webkit-scrollbar-button:single-button,
.msg--ai .msg__content::-webkit-scrollbar-button:single-button {
  display: block;
  height: 12px;
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 6px;
}

.msg--user .msg__content::-webkit-scrollbar-button:single-button:vertical:decrement,
.msg--ai .msg__content::-webkit-scrollbar-button:single-button:vertical:decrement {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8' viewBox='0 0 8 8'%3E%3Cpath d='M1 5.5L4 2.5l3 3' fill='none' stroke='%23666' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: center;
  background-size: 8px 8px;
}

.msg--user .msg__content::-webkit-scrollbar-button:single-button:vertical:increment,
.msg--ai .msg__content::-webkit-scrollbar-button:single-button:vertical:increment {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8' viewBox='0 0 8 8'%3E%3Cpath d='M1 2.5L4 5.5l3-3' fill='none' stroke='%23666' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: center;
  background-size: 8px 8px;
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
