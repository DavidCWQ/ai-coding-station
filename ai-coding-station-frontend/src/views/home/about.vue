<script setup lang="ts">
import { BulbOutlined, HeartOutlined, TeamOutlined } from '@ant-design/icons-vue'
</script>

<template>
  <div class="about">
    <header class="about__hero">
      <p class="about__eyebrow">关于我们</p>
      <h1 class="about__title">AI Coding Station</h1>
      <p class="about__tagline">让 AI 应用的创建与使用，保持简单、可靠、有一点温度。</p>
    </header>

    <div class="about__body">
      <section class="about__block" aria-labelledby="about-mission">
        <h2 id="about-mission" class="about__heading">我们在做什么</h2>
        <p class="about__text">
          AI Coding Station 面向希望快速落地 AI 能力的开发者和团队：在<strong>应用工坊</strong>里搭建与发现应用，用直观的编辑与部署流程把原型推到可用状态，再通过对话与 AI 协同完成日常任务。
        </p>
        <p class="about__text">
          我们不堆砌概念，而是把路径收短：界面清爽、步骤清楚，让你把精力放在产品与体验上，而不是被工具绊住。
        </p>
      </section>

      <section class="about__values" aria-label="理念">
        <ul class="about__value-list">
          <li class="about__value-card">
            <div class="about__value-icon" aria-hidden="true">
              <BulbOutlined />
            </div>
            <h3 class="about__value-title">简约不简单</h3>
            <p class="about__value-desc">默认界面克制，关键能力一步可达；复杂能力藏在需要时出现。</p>
          </li>
          <li class="about__value-card">
            <div class="about__value-icon" aria-hidden="true">
              <TeamOutlined />
            </div>
            <h3 class="about__value-title">面向真实场景</h3>
            <p class="about__value-desc">功能围绕「创建—部署—使用」闭环迭代，并持续扩展垂直场景。</p>
          </li>
          <li class="about__value-card">
            <div class="about__value-icon" aria-hidden="true">
              <HeartOutlined />
            </div>
            <h3 class="about__value-title">长期主义</h3>
            <p class="about__value-desc">财税助手、心灵大师等方向正在规划中，欢迎与我们一同见证产品成长。</p>
          </li>
        </ul>
      </section>

      <section class="about__soon" aria-labelledby="about-soon-title">
        <h2 id="about-soon-title" class="about__heading">路线图</h2>
        <p class="about__text about__text--tight">
          以下模块仍在设计与开发中，上线后会第一时间在站内开放入口。
        </p>
        <ul class="about__soon-list">
          <li class="about__soon-item">
            <span class="about__soon-name">财税助手</span>
            <a-tag>敬请期待</a-tag>
          </li>
          <li class="about__soon-item">
            <span class="about__soon-name">心灵大师</span>
            <a-tag>有待开发</a-tag>
          </li>
        </ul>
      </section>

      <footer class="about__footer">
        <p class="about__credit">
          项目由
          <a
            href="https://github.com/DavidCWQ"
            target="_blank"
            rel="noopener noreferrer"
            class="about__link"
          >
            DavidCWQ
          </a>
          发起与维护。
        </p>
        <RouterLink to="/" class="about__back">返回首页</RouterLink>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.about {
  --about-accent: #2563eb;
  --about-accent-soft: rgba(37, 99, 235, 0.08);
  --about-text: rgba(0, 0, 0, 0.88);
  --about-muted: rgba(0, 0, 0, 0.55);
  --about-border: rgba(5, 5, 5, 0.06);
  --about-radius: 16px;
  max-width: 720px;
  margin: 0 auto;
  padding-bottom: 24px;
}

.about__hero {
  text-align: center;
  padding: 12px 0 36px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--about-border);
}

.about__eyebrow {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--about-accent);
  margin-bottom: 12px;
}

.about__title {
  font-size: clamp(1.85rem, 4.5vw, 2.5rem);
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--about-text);
  margin-bottom: 12px;
  background: linear-gradient(120deg, #1e293b 0%, #334155 40%, #2563eb 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.about__tagline {
  font-size: 16px;
  line-height: 1.65;
  color: var(--about-muted);
  max-width: 36em;
  margin: 0 auto;
}

.about__body {
  padding-top: 8px;
}

.about__block {
  margin-bottom: 40px;
}

.about__heading {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--about-text);
  letter-spacing: -0.02em;
  margin-bottom: 14px;
}

.about__text {
  font-size: 15px;
  line-height: 1.75;
  color: var(--about-muted);
  margin: 0 0 14px;
}

.about__text:last-child {
  margin-bottom: 0;
}

.about__text--tight {
  margin-bottom: 16px;
}

.about__text strong {
  color: rgba(0, 0, 0, 0.78);
  font-weight: 600;
}

.about__values {
  margin-bottom: 40px;
}

.about__value-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 12px;
}

.about__value-card {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 18px 20px;
  border-radius: var(--about-radius);
  border: 1px solid var(--about-border);
  background: #fafbff;
}

.about__value-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--about-accent);
  background: var(--about-accent-soft);
}

.about__value-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--about-text);
  margin: 0 0 6px;
}

.about__value-desc {
  font-size: 14px;
  line-height: 1.65;
  color: var(--about-muted);
  margin: 0;
}

.about__soon {
  margin-bottom: 36px;
  padding: 22px 22px 20px;
  border-radius: var(--about-radius);
  border: 1px dashed rgba(37, 99, 235, 0.25);
  background: rgba(37, 99, 235, 0.03);
}

.about__soon-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.about__soon-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--about-border);
}

.about__soon-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--about-text);
}

.about__footer {
  text-align: center;
  padding-top: 8px;
  border-top: 1px solid var(--about-border);
}

.about__credit {
  font-size: 14px;
  color: var(--about-muted);
  margin: 0 0 12px;
}

.about__link {
  color: var(--about-accent);
  font-weight: 500;
  text-decoration: none;
}

.about__link:hover {
  text-decoration: underline;
}

.about__back {
  font-size: 14px;
  font-weight: 500;
  color: var(--about-accent);
  text-decoration: none;
}

.about__back:hover {
  text-decoration: underline;
}
</style>
