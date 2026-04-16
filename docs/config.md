## 配置与环境变量说明（Config）

本文件汇总 AI Coding Station 中与运行环境相关的主要配置项，便于本地开发、测试与生产部署时查阅。  
对应的核心配置文件包括：

- `application.yml`（本地开发默认 Profile：`local`）
- `application-prod.yml`（生产 / Docker Profile：`prod`）
- `application-test.yml`（测试 Profile：`test`）
- `compose.yaml` / `compose.prod.yaml`（依赖服务与容器编排）

> 实际值请根据自己的环境与安全要求设置，以下示例仅作说明，**不要在仓库中提交真实的密钥或账号信息**。

---

### 1. 数据库与缓存（MySQL / Redis / H2）

相关配置主要分布在：

- Docker Compose：`compose.yaml` / `compose.prod.yaml` 中的环境变量；
- Spring 配置：`application.yml`、`application-prod.yml`、`application-test.yml`（以及可选的 `application-local.yml`）。

常见环境变量与配置项：

- **Compose 中常见变量**
  - `MYSQL_ROOT_PASSWORD` / `MYSQL_DATABASE` / `MYSQL_USER` / `MYSQL_PASSWORD`
  - `REDIS_PASSWORD`
  - `POSTGRES_RAG_PASSWORD`
- **Spring 中常见配置（`application.yml` 本地开发）**
  - `spring.datasource.url`：MySQL JDBC 地址，例如  
    `jdbc:mysql://localhost:3306/ai_coding_station_memo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai`
  - `spring.datasource.username` / `spring.datasource.password`：默认 `ai_user` / `ai_secret`
  - `spring.data.redis.host` / `spring.data.redis.port`：默认 `localhost:6380`
  - `spring.data.redis.password`：默认 `redis_secret`
- **测试配置（`application-test.yml`）**
  - 使用内存 H2 数据库：`jdbc:h2:mem:ai_coding_station_test;MODE=MySQL;...`
  - 关闭 Session 持久化与 RAG（`ai.rag.enabled=false`）

本地开发可以直接使用 `compose.yaml` 启动 MySQL / Redis / Nginx / postgres-rag：

```bash
docker compose -f compose.yaml up -d mysql redis nginx postgres-rag
```

---

### 2. 对话模型（DeepSeek / LangChain4j）

对话模型调用通过 LangChain4j 封装，主要配置项分布在 `application.yml` 和 `application-prod.yml` 中：

- **核心环境变量**
  - `DEEPSEEK_API_KEY`：DeepSeek 接口调用所需的 API Key。
- **Spring 中相关配置（`application.yml`）**
  - `langchain4j.open-ai.chat-model.base-url`：OpenAI 兼容接口的 Base URL（默认 `https://api.deepseek.com`）。
  - `langchain4j.open-ai.chat-model.model-name`：对话模型名称（默认 `deepseek-chat`）。
  - `langchain4j.open-ai.chat-model.max-tokens`：最大输出长度限制（默认 `8192`）。
  - `langchain4j.open-ai.streaming-chat-model.*`：流式对话模型配置。
- **生产覆盖（`application-prod.yml`）**
  - 将 `chat-model.api-key` 与 `streaming-chat-model.api-key` 绑定到 `DEEPSEEK_API_KEY` 环境变量。

推荐做法：

- 本地将 `DEEPSEEK_API_KEY` 放在 `application-local.yml` 或系统环境变量中，并加入 `.gitignore`；
- 生产环境通过 `compose.prod.yaml` 的 `environment` 字段注入。

---

### 3. 智能体 RAG（DashScope + pgvector）

智能体模块默认启用检索增强生成（RAG），相关配置主要在 `application.yml` 与 `application-prod.yml` 中，依赖：

- PostgreSQL + pgvector 作为向量存储；
- DashScope 作为嵌入模型提供方；
- `sql/rag/` 目录中的建表与初始化脚本。

#### 3.1 全局开关与基本属性（`ai.rag.*`）

对应 `RagProperties`，常见配置：

| 配置项                             | 含义                                       |
| ---------------------------------- | ------------------------------------------ |
| `ai.rag.enabled`                  | RAG 总开关（默认 `true`，测试 profile 通常为 `false`） |
| `ai.rag.ingest-on-startup`        | 启动时是否扫描 `docs-classpath-pattern` 并入库 |
| `ai.rag.cleanup-deleted`          | 是否清理已移除 classpath 文档的向量（默认 `false`） |
| `ai.rag.docs-classpath-pattern`   | 待索引文档 glob，默认 `classpath:rag/docs/**/*.md` |
| `ai.rag.embedding.api-key`        | DashScope API Key（建议从 `DASHSCOPE_API_KEY` 读取） |
| `ai.rag.embedding.model-name`     | 嵌入模型名，默认 `text-embedding-v4`        |

常见环境变量：

- `DASHSCOPE_API_KEY=<YOUR_DASHSCOPE_API_KEY>`

#### 3.2 RAG 数据源（`rag.datasource.*`）

用于连接 pgvector 数据库，常见配置：

| 配置项                     | 说明                                                                           |
| -------------------------- | ------------------------------------------------------------------------------ |
| `rag.datasource.jdbc-url` | JDBC 地址，本地通常为 `jdbc:postgresql://localhost:5432/ai_coding_vector_db` |
| `rag.datasource.username` | 用户名，与 Compose 中 `POSTGRES_RAG_*` 对齐                                    |
| `rag.datasource.password` | 密码，与 Compose 中 `POSTGRES_RAG_*` 对齐                                    |
| `rag.datasource.driver-class-name` | 一般为 `org.postgresql.Driver`                                    |

注意：

- `rag_embedding` 表的向量维度须与嵌入模型及 `RagStoreConfig` 等常量一致；
- `sql/rag/001_rag_schema.sql` 负责初始化或重建相关表结构。

---

### 4. 输入护轨（`ai.guardrail`）

输入护轨用于在请求进入大模型前进行基础过滤，例如敏感词拦截。

常见配置：

- `ai.guardrail.enabled`：是否启用护轨（布尔值）；
- `ai.guardrail.sensitive-keywords`：额外敏感词列表，通常以小写存储，匹配时忽略大小写。

根据实际业务场景可以选择开启或关闭，并调整关键词列表。

---

### 5. 部署、注册开关与前端环境

#### 5.1 部署相关（后端）

在 `application.yml` / `application-prod.yml` 中通常会有例如：

- `app.deploy.output-dir` / `app.deploy.deploy-dir`：代码生成输出与静态部署目录（默认 `${user.dir}/tmp/code_output` / `${user.dir}/tmp/code_deploy`）；
- `app.deploy.deploy-host`：本地预览使用的 Nginx 地址（本地默认为 `http://localhost:8088`，生产中在 `application-prod.yml` 中改为 `http://nginx`）；
- `app.screenshot.script-path` / `app.screenshot.output-dir` / `app.screenshot.base-url`：截图脚本路径、封面输出目录与访问基址；
- `PUBLIC_DEPLOY_HOST`、`PUBLIC_COVERS_BASE` 等环境变量：在容器环境中覆盖公开访问地址。

生产部署时，请确保：

- Nginx 的静态目录与 `tmp/code_deploy` 等路径一致；
- 外部访问域名或 IP 与这些配置匹配。

#### 5.2 注册与用户开关

常见配置：

- `app.user.registration-enabled`（本地默认 `true`）：在 `application.yml` 中配置，允许新用户注册；
- `APP_USER_REGISTRATION_ENABLED`：生产环境通过环境变量覆盖该值（在 `application-prod.yml` 中默认 `false`）。

用于控制前端注册入口与后端接口的行为，避免在公开环境中无控制地开放注册。

#### 5.3 前端环境变量（Vite）

前端使用 Vite，常见环境变量包括（参考前端根目录下的 `.env.*` 与 `vite.config.ts`）：

- `VITE_APP_API_BASE_URL`：前端请求的 API 基础路径，例如 `/api`；
- `VITE_DEV_PROXY_TARGET`：开发模式下 Vite 代理到的后端地址，例如 `http://127.0.0.1:8142`；
- `VITE_APP_DEPLOY_BASE_URL`：展示部署结果时使用的基址；
- `OPENAPI_SCHEMA_URL`：OpenAPI 文档地址，用于 `npm run openapi2ts` 生成 TS SDK。

这些变量通常分别写在：

- `.env.development`：开发环境；
- `.env.production`：生产构建环境。

---

### 6. 总结

- 主 README 更关注「项目是什么、能做什么、怎么快速跑起来」；
- 本文件和 `docs/README.md` 则详细记录各类配置项与运行细节，方便二次开发和长期维护；
- 若后续对环境依赖或架构有调整，建议同步更新本文件，以免未来自己或协作者踩坑。

