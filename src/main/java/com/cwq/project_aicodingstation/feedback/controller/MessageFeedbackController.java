package com.cwq.project_aicodingstation.feedback.controller;

import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.feedback.dto.MessageVoteRequest;
import com.cwq.project_aicodingstation.feedback.service.MessageFeedbackService;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feedback")
public class MessageFeedbackController {

    @Resource
    private MessageFeedbackService messageFeedbackService;

    @Resource
    private UserService userService;

    @PostMapping("/vote")
    public BaseResponse<Boolean> vote(@RequestBody @Valid MessageVoteRequest req, HttpServletRequest request) {
        UserLoginVO user = userService.getUserLoginVO(request);
        messageFeedbackService.vote(user, req.getSource(), req.getMessageId(), req.getVote(), req.getReason());
        return ResultUtils.success(true);
    }
}
