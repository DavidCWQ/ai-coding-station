<script setup lang="ts">
import {
  SITE_COPYRIGHT_NAME,
  SITE_GITHUB_HANDLE,
  SITE_GITHUB_URL,
  SITE_ICP_TEXT,
} from '@/constants/siteFooter'

const year = new Date().getFullYear()
</script>

<template>
  <a-layout-footer class="global-footer" aria-label="网站页脚">
    <p class="global-footer__line global-footer__line--muted">
      本站用于个人 AI 应用开发测试与技术分享，不含企业信息。
    </p>
    <p class="global-footer__line">
      <span>© {{ year }} {{ SITE_COPYRIGHT_NAME }} 版权所有</span>
      <span aria-hidden="true" class="global-footer__sep"> ｜ </span>
      <span>
        Project by
        <a
          :href="SITE_GITHUB_URL"
          target="_blank"
          rel="noopener noreferrer"
          class="global-footer__link"
        >
          {{ SITE_GITHUB_HANDLE }}
        </a>
      </span>
      <span aria-hidden="true" class="global-footer__sep"> ｜ </span>
      <a
        class="global-footer__link"
        href="https://beian.miit.gov.cn/"
        target="_blank"
        rel="noopener noreferrer"
      >
        {{ SITE_ICP_TEXT }}
      </a>
      <span aria-hidden="true" class="global-footer__sep"> ｜ </span>
      <span class="global-footer__muted">沪公网安备XXXXXXXXXXXX号（公安联网备案审核中，占位）</span>
    </p>
  </a-layout-footer>
</template>

<style scoped>
.global-footer {
  text-align: center;
  background: transparent;
  padding: 16px 12px 20px;
  color: rgba(0, 0, 0, 0.65);
}

.global-footer__line {
  margin: 0;
  font-size: 12px;
  line-height: 1.8;
}

.global-footer__line + .global-footer__line {
  margin-top: 6px;
}

.global-footer__line--muted,
.global-footer__muted {
  color: rgba(0, 0, 0, 0.45);
}

.global-footer__sep {
  color: rgba(0, 0, 0, 0.35);
}

.global-footer__link {
  color: inherit;
  text-decoration: none;
}

.global-footer__link:hover {
  text-decoration: underline;
}
</style>
