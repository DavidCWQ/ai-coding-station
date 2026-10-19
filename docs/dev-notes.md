# 开发笔记

给以后复盘用。每条尽量写成：当时看到什么、原因是什么、下次怎么做。基岩服管理台的细节在 [基岩服管理台复盘](./mc-bedrock-retrospective.md)，这里只留和网站本身交叉的结论。

笔记里不写授权码、API Key、数据库密码。这些只放本机 `application-local.yml` 或服务器 `.env`，两个文件都不进 Git。

---

## 1. 登录失败限制与邮箱重置（2026-10）

个人站点的安全边界：不做双因素认证，也不做 DDoS 防护。要挡住的是连续试密码，以及「这个账号存不存在」这种信息泄露。

### 限流怎么锁

- 按 **账号 + IP** 计连续失败，不是把整个账号锁死。同一账号从另一台机器登录不受这一组计数影响。
- 连续失败 **5 次** 后锁定 **10 分钟**。同一 IP 在 10 分钟窗口里累计失败 **30 次**，也锁定 10 分钟，用来挡换账号扫密码。
- 登录成功只清掉这一对「账号 + IP」的失败计数。
- 账号不存在和密码错误都返回「账号或密码错误」。账号不存在时仍做一次假的 BCrypt 比较，避免响应时间把账号是否存在漏出去。
- 忘记密码：同一账号 15 分钟 3 次，同一 IP 15 分钟 10 次。提交新密码：同一 IP 15 分钟 10 次。
- 计数放在现有 Redis，键前缀 `auth:login:*`、`auth:reset:*`。不要用 Jackson 那份 `RedisTemplate` 做 `INCR`，递增要用 `StringRedisTemplate`。

客户端 IP：只在 TCP 对端是本机回环或内网（Nginx、Docker）时才信 `X-Real-IP`。有人直接打到后端发布端口时，用 TCP 对端，忽略伪造头。Nginx 已经在写 `X-Real-IP`。

### 邮箱与重置令牌

- `sys_user.user_email` 可空，唯一索引 `uk_user_email`。MySQL 里多个 `NULL` 不互相冲突，旧账号不用立刻补邮箱。登录名仍然是账号。
- 邮箱存小写。绑定、更换、解除都要验证当前密码。公开的用户 VO 不带邮箱，只有登录态 VO 带。
- 解除邮箱必须用原生 SQL 把列写成 `NULL`。MyBatis-Flex 的 `updateById` 会跳过 null，看起来像保存成功，库里的旧邮箱还在。
- 重置令牌是 32 字节随机数，URL-safe Base64。Redis 里只存 SHA-256。有效期 30 分钟，用一次就删。同一账号再申请会作废上一封。
- 不论账号是否存在、是否绑定邮箱，接口都返回同一句「如果该账号已绑定邮箱，重置邮件已发送，请查收」。发信失败也记日志后返回这句，避免用「没发出去」反推出账号状态。邮件服务整体没配好时，才明确返回「邮件服务暂不可用」。
- 改密或重置成功后，旧会话里的密码哈希和下一次从数据库读到的不一致，那次请求会把登录态作废。

### 发信

- 免费 `@proton.me` 不能当公网 SMTP 账号。以前能从 Python 发出 `xxx@proton.me`，是本机 Proton Mail Bridge（付费，`127.0.0.1:1025`）。公网 `smtp.protonmail.ch:587` 也要付费套餐、自有域名和 SMTP token。这个站点用的是免费 163 邮箱，不在服务器上自建邮局。
- 163 加密端口是 **465**（隐式 SSL），不是 587。用户名和发件人必须是完整地址。密码是 16 位授权码，不是网页登录密码。QQ 邮箱才是 `smtp.qq.com:587` + STARTTLS。
- 本地开关和授权码写在 gitignore 的 `application-local.yml`。生产由 `compose.prod.yaml` 读服务器 `.env` 的 `MAIL_*` 和 `APP_PUBLIC_BASE_URL`。`SPRING_PROFILES_ACTIVE=prod` **不会**加载 `application-local.yml`。
- `.dockerignore` 要排除 `.env` 和 `application-local.yml`，否则构建镜像时会把密钥打进去。
- 重置链接的根地址：本地是 Vite 的地址。这个前端端口是 **5876**，不是 Vite 默认的 5173。生产是 `https://davidcwq.online`。配错端口时，邮件里的链接能复制，但打开的是空页面。

### 邮件里的链接点不开

`SimpleMailMessage` 是纯文本。163 网页邮箱经常把整段 URL 显示成普通文字，没有 `<a>`。

要同时发纯文本和 HTML：`MimeMessageHelper` 的第二个参数必须是 `multipart = true`，再 `setText(plain, html)`。HTML 里放一个按钮，再放一条完整地址的链接。`multipart = false` 时，`setText(纯文本, HTML)` 会在运行期抛「不在 multipart 模式」。

`MimeMessageHelper` 的构造和 `setFrom` / `setTo` / `setSubject` / `setText` 抛的是受检异常 `jakarta.mail.MessagingException`。方法签名不声明它，`mvn compile` 会直接失败，报「未报告的异常错误」。在组装邮件处接住，包成 `IllegalStateException` 再抛出。调用方本来就按 `RuntimeException` 记日志并返回那句统一提示，包一层之后这条路径还在。`JavaMailSender.send` 抛的 `MailException` 本身就是运行时异常，不用再声明。

改完发信代码后，已经在跑的 `spring-boot:run` 不会自己换成新 class。要停掉再启动，然后 **重新申请一封**。旧邮件还是纯文本。

### 前端三处容易看走眼

- 登录页必须勾选《用户须知》才提交。没勾时前端直接提示，不发登录请求。
- 「忘记密码」和「没有账号？去注册」放在同一行两端。
- 个人主页「保存邮箱」曾经点了没有任何请求。原因是 `a-form-item` 写了 `required` 却没有 `name`，Ant Design Vue 校验失败但不显示错误，`@finish` 不会跑。表单项要有 `name`，按钮用点击处理函数，不要只靠校验通过才提交。

### 本地库不会因为重启 Docker 而执行新 SQL

`compose.yaml` 把 `./sql` 挂到 `docker-entrypoint-initdb.d`，**只在数据卷第一次初始化时**执行。已有 `mysql_data` 时，按 Docker Desktop 的启动按钮不会重放 `sql/006_add_user_email.sql`。

生产才有 `db-migrate`，部署时重放 `sql/*.sql`（`002` 不是幂等的，会被跳过）。`006` 用 `information_schema` 判断列和索引是否已在，可以重复执行。

在 Windows 上灌这段 SQL：

- PowerShell 管道进 `docker exec` 会吃掉反引号，MySQL 报 1064。
- `mysql -e "source ..."` 不行，`SOURCE` 是客户端命令，不是服务器语句。
- `sh -c "mysql ... < file"` 时，重定向常常没有真正进到容器，mysql 会卡在等标准输入。
- 可用的方式是 `cmd /c` 把文件重定向进 `docker exec -i`。

改完后自己查一次：`user_email` 可空，并且存在 `uk_user_email`。

### 不要覆盖本机密钥文件

给本地补邮件配置时，只追加 `app.mail`。`application-local.yml` 里已有的对话模型和嵌入模型密钥不能被整文件替换。这个文件被 gitignore，覆盖后 Git 也找不回来，只能从编辑器本地历史里捞。

---

## 2. 本地开发环境

- 日常 Git 在 **WSL** 里用。Windows 的 PowerShell 没有 `gh`，也不要为了建一个 issue 去装一套 CLI。`&` 在 PowerShell 里是调用运算符，不是 bash 的命令分隔符。
- 前端在 WSL、后端在 Windows 时，Vite 代理目标是 Windows 主机在 WSL 里的地址（当前是 `172.25.160.1:8142`），不是 `127.0.0.1`。`127.0.0.1` 指向的是 WSL 自己，`openapi2ts` 和页面请求都会连错。开发代理写在 `.env.development`，生产写在 `.env.production`。
- 浏览器里的登录 Cookie 走 Vite 同源代理。代理目标机器变了，页面能开、接口却失败。
- PowerShell 里跑 `mvn -Dtest=A,B` 时，逗号会被当成数组。用 `.\mvnw.cmd "-Dtest=A,B" test`。本机 PATH 上不一定有 `mvn`，用仓库里的 `mvnw`。
- `vue-tsc --build` 可能长时间不退出。类型检查用 `npx vue-tsc --noEmit -p tsconfig.app.json`。
- Windows 上 Spring DevTools 热重启配 Lombok `@Data`，可能生成空方法字节码，启动报 `ClassFormatError: Invalid method Code length 0`。`InputGuardrailProperties`、`McBedrockProperties`、`AppMailProperties` 都因此改成手写 getter/setter，并写一个空的显式构造器。不要只靠热重启：改完先停掉 `spring-boot:run`，再 `compile` 后重新启动。
- `mvn clean` 报「另一个程序正在使用此文件」时，先确认是不是本仓库的 `spring-boot:run` 还在。应用进程停了之后仍可能失败：Cursor 自带的 Java 语言服务会在文件刚复制进 `target\classes` 时打开它，中文文件名的 RAG 笔记最容易撞上。这个语言服务进程不要关，过几秒再执行一次 `compile` 即可。
- 缺 `hibernate-validator` 时，启动会报找不到 Jakarta Bean Validation 的实现。

---

## 3. 部署、证书、Nginx

- 容器内的后端不写宿主机端口。Nginx 反代到 compose 服务名和容器端口。对外只开 80/443。域名根路径就要能打开，不要强迫用户写端口。
- 证书放在服务器上，不进 Git，也不要打开 `nginx/ssl/` 里的文件来「确认一下」。Let's Encrypt 用 webroot + certbot。占位的自签名证书和正式证书的 lineage 不是同一套，续期配置要指向正式那份。
- GitHub CD 会重新 `docker compose up`。证书目录必须是独立挂载，部署不能把刚签好的证书覆盖成仓库里的占位文件。
- 用户说的「SSH 证书过期」通常是 **TLS 证书**过期，不是 SSH 登录密钥。
- 有 SSL 之后，用 HTTP 或旧端口（例如以前的 `:8088`）会一起失效。先看 `docker ps` 和 `nginx-proxy` 日志，再判断是证书、端口映射，还是容器没起来。
- 构建镜像时 apt/npm 层缓存失效，会像「卡了好几天」一样重新下载。看的是 Dockerfile 那一层的输入变没变，不是时间本身。
- 生产 RAG 用 PostgreSQL + pgvector，backend 要等数据库就绪。RAG 已经是站点能力的一部分，不是可选旁路。
- 发版后如果出现「智能体系统内部异常」或新表不存在，先对一下 `sql/` 是否在这次部署里执行过。已有卷不会自动吃新脚本。
- `mvnw` 必须进仓库。CI 里 `chmod +x mvnw` 找不到文件，就是它没被提交。分支保护要求的检查名要和 workflow 里的 job 名一致；workflow 只在 `dev` 上时，`main` 会一直停在 Expected。

---

## 4. 应用工坊

- 精选应用的历史：未登录不能看。登录后的用户和管理员都能看所有精选应用的历史。只有应用所有者能继续对话，其他人提示无法在别人的对话下接着改。管理员曾经被额外拦住，是权限判断把「查看」和「继续写」绑在一起了。
- 应用编辑页不要带全站页脚。布局开关要作用到真正包住该路由的那一层，只改页面组件本身时页脚还会出现。
- 精选列表按 `priority` 从大到小。我的应用按修改时间，两条排序不要混用。
- 删除应用先做软删除，用 `update_time` 算超过 14 天，再由定时任务清磁盘上的生成文件和封面。不要为这件事再加一列 `delete_at`。关联的对话也按 `app_id` 逻辑删除。
- 部署页有时还是旧版，预览和截图却是新的：浏览器缓存了 `index.html` 或脚本。强制刷新能好，说明文件已经更新。部署目录的 HTML 要给短缓存或协商缓存，不能和带指纹的静态资源用同一条长期缓存。
- 精选卡片封面空白：用域名打开站点时，前端生产环境变量仍指向旧的 IP 或协议，图片请求被拦。先对 `.env.production`，再看 Nginx 是否把 `/api` 和静态部署目录分开。
- 截图服务在后端容器里用 Playwright / Node，和前端构建不是同一份 `node_modules`。新页面一直是占位图时，看容器日志里截图命令是否失败，以及部署目录是否已经有新的 `index.html`。
- 对话里替换页面图片：上传文件落到生成目录，部署时复制到部署目录，再按页面里的路径替换。不要把用户图片写进仓库。

---

## 5. 智能体与 RAG

- 应用生成和智能体对话是两条链路。当前只有内置智能体，没有开放「用户自己创建一个智能体」。
- 从旧的 assistant 仓库迁进来时，RAG、工具、护栏放在现有 `ai/` 包下，按职责分子目录，不要另起一套平行框架。DTO 上不要重复声明同一个字段（出现过 `sessionId` 重复导致编译失败）。
- 检索配置和存储配置看起来像，职责不同：一个负责把文档放进 pgvector，一个负责按智能体取回。合并前先确认调用点，避免只剩一条路径却丢了过滤条件。
- 个人笔记语料：几十个字的条目整段一条向量，不要按固定窗口硬切。中等长度按自然段或 Markdown 标题切，单段上限大约 2000 字。metadata 里留稳定的笔记 id、来源和类型，方便去重和回溯。
- 「灵感回声」只对管理员显示。提示词写成和过去的自己对话，检索不够时说明边界即可，不要写成客服拒答。
- 嵌入和对话不是同一个密钥、也不是同一个供应商。本地对话走 DeepSeek，嵌入走 DashScope。模型名升级时只改配置里的模型 id 和上下文长度，不要把两套密钥抄混。

---

## 6. 测试与其它容易忘的点

- 依赖外部模型或浏览器的测试不要放进默认 CI 门禁，单独一条任务跑。覆盖率门槛改过之后，新增大块代码要把对应单测补上，否则 JaCoCo 会掉到线下面。
- `@MockBean` 在 Spring Boot 3.4 之后过时，新测试改用当前推荐的 mock 方式，避免整仓测试类被标成弃用。
- 改密码成功后作废当前 HTTP 会话，这是重置密码之外、更早一次就定下的策略。
- 页脚备案号链到工信部。公安备案图标和文字都要能点。同一行里图标和文字的对齐用 flex 的居中，不要只调图标尺寸。
- 站点对外表述保持「个人业余维护的 AI 应用开发与测试记录」，标题和关于页不要写成商业产品，也不要堆概念。

---

## 7. 下次改这块时先看哪里

| 要改的事 | 先打开 |
|---|---|
| 登录次数、锁定时间 | `user/security/AuthThrottle.java` |
| 重置链接有效期、一次性 | `PasswordResetTokenStore.java` |
| 邮件长什么样、SMTP 端口 | `PasswordResetMailer.java`、`PasswordResetMailConfig.java`、`application.yml` 的 `app.mail` |
| 本地邮件开关与密钥 | `src/main/resources/application-local.yml`（不要提交） |
| 生产邮件与公网根地址 | 服务器 `.env`，对照 `compose.prod.yaml` |
| 邮箱列 | `sql/006_add_user_email.sql` |
| 登录勾选、忘记密码入口 | `views/user/login.vue` |
| 绑定邮箱 | `views/user/profile.vue` |
| 基岩服 | [基岩服管理台复盘](./mc-bedrock-retrospective.md) |
