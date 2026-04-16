package com.cwq.project_aicodingstation.chat.controller;

import com.cwq.project_aicodingstation.chat.dto.ChatHistoryAddRequest;
import com.cwq.project_aicodingstation.chat.dto.ChatHistoryQueryRequest;
import com.cwq.project_aicodingstation.chat.dto.ChatSessionAddRequest;
import com.cwq.project_aicodingstation.chat.dto.ChatSessionQueryRequest;
import com.cwq.project_aicodingstation.chat.dto.ChatSessionUpdateTitleRequest;
import com.cwq.project_aicodingstation.chat.service.ChatHistoryService;
import com.cwq.project_aicodingstation.chat.service.ChatSessionService;
import com.cwq.project_aicodingstation.chat.vo.ChatHistoryVO;
import com.cwq.project_aicodingstation.chat.vo.ChatSessionVO;
import com.cwq.project_aicodingstation.common.annotation.AuthCheck;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BaseException;
import com.cwq.project_aicodingstation.common.request.DeleteRequest;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话会话与对话历史接口层（参数接收与转发）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private UserService userService;

    /**
     * 按需获取登录用户：未登录时返回 null（用于匿名可读接口），其余异常继续抛出。
     */
    private UserLoginVO resolveLoginUserOrNull(HttpServletRequest request) {
        try {
            return userService.getUserLoginVO(request);
        } catch (BaseException e) {
            BusinessAssert.equals(e.getCode(), ErrorCode.NOT_LOGIN.getCode(),
                    ErrorCode.BUSINESS_ERROR, "登陆状态异常"
            );
            return null;
        }
    }

    /**
     * 新增一条对话消息（用于消息持久化）。
     */
    @PostMapping("/history/add")
    public BaseResponse<Long> addMessage(@RequestBody @Valid ChatHistoryAddRequest req,
                                         HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        return ResultUtils.success(chatHistoryService.addMessage(
                req.getAppId(), req.getSessionId(), req.getMessage(), req.getMessageType(), userVO.getId()
        ));
    }

    /**
     * 游标分页查询某会话下的对话历史（时间正序）。
     */
    @PostMapping("/history/list")
    public BaseResponse<List<ChatHistoryVO>> listHistory(@RequestBody @Valid ChatHistoryQueryRequest req,
                                                         HttpServletRequest request) {
        UserLoginVO userVO = resolveLoginUserOrNull(request);
        return ResultUtils.success(chatHistoryService.listHistory(req, userVO));
    }

    /**
     * 管理员查看全局最近对话（按创建时间倒序）。
     */
    @GetMapping("/history/admin/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<ChatHistoryVO>> listAllHistory(@RequestParam(defaultValue = "10") Long pageSize,
                                                            HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        return ResultUtils.success(chatHistoryService.listAll(pageSize, userVO));
    }

    /**
     * 创建新会话。
     */
    @PostMapping("/session/create")
    public BaseResponse<Long> createSession(@RequestBody @Valid ChatSessionAddRequest req,
                                            HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        Long sessionId = chatSessionService.createSession(
                req.getAppId(), userVO.getId(), req.getTitle(), userVO);
        return ResultUtils.success(sessionId);
    }

    /**
     * 分页查询当前用户在指定应用下的会话列表。
     */
    @PostMapping("/session/list")
    public BaseResponse<Page<ChatSessionVO>> listSessions(@RequestBody @Valid ChatSessionQueryRequest req,
                                                          HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        return ResultUtils.success(chatSessionService.listByAppId(req, userVO));
    }

    /**
     * 逻辑删除会话。
     */
    @PostMapping("/session/delete")
    public BaseResponse<Boolean> deleteSession(@RequestBody DeleteRequest req,
                                               HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        return ResultUtils.success(chatSessionService.deleteSession(req.getId(), userVO));
    }

    /**
     * 修改会话标题。
     */
    @PostMapping("/session/update/title")
    public BaseResponse<Boolean> updateSessionTitle(@RequestBody @Valid ChatSessionUpdateTitleRequest req,
                                                    HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        return ResultUtils.success(
                chatSessionService.updateTitle(req.getSessionId(), req.getTitle(), userVO));
    }
}
