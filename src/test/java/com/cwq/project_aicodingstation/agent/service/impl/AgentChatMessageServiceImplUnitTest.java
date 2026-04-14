package com.cwq.project_aicodingstation.agent.service.impl;

import com.cwq.project_aicodingstation.agent.entity.AgentChatMessage;
import com.cwq.project_aicodingstation.agent.service.AgentChatSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;

class AgentChatMessageServiceImplUnitTest {

    @Test
    void listRecentForContext_returnsEmptyWhenInvalidArgs() {
        AgentChatMessageServiceImpl service = new AgentChatMessageServiceImpl();
        ReflectionTestUtils.setField(service, "agentChatSessionService", mock(AgentChatSessionService.class));

        assertTrue(service.listRecentForContext(null, 10).isEmpty());
        assertTrue(service.listRecentForContext(1L, 0).isEmpty());
    }

    @Test
    void listRecentForContext_returnsAscendingOrder() {
        AgentChatMessageServiceImpl service = spy(new AgentChatMessageServiceImpl());
        ReflectionTestUtils.setField(service, "agentChatSessionService", mock(AgentChatSessionService.class));

        AgentChatMessage m1 = new AgentChatMessage();
        m1.setId(3L);
        m1.setCreateTime(LocalDateTime.of(2026, 1, 1, 10, 0));

        AgentChatMessage m2 = new AgentChatMessage();
        m2.setId(2L);
        m2.setCreateTime(LocalDateTime.of(2026, 1, 1, 9, 0));

        AgentChatMessage m3 = new AgentChatMessage();
        m3.setId(1L);
        m3.setCreateTime(LocalDateTime.of(2026, 1, 1, 8, 0));

        // Simulate mapper returning DESC order from DB.
        doReturn(new ArrayList<>(List.of(m1, m2, m3)))
                .when(service).list(org.mockito.ArgumentMatchers.any(com.mybatisflex.core.query.QueryWrapper.class));

        List<AgentChatMessage> result = service.listRecentForContext(9L, 3);
        assertEquals(List.of(1L, 2L, 3L), result.stream().map(AgentChatMessage::getId).toList());
    }
}
