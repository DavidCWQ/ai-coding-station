package com.cwq.project_aicodingstation.ai.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class SSEStreamUtilsTest {

    @Test
    void toJsonDataSSE_wrapsChunksAndAddsDoneEvent() {
        Flux<ServerSentEvent<String>> stream = SSEStreamUtils.toJsonDataSSE(Flux.just("hi", "there"));

        StepVerifier.create(stream)
                .assertNext(event -> {
                    org.junit.jupiter.api.Assertions.assertNull(event.event());
                    org.junit.jupiter.api.Assertions.assertEquals("{\"d\":\"hi\"}", event.data());
                })
                .assertNext(event -> org.junit.jupiter.api.Assertions.assertEquals("{\"d\":\"there\"}", event.data()))
                .assertNext(event -> {
                    org.junit.jupiter.api.Assertions.assertEquals("done", event.event());
                    org.junit.jupiter.api.Assertions.assertEquals("", event.data());
                })
                .verifyComplete();
    }
}
