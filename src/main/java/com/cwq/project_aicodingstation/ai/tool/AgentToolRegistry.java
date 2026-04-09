package com.cwq.project_aicodingstation.ai.tool;

import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import com.cwq.project_aicodingstation.ai.tool.impl.InterviewQuestionTool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 按智能体类型选择可用 Tools 列表。
 */
@Component
public class AgentToolRegistry {

    private final ObjectProvider<InterviewQuestionTool> interviewQuestionToolProvider;

    public AgentToolRegistry(ObjectProvider<InterviewQuestionTool> interviewQuestionToolProvider) {
        this.interviewQuestionToolProvider = interviewQuestionToolProvider;
    }

    public Object[] toolsFor(AgentCodeEnum agent) {
        List<Object> tools = new ArrayList<>();
        if (agent == AgentCodeEnum.CODE_ASSISTANT) {
            InterviewQuestionTool tool = interviewQuestionToolProvider.getIfAvailable();
            if (tool != null) {
                tools.add(tool);
            }
        }
        return tools.toArray();
    }
}

