package com.cwq.project_aicodingstation.core.saver;

import com.cwq.project_aicodingstation.ai.config.AICodeProperties;
import com.cwq.project_aicodingstation.ai.enums.CodeGenTypeEnum;
import com.cwq.project_aicodingstation.ai.result.HtmlFileCodeResult;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import org.springframework.stereotype.Component;

/**
 * HTML代码文件保存器
 */
@Component
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlFileCodeResult> {

    protected HtmlCodeFileSaverTemplate(AICodeProperties properties) {
        super(properties);
    }

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlFileCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlFileCodeResult result) {
        super.validateInput(result);
        // HTML代码内容不能为空
        BusinessAssert.notBlank(
                result.getHtmlCode(), ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空"
        );
    }
}
