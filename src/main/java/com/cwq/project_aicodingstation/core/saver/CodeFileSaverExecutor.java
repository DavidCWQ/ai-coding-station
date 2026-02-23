package com.cwq.project_aicodingstation.core.saver;

import com.cwq.project_aicodingstation.ai.enums.CodeGenTypeEnum;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CodeFileSaverExecutor {

    private final Map<CodeGenTypeEnum, CodeFileSaverTemplate<?>> saverMap = new HashMap<>();

    public CodeFileSaverExecutor(List<CodeFileSaverTemplate<?>> savers) {
        for (CodeFileSaverTemplate<?> saver : savers) {
            saverMap.put(saver.getCodeType(), saver);
        }
    }

    /**
     * 执行代码保存（类型安全版本）
     *
     * @param codeResult  代码结果对象
     * @param codeGenType 代码生成类型
     * @return 保存的目录
     */
    @SuppressWarnings("unchecked")
    public <T> File executeSaver(T codeResult, CodeGenTypeEnum codeGenType, Long appId) {
        CodeFileSaverTemplate<T> saver = (CodeFileSaverTemplate<T>) saverMap.get(codeGenType);
        BusinessAssert.notNull(
                saver, ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType
        );
        return saver.saveCode(codeResult, appId);
    }
}
