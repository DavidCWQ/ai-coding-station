package com.cwq.project_aicodingstation.app.controller;

import com.cwq.project_aicodingstation.app.config.AppDeployConfig;
import com.cwq.project_aicodingstation.app.dto.AppAddRequest;
import com.cwq.project_aicodingstation.app.service.AppService;
import com.cwq.project_aicodingstation.app.vo.AppVO;
import com.cwq.project_aicodingstation.common.handler.GlobalExceptionHandler;
import com.cwq.project_aicodingstation.core.download.ProjectDownloadService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppControllerWebMvcTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private AppService appService;

    private UserService userService;

    private AppDeployConfig appDeployConfig;

    private ProjectDownloadService projectDownloadService;

    @BeforeEach
    void setUp() {
        appService = mock(AppService.class);
        userService = mock(UserService.class);
        appDeployConfig = mock(AppDeployConfig.class);
        projectDownloadService = mock(ProjectDownloadService.class);
        objectMapper = new ObjectMapper();

        AppController controller = new AppController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "appService", appService);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "userService", userService);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "appDeployConfig", appDeployConfig);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "projectDownloadService", projectDownloadService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createApp_returnsCreatedId() throws Exception {
        AppAddRequest req = new AppAddRequest();
        req.setAppName("todo");
        req.setInitPrompt("build todo");
        req.setCodeGenType("HTML");

        UserLoginVO user = new UserLoginVO();
        user.setId(11L);
        when(userService.getUserLoginVO(any())).thenReturn(user);
        when(appService.createApp(any(AppAddRequest.class), any(UserLoginVO.class))).thenReturn(88L);

        mockMvc.perform(post("/app/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(88));
    }

    @Test
    void downloadAppCode_rejectsInvalidAppId() throws Exception {
        mockMvc.perform(get("/app/download/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.message").value("应用ID无效"));
    }

    @Test
    void getApp_returnsAppVO() throws Exception {
        AppVO vo = new AppVO();
        vo.setId(100L);
        vo.setAppName("sample");
        when(appService.getAppVOById(100L)).thenReturn(vo);

        mockMvc.perform(get("/app/get/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.appName").value("sample"));
    }
}
