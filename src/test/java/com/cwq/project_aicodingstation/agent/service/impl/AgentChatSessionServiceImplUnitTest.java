package com.cwq.project_aicodingstation.agent.service.impl;

import com.cwq.project_aicodingstation.agent.constant.AgentConstant;
import com.cwq.project_aicodingstation.agent.entity.AgentChatSession;
import com.cwq.project_aicodingstation.common.auth.ResourceAuthHelper;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class AgentChatSessionServiceImplUnitTest {

    @Test
    void autoTitleFromUserMessage_skipsWhenTitleNotDefault() {
        AgentChatSessionServiceImpl service = spy(new AgentChatSessionServiceImpl());
        ReflectionTestUtils.setField(service, "resourceAuthHelper", mock(ResourceAuthHelper.class));

        UserLoginVO user = new UserLoginVO();
        user.setId(1L);

        AgentChatSession existing = new AgentChatSession();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setTitle("custom-title");

        doReturn(existing).when(service).getById(10L);

        service.autoTitleFromUserMessage(10L, "hello world", user);

        verify(service, never()).updateById(any(AgentChatSession.class));
    }

    @Test
    void autoTitleFromUserMessage_updatesWhenDefaultTitle() {
        AgentChatSessionServiceImpl service = spy(new AgentChatSessionServiceImpl());
        ReflectionTestUtils.setField(service, "resourceAuthHelper", mock(ResourceAuthHelper.class));

        UserLoginVO user = new UserLoginVO();
        user.setId(2L);

        AgentChatSession existing = new AgentChatSession();
        existing.setId(11L);
        existing.setUserId(2L);
        existing.setTitle(AgentConstant.DEFAULT_SESSION_TITLE);

        doReturn(existing).when(service).getById(11L);
        doReturn(true).when(service).updateById(any(AgentChatSession.class));

        service.autoTitleFromUserMessage(11L, "  title from first user message  ", user);

        org.mockito.ArgumentCaptor<AgentChatSession> captor =
                org.mockito.ArgumentCaptor.forClass(AgentChatSession.class);
        verify(service).updateById(captor.capture());
        assertEquals(11L, captor.getValue().getId());
        String title = captor.getValue().getTitle();
        org.junit.jupiter.api.Assertions.assertTrue(title.startsWith("title from first user me"));
        org.junit.jupiter.api.Assertions.assertTrue(title.length() <= AgentConstant.AUTO_TITLE_MAX_LEN + 1);
    }
}
