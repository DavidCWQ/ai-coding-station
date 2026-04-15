package com.cwq.project_aicodingstation.ai.rag.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "true")
public class RagModelConfig {

    public static final String RAG_EMBEDDING_MODEL_BEAN = "ragEmbeddingModel";

    @Bean(name = RAG_EMBEDDING_MODEL_BEAN)
    public EmbeddingModel ragEmbeddingModel(RagProperties ragProperties) {
        String key = ragProperties.getEmbedding().getApiKey();
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException(
                    "ai.rag.enabled=true but ai.rag.embedding.api-key is missing");
        }
        return QwenEmbeddingModel.builder()
                .apiKey(key.trim())
                .modelName(ragProperties.getEmbedding().getModelName())
                .build();
    }
}

