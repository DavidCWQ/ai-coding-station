package com.cwq.project_aicodingstation.chat.controller;

import com.cwq.project_aicodingstation.chat.dto.ChatHistoryAddRequest;
import com.cwq.project_aicodingstation.chat.dto.ChatSessionAddRequest;
import com.cwq.project_aicodingstation.chat.service.ChatHistoryService;
import com.cwq.project_aicodingstation.chat.service.ChatSessionService;
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

class ChatControllerWebMvcTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ChatHistoryService chatHistoryService;
    private ChatSessionService chatSessionService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        chatHistoryService = mock(ChatHistoryService.class);
        chatSessionService = mock(ChatSessionService.class);
        userService = mock(UserService.class);
        objectMapper = new ObjectMapper();

        ChatController controller = new ChatController();
        ReflectionTestUtils.setField(controller, "chatHistoryService", chatHistoryService);
        ReflectionTestUtils.setField(controller, "chatSessionService", chatSessionService);
        ReflectionTestUtils.setField(controller, "userService", userService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addMessage_returnsMessageId() throws Exception {
        ChatHistoryAddRequest req = new ChatHistoryAddRequest();
        req.setAppId(1L);
        req.setSessionId(2L);
        req.setMessage("hello");
        req.setMessageType("user");

        UserLoginVO user = new UserLoginVO();
        user.setId(9L);
        when(userService.getUserLoginVO(any())).thenReturn(user);
        when(chatHistoryService.addMessage(1L, 2L, "hello", "user", 9L)).thenReturn(99L);

        mockMvc.perform(post("/chat/history/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(99));
    }

    @Test
    void createSession_returnsSessionId() throws Exception {
        ChatSessionAddRequest req = new ChatSessionAddRequest();
        req.setAppId(100L);
        req.setTitle("new chat");

        UserLoginVO user = new UserLoginVO();
        user.setId(7L);
        when(userService.getUserLoginVO(any())).thenReturn(user);
        when(chatSessionService.createSession(100L, 7L, "new chat", user)).thenReturn(1234L);

        mockMvc.perform(post("/chat/session/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(1234));
    }
}
