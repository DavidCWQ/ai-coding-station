package com.cwq.project_aicodingstation.chat.service;

import com.cwq.project_aicodingstation.chat.dto.ChatSessionQueryRequest;
import com.cwq.project_aicodingstation.chat.entity.ChatSession;
import com.cwq.project_aicodingstation.chat.vo.ChatSessionVO;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.time.LocalDateTime;

/**
 * 对话会话业务接口。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
public interface ChatSessionService extends IService<ChatSession> {

    /**
     * 校验当前用户是否可访问该应用（创建者或管理员）。
     *
     * @param appId  应用 id（用于校验应用是否存在）
     * @param userVO 当前登录用户（用于校验对应用的访问权限）
     */
    void assertAppAccessible(Long appId, UserLoginVO userVO);

    /**
     * 创建会话（雪花 id 由持久层生成）。须为应用创建者或管理员；新会话归属 {@code userId}（通常为当前登录用户）。
     *
     * @param appId  应用 id
     * @param userId 会话所属用户 id
     * @param title  标题，可空则使用默认标题
     * @param userVO 当前登录用户（用于校验对应用的访问权限）
     * @return 新会话 id
     */
    Long createSession(Long appId, Long userId, String title, UserLoginVO userVO);

    /**
     * 分页查询当前用户在指定应用下的会话列表（按 last_msg_time 倒序）。
     *
     * @param req    查询条件（含 appId、分页）
     * @param userVO 登录用户
     * @return 会话分页数据
     */
    Page<ChatSessionVO> listByAppId(ChatSessionQueryRequest req, UserLoginVO userVO);

    /**
     * 逻辑删除会话（仅创建者或管理员）。
     *
     * @param sessionId 会话 id
     * @param userVO    当前用户
     * @return 是否成功
     */
    boolean deleteSession(Long sessionId, UserLoginVO userVO);

    /**
     * 更新会话标题（仅创建者或管理员）。
     *
     * @param sessionId 会话 id
     * @param title     新标题
     * @param userVO    当前用户
     * @return 是否成功
     */
    boolean updateTitle(Long sessionId, String title, UserLoginVO userVO);

    /**
     * 在 chat_history 插入新消息后，刷新会话的最后消息时间。
     *
     * @param sessionId 会话 id
     * @param at        消息时间
     */
    void touchLastMessageTime(Long sessionId, LocalDateTime at);
}
