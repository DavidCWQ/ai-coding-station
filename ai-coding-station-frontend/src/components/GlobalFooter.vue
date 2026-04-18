<script setup lang="ts">
import { GithubOutlined } from '@ant-design/icons-vue'
import {
  SITE_COPYRIGHT_NAME,
  SITE_GITHUB_HANDLE,
  SITE_GITHUB_URL,
  SITE_ICP_TEXT,
  SITE_ICP_URL,
  SITE_PSB_TEXT,
  SITE_PSB_URL,
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
          <GithubOutlined class="global-footer__github-icon" aria-hidden="true" />
          {{ SITE_GITHUB_HANDLE }}
        </a>
      </span>
      <span aria-hidden="true" class="global-footer__sep"> ｜ </span>
      <a
        class="global-footer__link"
        :href="SITE_ICP_URL"
        target="_blank"
        rel="noopener noreferrer"
      >
        {{ SITE_ICP_TEXT }}
      </a>
      <span aria-hidden="true" class="global-footer__sep"> ｜ </span>
      <a
        class="global-footer__link global-footer__muted"
        :href="SITE_PSB_URL"
        target="_blank"
        rel="noopener noreferrer"
      >
        <img src="/logo_police.png" alt="" class="global-footer__police-icon" />
        {{ SITE_PSB_TEXT }}
      </a>
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

.global-footer__link.global-footer__muted {
  color: rgba(0, 0, 0, 0.45);
}

.global-footer__github-icon {
  margin-bottom: 5px;
  font-size: 14px;
  vertical-align: middle;
}

.global-footer__link:hover {
  text-decoration: underline;
}

.global-footer__police-icon {
  width: auto;
  height: 18px;
  margin-right: 5px;
  margin-bottom: 3px;
  vertical-align: middle;
}
</style>
