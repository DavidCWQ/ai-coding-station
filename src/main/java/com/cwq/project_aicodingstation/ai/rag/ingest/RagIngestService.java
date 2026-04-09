package com.cwq.project_aicodingstation.ai.rag.ingest;

import com.cwq.project_aicodingstation.ai.rag.config.RagEmbeddingConfig;
import com.cwq.project_aicodingstation.ai.rag.config.RagProperties;
import com.cwq.project_aicodingstation.ai.rag.repository.RagFileRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "true")
public class RagIngestService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RagFileRepository fileRepository;
    private final RagProperties ragProperties;
    private final ApplicationContext applicationContext;

    public RagIngestService(
            @Qualifier(RagEmbeddingConfig.RAG_EMBEDDING_MODEL_BEAN) EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            RagFileRepository fileRepository,
            RagProperties ragProperties,
            ApplicationContext applicationContext
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.fileRepository = fileRepository;
        this.ragProperties = ragProperties;
        this.applicationContext = applicationContext;
    }

    public void ingestFromClasspath(String classpathPattern) throws IOException {
        Resource[] resources = applicationContext.getResources(classpathPattern);
        List<DocPayload> payloads = new ArrayList<>();
        for (Resource resource : resources) {
            if (!resource.isReadable()) {
                continue;
            }
            String fileKey = resolveFileKey(resource);
            if (fileKey == null || fileKey.isBlank()) {
                continue;
            }
            String text = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            String contentHash = sha256Hex(text);
            String fileName = resource.getFilename() == null ? fileKey : resource.getFilename();
            payloads.add(new DocPayload(fileKey, fileName, text, contentHash));
        }

        List<String> activeFileKeys = payloads.stream().map(DocPayload::fileKey).toList();
        List<DocPayload> hashBackfillOnly = new ArrayList<>();
        List<DocPayload> fullReingest = new ArrayList<>();
        for (DocPayload p : payloads) {
            switch (ingestDisposition(p)) {
                case SKIP -> { }
                case HASH_BACKFILL -> hashBackfillOnly.add(p);
                case FULL_REINGEST -> fullReingest.add(p);
            }
        }

        hashBackfillOnly.forEach(p -> fileRepository.upsert(p.fileKey(), p.contentHash()));

        if (!fullReingest.isEmpty()) {
            fullReingest.forEach(p -> fileRepository.deleteEmbeddingsByFileKey(p.fileKey()));
            List<Document> changedDocs = fullReingest.stream()
                    .map(p -> Document.from(
                            p.text(),
                            Metadata.metadata("file_name", p.fileKey())))
                    .toList();
            DocumentSplitter splitter = new DocumentByParagraphSplitter(2000, 200);
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(splitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            ingestor.ingest(changedDocs);
            fullReingest.forEach(p -> fileRepository.upsert(p.fileKey(), p.contentHash()));
        }

        if (hashBackfillOnly.isEmpty() && fullReingest.isEmpty()) {
            cleanupDeletedIfEnabled(activeFileKeys);
            return;
        }

        cleanupDeletedIfEnabled(activeFileKeys);
        log.info(
                "RAG ingest complete, hashBackfill={}, reingested={}, activeDocs={}",
                hashBackfillOnly.size(),
                fullReingest.size(),
                payloads.size());
    }

    private void cleanupDeletedIfEnabled(List<String> activeFileKeys) {
        if (!ragProperties.isCleanupDeleted()) {
            return;
        }
        fileRepository.deleteEmbeddingsNotIn(activeFileKeys);
        fileRepository.deleteNotIn(activeFileKeys);
    }

    /**
     * 迁移库常见：向量已在 rag_embedding，但 content_hash 为空。此时只回填哈希，避免重复调用 embedding API。
     * 若 classpath 文件与已存向量实际不一致，需手动删该 file_key 的向量或清空 content_hash 后重启 ingest。
     */
    private IngestDisposition ingestDisposition(DocPayload payload) {
        String oldHash = fileRepository.getContentHash(payload.fileKey());
        if (oldHash != null && !oldHash.isBlank() && oldHash.equals(payload.contentHash())) {
            return IngestDisposition.SKIP;
        }
        if (oldHash == null || oldHash.isBlank()) {
            if (fileRepository.countEmbeddingsByFileKey(payload.fileKey()) > 0) {
                return IngestDisposition.HASH_BACKFILL;
            }
        }
        return IngestDisposition.FULL_REINGEST;
    }

    private enum IngestDisposition {
        SKIP,
        HASH_BACKFILL,
        FULL_REINGEST
    }

    private String resolveFileKey(Resource resource) {
        String description = resource.getDescription();
        int left = description.indexOf('[');
        int right = description.lastIndexOf(']');
        if (left >= 0 && right > left) {
            return description.substring(left + 1, right);
        }
        return resource.getFilename();
    }

    private String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private record DocPayload(String fileKey, String fileName, String text, String contentHash) {
    }
}
