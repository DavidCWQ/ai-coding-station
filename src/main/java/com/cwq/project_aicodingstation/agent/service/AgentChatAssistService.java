package com.cwq.project_aicodingstation.agent.service;

import com.cwq.project_aicodingstation.agent.dto.AgentChatStreamRequest;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import reactor.core.publisher.Flux;

/**
 * 智能体对话业务编排（鉴权、落库、调用 AI 门面），对应应用侧 {@link com.cwq.project_aicodingstation.app.service.AppService#chatToGenCode} 的角色。
 * <p>
 * {@link com.cwq.project_aicodingstation.ai.AgentChatService} 为 LangChain4j 生成的纯模型接口，二者勿混用包名。
 * </p>
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
public interface AgentChatAssistService {

    /**
     * 智能体流式对话：写入用户消息、流式生成、结束时写入助手消息。
     *
     * @param req    请求
     * @param userVO 当前用户
     * @return 助手文本片段流
     */
    Flux<String> chatStream(AgentChatStreamRequest req, UserLoginVO userVO);
}
