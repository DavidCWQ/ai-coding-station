package com.cwq.project_aicodingstation.ai.utils;

import cn.hutool.json.JSONUtil;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 将文本流封装为前端约定的 SSE：每条 {@code data} 为 JSON {@code {"d":"片段"}}，末尾追加 {@code event:done}。
 * <p>
 * 与 {@code useSSEChat.ts} / {@code streamAgentChat} 的解析逻辑一致。
 * </p>
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
public final class SSEStreamUtils {

    private SSEStreamUtils() {}

    /**
     * 把字符串片段流转为 SSE 事件流
     *
     * @param textChunks 模型或业务产生的文本增量
     * @return SSE 事件流（含结束标记）
     */
    public static Flux<ServerSentEvent<String>> toJsonDataSSE(Flux<String> textChunks) {
        return textChunks
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String json = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(json)
                            .build();
                })
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }
}
