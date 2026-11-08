package com.cwq.project_aicodingstation.ai;

import com.cwq.project_aicodingstation.ai.result.AppTitleResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AppTitleService {

    @SystemMessage("""
            你为用户正在创建的网站起一个标题。
            只根据描述概括这个网站是什么。
            标题使用中文，2到12个字，不要标点、引号和解释。
            """)
    AppTitleResult suggest(@UserMessage String description);
}
