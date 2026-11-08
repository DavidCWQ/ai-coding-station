package com.cwq.project_aicodingstation.ai.utils;

import com.cwq.project_aicodingstation.ai.model.StreamChunk;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * 把 LangChain4j {@link TokenStream} 收成带思考通道的 Flux。
 * 官方的 Flux 适配器只转发正文，思考增量会丢掉。
 */
public final class TokenStreamSupport {

    private TokenStreamSupport() {}

    public static Flux<StreamChunk> flux(TokenStream tokenStream) {
        return Flux.create(sink -> {
            boolean[] sawReasoning = {false};
            tokenStream
                    .onPartialThinking(partial -> {
                        sawReasoning[0] = true;
                        emit(sink, StreamChunk.reasoning(textOf(partial)));
                    })
                    .onPartialResponse(text -> emit(sink, StreamChunk.content(text)))
                    .onCompleteResponse(response -> {
                        if (!sawReasoning[0]) {
                            emit(sink, StreamChunk.reasoning(thinkingOf(response)));
                        }
                        sink.complete();
                    })
                    .onError(sink::error)
                    .start();
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    private static String thinkingOf(ChatResponse response) {
        if (response == null) {
            return "";
        }
        AiMessage message = response.aiMessage();
        if (message == null || message.thinking() == null) {
            return "";
        }
        return message.thinking();
    }

    private static String textOf(PartialThinking partial) {
        return partial == null ? "" : partial.text();
    }

    private static void emit(FluxSink<StreamChunk> sink, StreamChunk chunk) {
        if (chunk.text().isEmpty() || sink.isCancelled()) {
            return;
        }
        sink.next(chunk);
    }
}
