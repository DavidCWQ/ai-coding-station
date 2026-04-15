package com.cwq.project_aicodingstation.agent.service;

import com.cwq.project_aicodingstation.agent.dto.AgentHistoryQueryRequest;
import com.cwq.project_aicodingstation.agent.entity.AgentChatMessage;
import com.cwq.project_aicodingstation.agent.vo.AgentChatMessageVO;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 智能体消息业务接口。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
public interface AgentChatMessageService extends IService<AgentChatMessage> {

    /**
     * 写入一条智能体会话消息
     *
     * @param sessionId   会话 id
     * @param agentCode   智能体编码
     * @param userId      用户 id（会话属主）
     * @param text        正文
     * @param messageType 消息类型，取值见
     * {@link com.cwq.project_aicodingstation.chat.enums.MessageTypeEnum}
     * @return 新消息 id
     */
    Long addMessage(Long sessionId, String agentCode, Long userId, String text, String messageType);

    /**
     * 游标分页拉取历史（时间正序）
     *
     * @param agentCode 智能体编码（与会话一致）
     * @param req       请求
     * @param userVO    当前用户
     * @return 消息列表
     */
    List<AgentChatMessageVO> listHistory(String agentCode, AgentHistoryQueryRequest req, UserLoginVO userVO);

    /**
     * 加载最近若干条用于模型上下文（时间正序，不含本次用户新消息）
     *
     * @param sessionId 会话 id
     * @param limit     最大条数
     * @return 消息实体列表
     */
    List<AgentChatMessage> listRecentForContext(Long sessionId, int limit);

    /**
     * 将会话内最近若干条消息载入 LangChain 记忆（排除最新一条用户消息，避免与当前轮重复；与 chat 模块语义一致）。
     *
     * @param sessionId  智能体会话 id
     * @param chatMemory 目标记忆
     * @param maxCount   最大条数
     */
    void loadChatHistoryToMemory(Long sessionId, MessageWindowChatMemory chatMemory, int maxCount);
}
