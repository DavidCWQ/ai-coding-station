package com.cwq.project_aicodingstation.agent.service.impl;

import com.cwq.project_aicodingstation.agent.config.AgentSystemPromptResolver;
import com.cwq.project_aicodingstation.agent.dto.AgentChatStreamRequest;
import com.cwq.project_aicodingstation.agent.entity.AgentChatSession;
import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import com.cwq.project_aicodingstation.agent.service.AgentChatAssistService;
import com.cwq.project_aicodingstation.agent.service.AgentChatMessageService;
import com.cwq.project_aicodingstation.agent.service.AgentChatSessionService;
import com.cwq.project_aicodingstation.ai.facade.AgentChatFacade;
import com.cwq.project_aicodingstation.chat.enums.MessageTypeEnum;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

/**
 * 智能体对话业务编排实现。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Slf4j
@Service
public class AgentChatAssistServiceImpl implements AgentChatAssistService {

    @Resource
    private AgentChatSessionService agentChatSessionService;

    @Resource
    private AgentChatMessageService agentChatMessageService;

    @Resource
    private AgentSystemPromptResolver agentSystemPromptResolver;

    @Resource
    private AgentChatFacade agentChatFacade;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public Flux<String> chatStream(AgentChatStreamRequest req, UserLoginVO userVO) {
        BusinessAssert.notNull(req, ErrorCode.PARAMS_MISSING, "请求为空");
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");

        AgentCodeEnum agent = AgentCodeEnum.requireValid(req.getAgentCode());
        Long sessionId = req.getSessionId();
        String userText = req.getMessage().trim();

        AgentChatSession session = agentChatSessionService.requireSessionAccessible(
                sessionId, agent.getCode(), userVO);
        Long ownerUserId = session.getUserId();
        BusinessAssert.notNull(ownerUserId, ErrorCode.SYSTEM_ERROR, "会话用户异常");

        log.info("智能体流式对话开始 userId={}, agentCode={}, sessionId={}, messageLen={}",
                userVO.getId(), agent.getCode(), sessionId, userText.length());

        transactionTemplate.executeWithoutResult(status -> {
            agentChatMessageService.addMessage(
                    sessionId, agent.getCode(), ownerUserId, userText, MessageTypeEnum.USER.getValue());
            agentChatSessionService.autoTitleFromUserMessage(sessionId, userText, userVO);
        });

        String systemPrompt = agentSystemPromptResolver.resolve(agent);

        StringBuilder accumulator = new StringBuilder();
        return agentChatFacade.chatStream(sessionId, agent.getCode(), systemPrompt, userText)
                .doOnNext(accumulator::append)
                .doOnComplete(() -> {
                    String full = accumulator.toString();
                    if (full.isEmpty()) {
                        full = "（模型未返回有效内容，请稍后重试。）";
                    }
                    String finalFull = full;
                    transactionTemplate.executeWithoutResult(st ->
                            agentChatMessageService.addMessage(
                                    sessionId, agent.getCode(), ownerUserId, finalFull,
                                    MessageTypeEnum.AI.getValue())
                    );
                });
    }
}
