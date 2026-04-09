package com.cwq.project_aicodingstation;

import com.cwq.project_aicodingstation.ai.guardrail.InputGuardrailProperties;
import com.cwq.project_aicodingstation.ai.rag.config.RagProperties;
import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication( // 未使用 Redis embedding 自动配置；RAG 使用独立 pgvector 数据源
        exclude = { RedisEmbeddingStoreAutoConfiguration.class }
)
@EnableConfigurationProperties({InputGuardrailProperties.class, RagProperties.class})
@MapperScan({"com.cwq.project_aicodingstation.*.mapper"}) // 添加 Mapper 到 Spring Boot 扫描范围内
@EnableScheduling
public class ProjectAiCodingStationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectAiCodingStationApplication.class, args);
    }

}

// ====== 笔记区（NOTES）开始 ======

/* 注意`common`是无业务层，避免放入`UserService`架构污染，后续按业务域分包，按业务模块纵向拆 */

/* 调用链
 * Controller → Service (业务规则、Assert) → Mapper → DataBase
 * */

/*
 * 工程规范 ≠ 文件数量优化：
 * 在 production 代码里，`package数量` 不重要，
 * 重要的是：是否可扩展？是否职责清晰？是否一致？
 * 没有任何一个成熟项目会因为 “这个 package 现在只有一个类” 而不建。
 * 哪怕每个目录只有一个类：一眼就知道职责，新人加入成本极低，不会推翻结构重构（该文件，移 import）。
 * */

/*
 * Mybatis 数据访问层框架，能够通过 Java 快速操作数据库
 * MyBatis is a first class persistence framework with support for custom SQL,
 * stored procedures and advanced mappings. It eliminates almost all the JDBC code... */

/*
 * Spring Boot 4.0 兼容性很差，ORM 框架基本都在追进度 (Flex/Sharding)，与 MyBatis-Flex 等生态不兼容，
 * 继续现在这样「硬凑」，结果只会是：今天修 Mapper，明天炸 Transaction，后天炸 AOP。
 * 选择合适稳定的版本，比如 Spring Boot 3.5，避免「生态版本错位」导致的一系列问题。
 * */

/* dto (Data Transfer Object); vo (Value Object) */

/*
 * 当对象被序列化时，serialVersionUID 会被写入字节流。
 * 当反序列化时，Java 会读取字节流中的 serialVersionUID，并与当前类的 serialVersionUID 进行比对。
 * 若两者一致，则反序列化成功；若不一致，则抛出 InvalidClassException。
 * [1] https://blog.csdn.net/jam_yin/article/details/150858817
 * */

/*
 * 面向切面编程（AOP）作为一种编程范式，
 * 通过将与核心业务逻辑无关的横切关注点（如日志、安全、事务管理等）从主逻辑中分离出来，
 * 极大地提升了代码的整洁性和可管理性。
 * */

/*
 * 门面模式[设计模式]
 * 门面模式通过提供一个统一的高层接口来隐藏子系统的复杂性，
 * 让客户端只需要与这个简化的接口交互，而不用了解内部的复杂实现细节。
 * */

/*
 * 预期的代码架构是一种[混合模式]：
 * [执行器模式]：提供统一的执行入口，根据生成类型执行不同的操作
 * [策略模式]：每种模式对应的解析方法单独作为一个类来维护
 * [模板模式]：抽象模板类定义了通用的文件保存流程，子类可以有自己的实现
 * */

/*
 * 注解       |作用对象   |注册方式         |典型用途
   @Component   |class    |组件扫描自动注册   |自己写的类
   @Bean        |method   |手动返回对象注册   |第三方类/复杂创建逻辑
 * ---
 * 注解       |默认方式           |来源                 |备注
   @Autowired   |按类型 (byType)   |Spring             |常用(配合@Qualifier)
   @Resource    |按名称 (byName)   |JSR-250(Java标准)    |可指定 name
 * ---
 * @Service / @Repository / @Controller 都是 @Component 的语义化封装
 * @Configuration 声明配置类 + 定义 Bean (Bean 工厂)
 * */

/*
 * 核心业务流程（完整的应用生命周期管理体系）
 * 用户在主页输入提示词后，系统会创建一个应用记录，然后跳转到对话页面与 AI 交互生成网站。
 * 生成完成后，用户可以预览效果，满意后进行部署，让网站真正对外提供服务。
 * 这个流程涉及到：数据存储、权限控制、文件管理、网站部署等多个技术环节。
 * */

/*
 * nginx: 一个高性能的 HTTP 和 反向代理 web 服务器。
 * 在这里，nginx 做的就是根据配置，把进来的 HTTP 请求转到「本地（挂载）文件」或「后面的服务」
 * -「URL → 本地目录里的文件」的映射
 * -「URL → 另一台服务器后端」的映射
 * */

/*
 * 8088:80 表示：宿主机 8088 -> nginx 容器 80，所以：
 * 容器内部互访（backend -> nginx）：用 http://nginx 或 http://nginx:80
 * 宿主机/外网访问：用 http://<服务器IP>:8088（localhost/外网IP）
 * 注意：前端是跑在用户浏览器里的，浏览器不在 Docker 网络里，解析不到 http://backend，会直接请求失败
 * */

/* [!!CAUTION!!]
 * 该项目前端开发于 WSL2.0 Ubuntu 24.04
 * 该项目后端开发于 Windows
 * */

/*
 * padding: 内边距，e.g.某按钮与内部文字的距离
 * margin:  外边距，e.g.某按钮与外部组件的距离
 * */

/*
 * 反向代理（proxy）
 * 你（浏览器）→ 前台接待（Nginx）→ 后端部门（Java 服务）
 *               ↑
 *          这就是反向代理
 * 你不需要知道后端部门在哪，你只需要找前台（Nginx），它帮你转达。
 * ---
 * 没有反代：
 * 浏览器 → http://your-domain.com/api/app
 *                    ↓
 *              404 Not Found（因为 Nginx 不知道 /api 要转发到哪里）
 * ---
 * 有了反代：
 * # nginx.conf
 * location /api {
 *   proxy_pass http://backend:8142;  # 转发到后端容器（容器间通信）
 * }
 * 浏览器 → http://your-domain.com/api/users
 *   ↑                ↓
 *   ↑        Nginx 接收请求 → proxy_pass 转发到 backend:8142 传给后端
 *   ↑                                                        ↓
 * Nginx 返回给浏览器  ←  返回数据  ←  后端处理：/api/users  ←
 * */

/*
 * 根据 nginx 的配置，浏览器可能会缓存 index.html 或 JS 一段时间，导致页面不更新
 * 可以手动 Ctrl+Shift+R 强制刷新，或者修改 nginx 配置 为 no-cache。
 * */

/*
 * 传统分页查询的问题（offset） vs 游标查询（select by id/time）
 * - 在传统分页中，数据通常是「基于页码或偏移量」进行加载的。如果数据在分页过程发生了变化，
 * - 比如插入新数据、删除老数据，用户看到的分页数据可能不一致，导致用户错过或重复某些数据。
 * 为了解决这些问题，可以使用「游标分页」
 * - 使用一个游标来跟踪分页位置，每次请求从上一次请求的游标开始加载数据，而不是基于页码。
 * - 选择数据记录的唯一标识符（主键）、时间戳、或者 具有排序能力的字段 作为游标。
 * 比如即时通讯系统中的每个消息，通常都有一个唯一自增的 id，就可以作为游标。
 * 每次查询完当前页面的数据后，可以将最后一条消息记录的 id，作为游标值传递给前端（客户端）。
 * */

/*
 * 先确保后端在运行中，再运行 `npm run openapi2ts` 以生成前端 @api/
 * */

/*
 * 为什么用 Redis 不用 MySQL 来存储会话记忆？
 * 相比 MySQL，作为内存数据库的 Redis 在读写对话记忆时性能更高；
 * 数据库中的对话历史表包含其他业务字段，不适合直接交给 LangChain4j 的对话记忆组件管理。
 * 注意，Redis 的内存也不是无限的！一般情况下要给存入 Redis 的每个 Key 都设置合理的过期时间！
 * */

/*
 * User
 * └── App（应用）
 *      └── Session（会话）
 *            └── Messages（对话）
 * */

/*
 * 自动截图生成封面：
 * 代码生成 → 保存 HTML → 自动截图网页 → 存封面 → 保存并返回 URL
 * */

/*
 * Java 21 的虚拟线程 (Virtual Thread)，是由 JVM 管理的轻量级线程。
 * - 它的创建成本极低 (几乎无内存开销)，且在执行 I/O 操作时会自动让出 CPU 给其他虚拟线程，
 * - 从而在同样的系统资源下，支持百万级并发，而不是传统平台线程的几千级并发。
 * - 它的使用和传统 Java 线程几乎没有区别，非常适合处理 I/O 密集型的异步任务 (截图服务)。
 * */

/*
 * 浏览器的同源策略 (Same-Origin Policy) 规定，
 * 两个页面若要被视为 “同源”，必须同时满足三个条件：协议相同，域名相同，端口相同。
 *
 * 如果 iframe 是跨域的，你不能：
 * - iframe.contentWindow.document
 * - iframe.contentWindow.addEventListener
 * - ...
 *
 * 对于同源网站，可以外部注入 js，实现动态编辑页面：
 * const script = doc.createElement('script');
 * script.src = '/inject/iframe-editor.js';
 * doc.body.appendChild(script);
 * */

/*
 * 用户删除应用时，先软删除(mark is_deleted)，满 14-days 再清磁盘 + 物理删库，避免内存堆积。
 * */

/*
 * 在 Docker 里建议使用 BuildKit 的缓存挂载，缓存 Maven 依赖到 ~/.m2/repository，
 * 同一台构建机上，后续构建时，依赖优先从 /root/.m2 缓存读。
 * */

/*
 * Notice: Registering a tool does NOT mean it will be called.
 * If the answer is in RAG, the model may infer 'I don't need to call tools'.
 * */

/*
 * Spring Boot defaults to HikariCP.
 * But in multi-DB apps, you need to set PRIMARY datasource.
 * E.g., define it explicitly via `.type(HikariDataSource.class)`.
 * When using Hikari + driver-class-name, you MUST use: `jdbc-url` instead of `url`.
 * */

// ====== 笔记区（NOTES）结束 ======
