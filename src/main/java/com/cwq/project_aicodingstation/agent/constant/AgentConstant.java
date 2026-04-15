package com.cwq.project_aicodingstation.agent.constant;

/**
 * 智能体模块通用常量。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
public interface AgentConstant {

    /**
     * 新建会话默认标题（与应用侧「新对话」保持一致语义）
     */
    String DEFAULT_SESSION_TITLE = "新对话";

    /**
     * 带入模型上下文的最近消息条数上限（user/ai 交替，含双方）
     */
    int MAX_CONTEXT_MESSAGES = 30;

    /**
     * 首条用户消息自动生成标题时的最大字符数
     */
    int AUTO_TITLE_MAX_LEN = 24;
}
