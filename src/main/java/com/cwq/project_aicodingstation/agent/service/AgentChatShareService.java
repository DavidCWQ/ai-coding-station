package com.cwq.project_aicodingstation.agent.service;

import com.cwq.project_aicodingstation.agent.vo.AgentShareCreatedVO;
import com.cwq.project_aicodingstation.agent.vo.AgentShareView;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;

import java.util.List;

public interface AgentChatShareService {

    AgentShareCreatedVO create(Long sessionId, List<Long> messageIds, UserLoginVO user);

    void revoke(String token, UserLoginVO user);

    /**
     * 会话删除时，让已经发出的链接失效。没有分享时也成功。
     */
    void revokeBySession(Long sessionId);

    AgentShareView view(String token);
}
