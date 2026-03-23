<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

import AppCard from '@/components/app/AppCard.vue'
import { createApp } from '@/api/appController'
import { useAppList } from '@/hooks/useAppList'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const promptText = ref('')
const creating = ref(false)

const myList = useAppList('my')
const featuredList = useAppList('featured')

const isLoggedIn = computed(() => loginUserStore.isLoggedIn)

type Suggestion = {
  title: string
  prompt: string
}

const suggestions: Suggestion[] = [
  {
    title: '极简个人博客',
    prompt:
        '极简个人博客网站，支持文章分类、标签管理和阅读统计，支持 Markdown 写作，整体风格简洁现代',
  },
  {
    title: '企业官网',
    prompt:
        '企业官网，包含首页介绍、产品展示、团队介绍和联系方式，设计风格专业大气，适合展示公司形象',
  },
  {
    title: '作品展示网站',
    prompt:
        '设计师作品集网站，用于展示项目案例和图片作品，支持分类浏览和详情页展示，风格简洁高级',
  },
  {
    title: '在线商城',
    prompt:
        '在线商城网站，支持商品搜索、列表展示、商品详情、加入购物车和下单流程，界面现代简洁',
  },
]

const applySuggestion = (s: Suggestion) => {
  promptText.value = `轻松创建一个：${s.prompt}`
}

const getErr = (err: unknown): string => {
  if (typeof err === 'object' && err !== null) {
    const m = (err as Record<string, unknown>).message
    if (typeof m === 'string') return m
  }
  return '创建失败'
}

const onCreate = async () => {
  const t = promptText.value.trim() + "\n保持代码部分简洁，无需代码注释"
  if (!t) {
    message.warning('请先描述你想创建的应用')
    return
  }
  if (!isLoggedIn.value) {
    message.info('请先登录')
    await router.push({ path: '/user/login', query: { redirect: '/app' } })
    return
  }
  creating.value = true
  try {
    const res = await createApp({
      appName: '',
      initPrompt: t,
      codeGenType: 'HTML',
    })
    const id = res.data?.data
    if (id === undefined || id === null) {
      message.error('创建失败')
      return
    }
    await router.push(`/app/${String(id)}`)
  } catch (e) {
    message.error(getErr(e))
  } finally {
    creating.value = false
  }
}

const goDetail = (appId: string) => {
  void router.push({ path: `/app/${appId}`, query: { view: '1' } })
}

const viewWork = (deployKey: string) => {
  const key = deployKey.trim()
  if (!key) return
  window.open(`http://localhost:8088/${encodeURIComponent(key)}/`, '_blank', 'noopener,noreferrer')
}

onMounted(() => {
  void featuredList.fetchList()
  if (isLoggedIn.value) {
    void myList.fetchList()
  }
})
</script>

<template>
  <div class="app-home">
    <section class="app-home__hero">
      <h1 class="app-home__title">AI Coding Station</h1>
      <p class="app-home__subtitle">与 AI 对话轻松创建应用和网站</p>

      <div class="app-home__composer">
        <a-textarea
          v-model:value="promptText"
          :rows="5"
          class="app-home__textarea"
          placeholder="使用 AI Coding 高效创建一个网站，比如：个人博客，企业官网，产品展示网站……"
          :disabled="creating"
          @keydown.enter.exact.ctrl.prevent="onCreate"
        />
        <div class="app-home__composer-bar">
          <div class="app-home__composer-left">
            <a-button type="text" disabled>上传</a-button>
            <a-button type="text" disabled>优化</a-button>
          </div>
          <a-button type="primary" shape="circle" :loading="creating" class="app-home__send" @click="onCreate">
            <svg
              v-if="!creating"
              class="app-home__send-icon"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              aria-hidden="true"
            >
              <path
                d="M12 19V6.5M6.5 12L12 5.5 17.5 12"
                stroke="currentColor"
                stroke-width="2.25"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </a-button>
        </div>
      </div>

      <div class="app-home__pills">
        <button
          v-for="s in suggestions"
          :key="s.title"
          type="button"
          class="app-home__pill"
          @click="applySuggestion(s)"
        >
          {{ s.title }}
        </button>
      </div>
    </section>

    <section v-if="isLoggedIn" class="app-home__section">
      <div class="app-home__section-head">
        <h2 class="app-home__h2">我的应用</h2>
        <a-space wrap>
          <a-input
            v-model:value="myList.query.appName"
            allow-clear
            placeholder="按名称筛选"
            style="width: 200px"
            @press-enter="myList.doSearch"
          />
          <a-button type="primary" @click="myList.doSearch">搜索</a-button>
        </a-space>
      </div>
      <a-spin :spinning="myList.loading">
        <a-empty v-if="!myList.records.length" description="暂无应用，试试上方创建" />
        <a-row v-else :gutter="[16, 16]">
          <a-col v-for="app in myList.records" :key="String(app.id)" :xs="24" :sm="12" :lg="8">
            <AppCard :app="app" mode="mine" @click="goDetail" @view-chat="goDetail" @view-work="viewWork" />
          </a-col>
        </a-row>
        <div v-if="myList.total > 0" class="app-home__pager">
          <a-pagination
            :current="myList.query.pageNum"
            :page-size="myList.query.pageSize"
            :total="myList.total"
            :show-size-changer="true"
            :page-size-options="['12', '16', '20']"
            @change="myList.onPageChange"
          />
        </div>
      </a-spin>
    </section>

    <section class="app-home__section app-home__section--featured">
      <div class="app-home__section-head">
        <h2 class="app-home__h2">精选应用</h2>
        <a-space wrap>
          <a-input
            v-model:value="featuredList.query.appName"
            allow-clear
            placeholder="按名称搜索"
            style="width: 200px"
            @press-enter="featuredList.doSearch"
          />
          <a-button type="primary" @click="featuredList.doSearch">搜索</a-button>
        </a-space>
      </div>
      <a-spin :spinning="featuredList.loading">
        <a-empty v-if="!featuredList.records.length" description="暂无精选" />
        <a-row v-else :gutter="[16, 16]">
          <a-col v-for="app in featuredList.records" :key="String(app.id)" :xs="24" :sm="12" :lg="8">
            <AppCard :app="app" mode="featured" @click="goDetail" @view-chat="goDetail" @view-work="viewWork" />
          </a-col>
        </a-row>
        <div v-if="featuredList.total > 0" class="app-home__pager">
          <a-pagination
            :current="featuredList.query.pageNum"
            :page-size="featuredList.query.pageSize"
            :total="featuredList.total"
            :show-size-changer="true"
            :page-size-options="['12', '16', '20']"
            @change="featuredList.onPageChange"
          />
        </div>
      </a-spin>
    </section>
  </div>
</template>

<style scoped>
.app-home {
  margin: -24px -24px 0;
  padding-bottom: 32px;
}

@media (max-width: 768px) {
  .app-home {
    margin: -16px -16px 0;
  }
}

.app-home__hero {
  text-align: center;
  padding: 40px 20px 36px;
  border-radius: 0 0 24px 24px;
  background: linear-gradient(165deg, #e6fffb 0%, #f0f5ff 42%, #ffffff 100%);
}

.app-home__title {
  margin: 0;
  font-size: clamp(22px, 4vw, 30px);
  font-weight: 700;
  letter-spacing: 0.02em;
}

.app-home__subtitle {
  margin: 10px 0 0;
  font-size: 15px;
  color: rgba(0, 0, 0, 0.55);
}

.app-home__composer {
  max-width: 720px;
  margin: 28px auto 0;
  padding: 16px 18px 12px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(15, 30, 60, 0.08);
  border: 1px solid rgba(5, 5, 5, 0.06);
}

.app-home__textarea {
  border: none !important;
  box-shadow: none !important;
  font-size: 15px;
  resize: none;
}

.app-home__composer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.app-home__composer-left {
  display: flex;
  gap: 4px;
}

.app-home__send {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.app-home__send-icon {
  width: 1.5rem;
  height: 1.5rem;
  display: block;
}

.app-home__pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 800px;
  margin: 20px auto 0;
}

.app-home__pill {
  border: 1px solid rgba(5, 5, 5, 0.08);
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(6px);
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.75);
  cursor: pointer;
}

.app-home__pill:hover {
  border-color: #1677ff;
  color: #1677ff;
}

.app-home__section {
  padding: 32px 24px 0;
  max-width: 1200px;
  margin: 0 auto;
}

.app-home__section--featured {
  padding-bottom: 8px;
}

.app-home__section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.app-home__h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.app-home__pager {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

</style>
