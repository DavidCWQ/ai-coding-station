package com.cwq.project_aicodingstation.ai.rag.ingest;

import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import com.cwq.project_aicodingstation.ai.rag.config.RagModelConfig;
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
            @Qualifier(RagModelConfig.RAG_EMBEDDING_MODEL_BEAN) EmbeddingModel embeddingModel,
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
        ingestFromClasspath(classpathPattern, null);
    }

    /**
     * 从 classpath 扫描并注入向量。
     *
     * @param classpathPattern 资源扫描表达式
     * @param corpus           文档集标识（为空时将从文件路径自动推断）
     */
    public void ingestFromClasspath(String classpathPattern, String corpus) throws IOException {
        Resource[] resources = applicationContext.getResources(classpathPattern);
        List<DocPayload> payloads = new ArrayList<>();
        for (Resource resource : resources) {
            if (!resource.isReadable()) {
                continue;
            }
            String fileName = resource.getFilename();
            if (fileName == null || fileName.isBlank()) {
                continue;
            }
            String fileDir = resolveFileDir(resource);
            String text = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            String contentHash = sha256Hex(text);
            String resolvedCorpus = resolveCorpus(fileDir, corpus);
            payloads.add(new DocPayload(fileName, fileDir, text, contentHash, resolvedCorpus));
        }

        List<String> activeFileCodes = payloads.stream().map(DocPayload::fileCode).toList();
        List<DocPayload> changedPayloads = payloads.stream()
                .filter(this::isNewOrChanged)
                .toList();

        if (!changedPayloads.isEmpty()) {
            changedPayloads.forEach(p -> fileRepository.deleteEmbeddingsByFile(p.fileName(), p.fileDir()));
            List<Document> changedDocs = changedPayloads.stream()
                    .map(p -> {
                        Metadata metadata = new Metadata();
                        metadata.put("file_name", p.fileName());
                        metadata.put("file_dir", p.fileDir());
                        metadata.put("corpus", p.corpus());
                        InspirationEchoNoteMetadata.apply(metadata, p.corpus(), p.fileName(), p.text());
                        return Document.from(p.text(), metadata);
                    })
                    .toList();
            DocumentSplitter splitter = new DocumentByParagraphSplitter(2000, 200);
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(splitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            ingestor.ingest(changedDocs);
            changedPayloads.forEach(p -> fileRepository.upsert(p.fileName(), p.fileDir(), p.contentHash()));
        }

        if (changedPayloads.isEmpty()) {
            cleanupDeletedIfEnabled(activeFileCodes);
            return;
        }

        cleanupDeletedIfEnabled(activeFileCodes);
        log.info(
                "RAG ingest complete, reingested={}, activeDocs={}",
                changedPayloads.size(),
                payloads.size());
    }

    private void cleanupDeletedIfEnabled(List<String> activeFileCodes) {
        if (!ragProperties.isCleanupDeleted()) {
            return;
        }
        fileRepository.deleteEmbeddingsNotIn(activeFileCodes);
        fileRepository.deleteNotIn(activeFileCodes);
    }

    private boolean isNewOrChanged(DocPayload payload) {
        String oldHash = fileRepository.getContentHash(payload.fileName(), payload.fileDir());
        return oldHash == null || !oldHash.equals(payload.contentHash());
    }

    private String resolveCorpus(String fileDir, String explicitCorpus) {
        if (explicitCorpus != null && !explicitCorpus.isBlank()) {
            return explicitCorpus.trim();
        }
        // 约定：resources/rag/docs/<agentCode>/** -> corpus=<agentCode>
        // fileDir 示例：rag/docs/code_assistant
        for (AgentCodeEnum agent : AgentCodeEnum.values()) {
            String code = agent.getCode();
            if (fileDir.contains("/rag/docs/" + code) || fileDir.contains("\\rag\\docs\\" + code)) {
                return code;
            }
        }
        return "default";
    }

    private String resolveFileDir(Resource resource) {
        String description = resource.getDescription();
        int left = description.indexOf('[');
        int right = description.lastIndexOf(']');
        if (left >= 0 && right > left) {
            String path = description.substring(left + 1, right).replace('\\', '/');
            int slash = path.lastIndexOf('/');
            return slash >= 0 ? path.substring(0, slash) : "";
        }
        return "";
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

    private record DocPayload(String fileName, String fileDir, String text, String contentHash, String corpus) {
        String fileCode() {
            return fileDir + "/" + fileName;
        }
    }
}
