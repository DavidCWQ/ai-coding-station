package com.cwq.project_aicodingstation.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.app.constant.AppConstant;
import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.mapper.AppMapper;
import com.cwq.project_aicodingstation.chat.dto.ChatHistoryQueryRequest;
import com.cwq.project_aicodingstation.chat.entity.ChatHistory;
import com.cwq.project_aicodingstation.chat.entity.ChatSession;
import com.cwq.project_aicodingstation.chat.enums.MessageTypeEnum;
import com.cwq.project_aicodingstation.chat.export.ConversationMarkdown;
import com.cwq.project_aicodingstation.chat.export.MarkdownFile;
import com.cwq.project_aicodingstation.chat.mapper.ChatHistoryMapper;
import com.cwq.project_aicodingstation.chat.service.ChatHistoryService;
import com.cwq.project_aicodingstation.chat.service.ChatSessionService;
import com.cwq.project_aicodingstation.chat.vo.ChatHistoryVO;
import com.cwq.project_aicodingstation.feedback.service.MessageFeedbackService;
import com.cwq.project_aicodingstation.common.auth.ResourceAuthHelper;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mybatisflex.core.query.QueryMethods.column;

/**
 * 对话历史业务实现。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    private AppMapper appMapper;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ResourceAuthHelper resourceAuthHelper;

    @Resource
    private MessageFeedbackService messageFeedbackService;

    /**
     * 校验是否可读取该会话下的历史：
     * 应用满足历史只读权限（管理员 / 应用创建者 / 精选应用），且会话属于该应用。
     */
    private void assertHistoryReadable(Long appId, Long sessionId, UserLoginVO userVO) {
        resourceAuthHelper.requireAppReadable(appId, userVO);
        ChatSession session = chatSessionService.getById(sessionId);
        BusinessAssert.notNull(session, ErrorCode.NOT_FOUND, "会话不存在");
        BusinessAssert.equals(session.getAppId(), appId, ErrorCode.PARAMS_ERROR, "会话与应用不匹配");
    }

    @Override
    public Long addMessage(Long appId, Long sessionId, String message, String messageType, Long userId) {
        return addMessage(appId, sessionId, message, messageType, userId, null);
    }

    @Override
    public Long addMessage(Long appId, Long sessionId, String message, String messageType, Long userId,
                           String reasoning) {
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
                .reasoning(StrUtil.isBlank(reasoning) ? null : reasoning)
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
        List<ChatHistoryVO> vos = ordered.stream().map(this::toHistoryVO).collect(Collectors.toList());
        attachVotes(vos, userVO);
        return vos;
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

    @Override
    public void loadChatHistoryToMemory(Long sessionId, MessageWindowChatMemory chatMemory,
                                        int maxCount) {
        try {
            List<ChatHistory> historyList = this.list(
                    QueryWrapper.create()
                            .eq(ChatHistory::getSessionId, sessionId)
                            .orderBy(ChatHistory::getCreateTime, false)
                            .limit(1, maxCount) // 起始点为 1 而不是 0，用于排除最新的用户消息
            ); // 不排除会导致消息重复！

            if (historyList.isEmpty()) { return; }

            // 反转列表，确保按照时间正序（老的在前，新的在后）
            Collections.reverse(historyList);
            // 清理历史缓存，防止重复加载
            int count = 0;
            chatMemory.clear();
            // 记录非系统历史对话条数
            for (ChatHistory h : historyList) {
                if (MessageTypeEnum.USER.getValue().equals(h.getMessageType())) {
                    chatMemory.add(UserMessage.from(h.getMessage()));
                } else if (MessageTypeEnum.AI.getValue().equals(h.getMessageType())){
                    chatMemory.add(AiMessage.from(h.getMessage()));
                }
                count++;
            }
            // 加载成功，记录对话条数
            log.info("加载历史对话成功，sessionId={}, count={}", sessionId, count);
        } catch (Exception e) {
            // 加载失败不影响系统运行，只是没有历史上下文
            log.error("加载历史对话失败，sessionId: {}, error: {}", sessionId, e.getMessage(), e);
        }
    }

    @Override
    public String prepareRewrite(Long appId, Long sessionId, Long userMessageId, String newContent, UserLoginVO userVO) {
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.notNull(userVO.getId(), ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.notNull(appId, ErrorCode.PARAMS_MISSING, "应用 id 不能为空");
        BusinessAssert.notNull(sessionId, ErrorCode.PARAMS_MISSING, "会话 id 不能为空");
        BusinessAssert.notNull(userMessageId, ErrorCode.PARAMS_MISSING, "消息 id 不能为空");

        ChatSession session = chatSessionService.getById(sessionId);
        BusinessAssert.notNull(session, ErrorCode.NOT_FOUND, "会话不存在");
        BusinessAssert.equals(session.getAppId(), appId, ErrorCode.PARAMS_ERROR, "会话与应用不匹配");
        BusinessAssert.equals(session.getUserId(), userVO.getId(), ErrorCode.NO_PERMISSION, "只能修改自己的对话");

        ChatHistory anchor = this.getById(userMessageId);
        BusinessAssert.notNull(anchor, ErrorCode.NOT_FOUND, "消息不存在");
        BusinessAssert.equals(anchor.getSessionId(), sessionId, ErrorCode.PARAMS_ERROR, "消息不属于该会话");
        BusinessAssert.equals(MessageTypeEnum.USER.getValue(), anchor.getMessageType(),
                ErrorCode.PARAMS_ERROR, "只能从用户发言重新生成");

        String next = StrUtil.isBlank(newContent) ? anchor.getMessage() : newContent.trim();
        BusinessAssert.notBlank(next, ErrorCode.PARAMS_MISSING, "消息不能为空");
        BusinessAssert.requireTrue(next.length() <= 32000, ErrorCode.PARAMS_ERROR, "消息过长");
        if (!next.equals(anchor.getMessage())) {
            ChatHistory update = new ChatHistory();
            update.setId(anchor.getId());
            update.setMessage(next);
            update.setUpdateTime(LocalDateTime.now());
            BusinessAssert.requireTrue(this.updateById(update), ErrorCode.BUSINESS_ERROR, "更新发言失败");
        }
        removeAfter(sessionId, anchor.getCreateTime(), anchor.getId());
        return next;
    }

    @Override
    public MarkdownFile exportMarkdown(Long appId, Long sessionId, Long untilMessageId, UserLoginVO userVO) {
        assertHistoryReadable(appId, sessionId, userVO);
        BusinessAssert.notNull(untilMessageId, ErrorCode.PARAMS_MISSING, "消息 id 不能为空");
        ChatHistory until = this.getById(untilMessageId);
        BusinessAssert.notNull(until, ErrorCode.NOT_FOUND, "消息不存在");
        BusinessAssert.equals(until.getAppId(), appId, ErrorCode.PARAMS_ERROR, "消息不属于该应用");
        BusinessAssert.equals(until.getSessionId(), sessionId, ErrorCode.PARAMS_ERROR, "消息不属于该会话");

        QueryCondition upTo = column("create_time").lt(until.getCreateTime())
                .or(column("create_time").eq(until.getCreateTime()).and(column("id").le(until.getId())));
        QueryWrapper qw = QueryWrapper.create()
                .eq("app_id", appId)
                .eq("session_id", sessionId)
                .and(upTo)
                .orderBy("create_time", false)
                .orderBy("id", false)
                .limit(ConversationMarkdown.MAX_TURNS + 1);
        List<ChatHistory> rows = new ArrayList<>(this.list(qw));
        boolean truncated = rows.size() > ConversationMarkdown.MAX_TURNS;
        if (truncated) {
            rows = new ArrayList<>(rows.subList(0, ConversationMarkdown.MAX_TURNS));
        }
        Collections.reverse(rows);

        App app = appMapper.selectOneById(appId);
        String appName = app == null ? "对话" : StrUtil.blankToDefault(StrUtil.trim(app.getAppName()), "对话");
        LocalDateTime now = LocalDateTime.now();
        List<ConversationMarkdown.Turn> turns = new ArrayList<>();
        for (ChatHistory row : rows) {
            turns.add(new ConversationMarkdown.Turn(row.getMessageType(), row.getMessage(), row.getReasoning()));
        }
        String markdown = ConversationMarkdown.render(
                appName,
                "应用「" + appName + "」",
                now,
                turns,
                true,
                truncated
        );
        return new MarkdownFile(
                ConversationMarkdown.filename(appName, now),
                markdown.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void removeAfter(Long sessionId, LocalDateTime createTime, Long id) {
        QueryCondition after = column("create_time").gt(createTime)
                .or(column("create_time").eq(createTime).and(column("id").gt(id)));
        this.remove(QueryWrapper.create().eq("session_id", sessionId).and(after));
    }

    private void attachVotes(List<ChatHistoryVO> vos, UserLoginVO userVO) {
        if (userVO == null || userVO.getId() == null || vos.isEmpty()) {
            return;
        }
        List<Long> ids = vos.stream().map(ChatHistoryVO::getId).filter(id -> id != null).toList();
        Map<Long, Integer> votes = messageFeedbackService.votesOf(userVO.getId(), MessageFeedbackService.SOURCE_APP, ids);
        for (ChatHistoryVO vo : vos) {
            vo.setVote(votes.get(vo.getId()));
        }
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
