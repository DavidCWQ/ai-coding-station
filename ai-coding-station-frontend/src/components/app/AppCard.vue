<script setup lang="ts">
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

import { appIdFromData } from '@/utils/appId'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const props = defineProps<{
  app: API.AppVO
  mode: 'mine' | 'featured'
}>()

const emit = defineEmits<{
  click: [appId: string]
}>()

const appId = () => appIdFromData(props.app.id)

const coverSrc = () => props.app.cover || ''

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
</script>

<template>
  <a-card class="app-card" hoverable @click="onCardClick">
    <div class="app-card__thumb">
      <img v-if="coverSrc()" class="app-card__img" :src="coverSrc()" alt="" />
      <div v-else class="app-card__placeholder">预览</div>
    </div>
    <div class="app-card__body">
      <div class="app-card__title">{{ app.appName || '未命名应用' }}</div>
      <div v-if="mode === 'mine'" class="app-card__meta">创建于 {{ relativeCreate() }}</div>
      <div v-else class="app-card__footer">
        <div class="app-card__author">
          <a-avatar v-if="app.user?.userAvatar" :size="22" :src="app.user.userAvatar" />
          <a-avatar v-else :size="22">{{ (app.user?.userName || app.user?.userAccount || '?').slice(0, 1) }}</a-avatar>
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
  aspect-ratio: 16 / 9;
  background: linear-gradient(135deg, #f0f5ff 0%, #e6fffb 100%);
  overflow: hidden;
}

.app-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.app-card__placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(0, 0, 0, 0.35);
  font-size: 14px;
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
</style>
