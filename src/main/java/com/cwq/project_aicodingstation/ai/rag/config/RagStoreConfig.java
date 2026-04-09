package com.cwq.project_aicodingstation.ai.rag.config;

import com.zaxxer.hikari.HikariDataSource;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "true")
public class RagStoreConfig {

    private static final int EMBEDDING_DIMENSION = 1024;

    @Bean(name = "pgVectorDataSource")
    @ConfigurationProperties(prefix = "rag.datasource")
    public DataSource pgVectorDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @Qualifier("ragJdbcTemplate")
    public JdbcTemplate ragJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("rag_embedding")
                .dimension(EMBEDDING_DIMENSION) // MUST match schema + model
                .build();
    }

    @Bean
    public ContentRetriever agentRagContentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            @Qualifier(RagModelConfig.RAG_EMBEDDING_MODEL_BEAN) EmbeddingModel embeddingModel
    ) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)  // Show at most 5 results
                .minScore(0.75) // Filter results (scores < 0.75)
                .build();
    }
}
