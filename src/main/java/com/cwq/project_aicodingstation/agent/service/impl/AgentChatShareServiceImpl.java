package com.cwq.project_aicodingstation.agent.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.agent.entity.AgentChatMessage;
import com.cwq.project_aicodingstation.agent.entity.AgentChatSession;
import com.cwq.project_aicodingstation.agent.entity.AgentChatShare;
import com.cwq.project_aicodingstation.agent.entity.AgentChatShareItem;
import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import com.cwq.project_aicodingstation.agent.mapper.AgentChatMessageMapper;
import com.cwq.project_aicodingstation.agent.mapper.AgentChatSessionMapper;
import com.cwq.project_aicodingstation.agent.mapper.AgentChatShareItemMapper;
import com.cwq.project_aicodingstation.agent.mapper.AgentChatShareMapper;
import com.cwq.project_aicodingstation.agent.service.AgentChatShareService;
import com.cwq.project_aicodingstation.agent.vo.AgentShareCreatedVO;
import com.cwq.project_aicodingstation.agent.vo.AgentShareView;
import com.cwq.project_aicodingstation.chat.enums.MessageTypeEnum;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class AgentChatShareServiceImpl extends ServiceImpl<AgentChatShareMapper, AgentChatShare>
        implements AgentChatShareService {

    private static final int MAX_MESSAGES = 200;

    @Resource
    private AgentChatSessionMapper agentChatSessionMapper;

    @Resource
    private AgentChatMessageMapper agentChatMessageMapper;

    @Resource
    private AgentChatShareItemMapper agentChatShareItemMapper;

    @Override
    public AgentShareCreatedVO create(Long sessionId, List<Long> messageIds, UserLoginVO user) {
        BusinessAssert.notNull(user, ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.notNull(user.getId(), ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.notNull(sessionId, ErrorCode.PARAMS_MISSING, "会话 id 不能为空");
        BusinessAssert.requireTrue(messageIds != null && !messageIds.isEmpty(),
                ErrorCode.PARAMS_MISSING, "请选择要分享的对话");
        BusinessAssert.requireTrue(messageIds.size() <= MAX_MESSAGES,
                ErrorCode.PARAMS_ERROR, "一次最多分享 200 条消息");

        AgentChatSession session = agentChatSessionMapper.selectOneById(sessionId);
        BusinessAssert.notNull(session, ErrorCode.NOT_FOUND, "会话不存在");
        BusinessAssert.equals(session.getUserId(), user.getId(), ErrorCode.NO_PERMISSION, "只能分享自己的对话");

        List<AgentChatMessage> rows = agentChatMessageMapper.selectListByQuery(QueryWrapper.create()
                .eq("session_id", sessionId)
                .in("id", messageIds));
        BusinessAssert.requireTrue(rows.size() == new HashSet<>(messageIds).size(),
                ErrorCode.PARAMS_ERROR, "有消息不在这个会话里");
        rows.sort(Comparator.comparing(AgentChatMessage::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AgentChatMessage::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        LocalDateTime now = LocalDateTime.now();
        AgentChatShare share = AgentChatShare.builder()
                .userId(user.getId())
                .sessionId(sessionId)
                .agentCode(session.getAgentCode())
                .token(newToken())
                .title(shareTitle(session.getTitle()))
                .revoked(0)
                .createTime(now)
                .updateTime(now)
                .build();
        BusinessAssert.requireTrue(this.save(share), ErrorCode.SYSTEM_ERROR, "创建分享失败");

        int order = 0;
        int saved = 0;
        for (AgentChatMessage row : rows) {
            if (StrUtil.isBlank(row.getMessage())) {
                continue;
            }
            AgentChatShareItem item = AgentChatShareItem.builder()
                    .shareId(share.getId())
                    .sortOrder(order++)
                    .role(publicRole(row.getMessageType()))
                    .content(row.getMessage())
                    .build();
            BusinessAssert.requireTrue(agentChatShareItemMapper.insert(item) > 0,
                    ErrorCode.SYSTEM_ERROR, "保存分享内容失败");
            saved++;
        }
        BusinessAssert.requireTrue(saved > 0, ErrorCode.PARAMS_ERROR, "没有可以分享的内容");

        AgentShareCreatedVO vo = new AgentShareCreatedVO();
        vo.setToken(share.getToken());
        vo.setTitle(share.getTitle());
        return vo;
    }

    @Override
    public void revoke(String token, UserLoginVO user) {
        BusinessAssert.notNull(user, ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.notBlank(token, ErrorCode.PARAMS_MISSING, "分享令牌不能为空");
        AgentChatShare share = findByToken(token.trim());
        BusinessAssert.notNull(share, ErrorCode.NOT_FOUND, "分享已失效");
        BusinessAssert.equals(share.getUserId(), user.getId(), ErrorCode.NO_PERMISSION, "只能停止自己的分享");
        markRevoked(share.getId());
    }

    @Override
    public void revokeBySession(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        AgentChatShare patch = new AgentChatShare();
        patch.setRevoked(1);
        patch.setUpdateTime(LocalDateTime.now());
        this.update(patch, QueryWrapper.create().eq("session_id", sessionId).eq("revoked", 0));
    }

    @Override
    public AgentShareView view(String token) {
        BusinessAssert.notBlank(token, ErrorCode.NOT_FOUND, "分享已失效");
        AgentChatShare share = findByToken(token.trim());
        BusinessAssert.notNull(share, ErrorCode.NOT_FOUND, "分享已失效");
        BusinessAssert.requireTrue(share.getRevoked() == null || share.getRevoked() == 0,
                ErrorCode.NOT_FOUND, "分享已失效");

        List<AgentChatShareItem> items = agentChatShareItemMapper.selectListByQuery(QueryWrapper.create()
                .eq("share_id", share.getId())
                .orderBy("sort_order", true));
        AgentShareView view = new AgentShareView();
        view.setToken(share.getToken());
        view.setTitle(StrUtil.blankToDefault(share.getTitle(), "对话分享"));
        view.setAgentCode(share.getAgentCode());
        AgentCodeEnum agent = AgentCodeEnum.fromCode(share.getAgentCode());
        view.setAgentTitle(agent == null ? share.getAgentCode() : agent.getDisplayName());
        List<AgentShareView.Item> messages = new ArrayList<>();
        for (AgentChatShareItem item : items) {
            AgentShareView.Item row = new AgentShareView.Item();
            row.setRole(item.getRole());
            row.setContent(item.getContent());
            messages.add(row);
        }
        view.setMessages(messages);
        return view;
    }

    private AgentChatShare findByToken(String token) {
        return this.getOne(QueryWrapper.create().eq("token", token));
    }

    private void markRevoked(Long id) {
        AgentChatShare patch = new AgentChatShare();
        patch.setId(id);
        patch.setRevoked(1);
        patch.setUpdateTime(LocalDateTime.now());
        BusinessAssert.requireTrue(this.updateById(patch), ErrorCode.BUSINESS_ERROR, "停止分享失败");
    }

    private String newToken() {
        for (int i = 0; i < 8; i++) {
            String token = RandomUtil.randomString(22);
            long exists = this.count(QueryWrapper.create().eq("token", token));
            if (exists == 0) {
                return token;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成分享链接失败");
    }

    private static String shareTitle(String sessionTitle) {
        String title = StrUtil.blankToDefault(StrUtil.trim(sessionTitle), "对话分享");
        return title.length() > 256 ? title.substring(0, 256) : title;
    }

    private static String publicRole(String messageType) {
        if (MessageTypeEnum.USER.getValue().equals(messageType)) {
            return "user";
        }
        if (MessageTypeEnum.SYSTEM.getValue().equals(messageType)) {
            return "system";
        }
        return "assistant";
    }
}
