<script setup lang="ts">
import { SITE_GITHUB_HANDLE, SITE_GITHUB_URL } from '@/constants/siteFooter'
</script>

<template>
  <div class="notice-page">
    <a-card class="notice-card" :bordered="false">
      <div class="notice-card__header">
        <div class="notice-card__title">用户须知</div>
        <div class="notice-card__desc">使用 AI Coding Station 前，请先读完这几条。</div>
      </div>

      <section class="notice-card__section">
        <h2>站点是做什么的</h2>
        <p>
          本站是个人搭建的 AI 应用开发测试与技术交流场所，用来试用代码生成、智能体对话等功能。它不是商业产品，也不提供稳定性、时效性方面的承诺。
        </p>
      </section>

      <section class="notice-card__section">
        <h2>请节制使用</h2>
        <p>
          计算、存储和邮件都是有限的个人资源。请按自己的学习或试验需要使用，不要脚本刷接口、长时间占满对话，或把这里当成不限量的公共算力。
        </p>
      </section>

      <section class="notice-card__section">
        <h2>禁止用于盈利</h2>
        <p>
          不得利用本站功能、生成结果或账号对外收费、接单、转售，或把它包装成自己的商业服务。个人学习、课程作业和非商业的技术交流可以使用。
        </p>
      </section>

      <section class="notice-card__section">
        <h2>账号与内容</h2>
        <p>
          请保管好自己的密码。邮箱只用于找回密码，不会公开展示。你在站内留下的对话和生成内容请自行判断是否适合公开；不要提交他人的隐私、账号凭据，或你没有权利使用的材料。
        </p>
      </section>

      <section class="notice-card__section">
        <h2>欢迎交流</h2>
        <p>
          使用中的想法、问题和改进建议都欢迎讨论。有具体问题时，可以通过 GitHub 联系
          <a :href="SITE_GITHUB_URL" target="_blank" rel="noopener noreferrer">{{ SITE_GITHUB_HANDLE }}</a>。
        </p>
      </section>

      <div class="notice-card__footer">
        <router-link to="/user/login">返回登录</router-link>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.notice-page {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 24px 16px;
}

.notice-card {
  width: 100%;
  max-width: 640px;
}

.notice-card__header {
  margin-bottom: 8px;
}

.notice-card__title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 4px;
}

.notice-card__desc {
  color: rgba(0, 0, 0, 0.65);
}

.notice-card__section {
  margin-top: 20px;
}

.notice-card__section h2 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 650;
}

.notice-card__section p {
  margin: 0;
  line-height: 1.75;
  color: rgba(0, 0, 0, 0.75);
}

.notice-card__footer {
  margin-top: 24px;
}
</style>
