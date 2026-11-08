package com.cwq.project_aicodingstation.ai;

import com.cwq.project_aicodingstation.ai.enums.CodeGenTypeEnum;
import com.cwq.project_aicodingstation.ai.result.HtmlFileCodeResult;
import com.cwq.project_aicodingstation.ai.result.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface AICodeGeneratorService {

    // =========== blocking ===========
    @SystemMessage(fromResource = "prompt/codegen/html-file.txt")
    HtmlFileCodeResult generateHtmlCode(@UserMessage String userMessage);

    @SystemMessage(fromResource = "prompt/codegen/multi-file.txt")
    MultiFileCodeResult generateMultiFileCode(@UserMessage String userMessage);

    // ========== streaming ===========
    @SystemMessage(fromResource = "prompt/codegen/html-file.txt")
    TokenStream generateHtmlCodeStream(@UserMessage String userMessage);

    @SystemMessage(fromResource = "prompt/codegen/multi-file.txt")
    TokenStream generateMultiFileCodeStream(@UserMessage String userMessage);

    // ========== Dispatcher ==========
    default Object generateCode(String msg, CodeGenTypeEnum type) {
        return switch (type) {
            case HTML -> generateHtmlCode(msg);
            case MULTI_FILE -> generateMultiFileCode(msg);
        };
    }

    default TokenStream generateCodeStream(String msg, CodeGenTypeEnum type) {
        return switch (type) {
            case HTML -> generateHtmlCodeStream(msg);
            case MULTI_FILE -> generateMultiFileCodeStream(msg);
        };
    }
}
