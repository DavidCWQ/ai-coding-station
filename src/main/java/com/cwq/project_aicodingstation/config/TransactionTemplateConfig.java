package com.cwq.project_aicodingstation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 注册 {@link TransactionTemplate}，供流式回调等非代理场景手动划定事务边界。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Configuration
public class TransactionTemplateConfig {

    /**
     * 与数据源事务管理器绑定的事务模板
     *
     * @param transactionManager 平台事务管理器（Spring Boot 根据数据源自动装配）
     * @return 事务模板
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
