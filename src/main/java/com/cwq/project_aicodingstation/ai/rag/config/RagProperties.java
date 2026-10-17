package com.cwq.project_aicodingstation.ai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "ai.rag")
public class RagProperties {

    private boolean enabled = false;

    /**
     * 是否在「开始运行时注入」文档对应向量与索引记录。
     * <p>
     * 默认 true：确保可使用检索增强生成。
     * </p>
     */
    private boolean ingestOnStartup = true;

    /**
     * 是否清理「已从 classpath 删除」的文档对应向量与索引记录。
     * <p>
     * 默认 false：避免误删历史知识；仅对「新增/变更」文档重建。
     * </p>
     */
    private boolean cleanupDeleted = false;

    private String docsClasspathPattern = "classpath:rag/docs/**/*.md";

    @NestedConfigurationProperty
    private Embedding embedding = new Embedding();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIngestOnStartup() {
        return ingestOnStartup;
    }

    public void setIngestOnStartup(boolean ingestOnStartup) {
        this.ingestOnStartup = ingestOnStartup;
    }

    public boolean isCleanupDeleted() {
        return cleanupDeleted;
    }

    public void setCleanupDeleted(boolean cleanupDeleted) {
        this.cleanupDeleted = cleanupDeleted;
    }

    public String getDocsClasspathPattern() {
        return docsClasspathPattern;
    }

    public void setDocsClasspathPattern(String docsClasspathPattern) {
        this.docsClasspathPattern = docsClasspathPattern;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public void setEmbedding(Embedding embedding) {
        this.embedding = embedding != null ? embedding : new Embedding();
    }

    public static class Embedding {
        private String apiKey = "";
        private String modelName = "text-embedding-v4";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }
    }
}
