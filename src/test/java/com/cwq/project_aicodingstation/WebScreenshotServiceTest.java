package com.cwq.project_aicodingstation;

import com.cwq.project_aicodingstation.core.screenshot.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class WebScreenshotServiceTest {

    @Resource
    private ScreenshotService screenshotService;

    @Test
    void saveWebPageScreenshot() {
        String testUrl = "https://www.bilibili.com";
        String webPageScreenshot = screenshotService.capture(testUrl, 100000000000000000L);
        Assertions.assertNotNull(webPageScreenshot);
    }
}
