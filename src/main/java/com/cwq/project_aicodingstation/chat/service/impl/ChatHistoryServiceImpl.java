package com.cwq.project_aicodingstation.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.cwq.project_aicodingstation.app.constant.AppConstant;
import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.mapper.AppMapper;
import com.cwq.project_aicodingstation.chat.dto.ChatHistoryQueryRequest;
import com.cwq.project_aicodingstation.chat.entity.ChatHistory;
import com.cwq.project_aicodingstation.chat.entity.ChatSession;
import com.cwq.project_aicodingstation.chat.enums.MessageTypeEnum;
import com.cwq.project_aicodingstation.chat.mapper.ChatHistoryMapper;
import com.cwq.project_aicodingstation.chat.service.ChatHistoryService;
import com.cwq.project_aicodingstation.chat.service.ChatSessionService;
import com.cwq.project_aicodingstation.chat.vo.ChatHistoryVO;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.mybatisflex.core.query.QueryMethods.column;

/**
 * 对话历史业务实现。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    private AppMapper appMapper;

    @Resource
    private ChatSessionService chatSessionService;

    /**
     * 校验是否可读取该会话下的历史：应用可访问，且会话属于该应用，且（管理员 / 会话本人 / 应用创建者）。
     */
    private void assertHistoryReadable(Long appId, Long sessionId, UserLoginVO userVO) {
        chatSessionService.assertAppAccessible(appId, userVO);
        ChatSession session = chatSessionService.getById(sessionId);
        BusinessAssert.notNull(session, ErrorCode.NOT_FOUND, "会话不存在");
        BusinessAssert.equals(session.getAppId(), appId, ErrorCode.PARAMS_ERROR, "会话与应用不匹配");

        QueryWrapper appQw = QueryWrapper.create();
        appQw.select("id", "user_id").eq("id", appId);
        App app = appMapper.selectOneByQuery(appQw);

        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");
        boolean admin = UserConstant.ADMIN_ROLE.equals(userVO.getUserRole());
        boolean sessionOwner = session.getUserId() != null && session.getUserId().equals(userVO.getId());
        boolean appOwner = app.getUserId() != null && app.getUserId().equals(userVO.getId());
        BusinessAssert.requireTrue(admin || sessionOwner || appOwner,
                ErrorCode.NO_PERMISSION, "无权限查看该会话消息"
        );
    }

    @Override
    public Long addMessage(Long appId, Long sessionId, String message, String messageType, Long userId) {
        BusinessAssert.notNull(appId, ErrorCode.PARAMS_MISSING, "应用 id 不能为空");
        BusinessAssert.notNull(sessionId, ErrorCode.PARAMS_MISSING, "会话 id 不能为空");
        BusinessAssert.notBlank(message, ErrorCode.PARAMS_MISSING, "消息内容不能为空");
        BusinessAssert.notBlank(messageType, ErrorCode.PARAMS_MISSING, "消息类型不能为空");
        BusinessAssert.notNull(userId, ErrorCode.PARAMS_MISSING, "用户 id 不能为空");
        BusinessAssert.requireTrue(MessageTypeEnum.getEnumByValue(messageType) != null,
                ErrorCode.PARAMS_ERROR, "非法的消息类型"
        );

        // 校验 app / session 关系存在且匹配
        QueryWrapper appQw = QueryWrapper.create();
        appQw.select("id", "user_id").eq("id", appId);
        App app = appMapper.selectOneByQuery(appQw);

        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");
        ChatSession session = chatSessionService.getById(sessionId);
        BusinessAssert.notNull(session, ErrorCode.NOT_FOUND, "会话不存在");
        BusinessAssert.equals(session.getAppId(), appId, ErrorCode.PARAMS_ERROR, "会话与应用不匹配");
        BusinessAssert.equals(session.getUserId(), userId, ErrorCode.NO_PERMISSION, "无权限向该会话写入消息");

        LocalDateTime now = LocalDateTime.now();
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .sessionId(sessionId)
                .userId(userId)
                .message(message)
                .messageType(messageType)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();

        BusinessAssert.requireTrue(this.save(chatHistory), ErrorCode.SYSTEM_ERROR, "保存消息失败");
        chatSessionService.touchLastMessageTime(sessionId, now);
        return chatHistory.getId();
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        BusinessAssert.notNull(appId, ErrorCode.PARAMS_MISSING, "应用 id 为空");
        BusinessAssert.requireTrue(appId > 0, ErrorCode.PARAMS_INVALID, "应用 id 非法");
        QueryWrapper qw = QueryWrapper.create();
        qw.eq("app_id", appId);
        return this.remove(qw);
    }

    @Override
    public List<ChatHistoryVO> listHistory(ChatHistoryQueryRequest req, UserLoginVO userVO) {
        BusinessAssert.notNull(req, ErrorCode.PARAMS_ERROR, "查询请求为空");
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");

        Long appId = req.getAppId();
        Long sessionId = req.getSessionId();
        assertHistoryReadable(appId, sessionId, userVO);

        int pageSize = req.getPageSize() == null ? 10 : req.getPageSize();
        BusinessAssert.requireTrue(pageSize > 0 && pageSize <= AppConstant.MAX_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页条数非法或超过上限"
        );

        ChatHistory anchor = null;
        if (req.getBeforeMessageId() != null) {
            anchor = this.getById(req.getBeforeMessageId());
            BusinessAssert.notNull(anchor, ErrorCode.NOT_FOUND, "游标消息不存在");
            BusinessAssert.equals(anchor.getAppId(), appId, ErrorCode.PARAMS_ERROR, "游标消息不属于该应用");
            BusinessAssert.equals(anchor.getSessionId(), sessionId, ErrorCode.PARAMS_ERROR, "游标消息不属于该会话");
        }

        QueryWrapper qw = getQueryWrapperForHistoryList(req, anchor, pageSize);
        List<ChatHistory> rows = this.list(qw);
        List<ChatHistory> ordered = new ArrayList<>(rows);
        Collections.reverse(ordered);
        return ordered.stream().map(this::toHistoryVO).collect(Collectors.toList());
    }

    @Override
    public List<ChatHistoryVO> listAll(Long pageSize, UserLoginVO userVO) {
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.equals(UserConstant.ADMIN_ROLE, userVO.getUserRole(),
                ErrorCode.NO_PERMISSION, "仅管理员可查看全部对话历史"
        );

        long size = pageSize == null ? 10L : pageSize;
        BusinessAssert.requireTrue(size > 0 && size <= AppConstant.MAX_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "条数非法或超过上限"
        );

        QueryWrapper qw = getQueryWrapperForAdminList((int) size);
        List<ChatHistory> rows = this.list(qw);
        return rows.stream().map(this::toHistoryVO).collect(Collectors.toList());
    }

    /**
     * 构建会话内对话历史列表的 QueryWrapper（列名与 sql/004_create_chat_history_table.sql 中 chat_history 表一致）。
     * <p>
     * 逻辑删除：不在此显式拼接 {@code is_deleted}，与 app/user 模块一致，依赖实体字段
     * {@link com.mybatisflex.annotation.Column#isLogicDelete()} 由框架自动追加条件。
     * </p>
     *
     * @param req      查询请求（含 appId、sessionId、可选 beforeCreateTime）
     * @param anchor   游标锚点消息；非空时表示取「比该条更早」的消息
     * @param pageSize 条数上限
     */
    private QueryWrapper getQueryWrapperForHistoryList(ChatHistoryQueryRequest req, ChatHistory anchor,
                                                       int pageSize) {
        QueryWrapper qw = QueryWrapper.create();
        qw.eq("app_id", req.getAppId());
        qw.eq("session_id", req.getSessionId());

        if (anchor != null) {
            // 早于锚点：(create_time < t) OR (create_time = t AND id < id0)，与 idx_session_time(session_id, create_time) 一致
            QueryCondition beforeAnchor = column("create_time").lt(anchor.getCreateTime())
                    .or(column("create_time").eq(anchor.getCreateTime())
                            .and(column("id").lt(anchor.getId())));
            qw.and(beforeAnchor);
        } else if (req.getBeforeCreateTime() != null) {
            qw.lt("create_time", req.getBeforeCreateTime());
        }

        qw.orderBy("create_time", false);
        qw.limit(pageSize);
        return qw;
    }

    /**
     * 管理员全表最近消息列表（按 create_time DESC，列名同 chat_history 表）。
     */
    private QueryWrapper getQueryWrapperForAdminList(int limit) {
        QueryWrapper qw = QueryWrapper.create();
        qw.orderBy("create_time", false);
        qw.limit(limit);
        return qw;
    }

    /**
     * 转 VO（脱敏：不包含 userId）
     */
    private ChatHistoryVO toHistoryVO(ChatHistory h) {
        ChatHistoryVO vo = new ChatHistoryVO();
        BeanUtil.copyProperties(h, vo);
        return vo;
    }
}
