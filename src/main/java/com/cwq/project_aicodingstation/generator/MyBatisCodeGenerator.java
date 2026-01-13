package com.cwq.project_aicodingstation.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.mybatisflex.codegen.config.PackageConfig;
import com.mybatisflex.codegen.config.StrategyConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;

public class MyBatisCodeGenerator {

    // 要生成的表
    private static final String[] TABLE_NAMES = {"sys_user"};

    public static void main(String[] args) {
        DataSource dataSource = createDataSource();
        GlobalConfig globalConfig = createGlobalConfig();

        Generator generator = new Generator(dataSource, globalConfig);
        generator.generate();
    }

    /**
     * 读取 application.yml，创建数据源
     */
    private static DataSource createDataSource() {
        Dict dict = YamlUtil.loadByPath("application.yml");
        Map<String, Object> ds = dict.getByPath("spring.datasource");

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(String.valueOf(ds.get("url")));
        dataSource.setUsername(String.valueOf(ds.get("username")));
        dataSource.setPassword(String.valueOf(ds.get("password")));
        dataSource.setDriverClassName(String.valueOf(ds.get("driver-class-name")));

        return dataSource;
    }

    /**
     * 代码生成配置
     */
    private static GlobalConfig createGlobalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();

        // ========= 包配置 =========
        PackageConfig packageConfig = globalConfig.getPackageConfig();

        packageConfig // 先生成到临时目录下，生成代码后，再移动到项目目录下
                .setBasePackage("com.cwq.project_aicodingstation.temp")
                .setEntityPackage("user.model.entity")
                .setMapperPackage("user.mapper")
                .setServicePackage("user.service")
                .setServiceImplPackage("user.service.impl")
                .setControllerPackage("user.controller");

        // ========= 策略配置 =========
        StrategyConfig strategyConfig = globalConfig.getStrategyConfig();

        strategyConfig
                .setGenerateTable(TABLE_NAMES)
                .setLogicDeleteColumn("is_deleted")
                .setTablePrefix(""); // 没有前缀就留空

        // ========= 生成内容 =========
        globalConfig
                .enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21);

        globalConfig.enableMapper();
        globalConfig.enableMapperXml();
        globalConfig.enableService();
        globalConfig.enableServiceImpl();
        globalConfig.enableController();

        // ========= 注释 =========
        globalConfig.getJavadocConfig()
                .setAuthor("<a href=\\\"https://github.com/DavidCWQ\\\">CWQ</a>")
                .setSince("");

        return globalConfig;
    }
}
