package com.cwq.project_aicodingstation.ai.tool.fetcher;

import com.cwq.project_aicodingstation.ai.tool.impl.InterviewQuestionTool;
import com.cwq.project_aicodingstation.ai.tool.dto.InterviewQuestion;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 面试题来源站点抓取抽象；具体实现注册为 Spring Bean 后由 {@link InterviewQuestionTool} 聚合调用。
 */
public interface InterviewSiteFetcher {

    String siteName();

    List<InterviewQuestion> fetch(String keyword) throws IOException;

    default void politeSleep(int millis) {
        try {
            Thread.sleep(millis + ThreadLocalRandom.current().nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
