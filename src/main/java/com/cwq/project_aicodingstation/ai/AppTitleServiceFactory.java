package com.cwq.project_aicodingstation.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppTitleServiceFactory {

    @Resource
    private ChatModel myChatModel;

    @Bean
    public AppTitleService appTitleService() {
        return AiServices.builder(AppTitleService.class)
                .chatModel(myChatModel)
                .build();
    }
}
