package com.cwq.project_aicodingstation.agent.controller;

import com.cwq.project_aicodingstation.agent.dto.AgentSessionAddRequest;
import com.cwq.project_aicodingstation.agent.service.AgentChatAssistService;
import com.cwq.project_aicodingstation.agent.service.AgentChatMessageService;
import com.cwq.project_aicodingstation.agent.service.AgentChatSessionService;
import com.cwq.project_aicodingstation.common.handler.GlobalExceptionHandler;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentControllerWebMvcTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AgentChatSessionService agentChatSessionService;
    private AgentChatMessageService agentChatMessageService;
    private AgentChatAssistService agentChatAssistService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        agentChatSessionService = mock(AgentChatSessionService.class);
        agentChatMessageService = mock(AgentChatMessageService.class);
        agentChatAssistService = mock(AgentChatAssistService.class);
        userService = mock(UserService.class);
        objectMapper = new ObjectMapper();

        AgentController controller = new AgentController();
        ReflectionTestUtils.setField(controller, "agentChatSessionService", agentChatSessionService);
        ReflectionTestUtils.setField(controller, "agentChatMessageService", agentChatMessageService);
        ReflectionTestUtils.setField(controller, "agentChatAssistService", agentChatAssistService);
        ReflectionTestUtils.setField(controller, "userService", userService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createSession_returnsId() throws Exception {
        AgentSessionAddRequest req = new AgentSessionAddRequest();
        req.setAgentCode("code_assistant");
        req.setTitle("assistant");

        UserLoginVO user = new UserLoginVO();
        user.setId(66L);
        when(userService.getUserLoginVO(any())).thenReturn(user);
        when(agentChatSessionService.createSession("code_assistant", "assistant", user)).thenReturn(3001L);

        mockMvc.perform(post("/agent/session/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(3001));
    }

    @Test
    void createSession_rejectsBlankAgentCode() throws Exception {
        AgentSessionAddRequest req = new AgentSessionAddRequest();
        req.setAgentCode(" ");
        req.setTitle("assistant");

        mockMvc.perform(post("/agent/session/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50000));
    }
}
