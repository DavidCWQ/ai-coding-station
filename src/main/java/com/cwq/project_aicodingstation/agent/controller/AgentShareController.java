package com.cwq.project_aicodingstation.agent.controller;

import com.cwq.project_aicodingstation.agent.dto.AgentShareCreateRequest;
import com.cwq.project_aicodingstation.agent.dto.AgentShareRevokeRequest;
import com.cwq.project_aicodingstation.agent.service.AgentChatShareService;
import com.cwq.project_aicodingstation.agent.vo.AgentShareCreatedVO;
import com.cwq.project_aicodingstation.agent.vo.AgentShareView;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/share")
public class AgentShareController {

    @Resource
    private AgentChatShareService agentChatShareService;

    @Resource
    private UserService userService;

    @PostMapping("/create")
    public BaseResponse<AgentShareCreatedVO> create(@RequestBody @Valid AgentShareCreateRequest req,
                                                    HttpServletRequest request) {
        UserLoginVO user = userService.getUserLoginVO(request);
        return ResultUtils.success(agentChatShareService.create(req.getSessionId(), req.getMessageIds(), user));
    }

    @PostMapping("/revoke")
    public BaseResponse<Boolean> revoke(@RequestBody @Valid AgentShareRevokeRequest req,
                                        HttpServletRequest request) {
        UserLoginVO user = userService.getUserLoginVO(request);
        agentChatShareService.revoke(req.getToken(), user);
        return ResultUtils.success(true);
    }

    /**
     * 公开只读。令牌无效或已停止时返回「分享已失效」。
     */
    @GetMapping("/{token}")
    public BaseResponse<AgentShareView> view(@PathVariable String token) {
        return ResultUtils.success(agentChatShareService.view(token));
    }
}
