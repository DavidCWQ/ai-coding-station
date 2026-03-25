package com.cwq.project_aicodingstation.chat.service;

import com.cwq.project_aicodingstation.chat.entity.ChatHistory;
import java.util.List;

public interface ChatHistoryService {

    Long addMessage(ChatHistory chatHistory);

    List<ChatHistory> listByAppId(Long appId);

    List<ChatHistory> listBySessionId(Long sessionId, int limit);

    List<ChatHistory> getContext(Long sessionId, Long beforeMessageId, int maxCount);
}
