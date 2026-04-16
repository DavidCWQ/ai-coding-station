<div align="center">

<img src="./ai-coding-station-frontend/public/img.png" alt="AI Coding Station Logo" width="120" height="120">

## AI CODING STATION

### 对话式速成 AI 应用工坊

通过对话生成与修改页面，配合图片素材与一键部署，几分钟内完成可演示、可上线的原型作品。

[**Our Philosophy**] Wrap complex technology into intuitive human interaction.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](#架构与技术栈)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](#架构与技术栈)
[![Node](https://img.shields.io/badge/Node.js-20%2B-informational.svg)](#安装与启动)
[![Vue](https://img.shields.io/badge/Vue-3.x-4FC08D.svg)](#架构与技术栈)

**文档入口**：[详细文档](./docs/README.md) · [配置说明](./docs/config.md)

</div>

---

## 项目简介

**AI Coding Station** 是一个面向个人创作者、小团队与技术爱好者的 **对话式 AI 应用工坊**。

你可以通过自然语言对话生成和修改网页应用，上传图片等素材，并一键部署到线上，快速完成从「想法」到「可演示、可分享原型」的闭环。

项目同时也是一个 **全栈 AI 工程实践作品集**，在真实可用的产品形态下，实践 Prompt Engineering、Harness Engineering、流式输出、Agent 编排与前后端协同等能力。

在线演示（当前部署环境）：[`https://davidcwq.online`](https://davidcwq.online)

---

## 核心特性

- **对话生成与修改页面**  
  用自然语言描述想法，生成页面结构与内容；之后可以继续对话进行分块修改、补充说明、重写文案等，形成持续迭代的工作流。

- **图片素材上传与资产管理**  
  支持上传图片等静态资源，用于页面展示或说明，方便搭建具有视觉完整度的落地页与应用页面。

- **一键部署与分享**  
  在平台内完成生成与修改后，可以一键部署为可访问的网址，方便对外分享、演示或收集反馈。

- **智能体对话页面（独立能力）**  
  平台提供独立的智能体对话页面，可用于自动答疑、FAQ、基础售前引导等场景，为后续「AI 建站 + 客服」一体化打基础。

- **全栈 AI 工程实践**  
  后端基于大模型实现对话、流式输出与上下文管理，落地 LangChain4j + DeepSeek Chat + DashScope 嵌入 + pgvector RAG；前端实现从构建页面到展示、部署的完整产品链路，体现工程化实践能力。

---

## 典型使用场景

- **个人创作者落地页**  
  在小红书 / 抖音等平台有内容和流量的创作者，可以用平台快速生成课程页、服务介绍页或销售落地页，并通过智能体承接常见问答。

- **小商家与本地服务展示页**  
  理发店、家政、摄影、个人律师 / 咨询师等，可以用最小成本搭建服务简介页、价目表与预约说明页面。

- **产品 / 技术原型验证**  
  产品经理与开发者可以用对话式方式快速搭建 PoC 页面，验证信息结构与交互流程，减少与前端的多轮沟通成本。

- **个人作品集与学习实验台**  
  把平时的 AI 想法、实验 Demo 整理成可访问的网页应用，作为在线作品集展示全栈 AI 能力。

---

## 安装与启动

> 本节给出简要步骤，完整运行 / 部署说明、Docker Compose 编排与生产部署细节，请参考 [`docs/README.md`](./docs/README.md) 与 [`docs/config.md`](./docs/config.md)。

### 前置依赖

- Java 21（JDK 21+）  
- Maven 3.9+  
- Node.js 20+（前端要求 `^20.19.0 || >=22.12.0`）  
- Docker（可选，用于本地 MySQL / Redis / PostgreSQL / Nginx 容器）  
- API Key（需在本地或环境中配置好所需的大模型，如 DeepSeek、Claude 等）

### 克隆项目

```bash
git clone https://github.com/DavidCWQ/project_ai-coding-station.git
cd project_ai-coding-station
```

### 启动后端（API / 模型接入）

```bash
# 使用 Maven Wrapper
./mvnw spring-boot:run

# 或使用本机 Maven
mvn spring-boot:run
```

默认后端地址：

- 基础路径：`http://localhost:8142/api`

> 确保本地 MySQL、Redis、PostgreSQL(pgvector) 与 `application*.yml` 中配置一致；也可以使用仓库自带的 `compose.yaml` 启动依赖服务，详见 `docs/README.md`。

### 启动前端（Web 控制台）

```bash
cd ai-coding-station-frontend
npm install
npm run dev
```

Vite 默认运行在 `http://localhost:5876`（以实际配置为准）。联调时推荐通过 Vite 代理到后端 `/api` 入口，避免跨域与 Cookie 问题。

---

## 架构与技术栈

### 整体架构

- **前端**：Vue 3 单页应用，负责页面构建、对话交互界面、应用列表与详情展示、一键部署等。  
- **后端**：Spring Boot 服务，负责会话管理、聊天与生成逻辑、资产管理、权限控制、RAG 检索等。  
- **AI 与 Agent**：通过 LangChain4j 封装大模型接口，使用 DeepSeek Chat 作为对话模型，DashScope 作为向量嵌入模型，配合 PostgreSQL + pgvector 实现智能体的检索增强能力。  
- **基础设施**：MySQL + Redis + PostgreSQL(pgvector) + Nginx + Docker / Docker Compose。

### 后端

- Java 21  
- Spring Boot 3.5.x（Web / AOP / Session Data Redis / Cache with Caffeine）  
- MyBatis-Flex（含代码生成器）  
- LangChain4j（OpenAI 兼容接口接 **DeepSeek Chat**）  
- DashScope 嵌入模型（RAG 向量生成）  
- Jedis / Redis / HikariCP  
- Knife4j OpenAPI 3（接口文档）

### 前端

- Vue 3 + TypeScript  
- Vite 7  
- Ant Design Vue 4  
- Pinia / Vue Router  
- Axios + OpenAPI TypeScript SDK（`@umijs/openapi`）

### 基础设施与数据

- MySQL（业务持久化）  
- Redis（会话、对话记忆、缓存）  
- PostgreSQL + pgvector（智能体 RAG 向量库）  
- Nginx（前端静态站与封面等静态资源）  
- Docker / Docker Compose（本地依赖与生产编排）

---

## 项目定位与背景

- **非商业化，重在实践与展示**  
  项目由个人在业余时间独立维护，定位为「AI 应用开发测试记录台」与全栈工程实践作品集，不追求短期商业化收益。

- **真实场景驱动的工程实验场**  
  平台选择“对话式建站 + 智能体对话”这一贴近真实需求的场景，实践 Prompt Engineering、流式输出、会话管理、Agent 编排、RAG 与前后端协作等能力。

- **长期迭代，渐进式完善**  
  功能会根据个人学习与实际使用反馈持续调整，优先保证可用性与体验，再逐步拓展模板库、工作流与更多智能体协作场景。

---

## 贡献与反馈

欢迎提出 Issue、建议或 Pull Request，一起讨论如何让 **AI Coding Station** 更好用，也更适合作为学习与实践平台。

如有反馈、建议或合作意向，可通过 Issue 或其他联系方式与我交流。

---

## 许可证与鸣谢

- 本项目使用 **MIT License**，详见仓库中的 `LICENSE` 文件。  
- 本项目参考开源项目 [AI零代码应用生成平台](https://github.com/liyupi/yu-ai-code-mother/)。  
- 本项目由 **[DavidCWQ](https://github.com/DavidCWQ)** 发起与维护。

如果本项目对你有帮助，欢迎 Star 或分享给同样在折腾 AI 应用与智能体的开发者。