package com.cwq.project_aicodingstation;

import com.cwq.project_aicodingstation.ai.enums.CodeGenTypeEnum;
import com.cwq.project_aicodingstation.ai.result.HtmlFileCodeResult;
import com.cwq.project_aicodingstation.ai.result.MultiFileCodeResult;
import com.cwq.project_aicodingstation.core.parser.CodeParserExecutor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CodeParserTest {

    @Resource
    private CodeParserExecutor codeParser;

    @Test
    void parseHtmlCode() {
        String codeContent = """
                随便写一段描述：
                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>测试页面</title>
                </head>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>
                ```html
                随便写一段描述
                """;
        HtmlFileCodeResult parsed = (HtmlFileCodeResult)
                codeParser.executeParser(codeContent, CodeGenTypeEnum.HTML);
        Assertions.assertNotNull(parsed);
        Assertions.assertNotNull(parsed.getHtmlCode());
    }

    @Test
    void parseMultiFileCode() {
        String codeContent = """
                创建一个完整的网页：
                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>多文件示例</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <h1>欢迎使用</h1>
                    <script src="script.js"></script>
                </body>
                </html>
                ```html
                ```css
                h1 {
                    color: blue;
                    text-align: center;
                }
                ```
                ```js
                console.log('页面加载完成');
                
                ```
                文件创建完成！
                """;
        MultiFileCodeResult result = (MultiFileCodeResult)
                codeParser.executeParser(codeContent, CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getHtmlCode());
        Assertions.assertNotNull(result.getCssCode());
        Assertions.assertNotNull(result.getJsCode());
    }

}
