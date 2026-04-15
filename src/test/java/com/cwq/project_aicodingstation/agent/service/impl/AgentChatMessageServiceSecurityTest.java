package com.cwq.project_aicodingstation.agent.service.impl;

import com.cwq.project_aicodingstation.agent.dto.AgentHistoryQueryRequest;
import com.cwq.project_aicodingstation.agent.mapper.AgentChatMessageMapper;
import com.cwq.project_aicodingstation.agent.service.AgentChatSessionService;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 智能体历史查询：会话校验失败时应立即失败（防止越权遍历 sessionId）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@ExtendWith(MockitoExtension.class)
class AgentChatMessageServiceSecurityTest {

    @Mock
    private AgentChatSessionService agentChatSessionService;

    @Mock
    private AgentChatMessageMapper agentChatMessageMapper;

    @InjectMocks
    private AgentChatMessageServiceImpl agentChatMessageService;

    @Test
    void listHistory_propagatesWhenSessionAccessDenied() {
        UserLoginVO attacker = new UserLoginVO();
        attacker.setId(200L);
        attacker.setUserRole("user");

        when(agentChatSessionService.requireSessionAccessible(eq(9001L), eq("code_assistant"), eq(attacker)))
                .thenThrow(new BusinessException(ErrorCode.NO_PERMISSION, "无权限操作该会话"));

        AgentHistoryQueryRequest req = new AgentHistoryQueryRequest();
        req.setAgentCode("code_assistant");
        req.setSessionId(9001L);
        req.setPageSize(10);

        assertThrows(BusinessException.class,
                () -> agentChatMessageService.listHistory("code_assistant", req, attacker));
    }
}
