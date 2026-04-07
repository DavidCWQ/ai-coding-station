package com.cwq.project_aicodingstation.agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.agent.constant.AgentConstant;
import com.cwq.project_aicodingstation.agent.dto.AgentSessionQueryRequest;
import com.cwq.project_aicodingstation.agent.entity.AgentChatSession;
import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import com.cwq.project_aicodingstation.agent.mapper.AgentChatSessionMapper;
import com.cwq.project_aicodingstation.agent.service.AgentChatSessionService;
import com.cwq.project_aicodingstation.agent.vo.AgentChatSessionVO;
import com.cwq.project_aicodingstation.app.constant.AppConstant;
import com.cwq.project_aicodingstation.common.auth.ResourceAuthHelper;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能体会话业务实现。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class AgentChatSessionServiceImpl extends ServiceImpl<AgentChatSessionMapper, AgentChatSession>
        implements AgentChatSessionService {

    @Resource
    private ResourceAuthHelper resourceAuthHelper;

    @Override
    public Long createSession(String agentCode, String title, UserLoginVO userVO) {
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.notNull(userVO.getId(), ErrorCode.NOT_LOGIN, "用户未登录");
        AgentCodeEnum agent = AgentCodeEnum.requireValid(agentCode);

        LocalDateTime now = LocalDateTime.now();
        String finalTitle = StrUtil.isBlank(title) ? AgentConstant.DEFAULT_SESSION_TITLE : title.trim();

        AgentChatSession session = AgentChatSession.builder()
                .userId(userVO.getId())
                .agentCode(agent.getCode())
                .title(finalTitle)
                .lastMsgTime(null)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();

        BusinessAssert.requireTrue(this.save(session), ErrorCode.SYSTEM_ERROR, "创建智能体会话失败");
        return session.getId();
    }

    @Override
    public Page<AgentChatSessionVO> listSessions(AgentSessionQueryRequest req, UserLoginVO userVO) {
        BusinessAssert.notNull(req, ErrorCode.PARAMS_ERROR, "查询请求为空");
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");
        AgentCodeEnum agent = AgentCodeEnum.requireValid(req.getAgentCode());

        long pageSize = req.getPageSize() == null ? 10L : req.getPageSize();
        BusinessAssert.requireTrue(pageSize > 0 && pageSize <= AppConstant.MAX_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页条数非法或超过上限");

        long pageNum = req.getPageNum() == null ? 1L : req.getPageNum();
        BusinessAssert.requireTrue(pageNum >= 1, ErrorCode.PARAMS_ERROR, "页码须从 1 开始");

        QueryWrapper qw = QueryWrapper.create();
        qw.eq("user_id", userVO.getId());
        qw.eq("agent_code", agent.getCode());
        qw.orderBy("last_msg_time", false);

        Page<AgentChatSession> page = this.page(Page.of(pageNum, pageSize), qw);
        Page<AgentChatSessionVO> voPage = new Page<>(pageNum, pageSize, page.getTotalRow());
        voPage.setRecords(toSessionVOList(page.getRecords()));
        return voPage;
    }

    @Override
    public boolean deleteSession(Long sessionId, UserLoginVO userVO) {
        assertSessionOwned(sessionId, userVO);
        return this.removeById(sessionId);
    }

    @Override
    public boolean updateTitle(Long sessionId, String title, UserLoginVO userVO) {
        assertSessionOwned(sessionId, userVO);
        BusinessAssert.notBlank(title, ErrorCode.PARAMS_MISSING, "标题不能为空");

        AgentChatSession update = new AgentChatSession();
        update.setId(sessionId);
        update.setTitle(title.trim());
        update.setUpdateTime(LocalDateTime.now());

        BusinessAssert.requireTrue(this.updateById(update), ErrorCode.BUSINESS_ERROR, "更新标题失败");
        return true;
    }

    @Override
    public AgentChatSession requireSessionAccessible(Long sessionId, String agentCode, UserLoginVO userVO) {
        AgentChatSession session = assertSessionOwned(sessionId, userVO);
        AgentCodeEnum expect = AgentCodeEnum.requireValid(agentCode);
        BusinessAssert.equals(session.getAgentCode(), expect.getCode(),
                ErrorCode.PARAMS_ERROR, "会话与智能体类型不匹配"
        );
        return session;
    }

    @Override
    public void touchLastMessageTime(Long sessionId, LocalDateTime at) {
        if (sessionId == null) {
            return;
        }
        LocalDateTime t = at != null ? at : LocalDateTime.now();
        AgentChatSession update = new AgentChatSession();
        update.setId(sessionId);
        update.setLastMsgTime(t);
        update.setUpdateTime(t);
        this.updateById(update);
    }

    @Override
    public void autoTitleFromUserMessage(Long sessionId, String userText, UserLoginVO userVO) {
        if (sessionId == null || StrUtil.isBlank(userText)) {
            return;
        }
        AgentChatSession session = assertSessionOwned(sessionId, userVO);
        if (!AgentConstant.DEFAULT_SESSION_TITLE.equals(StrUtil.nullToDefault(session.getTitle(), ""))) {
            return;
        }
        String snippet = userText.trim();
        if (snippet.length() > AgentConstant.AUTO_TITLE_MAX_LEN) {
            snippet = snippet.substring(0, AgentConstant.AUTO_TITLE_MAX_LEN) + "…";
        }
        AgentChatSession update = new AgentChatSession();
        update.setId(sessionId);
        update.setTitle(snippet);
        update.setUpdateTime(LocalDateTime.now());
        this.updateById(update);
    }

    private AgentChatSession assertSessionOwned(Long sessionId, UserLoginVO userVO) {
        BusinessAssert.notNull(sessionId, ErrorCode.PARAMS_MISSING, "会话 id 为空");
        AgentChatSession session = this.getById(sessionId);
        BusinessAssert.notNull(session, ErrorCode.NOT_FOUND, "会话不存在");
        resourceAuthHelper.requireOwnerOrAdmin(userVO, session.getUserId(), "无权限操作该会话");
        return session;
    }

    private List<AgentChatSessionVO> toSessionVOList(List<AgentChatSession> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().map(this::toSessionVO).collect(Collectors.toList());
    }

    private AgentChatSessionVO toSessionVO(AgentChatSession s) {
        AgentChatSessionVO vo = new AgentChatSessionVO();
        BeanUtil.copyProperties(s, vo);
        return vo;
    }
}
