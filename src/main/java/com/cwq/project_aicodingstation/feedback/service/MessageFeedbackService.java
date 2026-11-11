package com.cwq.project_aicodingstation.feedback.service;

import com.cwq.project_aicodingstation.user.vo.UserLoginVO;

import java.util.Collection;
import java.util.Map;

public interface MessageFeedbackService {

    String SOURCE_APP = "app";

    String SOURCE_AGENT = "agent";

    /**
     * 写入、覆盖或取消当前用户对一条消息的评价。
     *
     * @param vote 1、-1 或 0（取消）
     */
    void vote(UserLoginVO user, String source, Long messageId, Integer vote, String reason);

    /**
     * 当前用户在这批消息上的评价。没有评价的 id 不出现在结果里。
     */
    Map<Long, Integer> votesOf(Long userId, String source, Collection<Long> messageIds);
}
