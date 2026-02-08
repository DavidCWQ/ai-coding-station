package com.cwq.project_aicodingstation;

import com.cwq.project_aicodingstation.ai.AICodeGeneratorService;
import com.cwq.project_aicodingstation.ai.result.HtmlFileCodeResult;
import com.cwq.project_aicodingstation.ai.result.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AICodeGeneratorServiceTest {

    @Resource
    private AICodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlFileCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个工作记录小工具");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCode = aiCodeGeneratorService.generateMultiFileCode("做个小留言板");
        Assertions.assertNotNull(multiFileCode);
    }

}
