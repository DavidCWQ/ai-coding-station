<script setup lang="ts">
import { computed } from 'vue'

import type { VisualEditorSelectedElement } from '@/features/app-editor/utils/visualEditor'

const props = defineProps<{
  element: VisualEditorSelectedElement
}>()

const emit = defineEmits<{
  close: []
}>()

const MAX_TAG_BOXES = 10

const textDisplay = computed(() => {
  const t = props.element.text?.trim()
  if (!t) return '（无文本内容）'
  return t.length > 120 ? `${t.slice(0, 120)}…` : t
})

const tagChainFull = computed(() => {
  const p = props.element.tagPath
  if (Array.isArray(p) && p.length > 0) return p
  return [props.element.tag]
})

const tagsForDisplay = computed(() => {
  const full = tagChainFull.value
  if (full.length <= MAX_TAG_BOXES) return full
  return full.slice(0, MAX_TAG_BOXES)
})

const tagsTruncated = computed(() => tagChainFull.value.length > MAX_TAG_BOXES)

const tagChainTooltip = computed(() => tagChainFull.value.map((t) => `<${t}>`).join(' › '))
</script>

<template>
  <a-alert
    type="info"
    closable
    show-icon
    class="selected-element-detail"
    @close="emit('close')"
  >
    <template #message>已选中预览中的元素</template>
    <template #description>
      <div class="selected-element-detail__body">
        <div class="selected-element-detail__row selected-element-detail__row--tags">
          <span class="selected-element-detail__label">标签</span>
          <div
            class="selected-element-detail__tags"
            :title="tagsTruncated ? tagChainTooltip : undefined"
          >
            <template v-for="(t, i) in tagsForDisplay" :key="`${i}-${t}`">
              <span v-if="i > 0" class="selected-element-detail__sep" aria-hidden="true">›</span>
              <span class="selected-element-detail__tag">&lt;{{ t }}&gt;</span>
            </template>
            <template v-if="tagsTruncated">
              <span class="selected-element-detail__sep" aria-hidden="true">›</span>
              <span class="selected-element-detail__ellipsis">…</span>
            </template>
          </div>
        </div>
        <div class="selected-element-detail__row">
          <span class="selected-element-detail__label">文本</span>
          <span
            class="selected-element-detail__text"
            :title="element.text?.trim() || undefined"
          >{{ textDisplay }}</span>
        </div>
        <div class="selected-element-detail__row">
          <span class="selected-element-detail__label">路径</span>
          <code
            v-if="element.xpath"
            class="selected-element-detail__path"
            :title="element.xpath"
          >{{ element.xpath }}</code>
          <span v-else class="selected-element-detail__muted">（暂无路径）</span>
        </div>
      </div>
    </template>
  </a-alert>
</template>

<style scoped>
.selected-element-detail {
  margin: 0 0 8px;
  text-align: left;
}

.selected-element-detail :deep(.ant-alert-description) {
  margin-top: 8px;
}

.selected-element-detail__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.selected-element-detail__row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.selected-element-detail__row--tags {
  align-items: flex-start;
}

.selected-element-detail__tags {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 2px;
}

.selected-element-detail__sep {
  flex-shrink: 0;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.35);
  line-height: 22px;
  user-select: none;
  padding: 0 2px;
}

.selected-element-detail__ellipsis {
  flex-shrink: 0;
  font-size: 14px;
  line-height: 22px;
  color: rgba(0, 0, 0, 0.45);
  font-weight: 600;
  letter-spacing: 0.06em;
  user-select: none;
}

.selected-element-detail__label {
  flex-shrink: 0;
  width: 2.5em;
  font-size: 12px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.45);
  letter-spacing: 0.02em;
}

.selected-element-detail__tag {
  display: inline-block;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 22px;
  padding: 0 10px;
  border: 1px solid #91caff;
  background: linear-gradient(180deg, #f0f7ff 0%, #e6f4ff 100%);
  color: #0958d9;
  border-radius: 6px;
  box-shadow: 0 1px 0 rgba(5, 5, 5, 0.04);
}

.selected-element-detail__text {
  flex: 1;
  min-width: 0;
  display: block;
  padding: 4px 10px;
  font-size: 13px;
  line-height: 1.5;
  color: rgba(0, 0, 0, 0.88);
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(5, 5, 5, 0.06);
  border-radius: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-element-detail__path {
  flex: 1;
  min-width: 0;
  display: block;
  margin: 0;
  padding: 5px 10px;
  font-size: 12px;
  line-height: 1.45;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: rgba(0, 0, 0, 0.75);
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(5, 5, 5, 0.06);
  border-radius: 6px;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  scrollbar-width: thin;
}

.selected-element-detail__muted {
  flex: 1;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.35);
}
</style>
