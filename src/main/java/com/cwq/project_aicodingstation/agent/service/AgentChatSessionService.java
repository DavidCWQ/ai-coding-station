package com.cwq.project_aicodingstation.agent.service;

import com.cwq.project_aicodingstation.agent.dto.AgentSessionQueryRequest;
import com.cwq.project_aicodingstation.agent.entity.AgentChatSession;
import com.cwq.project_aicodingstation.agent.vo.AgentChatSessionVO;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

/**
 * 智能体会话业务接口。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
public interface AgentChatSessionService extends IService<AgentChatSession> {

    /**
     * 创建会话
     *
     * @param agentCode 智能体编码
     * @param title     标题，可空
     * @param userVO    当前用户
     * @return 新会话 id
     */
    Long createSession(String agentCode, String title, UserLoginVO userVO);

    /**
     * 分页列出当前用户在某智能体下的会话
     *
     * @param req    查询条件
     * @param userVO 当前用户
     * @return 分页结果
     */
    Page<AgentChatSessionVO> listSessions(AgentSessionQueryRequest req, UserLoginVO userVO);

    /**
     * 删除会话（软删除）
     *
     * @param sessionId 会话 id
     * @param userVO    当前用户
     * @return 是否成功
     */
    boolean deleteSession(Long sessionId, UserLoginVO userVO);

    /**
     * 更新标题
     *
     * @param sessionId 会话 id
     * @param title     新标题
     * @param userVO    当前用户
     * @return 是否成功
     */
    boolean updateTitle(Long sessionId, String title, UserLoginVO userVO);

    /**
     * 校验会话可读/可写，且 agentCode 与会话一致（管理员放行读取同会话需与业务约定；此处与会话属主或管理员）。
     *
     * @param sessionId 会话 id
     * @param agentCode 期望的智能体编码
     * @param userVO    当前用户
     * @return 会话实体
     */
    AgentChatSession requireSessionAccessible(Long sessionId, String agentCode, UserLoginVO userVO);

    /**
     * 更新最后消息时间
     *
     * @param sessionId 会话 id
     * @param at        时间
     */
    void touchLastMessageTime(Long sessionId, java.time.LocalDateTime at);

    /**
     * 若标题仍为默认，则用用户首句截取生成标题
     *
     * @param sessionId 会话 id
     * @param userText  用户原文
     * @param userVO    当前用户
     */
    void autoTitleFromUserMessage(Long sessionId, String userText, UserLoginVO userVO);
}
