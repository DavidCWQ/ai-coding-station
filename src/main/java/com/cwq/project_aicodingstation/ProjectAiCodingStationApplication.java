package com.cwq.project_aicodingstation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cwq.project_aicodingstation.user.mapper")
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

// ====== 笔记区（NOTES）结束 ======
