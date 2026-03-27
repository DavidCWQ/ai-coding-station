package com.cwq.project_aicodingstation.chat.service;

import com.cwq.project_aicodingstation.chat.dto.ChatHistoryQueryRequest;
import com.cwq.project_aicodingstation.chat.entity.ChatHistory;
import com.cwq.project_aicodingstation.chat.vo.ChatHistoryVO;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 对话历史业务接口。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 持久化一条消息（用户 / AI 成功 / AI 失败均可），并刷新所属会话的 last_msg_time。
     *
     * @param appId       应用 id
     * @param sessionId   会话 id
     * @param message     消息内容（AI 失败时可为错误说明）
     * @param messageType 消息类型：user / ai / system
     * @param userId      产生该条消息的用户 id
     * @return 消息 id（雪花 id）
     */
    Long addMessage(Long appId, Long sessionId, String message, String messageType, Long userId);

    /**
     * 根据 appId 关联删除对话历史（用于删除应用时兜底清理）。
     *
     * @param appId 应用 id
     * @return 是否成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 游标分页查询对话历史：默认最新一页；beforeMessageId 有值时向前加载更早消息。
     * 结果按时间正序（旧 → 新），便于前端直接渲染。
     *
     * @param req    查询参数
     * @param userVO 登录用户
     * @return 当前页消息列表（时间正序）
     */
    List<ChatHistoryVO> listHistory(ChatHistoryQueryRequest req, UserLoginVO userVO);

    /**
     * 管理员查看全局最近消息（按 create_time 倒序，仅未逻辑删除）。
     *
     * @param pageSize 条数上限
     * @param userVO   登录用户（须为管理员）
     * @return 消息列表（时间倒序，与数据查询顺序一致）
     */
    List<ChatHistoryVO> listAll(Long pageSize, UserLoginVO userVO);

    /**
     * 根据会话 id 加载历史对话（USER/AI）到 chatMemory
     *
     * @param sessionId  会话 id
     * @param chatMemory 对话记忆
     * @param maxCount   对话条数上限
     * */
    void loadChatHistoryToMemory(Long sessionId, MessageWindowChatMemory chatMemory, int maxCount);
}
