<script setup lang="ts">
import { getCurrentInstance } from 'vue'

import { AGENT_CARDS } from '@/constants/agents'

const vm = getCurrentInstance()
const router = vm?.proxy?.$router as any

const openAgent = (code: string) => {
  void router.push(`/agents/${encodeURIComponent(code)}/chat`)
}
</script>

<template>
  <div class="agent-list">
    <div class="agent-list__head">
      <h1 class="agent-list__title">智能体 Agent</h1>
      <p class="agent-list__sub">选择一位助手开始对话，内容将保存在您的账号下。</p>
    </div>
    <a-row :gutter="[16, 16]">
      <a-col v-for="a in AGENT_CARDS" :key="a.code" :xs="24" :sm="12" :lg="8">
        <div
          class="agent-card"
          role="button"
          tabindex="0"
          :style="{ background: a.color }"
          @click="openAgent(a.code)"
          @keydown.enter.prevent="openAgent(a.code)"
        >
          <div class="agent-card__inner">
            <h2 class="agent-card__title">{{ a.title }}</h2>
            <p class="agent-card__desc">{{ a.description }}</p>
            <span class="agent-card__cta">开始对话</span>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.agent-list {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.agent-list__head {
  margin-bottom: 24px;
}

.agent-list__title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
}

.agent-list__sub {
  margin: 0;
  color: rgba(0, 0, 0, 0.55);
  font-size: 14px;
}

.agent-card {
  border-radius: 12px;
  color: rgba(0, 0, 0, 0.85);
  cursor: pointer;
  min-height: 160px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border: 1px solid rgba(5, 5, 5, 0.06);
}

.agent-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.agent-card:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: 2px;
}

.agent-card__inner {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 160px;
}

.agent-card__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.agent-card__desc {
  margin: 0;
  flex: 1;
  font-size: 13px;
  line-height: 1.5;
  color: rgba(0, 0, 0, 0.62);
}

.agent-card__cta {
  font-size: 13px;
  font-weight: 600;
  color: #1677ff;
}
</style>
