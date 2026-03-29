<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

import UserAvatar from '@/components/UserAvatar.vue'
import { idFromData } from '@/utils/id'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const props = defineProps<{
  app: API.AppVO
  mode: 'mine' | 'featured'
}>()

const emit = defineEmits<{
  click: [appId: string]
  viewChat: [appId: string]
  viewWork: [deployKey: string]
}>()

// 稳定 hash（保证同一个 app 永远同一个渐变）
const hash = (input: string | number) => {
  const str = String(input || '')
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = (h << 5) - h + str.charCodeAt(i)
    h |= 0
  }
  return Math.abs(h)
}

// 渐变池（可自由扩展）
const gradients = [
  'linear-gradient(135deg, #e3f2fd, #bbdefb)',
  'linear-gradient(135deg, #e0f7fa, #b2ebf2)',
  'linear-gradient(135deg, #e8f5e9, #c8e6c9)',
  'linear-gradient(135deg, #e6fffa, #ccfbf1)',
  'linear-gradient(135deg, #fffde7, #fff9c4)',
  'linear-gradient(135deg, #fff3e0, #ffe0b2)',
  'linear-gradient(135deg, #fce4ec, #f8bbd0)',
  'linear-gradient(135deg, #f3e5f5, #e1bee7)',
]

const appId = () => idFromData(props.app.id)

/** 仅 null / undefined / 纯空白 视为无封面；有效图片 URL 用背景图，不叠「预览」字 */
const coverUrl = computed(() => {
  const c = props.app.cover
  if (c == null) return ''
  const s = typeof c === 'string' ? c : String(c)
  return s.trim()
})

const hasCoverImage = computed(() => coverUrl.value.length > 0)

const coverStyle = computed(() => {
  if (hasCoverImage.value) {
    return {
      backgroundImage: `url(${JSON.stringify(coverUrl.value)})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
    }
  }
  const index = hash(props.app.id ?? 'default') % gradients.length
  return {
    background: gradients[index],
  }
})

const relativeCreate = () => {
  const t = props.app.createTime
  if (!t) return ''
  const d = dayjs(t)
  return d.isValid() ? d.fromNow() : t
}

const tagLabel = () => {
  const t = props.app.codeGenType
  if (!t) return '应用'
  if (t.toLowerCase().includes('multi')) return '多文件'
  if (t.toLowerCase().includes('html')) return '网站'
  return t
}

const onCardClick = () => {
  const id = appId()
  if (id) emit('click', id)
}

const onViewChat = () => {
  const id = appId()
  if (id) emit('viewChat', id)
}

const onViewWork = () => {
  const key = props.app.deployKey?.trim()
  if (key) emit('viewWork', key)
}
</script>

<template>
  <a-card class="app-card" hoverable @click="onCardClick">
    <div class="app-card__thumb" :style="coverStyle">
      <div v-if="!hasCoverImage" class="app-card__placeholder">预览</div>
      <div class="app-card__actions" @click.stop>
        <a-space>
          <a-button size="small" @click="onViewChat">查看对话</a-button>
          <a-button
            v-if="app.deployKey"
            size="small"
            type="primary"
            class="app-card__work-btn"
            @click="onViewWork"
          >
            查看作品
          </a-button>
        </a-space>
      </div>
    </div>
    <div class="app-card__body">
      <div class="app-card__title">{{ app.appName || '未命名应用' }}</div>
      <div v-if="mode === 'mine'" class="app-card__meta">创建于 {{ relativeCreate() }}</div>
      <div v-else class="app-card__footer">
        <div class="app-card__author">
          <UserAvatar
            :size="24"
            letter-class="avatar-letter"
            :src="app.user?.userAvatar"
            :name="app.user?.userName"
            :account="app.user?.userAccount"
          />
          <span class="app-card__name">{{ app.user?.userName || app.user?.userAccount || '用户' }}</span>
        </div>
        <a-tag class="app-card__tag" color="processing">{{ tagLabel() }}</a-tag>
      </div>
    </div>
  </a-card>
</template>

<style scoped>
.app-card {
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  height: 100%;
}

.app-card :deep(.ant-card-body) {
  padding: 0;
}

.app-card__thumb {
  position: relative;
  aspect-ratio: 16 / 9;
  background: linear-gradient(135deg, #f0f5ff 0%, #e6fffb 100%);
  overflow: hidden;
}

.app-card__placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(0, 0, 0, 0.35);
  font-size: 14px;
}

.app-card__actions {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(6, 14, 30, 0.45);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.app-card:hover .app-card__actions {
  opacity: 1;
}

.app-card__body {
  padding: 12px 14px 14px;
}

.app-card__title {
  font-weight: 600;
  font-size: 15px;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.app-card__meta {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.app-card__footer {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.app-card__author {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  text-transform: capitalize;
}

.avatar-letter :deep(.ant-avatar-string) {
  position: relative;
  top: -2px;  /* 向上微调 */
  left: 5px;  /* 向右微调 */
  font-size: 16px;
  display: inline-block;
  transform: translateY(0);
}

.app-card__name {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__tag {
  flex-shrink: 0;
  margin: 0;
}

.app-card__work-btn {
  border-color: #1677ff !important;
  background: #1677ff !important;
  color: #fff !important;
}
</style>
