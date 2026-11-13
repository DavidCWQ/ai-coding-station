<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { toDataURL } from 'qrcode'

import LineIcon from '@/components/icons/LineIcon.vue'
import { copyText } from '@/utils/copyText'

const props = defineProps<{
  open: boolean
  title: string
  url: string
  blurb: string
  cover?: string
  kind: 'app' | 'agent'
  warning?: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  revoke: []
}>()

const qrUrl = ref('')
const copied = ref<'link' | 'blurb' | ''>('')
const qrPulse = ref(false)
let copyTimer: ReturnType<typeof setTimeout> | undefined

const modalOpen = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
})

const canSystemShare = computed(() => typeof navigator !== 'undefined' && typeof navigator.share === 'function')

const markCopied = (which: 'link' | 'blurb') => {
  copied.value = which
  if (copyTimer) clearTimeout(copyTimer)
  copyTimer = setTimeout(() => {
    copied.value = ''
  }, 1500)
}

watch(
  () => [props.open, props.url] as const,
  async ([open, url]) => {
    if (!open || !url) {
      qrUrl.value = ''
      qrPulse.value = false
      return
    }
    try {
      qrUrl.value = await toDataURL(url, {
        margin: 1,
        width: 196,
        color: { dark: '#1f1f1f', light: '#ffffff' },
      })
    } catch {
      qrUrl.value = ''
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (copyTimer) clearTimeout(copyTimer)
})

const onCopyLink = async () => {
  if (!props.url) return
  const ok = await copyText(props.url)
  if (!ok) {
    message.warning('复制失败，请手动复制')
    return
  }
  markCopied('link')
  message.success('已复制')
}

const onCopyBlurb = async () => {
  const ok = await copyText(props.blurb)
  if (!ok) {
    message.warning('复制失败，请手动复制')
    return
  }
  markCopied('blurb')
  message.success('已复制')
}

const openShare = (target: string) => {
  window.open(target, '_blank', 'noopener,noreferrer')
}

const onWeChat = () => {
  qrPulse.value = true
  message.info('用微信扫一扫')
}

const onQq = () => {
  const target = `https://connect.qq.com/widget/shareqq/index.html?url=${encodeURIComponent(props.url)}&title=${encodeURIComponent(props.title)}&summary=${encodeURIComponent(props.blurb)}`
  openShare(target)
}

const onWeibo = () => {
  const target = `https://service.weibo.com/share/share.php?url=${encodeURIComponent(props.url)}&title=${encodeURIComponent(props.blurb)}`
  openShare(target)
}

const onSystemShare = async () => {
  if (!navigator.share) return
  try {
    await navigator.share({ title: props.title, text: props.blurb, url: props.url })
  } catch (error) {
    if ((error as Error).name === 'AbortError') return
    message.warning('系统分享没有完成')
  }
}

const onRevoke = () => {
  emit('revoke')
}
</script>

<template>
  <a-modal
    v-model:open="modalOpen"
    :footer="null"
    :width="440"
    :centered="true"
    wrap-class-name="share-sheet-modal"
    :title="null"
  >
    <div class="share-sheet">
      <img v-if="cover" class="share-sheet__cover" :src="cover" alt="" />
      <div class="share-sheet__head">
        <LineIcon name="share" />
        <div>
          <div class="share-sheet__kicker">{{ kind === 'app' ? '分享作品' : '分享对话' }}</div>
          <h3 class="share-sheet__title">{{ title }}</h3>
        </div>
      </div>
      <p v-if="warning" class="share-sheet__warning">{{ warning }}</p>

      <div class="share-sheet__link">
        <span class="share-sheet__url">{{ url }}</span>
        <button type="button" class="share-sheet__copy" title="复制链接" @click="onCopyLink">
          <LineIcon :name="copied === 'link' ? 'check' : 'copy'" />
        </button>
      </div>

      <div class="share-sheet__qr" :class="{ 'share-sheet__qr--pulse': qrPulse }">
        <img v-if="qrUrl" :src="qrUrl" alt="分享二维码" />
        <span v-else class="share-sheet__qr-fallback">二维码生成失败</span>
        <p>微信扫一扫</p>
      </div>

      <div class="share-sheet__targets">
        <button type="button" class="share-target share-target--wechat" @click="onWeChat">
          <span><LineIcon name="wechat" :size="24" /></span>
          微信
        </button>
        <button type="button" class="share-target share-target--qq" @click="onQq">
          <span><LineIcon name="qq" :size="24" /></span>
          QQ
        </button>
        <button type="button" class="share-target share-target--weibo" @click="onWeibo">
          <span><LineIcon name="weibo" :size="24" /></span>
          微博
        </button>
        <button v-if="canSystemShare" type="button" class="share-target" @click="onSystemShare">
          <span><LineIcon name="external" :size="20" /></span>
          系统分享
        </button>
      </div>

      <div class="share-sheet__footer">
        <button type="button" class="share-sheet__text" @click="onCopyBlurb">
          <LineIcon :name="copied === 'blurb' ? 'check' : 'copy'" />
          复制分享文案
        </button>
        <button v-if="kind === 'agent'" type="button" class="share-sheet__stop" @click="onRevoke">
          停止分享
        </button>
      </div>
    </div>
  </a-modal>
</template>

<style>
.share-sheet-modal .ant-modal-content {
  border-radius: 16px;
  overflow: hidden;
  padding: 0;
}

.share-sheet-modal .ant-modal-body {
  padding: 0;
}

.share-sheet {
  padding: 22px 22px 18px;
}

.share-sheet__cover {
  display: block;
  width: calc(100% + 44px);
  height: 132px;
  margin: -22px -22px 16px;
  object-fit: cover;
  background: #f3f4f6;
}

.share-sheet__head {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  color: #1677ff;
}

.share-sheet__head svg {
  margin-top: 4px;
}

.share-sheet__kicker {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.share-sheet__title {
  margin: 2px 0 0;
  font-size: 18px;
  line-height: 1.35;
  color: rgba(0, 0, 0, 0.88);
}

.share-sheet__warning {
  margin: 12px 0 0;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff7e6;
  color: #ad6800;
  font-size: 13px;
  line-height: 1.5;
}

.share-sheet__link {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(5, 5, 5, 0.08);
  border-radius: 10px;
  background: #fafafa;
  padding: 8px 8px 8px 12px;
}

.share-sheet__url {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.75);
}

.share-sheet__copy {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 8px;
  background: #fff;
  color: rgba(0, 0, 0, 0.65);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.share-sheet__copy:hover {
  color: #1677ff;
}

.share-sheet__qr {
  margin: 18px auto 0;
  width: 168px;
  text-align: center;
}

.share-sheet__qr img {
  width: 168px;
  height: 168px;
  border-radius: 12px;
  border: 1px solid rgba(5, 5, 5, 0.06);
  background: #fff;
}

.share-sheet__qr--pulse img {
  box-shadow: 0 0 0 4px rgba(7, 193, 96, 0.2);
}

.share-sheet__qr p,
.share-sheet__qr-fallback {
  margin: 8px 0 0;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.share-sheet__targets {
  margin-top: 18px;
  display: flex;
  justify-content: center;
  gap: 18px;
}

.share-target {
  border: 0;
  background: transparent;
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
  cursor: pointer;
}

.share-target span {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: #8c8c8c;
}

.share-target--wechat span {
  background: #07c160;
}

.share-target--qq span {
  background: #12b7f5;
}

.share-target--weibo span {
  background: #e6162d;
}

.share-sheet__footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.share-sheet__text,
.share-sheet__stop {
  border: 0;
  background: transparent;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  cursor: pointer;
  color: rgba(0, 0, 0, 0.65);
  padding: 4px 0;
}

.share-sheet__text:hover {
  color: #1677ff;
}

.share-sheet__stop {
  color: #cf1322;
}

.share-sheet__stop:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
