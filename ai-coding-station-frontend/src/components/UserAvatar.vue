<script setup lang="ts">
import { computed, ref, watch } from 'vue'

export type UserAvatarSize = number | 'large' | 'small' | 'default'

const props = defineProps<{
  src?: string | null
  /** 无头像 URL 时，首字母优先取自 name */
  name?: string | null
  account?: string | null
  size?: UserAvatarSize
  /** 仅作用于字母头像时的样式类（如微调字形位置） */
  letterClass?: string
}>()

const resolvedSrc = computed(() => {
  const s = props.src
  if (s == null) return undefined
  const t = typeof s === 'string' ? s.trim() : String(s).trim()
  return t.length > 0 ? t : undefined
})

const initial = computed(() =>
  (props.name || props.account || '?').slice(0, 1).toUpperCase(),
)

/** 外链图床常校验 Referer；不带 Referer 往往可正常显示（如 zcool） */
const imgLoadFailed = ref(false)

watch(
  resolvedSrc,
  () => {
    imgLoadFailed.value = false
  },
  { immediate: true },
)

const onImgError = () => {
  imgLoadFailed.value = true
}

const showImage = computed(() => !!resolvedSrc.value && !imgLoadFailed.value)

/** 与 a-avatar 默认 / lg / sm 及数字 size 对齐；图片分支必须写死 px，否则在 flex 里会按原图尺寸撑开 */
const imagePxSize = computed(() => {
  const s = props.size
  if (typeof s === 'number') return s
  if (s === 'large') return 40
  if (s === 'small') return 24
  return 32
})

const imageWrapStyle = computed(() => ({
  width: `${imagePxSize.value}px`,
  height: `${imagePxSize.value}px`,
  lineHeight: `${imagePxSize.value}px`,
  flexShrink: 0,
  overflow: 'hidden',
  borderRadius: '50%',
}))

const imageWrapClass = ['ant-avatar', 'ant-avatar-circle', 'user-avatar', 'user-avatar--has-image']

const letterRootClass = computed(() => {
  const c: (string | Record<string, boolean>)[] = ['user-avatar']
  if (props.letterClass) c.push(props.letterClass)
  return c
})
</script>

<template>
  <span v-if="showImage" :class="imageWrapClass" :style="imageWrapStyle">
    <img
      :src="resolvedSrc"
      referrerpolicy="no-referrer"
      alt=""
      @error="onImgError"
    />
  </span>
  <a-avatar v-else :size="size" :class="letterRootClass">
    {{ initial }}
  </a-avatar>
</template>

<style scoped>
/* 图片头像：正圆形容器 + 居中 cover，避免方图撑成方框 */
.user-avatar.user-avatar--has-image {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
}

.user-avatar--has-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}
</style>
