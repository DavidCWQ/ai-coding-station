package com.cwq.project_aicodingstation.core.saver;

import com.cwq.project_aicodingstation.ai.config.AICodeProperties;
import com.cwq.project_aicodingstation.ai.enums.CodeGenTypeEnum;
import com.cwq.project_aicodingstation.ai.result.MultiFileCodeResult;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import org.springframework.stereotype.Component;

/**
 * 多文件代码保存器
 */
@Component
public class MultiCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    protected MultiCodeFileSaverTemplate(AICodeProperties properties) {
        super(properties);
    }

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        // 保存 CSS 文件
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        // 保存 JavaScript 文件
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        // 至少要有一类代码内容（HTML / CSS / JS）
        boolean hasHtml = result.getHtmlCode() != null && !result.getHtmlCode().trim().isEmpty();
        boolean hasCss = result.getCssCode() != null && !result.getCssCode().trim().isEmpty();
        boolean hasJs = result.getJsCode() != null && !result.getJsCode().trim().isEmpty();
        BusinessAssert.requireTrue(
                hasHtml || hasCss || hasJs,
                ErrorCode.SYSTEM_ERROR,
                "HTML/CSS/JS 至少需要提供一种代码内容"
        );
    }
}
