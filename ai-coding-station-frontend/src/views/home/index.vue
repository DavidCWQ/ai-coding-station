<script setup lang="ts">
import { computed } from 'vue'
import {
  AppstoreOutlined,
  CloudUploadOutlined,
  MessageOutlined,
  RocketOutlined,
} from '@ant-design/icons-vue'

import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()
const isLoggedIn = computed(() => loginUserStore.isLoggedIn)

const pillars = [
  {
    icon: AppstoreOutlined,
    title: '应用工坊',
    desc: '发现、创建与管理你的 AI 应用，把想法快速变成可运行的产品。',
  },
  {
    icon: CloudUploadOutlined,
    title: '编辑与部署',
    desc: '可视化调整界面与逻辑，部署后即可分享或自用，流程清晰顺手。',
  },
  {
    icon: MessageOutlined,
    title: '智能对话',
    desc: '在对话中与应用协作，让 AI 真正融入你的工作流。',
  },
] as const

const roadmap = [
  {
    name: '财税助手',
    hint: '智能财税咨询与工具集',
    status: '规划中',
  },
  {
    name: '心灵大师',
    hint: '情绪与心理陪伴方向探索',
    status: '规划中',
  },
] as const
</script>

<template>
  <div class="home">
    <section class="home__hero" aria-labelledby="home-hero-title">
      <h1 id="home-hero-title" class="home__title">
        用更轻的方式，
        <span class="home__title-accent">构建属于你的 AI 应用</span>
      </h1>
      <p class="home__eyebrow">AI Coding Station</p>
      <p class="home__lead">
        从灵感到上线，一站式完成创建、编辑、部署、与对话。
        界面简洁，让你专注在真正重要的事上。
      </p>
      <div class="home__cta">
        <RouterLink to="/app" custom v-slot="{ navigate }">
          <a-button type="primary" size="large" class="home__cta-primary" @click="navigate">
            <template #icon><AppstoreOutlined /></template>
            进入应用工坊
          </a-button>
        </RouterLink>
        <RouterLink v-if="isLoggedIn" to="/app/manage" custom v-slot="{ navigate }">
          <a-button size="large" @click="navigate">应用管理</a-button>
        </RouterLink>
        <RouterLink v-else to="/user/login" custom v-slot="{ navigate }">
          <a-button size="large" ghost type="primary" @click="navigate">登录后管理应用</a-button>
        </RouterLink>
        <RouterLink to="/about" custom v-slot="{ navigate }">
          <a-button size="large" type="link" class="home__cta-link" @click="navigate">了解平台</a-button>
        </RouterLink>
      </div>
    </section>

    <section class="home__pillars" aria-label="核心能力">
      <h2 class="home__section-title">核心能力</h2>
      <p class="home__section-sub">三块能力，覆盖从搭建到使用的完整路径。</p>
      <ul class="home__pillar-list">
        <li v-for="item in pillars" :key="item.title" class="home__pillar-card">
          <div class="home__pillar-icon" aria-hidden="true">
            <component :is="item.icon" />
          </div>
          <h3 class="home__pillar-title">{{ item.title }}</h3>
          <p class="home__pillar-desc">{{ item.desc }}</p>
        </li>
      </ul>
    </section>

    <section class="home__roadmap" aria-labelledby="home-roadmap-title">
      <div class="home__roadmap-head">
        <h2 id="home-roadmap-title" class="home__section-title">更多方向，敬请期待</h2>
        <p class="home__section-sub">我们正在筹备更多垂直场景，后续将逐步开放。</p>
      </div>
      <ul class="home__roadmap-list">
        <li v-for="row in roadmap" :key="row.name" class="home__roadmap-card">
          <div class="home__roadmap-main">
            <RocketOutlined class="home__roadmap-icon" aria-hidden="true" />
            <div>
              <h3 class="home__roadmap-name">{{ row.name }}</h3>
              <p class="home__roadmap-hint">{{ row.hint }}</p>
            </div>
          </div>
          <a-tag color="default" class="home__roadmap-tag">有待开发</a-tag>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.home {
  --home-accent: #2563eb;
  --home-accent-soft: rgba(37, 99, 235, 0.08);
  --home-text: rgba(0, 0, 0, 0.88);
  --home-muted: rgba(0, 0, 0, 0.55);
  --home-border: rgba(5, 5, 5, 0.06);
  --home-radius: 16px;
  max-width: 920px;
  margin: 0 auto;
  padding-bottom: 32px;
}

.home__hero {
  padding: 36px 0 40px;
  border-bottom: 1px solid var(--home-border);
  margin-bottom: 40px;
}

.home__eyebrow {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--home-accent);
  margin-bottom: 16px;
  margin-left: 2px;
}

.home__title {
  font-size: clamp(1.75rem, 4vw, 2.35rem);
  font-weight: 700;
  line-height: 1.25;
  color: var(--home-text);
  letter-spacing: -0.02em;
  margin-bottom: 16px;
}

.home__title-accent {
  display: inline;
  background: linear-gradient(120deg, #1d4ed8 0%, #6366f1 45%, #0ea5e9 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.home__lead {
  font-size: 16px;
  line-height: 1.7;
  color: var(--home-muted);
  max-width: 52ch;
  margin-bottom: 28px;
}

.home__cta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.home__cta-primary {
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.25);
}

.home__cta-link {
  padding-left: 4px;
}

.home__section-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--home-text);
  letter-spacing: -0.02em;
  margin-bottom: 8px;
}

.home__section-sub {
  font-size: 14px;
  color: var(--home-muted);
  margin-bottom: 24px;
  line-height: 1.6;
}

.home__pillars {
  margin-bottom: 48px;
}

.home__pillar-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 768px) {
  .home__pillar-list {
    grid-template-columns: 1fr;
  }
}

.home__pillar-card {
  border-radius: var(--home-radius);
  border: 1px solid var(--home-border);
  padding: 22px 20px;
  background: linear-gradient(160deg, #fff 0%, #fafbff 100%);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.home__pillar-card:hover {
  border-color: rgba(37, 99, 235, 0.2);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.home__pillar-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: var(--home-accent);
  background: var(--home-accent-soft);
  margin-bottom: 14px;
}

.home__pillar-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--home-text);
  margin-bottom: 8px;
}

.home__pillar-desc {
  font-size: 14px;
  line-height: 1.65;
  color: var(--home-muted);
  margin: 0;
}

.home__roadmap {
  padding: 28px 24px;
  border-radius: var(--home-radius);
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.04) 0%, rgba(14, 165, 233, 0.05) 100%);
  border: 1px solid var(--home-border);
}

.home__roadmap-head {
  margin-bottom: 20px;
}

.home__roadmap-head .home__section-sub {
  margin-bottom: 0;
}

.home__roadmap-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.home__roadmap-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 18px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--home-border);
}

.home__roadmap-main {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  min-width: 0;
}

.home__roadmap-icon {
  font-size: 20px;
  color: var(--home-accent);
  margin-top: 2px;
  flex-shrink: 0;
}

.home__roadmap-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--home-text);
  margin: 0 0 4px;
}

.home__roadmap-hint {
  font-size: 13px;
  color: var(--home-muted);
  margin: 0;
  line-height: 1.5;
}

.home__roadmap-tag {
  margin: 0;
  border-radius: 6px;
}
</style>
