# AI Coding Station

**AI Coding Station**：个人业余维护的 **AI 应用开发与测试记录台**，面向技术爱好者做原型、学习与自用。站内 **应用工坊**（创建—编辑—对话—部署）与 **智能体对话** 相互独立，侧重把想法做成可用小工具、以及与 AI 日常协作。

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

**个人向记录台 + 开放实验场**：应用工坊里搭应用、编辑部署；兼作技术博客。能力随业余时间迭代，适合 **自用、小范围试用、读源码**。

- **应用工坊**：创建 **AI 应用**，在 **与应用绑定的会话** 里用 DeepSeek Chat 生成前后端骨架；ZIP 下载、部署到 Nginx、预览链接与封面截图。
- **内置智能体**：选用平台已有角色（编程助手、财税助理、问道先生；管理员另有「灵感回声」），**独立会话与记忆**；可选 **RAG**（pgvector，见 §5）。**应用 ≠ 智能体**：建应用不会创建智能体；**当前仅内置列表**，日后或支持自建 / 配置。

**工程形态**：前后端分离（Spring Boot + Vue 3），依赖可用 Docker Compose 拉起。

---

### 2. 功能特性

- **应用工坊（App Module）**
  - 应用 CRUD、分页、精选、详情
  - **代码生成对话**：落盘默认 `tmp/code_output`，**ZIP** 下载
  - **部署**：产物进 `tmp/code_deploy`，Nginx 预览；**Playwright** 封面（路径见配置）
- **代码生成对话（Chat Module，依附于 App）**
  - LangChain4j + DeepSeek；SSE（后端 `Flux<ServerSentEvent<String>>`，前端 `useSSEChat`）
  - `ChatSession` / `ChatHistory` 持久化，按 `appId` 隔离与鉴权
  - **长文本 POST**，缓解超长对话请求头限制
- **内置智能体（Agent Module）**
  - 编码仅限内置：`code_assistant` / `tax_assistant` / `life_advisor` / `inspiration_echo`；**暂无自建入口**，日后或开放
  - **独立会话**（Redis 记忆与代码生成会话隔离）、历史分页、SSE
  - **按智能体挂载 Tools**（如编程助手的面试题检索）
  - **可选 RAG**：PostgreSQL + pgvector + DashScope 嵌入；默认索引 `classpath:rag/docs/**/*.md`；`metadata.corpus` 按智能体隔离
  - **输入输出护轨**（可关）：敏感词等前置拦截
- **用户与权限（User Module）**
  - 注册 / 登录 / 资料；用户与 **管理员**；生产可 **关闭注册**
  - `inspiration_echo` 等仅管理员
- **前端（Vue SPA）**
  - Vue 3 + TS + Vite + Ant Design Vue；首页、应用、关于等
  - **Visual Editor**（`visualEditor.ts` 等）辅助编辑与预览
  - OpenAPI → TS（`npm run openapi2ts`），`src/api` 统一调用
- **工程化与运维**
  - **业务主库**：MySQL + MyBatis-Flex；Redis（Session、记忆、缓存）+ 本地 **Caffeine**
  - **本地运行**：`compose.yaml`：MySQL、Redis、Nginx；**可选** `postgres-rag`（pgvector + `sql/rag`）
  - **生产编排**：`compose.prod.yaml`：`backend`、`frontend`、健康检查、可选 `postgres-rag`
  - **快速测试**：Knife4j：OpenAPI 3，按 `user` / `app` / `chat` / `agent` 分组

---

### 3. 技术栈与整体架构

- **后端**
  - Java 21
  - Spring Boot 3.5.x
  - Spring Web / Spring AOP / Spring Session Data Redis / Spring Cache (Caffeine)
  - MyBatis-Flex（含代码生成器）
  - LangChain4j（OpenAI 兼容接口接 **DeepSeek Chat**；可选 **DashScope** 嵌入模型供 RAG）
  - Jedis / Redis / HikariCP
  - Knife4j OpenAPI 3
- **前端**
  - Vue 3 + TypeScript
  - Vite 7
  - Ant Design Vue 4
  - Pinia / Vue Router
  - Axios + OpenAPI TypeScript SDK（`@umijs/openapi`）
- **基础设施**
  - MySQL（业务持久化）
  - Redis（会话、对话记忆、缓存）
  - **可选** PostgreSQL + **pgvector**（仅智能体 RAG：`postgres-rag` 服务 + `sql/rag` 初始化）
  - Nginx（已部署静态站、封面等）
  - Docker / Docker Compose（本地依赖与生产编排）
- 浏览器（前端 Vue SPA）
→ **Axios** 调用后端 `/api/`**（REST / SSE）
→ **Spring Boot**（业务域：`user`/`app`/`chat`/`agent` 等；智能体相关 Bean 在 `ai.rag.enabled=false` 时不加载 RAG 子系统）
→ **MySQL**（业务数据）/ **Redis**（Session、对话记忆）/ **本地目录**（`tmp/code_output|code_deploy|covers`）
→ **PostgreSQL(pgvector)** 仅在为智能体启用 RAG 时使用
→ **Nginx** 对外提供已部署静态站与封面等静态资源

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

如需启用智能体 RAG（可选），再额外启动 pgvector：

```bash
docker compose -f compose.yaml up -d postgres-rag
```

并确保本地配置包含：

- `ai.rag.enabled=true`
- `ai.rag.embedding.api-key=<YOUR DASHSCOPE API KEY>`
- `rag.datasource.jdbc-url=jdbc:postgresql://localhost:5432/ai_coding_vector_db`

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

Vite 默认 `http://localhost:5876`。联调建议：`VITE_APP_API_BASE_URL=/api`、`VITE_DEV_PROXY_TARGET=http://127.0.0.1:8142`（或你的后端），由 Vite 代理避免跨域与 Cookie 问题。

---

#### 4.2 使用 Docker Compose 一键启动（本地 / 测试环境）

根目录已提供标准的 `compose.yaml`，主要包含：

- `mysql`：数据库服务，挂载 `sql/` 目录自动初始化；
- `redis`：缓存与 Session 存储；
- `nginx`：前端静态站占位（开发模式下可仅用于静态文件）；
- `postgres-rag`：可选 RAG 向量库（pgvector），挂载 `sql/rag` 初始化脚本；

启动方式：

```bash
docker compose -f compose.yaml up -d
```

此编排不构建后端 Jar；后端容器见下一节。

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

浏览器请走 `/api`，由 Nginx 转发到 `backend:8142`，勿直连容器内后端地址。

---

### 5. 环境变量与配置说明

根目录 `.env`、`application*.yml`、Compose 编排。分节：**MySQL/Redis** → **DeepSeek** → **可选 RAG** → **护轨与部署**。

#### 5.1 数据库与缓存（MySQL / Redis）

- Compose：`MYSQL_*`、`REDIS_PASSWORD` 等，默认值见 `compose.yaml`。
- JDBC / Redis 地址：`application.yml`（本地）与 `application-prod.yml`（容器内 `mysql`、`redis`）。

#### 5.2 对话模型（DeepSeek / LangChain4j）

- `**DEEPSEEK_API_KEY`**：生产经 `compose.prod.yaml` 注入，对应 `application-prod.yml` 里 chat / streaming 的 `api-key`。
- 本地多在 `**application-local.yml**`（勿提交真实 Key）。
- 其余见 `application.yml` 的 `langchain4j.open-ai.*`（`base-url`、`model-name`、`max-tokens`、日志等）。

#### 5.3 可选：智能体 RAG（DashScope + pgvector）

`**ai.rag.enabled=true**` 时加载 RAG Bean（独立数据源、`RagIngestService`、`ContentRetrieverFactory` 等）。需 PostgreSQL **pgvector**、表结构就绪；`sql/rag/` 由 `**postgres-rag`** 首次启动执行。

`**ai.rag.*`（`RagProperties`）**


| 配置项                             | 含义                                       |
| ------------------------------- | ---------------------------------------- |
| `ai.rag.enabled`                | 总开关                                      |
| `ai.rag.ingest-on-startup`      | 启动时扫描 `docs-classpath-pattern` 并入库       |
| `ai.rag.cleanup-deleted`        | 是否清理已移除 classpath 文档的向量（默认 `false`）      |
| `ai.rag.docs-classpath-pattern` | 待索引 glob，默认 `classpath:rag/docs/**/*.md` |
| `ai.rag.embedding.api-key`      | DashScope，建议 `**DASHSCOPE_API_KEY`**     |
| `ai.rag.embedding.model-name`   | 默认 `text-embedding-v4`                   |


`**rag.datasource.*`（仅 RAG 开启时）**


| 配置项                     | 说明                                                                         |
| ----------------------- | -------------------------------------------------------------------------- |
| `jdbc-url`              | 本地见 `application.yml`；生产指向 `**postgres-rag:5432`**（`application-prod.yml`） |
| `username` / `password` | 与 Compose `**POSTGRES_RAG_PASSWORD**` 等对齐                                  |
| `driver-class-name`     | `org.postgresql.Driver`                                                    |


`**rag_embedding` 维度**须与嵌入模型及 `RagStoreConfig` 常量一致；表结构变更见 `sql/rag` 与 FAQ（`embedding_id`）。

#### 5.4 输入护轨（`ai.guardrail`）

- `enabled`：开关。`sensitive-keywords`：额外敏感词（小写存，匹配忽略大小写）。

#### 5.5 部署、注册与前端

- `**PUBLIC_DEPLOY_HOST`**、`**PUBLIC_COVERS_BASE**`：部署页与封面基址（见 `application-prod.yml` 中 `app.deploy` / `app.screenshot`）。
- `**SPRING_PROFILES_ACTIVE=prod**`；`**APP_USER_REGISTRATION_ENABLED**`：注册开关（生产 Compose 常 `false`）。
- 前端 `.env.development` / `.env.production`：`VITE_APP_API_BASE_URL=/api`、`VITE_DEV_PROXY_TARGET`、`VITE_APP_DEPLOY_BASE_URL`、`OPENAPI_SCHEMA_URL`（`openapi2ts`）。

**详见**：`application.yml`、`application-prod.yml`、`compose.yaml`、`compose.prod.yaml`。

---

### 6. 主要功能模块说明

后端按业务域分包，与 `springdoc.group-configs` 大致对应。

#### 6.1 User

注册 / 登录 / 登出 / 资料；普通用户与管理员；`**@AuthCheck`** 等鉴权；生产可关注册（`app.user.registration-enabled`）。

#### 6.2 App

应用 CRUD、分页、精选、详情；输出 / 部署目录与预览基址见 `application.yml` 的 `app.deploy`；部署与封面逻辑在 **service**（默认 `tmp/`）。

#### 6.3 Chat（应用代码生成）

`ChatSession` / `ChatHistory`，按 `**appId`** 隔离；`chat.controller` + 前端 `**useSSEChat**`；按 `**messageId**` 向前分页；管理端可查全局历史。

#### 6.4 Agent（内置智能体）

`agent.controller`：会话、重命名、流式、历史。编码：`code_assistant`、`tax_assistant`、`life_advisor`、`inspiration_echo`（展示名 `**AgentCodeEnum**`）。`**inspiration_echo**` 仅管理员。提示词 `resources/prompt/agent/*.txt`，`**AgentSystemPromptResolver**` 解析。`**AgentChatServiceFactory**`：Redis 记忆、可选 RAG `**ContentRetriever**`、`**AgentToolRegistry**`、可选护轨。表与 Redis key 与 Chat 路径分离。

#### 6.5 `ai` 包

- `**ai.tool**`：按智能体选 Tools。  
- `**ai.guardrail**`：输入护轨。  
- `**ai.rag**`（`enabled=true`）：`config`（`RagProperties`、`RagStoreConfig`、`RagModelConfig`）、`ingest`（`content_hash`、按文件 replace）、`retriever`（`metadata.corpus`）、`repository`（与 `rag_embedding` 协同；细节见代码与 FAQ）。

#### 6.6 前端

路由含首页、关于、用户、应用工坊、应用内聊天、智能体对话、后台等；Pinia；`src/api` + OpenAPI 生成类型。

不用 RAG 时设 `**ai.rag.enabled=false**`，Compose 可不启 `**postgres-rag**`。

---

### 7. 常见问题（FAQ）

- **Q: 首次启动前端空白或 404？**  
**A:** 开发用 Vite；生产需已构建 `dist` 并挂到 Nginx（`frontend-build` 卷）。
- **Q: DeepSeek 无响应？**  
**A:** 查 `DEEPSEEK_API_KEY`；后端 LangChain4j 默认打请求/响应日志。
- **Q: 登录态丢失？**  
**A:** 对齐 Redis 与 Spring Session；看 `session`、`cookie.max-age`（当前约一月）。
- **Q: 下载代码无权限或找不到？**  
**A:** 仅创建者可下；确认已生成且 `tmp/code_output` 有目录。
- **Q: 不想装 PostgreSQL / pgvector？**  
**A:** `**ai.rag.enabled=false`**，可不启 `**postgres-rag**`；智能体仍可用，无 RAG。
- **Q: RAG 报 `embedding_id` 列不存在？**  
**A:** 执行最新 `sql/rag/001_rag_schema.sql`（重建脚本，注意 DROP）。
- **Q: 改文档为何整文件重建向量？**  
**A:** 按文件 replace：`content_hash` 变则删旧向量再全量重建。
- **Q: 镜像构建时 Playwright 下载很慢？**  
**A:** Dockerfile 已用 BuildKit 缓存，重复 build 常能复用。极差网络可预下载 `chromium` 缓存拷到 `docker/ms-playwright/`（版本须与 `scripts/package-lock.json` 中 Playwright 一致）。

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

如果本项目对你有帮助，欢迎 Star 或分享给同样在折腾 AI 应用与智能体的开发者。