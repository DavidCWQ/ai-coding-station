package com.cwq.project_aicodingstation.agent.controller;

import com.cwq.project_aicodingstation.agent.dto.AgentChatStreamRequest;
import com.cwq.project_aicodingstation.agent.dto.AgentHistoryQueryRequest;
import com.cwq.project_aicodingstation.agent.dto.AgentSessionAddRequest;
import com.cwq.project_aicodingstation.agent.dto.AgentSessionQueryRequest;
import com.cwq.project_aicodingstation.agent.dto.AgentSessionUpdateTitleRequest;
import com.cwq.project_aicodingstation.agent.service.AgentChatMessageService;
import com.cwq.project_aicodingstation.agent.service.AgentChatSessionService;
import com.cwq.project_aicodingstation.agent.service.AgentChatAssistService;
import com.cwq.project_aicodingstation.agent.vo.AgentChatMessageVO;
import com.cwq.project_aicodingstation.agent.vo.AgentChatSessionVO;
import com.cwq.project_aicodingstation.common.request.DeleteRequest;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.ai.utils.SSEStreamUtils;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 智能体会话、历史与流式对话接口（与 {@code /chat} 应用对话隔离）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Resource
    private AgentChatSessionService agentChatSessionService;

    @Resource
    private AgentChatMessageService agentChatMessageService;

    @Resource
    private AgentChatAssistService agentChatAssistService;

    @Resource
    private UserService userService;

    /**
     * 创建智能体会话
     */
    @PostMapping("/session/create")
    public BaseResponse<Long> createSession(@RequestBody @Valid AgentSessionAddRequest req,
                                            HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        Long id = agentChatSessionService.createSession(req.getAgentCode(), req.getTitle(), userVO);
        return ResultUtils.success(id);
    }

    /**
     * 分页列出当前用户在某智能体下的会话
     */
    @PostMapping("/session/list")
    public BaseResponse<Page<AgentChatSessionVO>> listSessions(@RequestBody @Valid AgentSessionQueryRequest req,
                                                               HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        return ResultUtils.success(agentChatSessionService.listSessions(req, userVO));
    }

    /**
     * 删除会话
     */
    @PostMapping("/session/delete")
    public BaseResponse<Boolean> deleteSession(@RequestBody DeleteRequest req,
                                               HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        return ResultUtils.success(agentChatSessionService.deleteSession(req.getId(), userVO));
    }

    /**
     * 修改会话标题
     */
    @PostMapping("/session/update/title")
    public BaseResponse<Boolean> updateSessionTitle(@RequestBody @Valid AgentSessionUpdateTitleRequest req,
                                                    HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        return ResultUtils.success(
                agentChatSessionService.updateTitle(req.getSessionId(), req.getTitle(), userVO));
    }

    /**
     * 游标分页查询会话内历史消息（时间正序）
     */
    @PostMapping("/history/list")
    public BaseResponse<List<AgentChatMessageVO>> listHistory(@RequestBody @Valid AgentHistoryQueryRequest req,
                                                              HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        return ResultUtils.success(
                agentChatMessageService.listHistory(req.getAgentCode(), req, userVO));
    }

    /**
     * 智能体流式对话 (POST SSE，数据格式与 /app/chat/genCode 一致：{@code {"d":"..."}} + {@code event:done})
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody @Valid AgentChatStreamRequest req,
                                                    HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        return SSEStreamUtils.toJsonDataSSE(agentChatAssistService.chatStream(req, userVO));
    }
}
