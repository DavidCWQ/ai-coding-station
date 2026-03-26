package com.cwq.project_aicodingstation.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.app.constant.AppConstant;
import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.mapper.AppMapper;
import com.cwq.project_aicodingstation.chat.dto.ChatSessionQueryRequest;
import com.cwq.project_aicodingstation.chat.entity.ChatSession;
import com.cwq.project_aicodingstation.chat.mapper.ChatSessionMapper;
import com.cwq.project_aicodingstation.chat.service.ChatSessionService;
import com.cwq.project_aicodingstation.chat.vo.ChatSessionVO;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
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
 * 对话会话业务实现。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    private static final String DEFAULT_SESSION_TITLE = "新对话";

    @Resource
    private AppMapper appMapper;

    public void assertAppAccessible(Long appId, UserLoginVO userVO) {
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.notNull(appId, ErrorCode.PARAMS_MISSING, "应用 id 为空");
        QueryWrapper qw = QueryWrapper.create();
        qw.select("id", "user_id").eq("id", appId);
        App app = appMapper.selectOneByQuery(qw);
        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");
        boolean owner = app.getUserId() != null && app.getUserId().equals(userVO.getId());
        boolean admin = UserConstant.ADMIN_ROLE.equals(userVO.getUserRole());
        BusinessAssert.requireTrue(owner || admin, ErrorCode.NO_PERMISSION, "无权限访问该应用");
    }

    @Override
    public Long createSession(Long appId, Long userId, String title, UserLoginVO userVO) {
        BusinessAssert.notNull(userId, ErrorCode.PARAMS_MISSING, "用户 id 为空");
        assertAppAccessible(appId, userVO);

        LocalDateTime now = LocalDateTime.now();
        String finalTitle = StrUtil.isBlank(title) ? DEFAULT_SESSION_TITLE : title.trim();

        ChatSession session = ChatSession.builder()
                .appId(appId)
                .userId(userId)
                .title(finalTitle)
                .lastMsgTime(null)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();

        BusinessAssert.requireTrue(this.save(session), ErrorCode.SYSTEM_ERROR, "创建会话失败");
        return session.getId();
    }

    @Override
    public Page<ChatSessionVO> listByAppId(ChatSessionQueryRequest req, UserLoginVO userVO) {
        BusinessAssert.notNull(req, ErrorCode.PARAMS_ERROR, "查询请求为空");
        Long appId = req.getAppId();
        assertAppAccessible(appId, userVO);

        long pageSize = req.getPageSize();
        BusinessAssert.requireTrue(pageSize > 0 && pageSize <= AppConstant.MAX_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页条数非法或超过上限"
        );

        long pageNum = req.getPageNum();
        BusinessAssert.requireTrue(pageNum >= 1, ErrorCode.PARAMS_ERROR, "页码须从 1 开始");

        // 列名与 sql/004_create_chat_history_table.sql 中 chat_session 表一致；逻辑删除由实体 isDeleted 注解处理，与 app/user 一致
        QueryWrapper qw = getQueryWrapperForSessionPage(appId, userVO.getId());

        Page<ChatSession> page = this.page(Page.of(pageNum, pageSize), qw);
        Page<ChatSessionVO> voPage = new Page<>(pageNum, pageSize, page.getTotalRow());
        voPage.setRecords(toSessionVOList(page.getRecords()));
        return voPage;
    }

    @Override
    public boolean deleteSession(Long sessionId, UserLoginVO userVO) {
        assertSessionAccessible(sessionId, userVO);
        return this.removeById(sessionId);
    }

    @Override
    public boolean updateTitle(Long sessionId, String title, UserLoginVO userVO) {
        assertSessionAccessible(sessionId, userVO);
        BusinessAssert.notBlank(title, ErrorCode.PARAMS_MISSING, "标题不能为空");

        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setTitle(title.trim());
        update.setUpdateTime(LocalDateTime.now());

        BusinessAssert.requireTrue(this.updateById(update), ErrorCode.BUSINESS_ERROR, "更新标题失败");
        return true;
    }

    /**
     * 验证「会话」是否存在，和「用户」是否有权限访问该应用（创建者或管理员）
     *
     * @param sessionId 会话id
     * @param userVO    当前用户
     * */
    private void assertSessionAccessible (Long sessionId, UserLoginVO userVO) {
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.notNull(sessionId, ErrorCode.PARAMS_MISSING, "会话 id 为空");
        ChatSession session = this.getById(sessionId);
        BusinessAssert.notNull(session, ErrorCode.NOT_FOUND, "会话不存在");
        boolean owner = session.getUserId() != null && session.getUserId().equals(userVO.getId());
        boolean admin = UserConstant.ADMIN_ROLE.equals(userVO.getUserRole());
        BusinessAssert.requireTrue(owner || admin, ErrorCode.NO_PERMISSION, "无权限操作该会话");
    }

    /**
     * 构建「某应用下当前用户会话分页」查询条件（与 AppServiceImpl#getQueryWrapper 风格一致：QueryWrapper.create + eq + orderBy）。
     *
     * @param appId  应用 id（表字段 app_id）
     * @param userId 用户 id（表字段 user_id）
     */
    private QueryWrapper getQueryWrapperForSessionPage(Long appId, Long userId) {
        QueryWrapper qw = QueryWrapper.create();
        qw.eq("app_id", appId);
        qw.eq("user_id", userId);
        qw.orderBy("last_msg_time", false);
        return qw;
    }

    @Override
    public void touchLastMessageTime(Long sessionId, LocalDateTime at) {
        if (sessionId == null) {
            return;
        }
        LocalDateTime t = at != null ? at : LocalDateTime.now();
        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setLastMsgTime(t);
        update.setUpdateTime(t);
        this.updateById(update);
    }

    /**
     * 实体列表转 VO
     */
    private List<ChatSessionVO> toSessionVOList(List<ChatSession> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().map(this::toSessionVO).collect(Collectors.toList());
    }

    /**
     * 单条转 VO（脱敏）
     */
    private ChatSessionVO toSessionVO(ChatSession s) {
        ChatSessionVO vo = new ChatSessionVO();
        BeanUtil.copyProperties(s, vo);
        return vo;
    }
}
