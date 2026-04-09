package com.cwq.project_aicodingstation.ai.rag.retriever;

import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * 按智能体类型构建 RAG 检索器：同表存储，通过 metadata.corpus 做过滤隔离。
 */
@Component
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "true")
public class ContentRetrieverFactory {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public ContentRetrieverFactory(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel
    ) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public ContentRetriever forAgent(AgentCodeEnum agent) {
        String corpus = agent == null ? "default" : agent.getCode();
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)  // Show at most 5 results
                .minScore(0.75) // Filter results (scores < 0.75)
                .filter(metadataKey("corpus").isEqualTo(corpus))
                .build();
    }
}

