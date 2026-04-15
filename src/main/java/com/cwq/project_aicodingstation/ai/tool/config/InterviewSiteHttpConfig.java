package com.cwq.project_aicodingstation.ai.tool.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 面试题站点 HTTP 客户端（连接/读超时），与业务 RestTemplate 隔离。
 */
@Configuration
public class InterviewSiteHttpConfig {

    public static final String INTERVIEW_SITE_REST_TEMPLATE = "interviewSiteRestTemplate";

    @Bean
    @Qualifier(INTERVIEW_SITE_REST_TEMPLATE)
    public RestTemplate interviewSiteRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(8));
        factory.setReadTimeout(Duration.ofSeconds(20));
        RestTemplate template = new RestTemplate();
        template.setRequestFactory(factory);
        return template;
    }
}
