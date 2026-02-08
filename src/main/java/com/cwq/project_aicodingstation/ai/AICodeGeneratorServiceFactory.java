package com.cwq.project_aicodingstation.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AICodeGeneratorServiceFactory {

    @Resource
    private ChatModel myChatModel;

    @Resource
    private StreamingChatModel myStreamingChatModel;

    @Bean
    public AICodeGeneratorService aiCodeGeneratorService() {
        return AiServices.builder(AICodeGeneratorService.class)
                .chatModel(myChatModel)
                .streamingChatModel(myStreamingChatModel)
                .build();
    }

}
