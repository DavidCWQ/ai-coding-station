package com.cwq.project_aicodingstation.user.controller;

import com.cwq.project_aicodingstation.common.handler.GlobalExceptionHandler;
import com.cwq.project_aicodingstation.user.dto.UserLoginRequest;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerWebMvcTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        objectMapper = new ObjectMapper();
        UserController controller = new UserController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "userService", userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void userLogin_returnsWrappedSuccessResponse() throws Exception {
        UserLoginRequest req = new UserLoginRequest();
        req.setUserAccount("david");
        req.setUserPassword("secret");

        UserLoginVO vo = new UserLoginVO();
        vo.setId(101L);
        vo.setUserAccount("david");
        vo.setUserRole("user");

        when(userService.userLogin(any(UserLoginRequest.class), any())).thenReturn(vo);

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.userAccount").value("david"));

        verify(userService).userLogin(any(UserLoginRequest.class), any());
    }

    @Test
    void getLoginUser_returnsCurrentUser() throws Exception {
        UserLoginVO vo = new UserLoginVO();
        vo.setId(7L);
        vo.setUserName("David");
        vo.setUserRole("admin");

        when(userService.getUserLoginVO(any())).thenReturn(vo);

        mockMvc.perform(get("/user/get/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.userRole").value("admin"));
    }
}
