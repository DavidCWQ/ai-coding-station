package com.cwq.project_aicodingstation.core.parser;

import com.cwq.project_aicodingstation.ai.enums.CodeGenTypeEnum;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码解析执行器：
 * 根据代码生成类型执行相应的解析逻辑
 */
@Component // @Component 注解作用于类，@Bean 注解作用于方法.
public class CodeParserExecutor {

    private final Map<CodeGenTypeEnum, CodeParser<?>> parserMap;

    public CodeParserExecutor(List<CodeParser<?>> parsers) {
        parserMap = new HashMap<>();
        for (CodeParser<?> parser : parsers) {
            if (parser instanceof HtmlCodeParser) {
                parserMap.put(CodeGenTypeEnum.HTML, parser);
            } else if (parser instanceof MultiFileCodeParser) {
                parserMap.put(CodeGenTypeEnum.MULTI_FILE, parser);
            }
        }
    }

    /**
     * 执行代码解析
     *
     * @param codeContent 代码内容
     * @param codeGenType 代码生成类型
     * @return 解析结果 (HtmlCodeResult or MultiFileCodeResult)
     */
    public Object executeParser(String codeContent, CodeGenTypeEnum codeGenType) {
        CodeParser<?> parser = parserMap.get(codeGenType);
        BusinessAssert.notNull(parser, ErrorCode.SYSTEM_ERROR, "不支持类型: " + codeGenType);
        return parser.parseCode(codeContent);
    }
}
