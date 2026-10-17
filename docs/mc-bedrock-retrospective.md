# 基岩服管理台：开发 / 调试 / 运维复盘

本文记录「网站管基岩服、游戏进程独立部署」这一轮的架构约定、踩坑和结论，方便以后做类似项目时对照，避免再走一遍。

- 网站仓库：`project_ai-coding-station`（页面、Java API、GitHub CD）
- 游戏目录：`mc_bedrock`（BDS、Compose、脚本、存档；**不进**网站 CD）
- 协议参考：[Bedrock Wiki · RakNet](https://wiki.bedrock.dev/servers/raknet)

---

## 1. 先定边界：两套系统，不要揉成一份 Compose

### 结论

Minecraft 官方 C++ 端（BDS）必须和网站的 MySQL / Java / Nginx **进程隔离、编排隔离、发布隔离**。

| 项 | 网站 | 基岩服 |
|---|---|---|
| 目录 | `project_ai-coding-station` | `mc_bedrock`（本机 `D:\12_Coding\mc_bedrock`，线上常为 `~/bedrock` 或 `~/mc-bedrock-server`） |
| Compose | `compose.prod.yaml` | 自己的 `compose.yaml`，`name: mc-bedrock` |
| 发布 | GitHub CD | 本机 `scp` 脚本 / compose，**不要** `scp -r` 整个目录覆盖 `data/`、`backups/` |
| Nginx | 只做 80/443 | **游戏流量不经过 Nginx**，不必为 ping 改 `nginx.prod.conf` |

### 为什么

- BDS 吃内存（容器上限 1GB 仍会挤 Swap）。和网站抢同一套编排，一次 OOM 会拖垮会话和数据库。
- 网站 CD 重发会重启 Java / 前端，不应连带停游戏、更不应覆盖存档。
- 安全组端口（TCP/UDP 19132、UDP 19140–19143）和网站 443 是两回事。

### 以后怎么做

- 管理台只 **读挂载目录 + 写 ops 队列**；真正的 `docker compose up/stop` 由宿主机 worker 执行。
- 生产 `MC_BEDROCK_HOST_DIR` 挂进 Java 容器（如 `/opt/bedrock`），`application-prod.yml` 里 `exec-scripts: false`。
- 改 compose / 脚本后：只 scp 那几个文件，再 `docker compose up -d`（必要时 `--force-recreate`）。

---

## 2. 网络：NetherNet ≠ 旧版「只开 UDP 19132」

1.26.50 起 BDS 默认 NetherNet。客户端填的 **19132 是 TCP 握手**，真正玩走 **UDP 19140–19143**。只放行旧的 UDP 19132 时，手机会报 `InitialConnection-13`，人进不去。

加服列表上的「延迟」**不是 ICMP**，也不是 TCP 探活，而是旧协议 [RakNet Unconnected Ping](https://wiki.bedrock.dev/servers/raknet)（包头 `0x01`，Magic `00ffff00fefefefefdfdfdfd12345678`），服务器回 Unconnected Pong（`0x1c` + MOTD 字符串）。

NetherNet 的 BDS **不听、也不回答** 这个 UDP 19132 探测。所以会出现：

- 游戏能进、不卡 → TCP 19132 + 游戏 UDP 已通
- 列表一直 `Loading Ping` → UDP 19132 没有合法 Pong 回到手机

### 做过但无效 / 有害的做法

- 用系统 `ping`（ICMP）判断「延迟好不好」：腾讯云常禁 ICMP，失败说明不了问题。
- 让 ping 进程和 BDS `network_mode: service:bds` 共用网卡：BDS 仍可能占住 UDP 19132，探测包进了 BDS 而不是 RakNet 应答进程。
- 以为网站 Nginx 没放行 ping：Nginx 根本不经手 19132。

### 有效做法

- BDS 只映射：`19132/tcp`、`19140-19143/udp`。
- 独立容器 `bedrock-ping` **独占** `19132/udp`，用小脚本回 Unconnected Pong（双栈绑定 `::`，失败再退回 `0.0.0.0`）。
- 腾讯云安全组 **和** 1Panel 防火墙都放行：TCP 19132、UDP 19132、UDP 19140–19143。
- 改完 compose 必须 `docker compose up -d --force-recreate`，否则旧的 UDP 19132 还挂在 BDS 上。
- 本机自检：对 `127.0.0.1` 发 Unconnected Ping，看是否收到 `0x1c`。

`SERVER_UDP_PORTS` 要用逐个端口写法，并带 `<public_ip>`（镜像启动时替换）。区间写法在 1.26.52 有 bug，手机会拿到容器内网 IP。

---

## 3. 管理台状态：`runtime.json` + inbox，不要用「当前存档」冒充「在跑」

### 数据源

- `ops/runtime.json`：`running`、`phase`（`SLEEPING|STARTING|RUNNING|STOPPING|BACKING_UP`）、`joinHint`、`updatedAt`
- `ops/inbox/*.json`：网站写入的 START/STOP/BACKUP/ACTIVATE…，宿主机 `ops-worker` 消费后删掉并写 `outbox`

### 踩过的坑

**卡片角标「运行中」**  
那是「当前选中的 `LEVEL_NAME`」，不是 Docker 在跑。沉睡时当前卡也应显示「已沉睡」，只有 `runPhase === RUNNING` 才写「运行中」。

**启动成功后又变回「世界沉睡」**  
`.env` 里 `SERVER_NAME="My World v6.0"`（带引号）。旧 worker 用 `printf` 拼 JSON，变成非法的 `"joinHint":""My World v6.0""`。Java 一解析失败就退回沉睡。  
应用 **Python `json.dump`** 写 runtime，并去掉 `.env` 值两端的引号。

**过渡态被立刻覆盖**  
worker 每次 drain 先 `write_runtime()`：容器还没起来就写成 `SLEEPING`，把网站刚写的 `STARTING` 冲掉。应在短窗口内保留 `STARTING` / `BACKING_UP`，容器 `Running=true` 再升为 `RUNNING`。

**本地点冷备后一直「正在冷备」**  
Java 打完包仍把 phase 写成 `BACKING_UP`，再往 inbox 丢 `BACKUP`。本地没有 cron worker，inbox 越堆越多，读控制台时只要看到未完成的 BACKUP 就显示正在冷备。  
这不是默认态。默认（没有 runtime、没有 inbox）是 **世界沉睡**。  
线上 worker 正常时，一两分钟会回到 RUNNING/SLEEPING；worker 停了会和本地一样卡住。  
正确：站内冷备做完立刻写回 RUNNING/SLEEPING，不要再丢一条永远没人收的 BACKUP；inbox 工单超过约 3 分钟应视为过期。

**`if (running) phase = RUNNING`**  
容器在跑时以 running 为准是对的；容器没跑时不要把文件里残留的 BACKING_UP 当永久状态，看 `updatedAt` 是否过期。

---

## 4. 时间显示：先分清「信什么」，再防序列化

### 字段含义（约定）

| 字段 | 含义 | 可信来源 |
|---|---|---|
| 上次改动 | 这个存档最近一次被游戏写入 | 当前且在跑 → 现在；否则 `level.dat` 的 `LastPlayed`；再否则存档内文件 mtime（年份正常才用） |
| 上次冷备 | 最近一次成功打包 | 文件名 `worlds-YYYYMMDD-HHMMSS` 或卡片文件夹名里的同样戳；**不要**只信 zip 的 mtime |
| （可选）上次启动 | 进程状态刷新 | `runtime.json` 的 `updatedAt` |

不要用世界**文件夹自己的** mtime 当「上次改动」：复制、解压、Docker 挂载经常是脏的。

冷备是「全服最新一份包」还是「这个世界自己的那次」，要在产品里说清楚。当前实现：当前世界用 `backups/` 里最新一份；带时间戳的克隆卡片用文件夹名。

### 1793 年 / 「尚无记录」

两件事叠在一起：

1. **脏时间戳**：错误单位或异常 FileTime，被当成毫秒后年份离谱（曾显示 1793）。护栏：秒级（`< 1e12`）先乘 1000；年份不在 2015–2100 视为 0。
2. **护栏生效后变空白**：本地只有 `levelname.txt`、没有 `level.dat`、没有 `worlds-*.tar.gz` 时，第一优先级全是空的，页面就「尚无记录」。这不是 mtime API 坏了，是夹具不像真存档。
3. **接口数字其实是对的，页面仍空白**：全局 Jackson 把 `Long` 序列化成字符串（防 JS 精度丢失，见 `JsonConfig`）。`dayjs("1792221911158")` **不会**当成 Unix 毫秒，得到 Invalid Date，护栏再写成「尚无记录」。前端必须先 `Number(...)` 再 `dayjs`。

对账方法：看 `/api/mc/admin/console` 里的 `lastModifiedEpochMs` / `lastBackupEpochMs`。  
`1792183681000` 这类值是 2026-10-17 的合法毫秒；`0` / `null` 才是没数据。

### 本地夹具

网站克隆/起名只会写 `levelname.txt`。真基岩存档至少要有带 8 字节头的 `level.dat`（小端 NBT，`LastPlayed` 为 TAG_Long）和 `db/`。  
上传接口也强制压缩包里有 `level.dat`。本地调试应解一份真实 `.mcworld` / 冷备进去，不要只靠空壳文件夹。

---

## 5. 存档、冷备、上传

- 冷备：停服打包更稳；管理台也可以在能看到 `data/` 时直接打 zip，并 **clone 当前世界** 成一张新卡片（文件夹名带 `YYYYMMDD-HHMMSS`）。
- 备份列表文件名约定：`worlds-YYYYMMDD-HHMMSS.tar.gz`（或 `.zip`）。下载的 `.mcworld` 只要文件名里带同一时间戳，也可以进列表。
- 定时冷备 cron：每天 **04:48（UTC+8）**，不是口头「夜里 4 点」。
- 删除：当前世界不能删；备份只允许 `backups/` 下合法文件名。
- 权限 / 白名单热更：BDS 要 `permission reload`、`allowlist reload`，不是 `permission list`。
- 默认玩法：`PLAYER_IDLE_TIMEOUT=60`；不要默认开 keepinventory；PVP、游玩天数默认关；摔落/火焰/溺水在目录里写清 hint。

---

## 6. Windows 本机：Lombok + Eclipse 增量编译 + DevTools

反复出现的不是业务逻辑错，而是 **target 里的 class 被 IDE 写坏**：

- `ClassFormatError` / 构造器 Code length 0（`@Data` 生成的空 `<init>`）
- `The Java feature 'Flexible Constructor Bodies' is only available with source level 25`
- Surefire `VerifyError: Control flow falls through code end`（`XxxBuilder.<init>`）
- Maven 却提示 `Nothing to compile - all classes are up to date`，于是一直跑坏字节码

### 办法

- 关键配置类不要依赖会在本机编坏的 Lombok `@Data` / `@RequiredArgsConstructor`，手写 getter/setter/构造器。
- 单测前删掉 `target/classes/.../mc`（或 `mvn clean`）。`maven-clean-plugin` 在 Windows 上对锁住的 `target` 加 `retryOnError`。
- `spring.devtools.restart.enabled=false`，避免 RestartClassLoader 再加载半成品 class。
- CI 以 **Maven javac** 为准，不要信本机 Eclipse 增量编译。

其它本机噪音：Ctrl+C 之后 Java 锁住 `target`，clean 失败；JaCoCo 行覆盖因新模块行数上涨跌破阈值，补单测或只调该模块阈值；`vue-tsc` 对可能为 `undefined` 的元组取值要给 fallback。

---

## 7. 内存与小机器

3.3GB / 4 核、默认无 Swap 时：

- 先加 **2GB Swap**，`vm.swappiness=10`，宁可慢，不要把 MySQL/Java 杀掉。
- BDS：`mem_limit` 1g，人数 2，视距 8，模拟距离 6，`MAX_THREADS=2`。
- ping 容器：32m + 0.2 CPU 足够。
- 管理台轮询：启动/停止/冷备中 3s，平时 20s，不要一直 3s 打满。

---

## 8. 发布检查清单（两处都要动时）

**A. 网站（前端 + Java）**  
走平时的 CD。改卡片文案、时间格式、`runtime` 解析、上传，都在这里。

**B. 基岩服目录（不要碰 `data/`、`backups/`）**

```text
scp compose.yaml scripts/ops-worker.sh scripts/raknet-ping.py scripts/status.sh
chmod +x scripts/*.sh
docker compose up -d --force-recreate
docker compose logs --tail=30 ping
cat ops/runtime.json
./scripts/status.sh
```

`runtime.json` 在世界运行时应为 `"running": true, "phase": "RUNNING"`，`joinHint` 不能多层引号。  
ping 日志应有 `listening`。再用手机加服看延迟是否从 Loading 变成毫秒。

只拉网站、不更新 `mc_bedrock`：游戏和 ping 行为不会变。  
只更新 `mc_bedrock`、不发网站：卡片角标、dayjs 字符串时间、冷备收尾逻辑不会变。

---

## 9. 以后做「网站管游戏服」可直接套用的原则

1. **游戏进程独立编排**，网站只挂载数据目录 + 投递队列。
2. **先画端口表**：握手 / 玩法 / 列表探测往往不是同一个协议、同一个端口；ICMP 不能代表游戏延迟。
3. **状态机要有超时**：STARTING / BACKING_UP 必须能回到 RUNNING 或 SLEEPING，不能依赖「本地一定有 worker」。
4. **跨语言写 JSON 用标准库**，不要 `printf` 拼带空格、带引号的配置项。
5. **时间戳先约定单位和来源**（文件名 / NBT / 进程时钟），再统一在边界 `Number()`；全局 Long→String 时前端必须当字符串数字处理。
6. **本地夹具要像生产**：缺 `level.dat` 的空壳目录测不出 LastPlayed，也过不了上传校验。
7. **Windows + Lombok + DevTools**：测不过先怀疑 class 文件，再怀疑业务。
8. **安全组、面板防火墙、容器端口映射** 三处要一致；Web 反代通常管不到游戏口。

---

## 10. 关键路径速查

| 主题 | 网站仓库 | 游戏目录 |
|---|---|---|
| 控制台 API / 时间 / 上传 | `mc/service/impl/McBedrockServiceImpl.java` | `data/worlds`、`backups`、`ops` |
| 卡片与时间格式 | `McRealmView.vue`（`fmtTime` 先 `Number`） | — |
| Long 变字符串 | `config/JsonConfig.java` | — |
| 运行状态 | 读 `ops/runtime.json` | `scripts/ops-worker.sh` 的 `write_runtime` |
| 列表延迟 | — | `compose.yaml` 的 `ping`、`scripts/raknet-ping.py` |
| 协议 | — | [wiki.bedrock.dev/servers/raknet](https://wiki.bedrock.dev/servers/raknet) |
| 生产挂载 | `application-prod.yml` → `/opt/bedrock` | 宿主机 `MC_BEDROCK_HOST_DIR` |
