package com.cwq.project_aicodingstation.agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.cwq.project_aicodingstation.agent.dto.AgentHistoryQueryRequest;
import com.cwq.project_aicodingstation.agent.entity.AgentChatMessage;
import com.cwq.project_aicodingstation.agent.entity.AgentChatSession;
import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import com.cwq.project_aicodingstation.agent.mapper.AgentChatMessageMapper;
import com.cwq.project_aicodingstation.agent.service.AgentChatMessageService;
import com.cwq.project_aicodingstation.agent.service.AgentChatSessionService;
import com.cwq.project_aicodingstation.agent.vo.AgentChatMessageVO;
import com.cwq.project_aicodingstation.app.constant.AppConstant;
import com.cwq.project_aicodingstation.chat.enums.MessageTypeEnum;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.mybatisflex.core.query.QueryMethods.column;

/**
 * 智能体消息业务实现。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AgentChatMessageServiceImpl extends ServiceImpl<AgentChatMessageMapper, AgentChatMessage>
        implements AgentChatMessageService {

    @Resource
    private AgentChatSessionService agentChatSessionService;

    @Override
    public Long addMessage(Long sessionId, String agentCode, Long userId, String text, String messageType) {
        BusinessAssert.notNull(sessionId, ErrorCode.PARAMS_MISSING, "会话 id 不能为空");
        BusinessAssert.notBlank(agentCode, ErrorCode.PARAMS_MISSING, "智能体编码不能为空");
        BusinessAssert.notNull(userId, ErrorCode.PARAMS_MISSING, "用户 id 不能为空");
        BusinessAssert.notBlank(text, ErrorCode.PARAMS_MISSING, "消息内容不能为空");
        BusinessAssert.notNull(MessageTypeEnum.getEnumByValue(messageType),
                ErrorCode.PARAMS_ERROR, "非法的消息类型"
        );

        LocalDateTime now = LocalDateTime.now();
        AgentChatMessage row = AgentChatMessage.builder()
                .sessionId(sessionId)
                .userId(userId)
                .agentCode(agentCode.trim())
                .message(text)
                .messageType(messageType)
                .metadata(null)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();

        BusinessAssert.requireTrue(this.save(row), ErrorCode.SYSTEM_ERROR, "保存智能体消息失败");
        agentChatSessionService.touchLastMessageTime(sessionId, now);
        return row.getId();
    }

    @Override
    public List<AgentChatMessageVO> listHistory(String agentCode, AgentHistoryQueryRequest req, UserLoginVO userVO) {
        BusinessAssert.notNull(req, ErrorCode.PARAMS_ERROR, "查询请求为空");
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");
        AgentCodeEnum agent = AgentCodeEnum.requireValid(agentCode);

        Long sessionId = req.getSessionId();
        AgentChatSession session = agentChatSessionService.requireSessionAccessible(
                sessionId, agent.getCode(), userVO);

        int pageSize = req.getPageSize() == null ? 20 : req.getPageSize();
        BusinessAssert.requireTrue(pageSize > 0 && pageSize <= AppConstant.MAX_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页条数非法或超过上限");

        AgentChatMessage anchor = null;
        if (req.getBeforeMessageId() != null) {
            anchor = this.getById(req.getBeforeMessageId());
            BusinessAssert.notNull(anchor, ErrorCode.NOT_FOUND, "游标消息不存在");
            BusinessAssert.equals(anchor.getSessionId(), sessionId, ErrorCode.PARAMS_ERROR, "游标消息不属于该会话");
            BusinessAssert.equals(anchor.getUserId(), session.getUserId(), ErrorCode.NO_PERMISSION, "无权限查看该消息");
        }

        QueryWrapper qw = buildHistoryQuery(sessionId, anchor, pageSize, req.getBeforeCreateTime());
        List<AgentChatMessage> rows = this.list(qw);
        List<AgentChatMessage> ordered = new ArrayList<>(rows);
        Collections.reverse(ordered);
        return ordered.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<AgentChatMessage> listRecentForContext(Long sessionId, int limit) {
        if (sessionId == null || limit <= 0) {
            return List.of();
        }
        QueryWrapper qw = QueryWrapper.create();
        qw.eq("session_id", sessionId);
        qw.orderBy("create_time", false);
        qw.limit(limit);
        List<AgentChatMessage> rows = this.list(qw);
        Collections.reverse(rows);
        return rows;
    }

    @Override
    public void loadChatHistoryToMemory(Long sessionId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            List<AgentChatMessage> historyList = this.list(
                    QueryWrapper.create()
                            .eq(AgentChatMessage::getSessionId, sessionId)
                            .orderBy(AgentChatMessage::getCreateTime, false)
                            .limit(1, maxCount)
            );

            if (historyList.isEmpty()) {
                return;
            }

            Collections.reverse(historyList);
            chatMemory.clear();
            int count = 0;
            for (AgentChatMessage h : historyList) {
                if (MessageTypeEnum.USER.getValue().equals(h.getMessageType())) {
                    chatMemory.add(UserMessage.from(h.getMessage()));
                } else if (MessageTypeEnum.AI.getValue().equals(h.getMessageType())) {
                    chatMemory.add(AiMessage.from(h.getMessage()));
                }
                count++;
            }
            log.info("加载智能体历史对话成功，sessionId={}, count={}", sessionId, count);
        } catch (Exception e) {
            log.error("加载智能体历史对话失败，sessionId: {}, error: {}", sessionId, e.getMessage(), e);
        }
    }

    /**
     * 构建历史分页条件（与 chat_history 游标策略一致）
     */
    private QueryWrapper buildHistoryQuery(Long sessionId, AgentChatMessage anchor, int pageSize,
                                           LocalDateTime beforeCreateTime) {
        QueryWrapper qw = QueryWrapper.create();
        qw.eq("session_id", sessionId);

        if (anchor != null) {
            QueryCondition beforeAnchor = column("create_time").lt(anchor.getCreateTime())
                    .or(column("create_time").eq(anchor.getCreateTime())
                            .and(column("id").lt(anchor.getId())));
            qw.and(beforeAnchor);
        } else if (beforeCreateTime != null) {
            qw.lt("create_time", beforeCreateTime);
        }

        qw.orderBy("create_time", false);
        qw.limit(pageSize);
        return qw;
    }

    private AgentChatMessageVO toVO(AgentChatMessage m) {
        AgentChatMessageVO vo = new AgentChatMessageVO();
        BeanUtil.copyProperties(m, vo);
        return vo;
    }
}
