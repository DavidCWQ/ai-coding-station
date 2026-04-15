package com.cwq.project_aicodingstation.agent.config;

import cn.hutool.core.io.IoUtil;
import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按智能体编码加载 classpath 下的系统提示词（缓存）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Component
public class AgentSystemPromptResolver {

    /**
     * 编码 -> 提示词正文
     */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 解析并返回系统提示词
     *
     * @param agent 智能体枚举
     * @return 提示词文本
     */
    public String resolve(AgentCodeEnum agent) {
        BusinessAssert.notNull(agent, ErrorCode.PARAMS_ERROR, "智能体为空");
        return cache.computeIfAbsent(agent.getCode(), this::loadFromClasspath);
    }

    /**
     * 从 classpath 读取 prompt/agent/{code}.txt
     *
     * @param code 智能体编码
     * @return 文本
     */
    private String loadFromClasspath(String code) {
        String path = "prompt/agent/" + code + ".txt";
        ClassPathResource resource = new ClassPathResource(path);
        BusinessAssert.requireTrue(resource.exists(), ErrorCode.SYSTEM_ERROR,
                "未找到智能体提示词: " + path
        );
        try (InputStream in = resource.getInputStream()) {
            return IoUtil.read(in, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取智能体提示词失败: " + path);
        }
    }
}
