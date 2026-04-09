package com.cwq.project_aicodingstation.ai.rag.ingest;

import com.cwq.project_aicodingstation.ai.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "true")
public class RagBootstrap {

    private final RagProperties ragProperties;
    private final RagIngestService ingestService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!ragProperties.isIngestOnStartup()) {
            return;
        }
        try {
            ingestService.ingestFromClasspath(ragProperties.getDocsClasspathPattern());
        } catch (Exception e) {
            log.error("RAG startup ingest failed: {}", e.getMessage(), e);
        }
    }
}
