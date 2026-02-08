package com.cwq.project_aicodingstation.ai;

import com.cwq.project_aicodingstation.ai.enums.CodeGenTypeEnum;
import com.cwq.project_aicodingstation.ai.result.HtmlFileCodeResult;
import com.cwq.project_aicodingstation.ai.result.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AICodeGeneratorService {

    // =========== blocking ===========
    @SystemMessage(fromResource = "prompt/codegen/html-file.txt")
    HtmlFileCodeResult generateHtmlCode(String userMessage);

    @SystemMessage(fromResource = "prompt/codegen/multi-file.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    // ========== streaming ===========
    @SystemMessage(fromResource = "prompt/codegen/html-file.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    @SystemMessage(fromResource = "prompt/codegen/multi-file.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);

    // ========== Dispatcher ==========
    default Object generateCode(String msg, CodeGenTypeEnum type) {
        return switch (type) {
            case HTML -> generateHtmlCode(msg);
            case MULTI_FILE -> generateMultiFileCode(msg);
        };
    }

    default Flux<String> generateCodeStream(String msg, CodeGenTypeEnum type) {
        return switch (type) {
            case HTML -> generateHtmlCodeStream(msg);
            case MULTI_FILE -> generateMultiFileCodeStream(msg);
        };
    }
}
