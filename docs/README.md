## AI Coding Station 开发文档概览

个人业余维护的 **AI 应用开发与测试记录台**，面向技术爱好者做原型、学习与自用。  
本文件聚焦工程与运行细节，帮助你快速理解后端模块划分、启动方式和常见问题。

> 面向「想改代码、想自己部署」的读者；产品介绍请看仓库根目录的 `README.md`。

---

### 目录

1. [核心功能模块](#核心功能模块)
2. [技术栈与整体架构](#技术栈与整体架构)
3. [运行与部署（开发者视角）](#运行与部署开发者视角)
4. [环境变量与配置入口](#环境变量与配置入口)
5. [主要后端模块说明](#主要后端模块说明)
6. [常见问题（FAQ）](#常见问题faq)
7. [开发规范与约定](#开发规范与约定)
8. [贡献建议](#贡献建议)

---

### 核心功能模块

- **应用工坊（App Module）**
  - 应用 CRUD、分页、精选、详情；
  - 代码生成对话，落盘默认 `tmp/code_output`，支持 ZIP 下载；
  - 部署产物进入 `tmp/code_deploy`，由 Nginx 提供预览；使用 Playwright 生成封面截图。

- **代码生成对话（Chat Module，依附于 App）**
  - 基于 LangChain4j + DeepSeek 实现；后端使用 `Flux<ServerSentEvent<String>>` 流式输出，前端通过 `useSSEChat` 消费；
  - `ChatSession` / `ChatHistory` 持久化，按 `appId` 隔离与鉴权；
  - 提供长文本 POST 接口，缓解超长对话导致的请求头限制问题。

- **内置智能体（Agent Module）**
  - 当前仅提供内置编码：`code_assistant` / `tax_assistant` / `life_advisor` / `inspiration_echo` 等，暂不开放自建入口；
  - 智能体拥有独立会话与记忆，历史分页、流式输出，与应用代码生成路径分离；
  - 每个智能体可挂载不同的 Tools（例如编程助手的面试题检索）。

- **RAG 子系统**
  - 使用 PostgreSQL + pgvector 作为向量库，DashScope 作为嵌入模型；
  - 默认索引 `classpath:rag/docs/**/*.md`，通过 `metadata.corpus` 区分不同智能体的知识语料；
  - 支持按文件替换更新（`content_hash` 变化会触发旧向量删除并重建）。

- **用户与权限（User Module）**
  - 注册 / 登录 / 登出 / 资料；普通用户与管理员；
  - 生产环境可通过配置关闭注册，仅允许已存在账号登录；
  - 部分智能体（如 `inspiration_echo`）仅管理员可用。

- **前端（Vue SPA）**
  - Vue 3 + TS + Vite + Ant Design Vue；
  - 提供首页、应用工坊、应用内聊天、智能体对话、关于页及后台入口；
  - 使用 Visual Editor 辅助编辑与预览，OpenAPI → TS（`npm run openapi2ts`）统一接口调用。

---

### 技术栈与整体架构

- **后端**
  - Java 21 + Spring Boot 3.5.x
  - Spring Web / AOP / Session Data Redis / Cache (Caffeine)
  - MyBatis-Flex（含代码生成器）
  - LangChain4j（DeepSeek Chat 对话模型，DashScope 嵌入模型）
  - Jedis / Redis / HikariCP
  - Knife4j OpenAPI 3

- **前端**
  - Vue 3 + TypeScript + Vite 7
  - Ant Design Vue 4
  - Pinia / Vue Router
  - Axios + OpenAPI TypeScript SDK（`@umijs/openapi`）

- **基础设施**
  - MySQL（业务持久化）
  - Redis（会话、对话记忆、缓存）
  - PostgreSQL + pgvector（智能体 RAG：`postgres-rag` 服务 + `sql/rag` 初始化）
  - Nginx（静态站与封面等静态资源）
  - Docker / Docker Compose（本地依赖与生产编排）

---

### 运行与部署（开发者视角）

> 以下为简要版，细节如端口、环境变量等可结合根目录 `compose.yaml` / `compose.prod.yaml` 与 `docs/config.md` 一起看。

#### 本地开发（推荐）

前置要求：

- JDK 21+、Maven 3.9+；
- Node.js 20+（前端要求 `^20.19.0 || >=22.12.0`）；
- Docker（可选，用于 MySQL / Redis / postgres-rag / Nginx）。

典型步骤：

1. 启动依赖服务（可选）  
   使用 `compose.yaml` 一键拉起 MySQL / Redis / postgres-rag / Nginx：

   ```bash
   docker compose -f compose.yaml up -d mysql redis nginx postgres-rag
   ```

2. 启动后端  

   ```bash
   ./mvnw spring-boot:run
   # 或
   mvn spring-boot:run
   ```

   默认后端地址：`http://localhost:8142/api`。  
   Knife4j 文档：`/api/doc.html`（如未改动默认配置）。

3. 启动前端  

   ```bash
   cd ai-coding-station-frontend
   npm install
   npm run dev
   ```

   默认访问：`http://localhost:5876`。  
   推荐通过 Vite 代理 `/api` 到后端，避免跨域与 Cookie 问题。

#### Docker Compose 与生产部署

- `compose.yaml`：主要用于本地开发时启动 MySQL、Redis、postgres-rag 与 Nginx。  
- `compose.prod.yaml`：包含 `backend` / `frontend` / `db-migrate` / `postgres-rag` / `nginx` 等完整编排，适合生产或自建环境部署。  
- `SPRING_PROFILES_ACTIVE=prod`：在生产中启用 `application-prod.yml`，覆盖数据源、Redis、RAG 等配置。

更多部署细节请参考：

- `compose.yaml`、`compose.prod.yaml`
- `src/main/resources/application*.yml`
- [`docs/config.md`](./config.md)

---

### 环境变量与配置入口

完整配置说明见 [`docs/config.md`](./config.md)，这里只列出索引：

- 数据库与缓存：`spring.datasource.*`、`spring.data.redis.*`、`MYSQL_*`、`REDIS_PASSWORD`、`POSTGRES_RAG_PASSWORD`；
- 对话模型：`DEEPSEEK_API_KEY`、`langchain4j.open-ai.*`；
- RAG：`ai.rag.*`、`rag.datasource.*`、`DASHSCOPE_API_KEY`；
- 护轨：`ai.guardrail.*`；
- 部署与前端：`app.deploy.*`、`app.screenshot.*`、`PUBLIC_DEPLOY_HOST`、`PUBLIC_COVERS_BASE`、`VITE_APP_API_BASE_URL`、`VITE_DEV_PROXY_TARGET` 等。

---

### 主要后端模块说明

后端按业务域分包，与 `springdoc.group-configs` 大致对应。

- **User**
  - 注册 / 登录 / 登出 / 资料；
  - 管理普通用户与管理员角色；
  - 使用 `@AuthCheck` 等注解进行鉴权，生产可通过配置关闭注册入口。

- **App**
  - 应用 CRUD、分页、精选、详情；
  - 输出 / 部署目录与预览基址见 `application.yml` 的 `app.deploy.*`；
  - 部署与封面生成逻辑集中在 service 层，默认使用 `tmp/` 目录。

- **Chat（应用代码生成）**
  - `ChatSession` / `ChatHistory`，按 `appId` 隔离；
  - 控制层 + 前端 `useSSEChat` 组成流式对话链路；
  - 按 `messageId` 向前分页，管理端可查看全局历史（如有开启）。

- **Agent（内置智能体）**
  - 控制层负责会话创建、重命名、流式对话与历史记录；
  - 智能体编码：`code_assistant`、`tax_assistant`、`life_advisor`、`inspiration_echo` 等；
  - 提示词位于 `resources/prompt/agent/*.txt`，通过 `AgentSystemPromptResolver` 解析；
  - `AgentChatServiceFactory` 将 Redis 记忆、RAG 检索、Tools 与可选护轨组装在一起。

- **`ai` 包**
  - `ai.tool`：按智能体挂载 Tools；
  - `ai.guardrail`：输入护轨；
  - `ai.rag`：RAG 配置（`RagProperties`、`RagStoreConfig`、`RagModelConfig`）、入库与检索逻辑等。

---

### 常见问题（FAQ）

- **首屏空白或 404？**  
  通常是前端未构建或 Nginx 未正确挂载 `tmp/code_deploy`。开发使用 Vite，生产需构建 `dist` 并由 Nginx 提供服务。

- **DeepSeek 无响应？**  
  检查 `DEEPSEEK_API_KEY` 是否正确配置；LangChain4j 默认会打印请求 / 响应日志，可从日志排查。

- **登录态丢失？**  
  核对 Redis 与 Spring Session 的配置；注意 Session TTL、Cookie `max-age` 等设置。

- **下载代码无权限或文件缺失？**  
  仅创建者可下载；确认已生成并检查 `tmp/code_output` 目录。

- **本地未起 postgres-rag 导致后端启动失败？**  
  默认配置要求 RAG 库可达。可使用 `compose.yaml` 启动 `postgres-rag`，或调整 `rag.datasource.*` 指向已有实例；如只做轻量调试，可在本地 profile 中关闭 `ai.rag.enabled`。

---

### 开发规范与约定

- **后端**
  - 分层结构：`controller` / `service` / `service.impl` / `mapper` / `entity` / `dto` / `vo` / `enums`；
  - 统一返回结构：`BaseResponse<T> + ResultUtils`；
  - 统一异常与错误码：`BusinessException + ErrorCode`；
  - 关键业务校验通过 `BusinessAssert` 完成；
  - 数据库字段使用下划线命名，实体使用驼峰命名，统一软删除字段 `is_deleted`。

- **前端**
  - Vue 3 + `<script setup>` + TypeScript；
  - 状态管理使用 Pinia；
  - 接口调用通过 `src/api` 下封装（OpenAPI 生成 TS 类型）；
  - 路由拆分在 `src/router/routes` 中，按业务模块划分。

---

### 贡献建议

欢迎 Issue / PR，建议流程如下：

1. Fork 本仓库并创建特性分支（如 `feature/xxx-module`）；  
2. 确保本地通过基础检查：后端 `mvn test`（如有）、前端 `npm run build` 与 `npm run type-check`；  
3. 提交时使用清晰的 Commit Message，并在 PR 中说明变更目的与影响范围；  
4. 如涉及数据库结构变更，请同步更新 `sql/` 初始化脚本与相关文档。


