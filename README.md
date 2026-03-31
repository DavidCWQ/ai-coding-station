AI Coding Station
=================

一个面向开发者与小团队的 **AI 应用工坊**：通过「应用配置 + 对话生成代码 + 一键部署」的方式，帮助你快速搭建、管理并发布自己的 AI 小工具与业务助手。

> 推荐使用桌面端或宽屏浏览阅读本文档。

---

### 目录

- **1. 项目简介**
- **2. 功能特性**
- **3. 技术栈与整体架构**
- **4. 运行与部署**
  - 4.1 本地开发（后端 + 前端）
  - 4.2 使用 Docker Compose 一键启动
  - 4.3 生产部署（compose.prod.yaml）
- **5. 环境变量与配置说明**
- **6. 主要功能模块说明**
- **7. 常见问题（FAQ）**
- **8. 开发规范与约定**
- **9. 贡献指南**
- **X. 许可证与鸣谢**

---

### 1. 项目简介

**AI Coding Station** 的定位是一个「轻量但工程化完整」的 AI 应用工坊，提供以下闭环：

- **应用工坊**：在 Web 端创建、配置你的 AI 应用（如财税助手、心灵大师等）。
- **对话生成代码**：通过与 LLM（当前接入 DeepSeek Chat）对话，生成对应的前后端代码骨架。
- **一键部署与预览**：后端负责打包构建、静态资源部署至 Nginx，并支持自动截图与封面管理。

本项目采用 **前后端分离 + 全容器化** 方案，适合：

- 希望快速尝试/孵化 AI side project 的个人开发者；
- 想要为团队搭建 AI 工具箱的工程实践者；
- 想要参考一套完整的「AI 应用 + SaaS 管理台」技术实现的学习者。

---

### 2. 功能特性

- **应用管理（App Module）**
  - 创建 / 更新 / 删除个人应用
  - 精选应用列表与详情查看
  - 应用代码生成、代码下载（ZIP）
  - 一键部署到 Nginx，支持独立预览链接与封面图

- **AI 对话与代码生成（Chat Module）**
  - 基于 LangChain4j + DeepSeek Chat 的对话能力
  - SSE 流式输出（后端 `Flux<ServerSentEvent<String>>`，前端基于 `useSSEChat` Hook）
  - 支持长文本 POST 接口规避 `431` 等请求头限制

- **用户与权限体系（User Module）**
  - 用户注册 / 登录 / 个人信息
  - 基于角色的访问控制（普通用户 / 管理员）
  - 支持通过配置关闭新用户注册（生产环境默认关闭）

- **前端工坊与可视化预览**
  - 前端基于 Vue 3 + Vite + Ant Design Vue
  - 应用首页 / 列表 / 详情 / 编辑页 / 管理后台 / AI 聊天等页面
  - Visual Editor 相关工具（`visualEditor.ts` 系列）用于组件抽取与预览

- **工程化与运维支持**
  - **MySQL + MyBatis-Flex**：关系型数据存储与分页查询
  - **Redis + Spring Session + Caffeine Cache**：会话与缓存治理
  - **Docker Compose**：一键拉起 MySQL / Redis / Nginx / Backend / Frontend Build
  - **Playwright + Node**：后端自动执行截图脚本，为已部署应用生成封面图
  - **Knife4j + OpenAPI 3**：美观的接口文档 UI，按模块分组

---

### 3. 技术栈与整体架构

- **后端**
  - Java 21
  - Spring Boot 3.5.x
  - Spring Web / Spring AOP / Spring Session Data Redis / Spring Cache (Caffeine)
  - MyBatis-Flex（含代码生成器）
  - LangChain4j（OpenAI 兼容接口，当前配置为 DeepSeek Chat）
  - Jedis / Redis / HikariCP
  - Knife4j OpenAPI 3

- **前端**
  - Vue 3 + TypeScript
  - Vite 7
  - Ant Design Vue 4
  - Pinia / Vue Router
  - Axios + OpenAPI TypeScript SDK（`@umijs/openapi`）

- **基础设施**
  - MySQL（持久化存储，表前缀多为业务模块名）
  - Redis（会话与缓存）
  - Nginx（静态站点 + 前端路由转发）
  - Docker / Docker Compose（本地与生产环境统一编排）

整体架构可以理解为：

- 浏览器（前端 Vue SPA）
  → 通过 Axios 调用后端 `/api/**` 接口（SSE/REST）
  → Spring Boot 应用（业务模块：`user`、`app`、`chat` 等）
  → MySQL / Redis / 文件系统（`tmp/code_output`、`tmp/code_deploy`、`tmp/covers`）
  → Nginx 负责对外暴露静态站点与封面资源。

---

### 4. 运行与部署

#### 4.1 本地开发（推荐）

**前置要求**

- 已安装：
  - JDK 21+
  - Maven 3.9+
  - Node.js 20+（前端要求 `^20.19.0 || >=22.12.0`）
  - Docker（可选，用于本地 MySQL / Redis 容器）

**步骤 1：克隆仓库**

```bash
git clone https://github.com/你的用户名/project_ai-coding-station.git
cd project_ai-coding-station
```

**步骤 2：准备数据库 Mysql 与 Redis**

方式一：直接使用仓库自带的 `compose.yaml` 启动 MySQL + Redis + Nginx 静态站位（开发态）：

```bash
docker compose -f compose.yaml up -d mysql redis nginx
```

方式二：本地已有 MySQL / Redis，则确保与 `src/main/resources/application.yml` 中配置匹配：

- MySQL：`jdbc:mysql://localhost:3306/ai_coding_station_memo`
- Redis：`localhost:6380`（或按需调整）

初始化 SQL 位于 `sql/` 目录，MySQL 容器启动时会自动加载。

**步骤 3：启动后端**

```bash
# 使用 Maven Wrapper
./mvnw spring-boot:run

# 或使用本机 Maven
mvn spring-boot:run
```

后端默认地址：

- 基础路径：`http://localhost:8142/api`
- Knife4j 文档：启动后可访问 `/api/doc.html`（具体路径以实际配置为准）。

**步骤 4：启动前端**

```bash
cd ai-coding-station-frontend
npm install
npm run dev
```

Vite 开发服务器默认启动在 `http://localhost:5876`。推荐通过前端环境变量联调后端：

- `VITE_APP_API_BASE_URL=/api`（浏览器始终走同源路径）
- `VITE_DEV_PROXY_TARGET=http://127.0.0.1:8142`（或你的本地后端地址）

这样可避免跨域与 Cookie 问题；开发态由 Vite 代理转发到真实后端。

---

#### 4.2 使用 Docker Compose 一键启动（本地 / 测试环境）

根目录已提供标准的 `compose.yaml`，主要包含：

- `mysql`：数据库服务，挂载 `sql/` 目录自动初始化；
- `redis`：缓存与 Session 存储；
- `nginx`：前端静态站占位（开发模式下可仅用于静态文件）；

启动方式：

```bash
docker compose -f compose.yaml up -d
```

此文件不会构建后端 Jar，只用于本地依赖服务。如果你希望后端也以容器方式运行，请使用下一节的生产部署编排。

---

#### 4.3 生产部署（compose.prod.yaml）

`compose.prod.yaml` 提供了完整的生产级编排，包含：

- `mysql` / `redis` / `nginx` 与 `compose.yaml` 一致但加入了 **healthcheck**；
- `backend`：基于 `Dockerfile` 构建 Spring Boot 应用镜像；
- `frontend-build`：基于 `Dockerfile.frontend` 构建前端静态资源并拷贝到共享卷；
- **注意**：需自行配置 `.env`。`compose.prod.yaml` 已在 `backend.environment` 中固定 `SPRING_PROFILES_ACTIVE=prod`。

**核心流程**

1. `frontend-build` 容器使用 `Dockerfile.frontend`：
   - 安装前端依赖 → `npm run build-only`（即 `vite build`）→ 生成 `/frontend/dist`；
   - `vite build` 默认是 `production` mode，会自动读取前端根目录 `.env.production`（若存在）；
   - 将构建产物复制到宿主 `./tmp/code_deploy` 目录（通过 volume 挂载）。
2. `backend` 容器：
   - 通过 `Dockerfile` 打包后端 Jar；
   - 再安装 Node + Playwright 以支持截图脚本 `scripts/screenshot.js`；
   - 暴露 `8142` 端口，读取环境变量 `.env` 中的 DeepSeek API Key、数据库与 Redis 账号等。
3. `nginx` 容器：
   - 挂载 `./tmp/code_deploy` 作为 `/usr/share/nginx/html`；
   - 通过 `nginx.prod.conf` 将静态站暴露到外网。

**一键启动示例**

```bash
docker compose -f compose.prod.yaml up -d --build
```

启动后可访问（以当前服务器映射配置为例）：

- 前端应用：`http://<你的服务器 IP>:8090`
- 对外统一 API 入口（经 Nginx 转发）：`http://<你的服务器 IP>:8090/api`
- 容器内部反向代理目标（仅容器网络可见）：`http://backend:8142/api`

说明：浏览器运行时不要直接使用 `http://backend:8142`，应保持前端请求 `/api`，由 Nginx 反向代理到后端容器。

---

### 5. 环境变量与配置说明

根目录提供 `.env` 文件（默认空），以及若干 `application*.yml` 与 Compose 配置，常用变量包括：

- **数据库 Mysql / Redis**
  - `MYSQL_USERNAME` / `MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD`
  - `REDIS_USERNAME` / `REDIS_PASSWORD`

- **AI 服务（DeepSeek）**
  - `DEEPSEEK_API_KEY`：在 `compose.prod.yaml` 中通过 `environment` 传给后端；
  - 后端 `application-prod.yml` 中通过：
    - `langchain4j.open-ai.chat-model.api-key`
    - `langchain4j.open-ai.streaming-chat-model.api-key`
    - 从环境变量读取。
  - 本地部署时，`DEEPSEEK_API_KEY` 通过 `application-local.yml` 直接读取。

- **部署相关**
  - `PUBLIC_DEPLOY_HOST`：前端可访问的部署站点地址，例如 `http://your-domain.com`；
  - `PUBLIC_COVERS_BASE`：封面图访问基址，例如 `http://your-domain.com`；
  - `SPRING_PROFILES_ACTIVE`：生产容器中设置为 `prod`，启用 `application-prod.yml`（已在 compose.prod.yaml 写死）。
  - `APP_USER_REGISTRATION_ENABLED`：是否允许用户注册（`true`/`false`），对应 `app.user.registration-enabled`，生产默认关闭。

- **前端构建环境**
  - `ai-coding-station-frontend/.env.development`：本地 `npm run dev` 使用；
  - `ai-coding-station-frontend/.env.production`：`vite build`（含 Docker 的 `frontend-build`）自动使用。
  - 推荐变量口径：
    - `VITE_APP_DEPLOY_BASE_URL`：部署预览链接域名（`getDeployUrl` 使用）；
    - `VITE_APP_API_BASE_URL=/api`：前端运行时 API 基路径（开发/生产统一）；
    - `VITE_APP_PREVIEW_BASE_URL=/api`：静态预览接口基路径；
    - `VITE_DEV_PROXY_TARGET`：仅开发态使用，Vite `/api` 代理到真实后端；
    - `OPENAPI_SCHEMA_URL`：`npm run openapi2ts` 使用的 OpenAPI 文档地址。

详细配置可参考：

- `src/main/resources/application.yml`：本地开发默认配置；
- `src/main/resources/application-prod.yml`：生产容器部署配置；
- `compose.yaml` / `compose.prod.yaml`：服务编排与环境变量注入。

---

### 6. 主要功能模块说明

后端代码按业务域纵向拆分，典型模块包括：

- **User 模块**
  - 用户注册 / 登录 / 登出
  - 当前登录用户信息获取
  - 管理员角色管理与权限检查（`@AuthCheck` 等）

- **App 模块**
  - 应用的增删改查、分页查询
  - 精选应用列表
  - 代码生成与存储（输出目录：`tmp/code_output`）
  - 应用部署：将构建后的静态资源拷贝到 `tmp/code_deploy`，由 Nginx 负责对外提供
  - 代码下载：后端根据应用配置打包成 ZIP 下发

- **Chat 模块（根据 prompts 中的设计）**
  - 对话历史与会话管理（`ChatHistory` / `ChatSession`）
  - 支持基于 appId 的数据隔离与权限校验
  - Cursor 风格的分页加载（基于 messageId 的「向前加载更多」）
  - 管理员可以按条件查看全局对话历史

前端则围绕以上模块提供：

- 登录 / 注册 / 个人中心等公共页面；
- 应用列表、详情、编辑与管理页；
- AI 聊天与历史记录页面；
- 管理后台（用户管理、应用审核等）。

---

### 7. 常见问题（FAQ）

- **Q: 首次启动时访问前端空白或 404？**  
  **A:** 请确认前端是否已成功构建并将 `dist` 内容挂载到 Nginx 对应目录（开发态下使用 Vite 开发服务器；生产态由 `frontend-build` + Nginx 提供静态资源）。

- **Q: DeepSeek 调用失败或无响应？**  
  **A:** 检查 `DEEPSEEK_API_KEY` 是否正确配置；同时查看后端日志中 LangChain4j 的请求 / 响应日志（`log-requests` 与 `log-responses` 默认为 `true`）。

- **Q: 登录态频繁丢失？**  
  **A:** 请确认 Redis 与 Spring Session 配置是否一致，生产环境中建议使用稳定的 Redis 服务，并注意 `session` 与 `cookie.max-age` 的配置（当前默认为约 1 个月）。

- **Q: 下载应用代码提示无权限或找不到？**  
  **A:** 仅应用创建者可下载对应代码；同时需确保已完成代码生成与部署，`tmp/code_output` 中确实存在对应目录。

- **Q: 构建后端镜像时 Playwright 下载 `chrome-linux64.zip` 很慢？**  
  **A:** `Dockerfile` 已对 `npx playwright install chromium` 使用 BuildKit 缓存挂载，**同一台机器上重复 `docker build` 时一般会复用浏览器缓存**（首次仍会完整下载）。若网络极差，可在网速好的 Linux 环境（或 WSL）进入 `scripts/`，执行 `npm ci && npx playwright install chromium`，然后把本机的 `~/.cache/ms-playwright` 整目录打成压缩包，拷到构建机后在 Dockerfile 里增加 `COPY` 到 `/root/.cache/ms-playwright`（须与 `scripts/package-lock.json` 里 **同一 Playwright 版本** 生成的目录一致，否则容易版本不匹配）。

  ```bash
  cd ~/ai-coding-station

  # 1. 拷贝到 backend 容器
  docker cp ms-playwright-linux.tgz ai-coding-backend:/tmp/ms-playwright-linux.tgz

  # 2. 进容器
  docker exec -it ai-coding-backend bash

  # 3. 在容器里解压到 Playwright 预期的缓存目录
  mkdir -p /root/.cache
  tar -xzf /tmp/ms-playwright-linux.tgz -C /root/.cache

  # 4. 可选：让 Playwright 校验一下并补齐缺失部分（很快）
  cd /app/scripts
  npx playwright install chromium

  exit
  ```bash

---

### 8. 开发规范与约定

- **后端**
  - 分层结构：`controller` / `service` / `service.impl` / `mapper` / `entity` / `dto` / `vo` / `enums`
  - 统一返回结构：`BaseResponse<T> + ResultUtils`
  - 统一异常与错误码：`BusinessException + ErrorCode`
  - 必要的业务校验通过 `BusinessAssert` 完成
  - 数据库字段使用下划线命名，实体使用驼峰命名，并使用软删除字段 `is_deleted`

- **前端**
  - 使用 Vue 3 + `<script setup>` + TypeScript
  - 状态管理统一使用 Pinia
  - 接口调用通过 `src/api` 下的封装（OpenAPI 生成 TypeScript 类型）
  - 路由定义拆分在 `src/router/routes` 中，按业务模块划分

---

### 9. 贡献指南

欢迎 Issue / PR，建议流程如下：

1. Fork 本仓库并创建特性分支，例如 `feature/xxx-module`；
2. 确保本地通过基础检查：
   - 后端：`mvn test`（如后续补充单元测试）；
   - 前端：`npm run build` 与 `npm run type-check`；
3. 提交时请使用清晰的 Commit Message，并在 PR 中说明变更目的与影响范围；
4. 如涉及数据库结构变更，请同步更新 `sql/` 初始化脚本与相关文档。

---

### X. 许可证与鸣谢

- 本项目使用 **MIT License**，详见仓库中的 `LICENSE` 文件。
- 本项目参考开源项目 [AI零代码应用生成平台](https://github.com/liyupi/yu-ai-code-mother/)。
- 本项目由 **[DavidCWQ](https://github.com/DavidCWQ)** 发起与维护。

如果本项目对你有帮助，欢迎 Star 或分享给更多需要「AI 应用工坊」的朋友。
