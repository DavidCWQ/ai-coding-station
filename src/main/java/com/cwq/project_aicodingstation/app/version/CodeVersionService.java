package com.cwq.project_aicodingstation.app.version;

import com.cwq.project_aicodingstation.ai.AICodeGeneratorServiceFactory;
import com.cwq.project_aicodingstation.ai.enums.CodeGenTypeEnum;
import com.cwq.project_aicodingstation.ai.result.HtmlFileCodeResult;
import com.cwq.project_aicodingstation.ai.result.MultiFileCodeResult;
import com.cwq.project_aicodingstation.app.config.AppDeployConfig;
import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.mapper.AppMapper;
import com.cwq.project_aicodingstation.chat.entity.ChatHistory;
import com.cwq.project_aicodingstation.chat.entity.ChatSession;
import com.cwq.project_aicodingstation.chat.enums.MessageTypeEnum;
import com.cwq.project_aicodingstation.chat.service.ChatHistoryService;
import com.cwq.project_aicodingstation.chat.service.ChatSessionService;
import com.cwq.project_aicodingstation.common.auth.ResourceAuthHelper;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.core.parser.CodeParserExecutor;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 每次可保存的生成结果留一份快照，预览、部署和继续修改都指向其中一版。
 */
@Slf4j
@Service
public class CodeVersionService {

    @Resource
    private AppMapper appMapper;

    @Resource
    private AppDeployConfig appDeployConfig;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ResourceAuthHelper resourceAuthHelper;

    @Resource
    private CodeParserExecutor codeParserExecutor;

    @Resource
    private AICodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 按时间列出含代码的 AI 回复。缺快照时补上：最新一版拷当前目录，更早的从正文还原。
     */
    public List<Long> listVersions(Long appId, Long sessionId, UserLoginVO userVO) {
        assertReadable(appId, sessionId, userVO);
        App app = loadApp(appId);
        List<ChatHistory> codeMessages = codeMessages(sessionId);
        Path appDirectory = appDir(app);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < codeMessages.size(); i++) {
            ChatHistory row = codeMessages.get(i);
            boolean latest = i == codeMessages.size() - 1;
            Path snapshot = CodeVersionFiles.snapshotDir(appDirectory, row.getId());
            if (!CodeVersionFiles.hasIndex(snapshot)) {
                materialize(app, appDirectory, snapshot, row.getMessage(), latest);
            }
            if (CodeVersionFiles.hasIndex(snapshot)) {
                ids.add(row.getId());
            }
        }
        pruneKeeping(appDirectory, ids);
        return ids;
    }

    /**
     * 生成写完当前目录后，把这一版固定到消息 id 上。
     */
    public void pin(Long appId, Long sessionId, Long messageId, UserLoginVO userVO) {
        assertOwner(appId, sessionId, userVO);
        App app = loadApp(appId);
        ChatHistory message = requireAiMessage(appId, sessionId, messageId);
        Path appDirectory = appDir(app);
        BusinessAssert.requireTrue(CodeVersionFiles.hasIndex(appDirectory),
                ErrorCode.NOT_FOUND, "页面还没写好，无法保存这一版");
        try {
            CodeVersionFiles.replaceWithTreeExceptVersions(
                    appDirectory, CodeVersionFiles.snapshotDir(appDirectory, message.getId()));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存版本失败");
        }
    }

    /**
     * 从当前显示的 AI 回复接着改：删掉后面的对话和快照，把这一版拷回工作目录，并清掉会话记忆。
     */
    @Transactional(rollbackFor = Exception.class)
    public void continueFrom(Long appId, Long sessionId, Long messageId, UserLoginVO userVO) {
        assertOwner(appId, sessionId, userVO);
        App app = loadApp(appId);
        Path snapshot = requireSnapshot(app, messageId);
        List<Long> removed = chatHistoryService.truncateAfterAi(appId, sessionId, messageId, userVO);
        Path appDirectory = appDir(app);
        try {
            CodeVersionFiles.restoreSnapshot(appDirectory, snapshot);
            for (Long removedId : removed) {
                if (removedId != null) {
                    CodeVersionFiles.deleteRecursively(CodeVersionFiles.snapshotDir(appDirectory, removedId));
                }
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "恢复版本失败");
        }
        aiCodeGeneratorServiceFactory.evict(sessionId);
    }

    /**
     * 部署和下载用的目录。没有消息 id 时是当前工作目录。
     */
    public Path requireSnapshot(App app, Long messageId) {
        BusinessAssert.notNull(messageId, ErrorCode.PARAMS_MISSING, "版本不能为空");
        ChatHistory message = chatHistoryService.getById(messageId);
        BusinessAssert.notNull(message, ErrorCode.NOT_FOUND, "版本不存在");
        BusinessAssert.equals(message.getAppId(), app.getId(), ErrorCode.PARAMS_ERROR, "版本不属于该应用");
        Path appDirectory = appDir(app);
        Path snapshot = CodeVersionFiles.snapshotDir(appDirectory, messageId);
        if (!CodeVersionFiles.hasIndex(snapshot)) {
            boolean latest = isLatestCodeVersion(message);
            materialize(app, appDirectory, snapshot, message.getMessage(), latest);
        }
        BusinessAssert.requireTrue(CodeVersionFiles.hasIndex(snapshot),
                ErrorCode.NOT_FOUND, "未找到该版本的页面");
        return snapshot;
    }

    /**
     * 这一条是不是会话里最后一次可保存的生成。
     */
    public boolean isLatestCodeVersion(App app, Long messageId) {
        ChatHistory message = chatHistoryService.getById(messageId);
        if (message == null || message.getAppId() == null || !message.getAppId().equals(app.getId())) {
            return false;
        }
        return isLatestCodeVersion(message);
    }

    /**
     * 替换图片时，把新文件也放进这一版，预览才能读到。
     */
    public void copyImageIntoSnapshot(App app, Path snapshot, String webPath) {
        Path live = appDir(app);
        Path snap = snapshot.toAbsolutePath().normalize();
        Path from = live.resolve(webPath).normalize();
        Path to = snap.resolve(webPath).normalize();
        BusinessAssert.requireTrue(from.startsWith(live) && to.startsWith(snap),
                ErrorCode.NO_PERMISSION, "图片路径非法");
        try {
            if (to.getParent() != null) {
                Files.createDirectories(to.getParent());
            }
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "复制图片到版本目录失败");
        }
    }

    /**
     * 删掉已经不在对话里的快照目录。重新生成删消息后调用。
     */
    public void pruneOrphans(Long appId, Long sessionId) {
        App app = appMapper.selectOneById(appId);
        if (app == null || app.getCodeGenType() == null) {
            return;
        }
        Set<Long> liveIds = new HashSet<>();
        List<ChatHistory> rows = chatHistoryService.list(QueryWrapper.create()
                .eq("app_id", appId)
                .eq("session_id", sessionId));
        for (ChatHistory row : rows) {
            if (row.getId() != null) {
                liveIds.add(row.getId());
            }
        }
        pruneKeeping(appDir(app), liveIds.stream().toList());
    }

    private void materialize(App app, Path appDirectory, Path snapshot, String message, boolean latest) {
        try {
            if (latest && CodeVersionFiles.hasIndex(appDirectory)) {
                CodeVersionFiles.replaceWithTreeExceptVersions(appDirectory, snapshot);
                return;
            }
            if (!writeParsed(app.getCodeGenType(), message, snapshot)) {
                CodeVersionFiles.deleteRecursively(snapshot);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "还原版本失败");
        }
    }

    private boolean writeParsed(String codeGenType, String message, Path snapshot) throws IOException {
        CodeGenTypeEnum type = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (type == null || !CodeVersionFiles.hasSavableCode(message)) {
            return false;
        }
        Object parsed = codeParserExecutor.executeParser(message, type);
        boolean wroteIndex = false;
        if (parsed instanceof HtmlFileCodeResult html) {
            wroteIndex = writeFile(snapshot, "index.html", html.getHtmlCode());
        } else if (parsed instanceof MultiFileCodeResult multi) {
            wroteIndex = writeFile(snapshot, "index.html", multi.getHtmlCode());
            writeFile(snapshot, "style.css", multi.getCssCode());
            writeFile(snapshot, "script.js", multi.getJsCode());
        }
        return wroteIndex;
    }

    private boolean writeFile(Path dir, String name, String content) throws IOException {
        if (content == null || content.isBlank()) {
            return false;
        }
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
        return true;
    }

    private boolean isLatestCodeVersion(ChatHistory message) {
        if (message.getSessionId() == null || message.getCreateTime() == null || message.getId() == null) {
            return false;
        }
        List<ChatHistory> later = chatHistoryService.list(QueryWrapper.create()
                .eq("session_id", message.getSessionId())
                .eq("message_type", MessageTypeEnum.AI.getValue())
                .and(com.mybatisflex.core.query.QueryMethods.column("create_time").gt(message.getCreateTime())
                        .or(com.mybatisflex.core.query.QueryMethods.column("create_time").eq(message.getCreateTime())
                                .and(com.mybatisflex.core.query.QueryMethods.column("id").gt(message.getId())))));
        for (ChatHistory row : later) {
            if (CodeVersionFiles.hasSavableCode(row.getMessage())) {
                return false;
            }
        }
        return CodeVersionFiles.hasSavableCode(message.getMessage());
    }

    private List<ChatHistory> codeMessages(Long sessionId) {
        List<ChatHistory> rows = chatHistoryService.list(QueryWrapper.create()
                .eq("session_id", sessionId)
                .eq("message_type", MessageTypeEnum.AI.getValue())
                .orderBy("create_time", true)
                .orderBy("id", true));
        List<ChatHistory> codeMessages = new ArrayList<>();
        for (ChatHistory row : rows) {
            if (row.getId() != null && CodeVersionFiles.hasSavableCode(row.getMessage())) {
                codeMessages.add(row);
            }
        }
        return codeMessages;
    }

    private void pruneKeeping(Path appDirectory, List<Long> keepIds) {
        Set<String> keep = new HashSet<>();
        for (Long id : keepIds) {
            if (id != null) {
                keep.add(Long.toString(id));
            }
        }
        try {
            for (String name : CodeVersionFiles.snapshotNames(appDirectory)) {
                if (!keep.contains(name)) {
                    Long id = parseId(name);
                    if (id != null) {
                        CodeVersionFiles.deleteRecursively(CodeVersionFiles.snapshotDir(appDirectory, id));
                    }
                }
            }
        } catch (IOException e) {
            log.warn("清理版本目录失败: {}", e.getMessage());
        }
    }

    private Long parseId(String name) {
        try {
            return Long.valueOf(name);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ChatHistory requireAiMessage(Long appId, Long sessionId, Long messageId) {
        BusinessAssert.notNull(messageId, ErrorCode.PARAMS_MISSING, "消息 id 不能为空");
        ChatHistory message = chatHistoryService.getById(messageId);
        BusinessAssert.notNull(message, ErrorCode.NOT_FOUND, "消息不存在");
        BusinessAssert.equals(message.getAppId(), appId, ErrorCode.PARAMS_ERROR, "消息不属于该应用");
        BusinessAssert.equals(message.getSessionId(), sessionId, ErrorCode.PARAMS_ERROR, "消息不属于该会话");
        BusinessAssert.equals(MessageTypeEnum.AI.getValue(), message.getMessageType(),
                ErrorCode.PARAMS_ERROR, "只能保存一次生成结果");
        return message;
    }

    private Path appDir(App app) {
        return CodeVersionFiles.appDir(appDeployConfig.getOutputDir(), app.getCodeGenType(), app.getId());
    }

    private App loadApp(Long appId) {
        App app = appMapper.selectOneById(appId);
        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");
        BusinessAssert.notBlank(app.getCodeGenType(), ErrorCode.SYSTEM_ERROR, "应用缺少生成类型");
        return app;
    }

    private void assertReadable(Long appId, Long sessionId, UserLoginVO userVO) {
        resourceAuthHelper.requireAppReadable(appId, userVO);
        ChatSession session = chatSessionService.getById(sessionId);
        BusinessAssert.notNull(session, ErrorCode.NOT_FOUND, "会话不存在");
        BusinessAssert.equals(session.getAppId(), appId, ErrorCode.PARAMS_ERROR, "会话与应用不匹配");
    }

    private void assertOwner(Long appId, Long sessionId, UserLoginVO userVO) {
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.notNull(userVO.getId(), ErrorCode.NOT_LOGIN, "用户未登录");
        ChatSession session = chatSessionService.getById(sessionId);
        BusinessAssert.notNull(session, ErrorCode.NOT_FOUND, "会话不存在");
        BusinessAssert.equals(session.getAppId(), appId, ErrorCode.PARAMS_ERROR, "会话与应用不匹配");
        BusinessAssert.equals(session.getUserId(), userVO.getId(), ErrorCode.NO_PERMISSION, "只能修改自己的对话");
    }
}
