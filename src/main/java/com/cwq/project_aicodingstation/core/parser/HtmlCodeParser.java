package com.cwq.project_aicodingstation.core.parser;

import com.cwq.project_aicodingstation.ai.result.HtmlFileCodeResult;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 单文件代码解析器
 */
@Component
public class HtmlCodeParser implements CodeParser<HtmlFileCodeResult> {

    private static final Pattern HTML_CODE_PATTERN = Pattern.compile(
            "```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public HtmlFileCodeResult parseCode(String codeContent) {
        HtmlFileCodeResult result = new HtmlFileCodeResult();
        // 提取 HTML 代码
        String htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        } else if (isRawHtmlDocument(codeContent)) {
            // 模型有时直接输出整页 HTML，而不是代码块。
            result.setHtmlCode(codeContent.trim());
        }
        return result;
    }

    /**
     * 整段回复本身就是页面。普通说明文字不能当成新页面，否则会覆盖上一次的代码。
     */
    private static boolean isRawHtmlDocument(String content) {
        if (content == null) {
            return false;
        }
        String trimmed = content.stripLeading().toLowerCase();
        return trimmed.startsWith("<!doctype") || trimmed.startsWith("<html");
    }

    /**
     * 提取 HTML 代码内容
     *
     * @param content 原始内容
     * @return HTML代码
     */
    private String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
