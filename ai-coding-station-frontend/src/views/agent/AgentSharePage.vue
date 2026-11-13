<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import ChatMessage from '@/components/chat/ChatMessage.vue'
import { viewAgentShare, type AgentShareView } from '@/api/shareController'
import { getErrorMessage } from '@/utils/error'

const route = useRoute()
const loading = ref(true)
const invalid = ref(false)
const view = ref<AgentShareView | null>(null)

const token = computed(() => String(route.params.token || ''))
const tryPath = computed(() => (view.value?.agentCode ? `/agents/${view.value.agentCode}/chat` : '/agents'))

onMounted(async () => {
  try {
    const res = await viewAgentShare(token.value)
    view.value = res.data?.data ?? null
    invalid.value = !view.value?.messages?.length
  } catch (error) {
    invalid.value = true
    const text = getErrorMessage(error, '')
    if (text && text !== '分享已失效') {
      invalid.value = true
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="share-page">
    <a-spin :spinning="loading">
      <div v-if="invalid && !loading" class="share-page__gone">
        <h1>分享已失效</h1>
        <p>这条链接已经停止，或者内容不存在。</p>
        <router-link to="/agents">去看看智能体</router-link>
      </div>
      <article v-else-if="view" class="share-page__sheet">
        <header>
          <p>{{ view.agentTitle }}</p>
          <h1>{{ view.title || '对话分享' }}</h1>
        </header>
        <template v-for="(item, index) in view.messages" :key="index">
          <p v-if="item.role === 'system'" class="share-page__system">{{ item.content }}</p>
          <ChatMessage
            v-else
            :role="item.role === 'user' ? 'user' : 'assistant'"
            :content="item.content || ''"
            :trust-html="false"
          />
        </template>
        <router-link class="share-page__try" :to="tryPath">我也试试这个智能体</router-link>
      </article>
    </a-spin>
  </div>
</template>

<style scoped>
.share-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 12px 8px 48px;
}

.share-page__sheet,
.share-page__gone {
  background: #fff;
  border: 1px solid rgba(5, 5, 5, 0.06);
  border-radius: 16px;
  padding: 28px 28px 20px;
}

.share-page__sheet header p,
.share-page__gone p {
  margin: 0;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}

.share-page__sheet h1,
.share-page__gone h1 {
  margin: 6px 0 22px;
  font-size: 24px;
  line-height: 1.3;
}

.share-page__system {
  margin: 0 0 16px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}

.share-page__try,
.share-page__gone a {
  display: inline-flex;
  margin-top: 8px;
  height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  background: #1677ff;
  color: #fff;
  align-items: center;
  text-decoration: none;
}
</style>
