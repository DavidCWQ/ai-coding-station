package com.cwq.project_aicodingstation.chat.service;

import com.cwq.project_aicodingstation.chat.entity.ChatSession;

import java.util.List;

public interface ChatSessionService {

    Long createSession(Long appId, Long userId, String title);

    List<ChatSession> listByUserId(Long userId);

    boolean deleteSession(Long sessionId, Long userId);
}
