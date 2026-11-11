package com.cwq.project_aicodingstation.feedback.service.impl;

import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.agent.entity.AgentChatMessage;
import com.cwq.project_aicodingstation.agent.mapper.AgentChatMessageMapper;
import com.cwq.project_aicodingstation.chat.entity.ChatHistory;
import com.cwq.project_aicodingstation.chat.mapper.ChatHistoryMapper;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.feedback.entity.MessageFeedback;
import com.cwq.project_aicodingstation.feedback.mapper.MessageFeedbackMapper;
import com.cwq.project_aicodingstation.feedback.service.MessageFeedbackService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class MessageFeedbackServiceImpl extends ServiceImpl<MessageFeedbackMapper, MessageFeedback>
        implements MessageFeedbackService {

    @Resource
    private ChatHistoryMapper chatHistoryMapper;

    @Resource
    private AgentChatMessageMapper agentChatMessageMapper;

    @Override
    public void vote(UserLoginVO user, String source, Long messageId, Integer vote, String reason) {
        BusinessAssert.notNull(user, ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.notNull(user.getId(), ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.notNull(messageId, ErrorCode.PARAMS_MISSING, "消息 id 不能为空");
        BusinessAssert.notNull(vote, ErrorCode.PARAMS_MISSING, "评价不能为空");
        String src = normalizeSource(source);
        BusinessAssert.requireTrue(vote == 1 || vote == -1 || vote == 0,
                ErrorCode.PARAMS_ERROR, "评价取值不正确");
        assertOwnsMessage(src, messageId, user.getId());

        MessageFeedback existing = findOne(user.getId(), src, messageId);
        if (vote == 0) {
            if (existing != null) {
                this.removeById(existing.getId());
            }
            return;
        }

        String storedReason = vote == -1 && StrUtil.isNotBlank(reason) ? reason.trim() : "";
        if (storedReason != null) {
            BusinessAssert.requireTrue(storedReason.length() <= 200, ErrorCode.PARAMS_ERROR, "原因最多 200 字");
        }
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            MessageFeedback row = MessageFeedback.builder()
                    .userId(user.getId())
                    .source(src)
                    .messageId(messageId)
                    .vote(vote)
                    .reason(storedReason)
                    .createTime(now)
                    .updateTime(now)
                    .build();
            BusinessAssert.requireTrue(this.save(row), ErrorCode.SYSTEM_ERROR, "保存评价失败");
            return;
        }
        MessageFeedback update = new MessageFeedback();
        update.setId(existing.getId());
        update.setVote(vote);
        update.setReason(storedReason);
        update.setUpdateTime(now);
        BusinessAssert.requireTrue(this.updateById(update), ErrorCode.BUSINESS_ERROR, "更新评价失败");
    }

    @Override
    public Map<Long, Integer> votesOf(Long userId, String source, Collection<Long> messageIds) {
        if (userId == null || messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }
        String src = normalizeSource(source);
        List<MessageFeedback> rows = this.list(QueryWrapper.create()
                .eq("user_id", userId)
                .eq("source", src)
                .in("message_id", messageIds));
        Map<Long, Integer> out = new HashMap<>();
        for (MessageFeedback row : rows) {
            if (row.getMessageId() != null && row.getVote() != null && row.getVote() != 0) {
                out.put(row.getMessageId(), row.getVote());
            }
        }
        return out;
    }

    private MessageFeedback findOne(Long userId, String source, Long messageId) {
        return this.getOne(QueryWrapper.create()
                .eq("user_id", userId)
                .eq("source", source)
                .eq("message_id", messageId));
    }

    private void assertOwnsMessage(String source, Long messageId, Long userId) {
        if (MessageFeedbackService.SOURCE_APP.equals(source)) {
            ChatHistory row = chatHistoryMapper.selectOneById(messageId);
            BusinessAssert.notNull(row, ErrorCode.NOT_FOUND, "消息不存在");
            BusinessAssert.equals(row.getUserId(), userId, ErrorCode.NO_PERMISSION, "只能评价自己的对话");
            return;
        }
        AgentChatMessage row = agentChatMessageMapper.selectOneById(messageId);
        BusinessAssert.notNull(row, ErrorCode.NOT_FOUND, "消息不存在");
        BusinessAssert.equals(row.getUserId(), userId, ErrorCode.NO_PERMISSION, "只能评价自己的对话");
    }

    private static String normalizeSource(String source) {
        String src = StrUtil.blankToDefault(source, "").trim();
        BusinessAssert.requireTrue(MessageFeedbackService.SOURCE_APP.equals(src)
                        || MessageFeedbackService.SOURCE_AGENT.equals(src),
                ErrorCode.PARAMS_ERROR, "评价来源不正确");
        return src;
    }
}
