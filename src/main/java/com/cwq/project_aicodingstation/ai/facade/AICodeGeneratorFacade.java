/*
 * 门面模式[设计模式]
 * 门面模式通过提供一个统一的高层接口来隐藏子系统的复杂性，؜؜؜
 * 让客户端只需要与这个简化的接口交互，而不用了解内部的复杂实现细节。
 * */

package com.cwq.project_aicodingstation.ai.facade;

import com.cwq.project_aicodingstation.ai.AICodeGeneratorService;
import com.cwq.project_aicodingstation.ai.enums.CodeGenTypeEnum;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.core.parser.CodeParserExecutor;
import com.cwq.project_aicodingstation.core.saver.CodeFileSaverExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AICodeGeneratorFacade {

    @Resource
    private AICodeGeneratorService aiCodeGeneratorService;

    @Resource
    private CodeFileSaverExecutor codeFileSaver;

    @Resource
    private CodeParserExecutor codeParser;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        BusinessAssert.notNull(codeGenTypeEnum, ErrorCode.SYSTEM_ERROR, "生成类型为空");
        return codeFileSaver.executeSaver(
                aiCodeGeneratorService.generateCode(userMessage, codeGenTypeEnum),
                codeGenTypeEnum, appId
        );
    }

    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder builder = new StringBuilder();
        return codeStream
                .doOnNext(builder::append) // 实时收集代码片段
                .doOnComplete(() -> { // 流式返回完成后保存代码
                    try {
                        String full = builder.toString();
                        // 使用执行器解析代码
                        Object parsed = codeParser.executeParser(full, codeGenType);
                        // 使用执行器保存代码
                        File dir = codeFileSaver.executeSaver(parsed, codeGenType, appId);
                        // 保存成功，记录路径
                        log.info("(生成代码)保存成功: {}", dir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("(生成代码)保存失败", e);
                    }
                });
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage   用户提示词
     * @param codeGenType   生成类型
     * @return 生成的代码（流式）
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        BusinessAssert.notNull(codeGenType, ErrorCode.SYSTEM_ERROR, "生成类型为空");
        return processCodeStream(
                aiCodeGeneratorService.generateCodeStream(userMessage, codeGenType),
                codeGenType, appId
        );
    }

}
