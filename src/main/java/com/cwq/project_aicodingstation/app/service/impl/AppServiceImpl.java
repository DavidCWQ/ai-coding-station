package com.cwq.project_aicodingstation.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.ai.enums.CodeGenTypeEnum;
import com.cwq.project_aicodingstation.ai.facade.AICodeGeneratorFacade;
import com.cwq.project_aicodingstation.app.config.AppDeployConfig;
import com.cwq.project_aicodingstation.app.constant.AppConstant;
import com.cwq.project_aicodingstation.app.dto.*;
import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.mapper.AppMapper;
import com.cwq.project_aicodingstation.app.service.AppService;
import com.cwq.project_aicodingstation.app.vo.AppVO;
import com.cwq.project_aicodingstation.chat.service.ChatHistoryService;
import com.cwq.project_aicodingstation.chat.service.ChatSessionService;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.common.request.DeleteRequest;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.core.screenshot.ScreenshotService;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.cwq.project_aicodingstation.user.vo.UserVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    private AICodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private AppDeployConfig appDeployConfig;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ScreenshotService screenshotService;

    /**
     * 删除应用时关联删除对话历史（兜底清理；即使外键 CASCADE 生效也不影响）。
     * <p>
     * 容错设计：对话历史删除失败不阻止应用删除，只记录日志，确保核心业务稳定。
     * </p>
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        long appId;
        try {
            appId = Long.parseLong(id.toString());
        } catch (Exception e) {
            return false;
        }
        if (appId <= 0) {
            return false;
        }
        try {
            BusinessAssert.requireTrue(chatSessionService.deleteByAppId(appId),
                    ErrorCode.BUSINESS_ERROR, "清理会话失败, appId=" + appId
            );
        } catch (Exception e) {
            log.error("删除应用关联会话失败: {}", e.getMessage());
        }
        try {
            BusinessAssert.requireTrue(chatHistoryService.deleteByAppId(appId),
                    ErrorCode.BUSINESS_ERROR, "清理历史失败，appId=" + appId
            );
        } catch (Exception e) {
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        return super.removeById(id);
    }

    @Override
    public Long createApp(AppAddRequest req, UserLoginVO userVO) {

        // 1. 参数校验
        BusinessAssert.notNull(req, ErrorCode.PARAMS_MISSING, "创建应用请求为空");
        String initPrompt = req.getInitPrompt();
        BusinessAssert.notBlank(initPrompt, ErrorCode.PARAMS_MISSING, "初始化 prompt 不能为空");

        // 2. 构造对象并持久化 (应用名称默认 = prompt 前20位)
        App app = new App();
        BeanUtil.copyProperties(req, app);

        app.setUserId(userVO.getId());
        if (StrUtil.isBlank(req.getAppName())) {
            app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 20)));
        }
        app.setCodeGenType(CodeGenTypeEnum.HTML.getValue());
        if (StrUtil.isNotBlank(req.getCodeGenType())
                && req.getCodeGenType().toUpperCase().contains("MULTI")) {
            app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        }

        BusinessAssert.requireTrue(this.save(app), ErrorCode.SYSTEM_ERROR, "创建应用失败");
        return app.getId();
    }

    @Override
    public boolean updateApp(AppUpdateRequest req, UserLoginVO userVO) {

        // 1. 参数校验
        BusinessAssert.notNull(req, ErrorCode.PARAMS_MISSING, "更新请求为空");
        Long id = req.getId();
        BusinessAssert.notNull(id, ErrorCode.PARAMS_MISSING, "应用 id 为空");

        // 2. 判断应用是否存在
        App oldApp = this.getById(id);
        BusinessAssert.notNull(oldApp, ErrorCode.NOT_FOUND, "应用不存在");

        // 3. 权限校验（仅本人）
        BusinessAssert.equals(oldApp.getUserId(), userVO.getId(),
                ErrorCode.NO_PERMISSION, "无权限修改该应用"
        );

        // 4. 更新
        App app = new App();
        app.setId(id);
        app.setAppName(req.getAppName());
        app.setEditTime(LocalDateTime.now());

        BusinessAssert.requireTrue(
                this.updateById(app), ErrorCode.BUSINESS_ERROR, "更新应用失败"
        );

        return true;
    }

    @Override
    public boolean deleteApp(DeleteRequest req, UserLoginVO userVO) {

        // 1. 参数校验
        BusinessAssert.notNull(req, ErrorCode.PARAMS_MISSING, "删除请求为空");
        Long id = req.getId();
        BusinessAssert.requireTrue(id > 0, ErrorCode.PARAMS_INVALID, "应用 id 非法");

        // 2. 判断应用是否存在
        App oldApp = this.getById(id);
        BusinessAssert.notNull(oldApp, ErrorCode.NOT_FOUND, "应用不存在");

        // 3. 权限校验
        boolean isOwner = oldApp.getUserId().equals(userVO.getId());
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(userVO.getUserRole());

        BusinessAssert.requireTrue(isOwner || isAdmin,
                ErrorCode.NO_PERMISSION, "无权限删除该应用");

        // 4. 删除
        return this.removeById(id);
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);

        Long userId = app.getUserId();
        BusinessAssert.notNull(userId, ErrorCode.PARAMS_MISSING, "应用 id 为空");
        appVO.setUser(userService.getVOById(userId));

        return appVO;
    }

    @Override
    public AppVO getAppVOById(Long id) {

        // 1. 参数校验
        BusinessAssert.notNull(id, ErrorCode.PARAMS_MISSING, "应用 id 为空");

        // 2. 查询
        App app = this.getById(id);
        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");

        // 3. 转 VO
        return getAppVO(app);
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {

        // 1. 参数校验
        BusinessAssert.notNull(appQueryRequest,
                ErrorCode.PARAMS_ERROR, "查询请求参数为空"
        );

        // 2. 获取请求信息
        Long id = appQueryRequest.getId();
        Long userId = appQueryRequest.getUserId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        // 3. 原先无条件 .eq("id", id)，前端占位 id: 0 会变成 WHERE id = 0，结果集为空。
        //    现已经改为: iff id != null && id > 0 才加 id 条件，以此类推。
        QueryWrapper qw = QueryWrapper.create();
        if (id != null && id > 0) {
            qw.eq("id", id);
        }
        if (userId != null && userId > 0) {
            qw.eq("user_id", userId);
        }
        if (StrUtil.isNotBlank(appName)) {
            qw.like("app_name", appName);
        }
        if (StrUtil.isNotBlank(cover)) {
            qw.like("cover", cover);
        }
        if (StrUtil.isNotBlank(initPrompt)) {
            qw.like("init_prompt", initPrompt);
        }
        if (StrUtil.isNotBlank(codeGenType)) {
            qw.eq("code_gen_type", codeGenType);
        }
        if (StrUtil.isNotBlank(deployKey)) {
            qw.eq("deploy_key", deployKey);
        }
        if (priority != null) {
            qw.ge("priority", priority);
        }
        return qw.orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public Page<AppVO> listMyApps(AppQueryRequest req, UserLoginVO userVO) {

        // 1. 参数校验
        BusinessAssert.notNull(req, ErrorCode.PARAMS_ERROR, "查询请求为空");
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户不存在");

        long pageSize = req.getPageSize();
        BusinessAssert.requireTrue(pageSize <= AppConstant.MAX_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页最多查询 20 个"
        );

        long pageNum = req.getPageNum();

        // 2. 只查当前用户
        req.setUserId(userVO.getId());
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), getQueryWrapper(req));

        // 3. 封装 VO
        Page<AppVO> voPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        voPage.setRecords(getAppVOList(appPage.getRecords()));

        return voPage;
    }

    @Override
    public Page<AppVO> listFeaturedApps(AppQueryRequest req) {

        // 1. 参数校验
        BusinessAssert.notNull(req, ErrorCode.PARAMS_ERROR, "查询请求为空");

        long pageSize = req.getPageSize();
        BusinessAssert.requireTrue(pageSize <= AppConstant.MAX_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页最多查询 20 个"
        );

        long pageNum = req.getPageNum();

        // 2. 只查精选
        req.setPriority(AppConstant.GOOD_APP_PRIORITY);
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), getQueryWrapper(req));

        // 3. 封装
        Page<AppVO> voPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        voPage.setRecords(getAppVOList(appPage.getRecords()));

        return voPage;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {

        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }

        // 1. 批量收集 userId，用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. 批量查询用户
        Map<Long, UserVO> userVOMap = userService.ListUserVOByIds(userIds).stream()
                .collect(Collectors.toMap(UserVO::getId, userVO -> userVO));

        // 3. 组装 AppVO
        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            UserVO userVO = userVOMap.get(app.getUserId());
            BeanUtil.copyProperties(app, appVO);
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    // ======================= 管理员侧 =======================

    @Override
    public boolean adminDeleteApp(DeleteRequest req) {

        // 1. 参数校验
        BusinessAssert.notNull(req, ErrorCode.PARAMS_MISSING, "管理员删除请求为空");
        BusinessAssert.requireTrue(req.getId() != null && req.getId() > 0, ErrorCode.PARAMS_ERROR);

        // 2. 判断应用是否存在
        Long id = req.getId();
        App old = this.getById(id);
        BusinessAssert.notNull(old, ErrorCode.NOT_FOUND, "应用不存在");

        // 3. 删除应用
        BusinessAssert.requireTrue(
                this.removeById(id), ErrorCode.BUSINESS_ERROR, "管理员删除应用失败"
        );

        return true;
    }

    @Override
    public boolean adminUpdateApp(AppAdminUpdateRequest req) {

        // 1. 参数校验
        BusinessAssert.notNull(req, ErrorCode.PARAMS_MISSING, "管理员更新请求为空");
        BusinessAssert.notNull(req.getId(), ErrorCode.PARAMS_MISSING, "应用 id 为空");

        // 2. 判断应用是否存在
        App old = this.getById(req.getId());
        BusinessAssert.notNull(old, ErrorCode.NOT_FOUND, "应用不存在");

        // 3. 更新
        App app = new App();
        BeanUtil.copyProperties(req, app);
        app.setAppName(req.getAppName());
        app.setEditTime(LocalDateTime.now());

        BusinessAssert.requireTrue(
                this.updateById(app), ErrorCode.BUSINESS_ERROR, "管理员更新应用失败"
        );

        return true;
    }

    @Override
    public AppVO adminGetAppVOById(Long id) {

        // 1. 参数校验
        BusinessAssert.requireTrue(
                id != null && id > 0, ErrorCode.PARAMS_ERROR, "应用 id 不合法"
        );

        // 2. 查询数据库
        App app = this.getById(id);
        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");

        // 3. 获取封装类
        return this.getAppVO(app);
    }

    @Override
    public Page<AppVO> adminListAppVOByPage(AppQueryRequest req) {

        // 1. 参数校验
        BusinessAssert.notNull(req, ErrorCode.PARAMS_MISSING, "管理员查询请求为空");

        // 2. 按页查询
        long pageNum = req.getPageNum();
        long pageSize = req.getPageSize();
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), this.getQueryWrapper(req));

        // 3. 封装 VO
        Page<AppVO> voPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        voPage.setRecords(this.getAppVOList(appPage.getRecords()));

        return voPage;
    }

    // ======================= 应用侧 ========================

    @Override
    public Flux<String> chatToGenCode(Long appId, Long sessionId, String message, UserLoginVO userVO) {

        BusinessAssert.notNull(appId, ErrorCode.PARAMS_MISSING, "应用ID不能为空");
        BusinessAssert.notBlank(message, ErrorCode.PARAMS_MISSING, "消息不能为空");

        App app = this.getById(appId);
        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");
        BusinessAssert.equals(app.getUserId(), userVO.getId(),
                ErrorCode.NO_PERMISSION, "无权限访问该应用"
        );

        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        BusinessAssert.notNull(codeGenTypeEnum, ErrorCode.SYSTEM_ERROR, "不支持的生成类型");

        return aiCodeGeneratorFacade.generateAndSaveCodeStream(
                message,
                codeGenTypeEnum,
                appId,
                sessionId
        );
    }

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId     应用 ID
     * @param appUrl    应用访问 URL
     */
    private void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图
            String screenshotUrl = screenshotService.capture(appUrl, appId);
            // 更新应用封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            BusinessAssert.requireTrue(this.updateById(updateApp), ErrorCode.BUSINESS_ERROR,
                    "更新应用封面字段失败"
            );
        });
    }

    @Override
    public String deployApp(AppDeployRequest req, UserLoginVO userVO) {

        // 1. 参数校验
        BusinessAssert.notNull(req, ErrorCode.PARAMS_ERROR, "部署请求为空");
        BusinessAssert.notNull(req.getAppId(), ErrorCode.PARAMS_ERROR, "部署应用ID不能为空");
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");
        Long appId = req.getAppId();

        // 2. 查询应用
        App app = this.getById(appId);
        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "该应用不存在");

        // 3. 权限校验（仅本人可部署）
        BusinessAssert.equals(app.getUserId(), userVO.getId(),
                ErrorCode.NO_PERMISSION, "用户无权限部署该应用"
        );

        // 4. deployKey 处理（只生成一次）
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }

        // 5. 构建源目录
        String sourceDirName = app.getCodeGenType() + "_" + appId;
        String sourceDirPath = appDeployConfig.getOutputDir() + File.separator + sourceDirName;

        File sourceDir = new File(sourceDirPath);
        BusinessAssert.requireTrue((sourceDir.exists() && sourceDir.isDirectory()),
                ErrorCode.SYSTEM_ERROR, "未找到生成代码，请先生成代码"
        );

        // 6. 部署目录&链接（对内截图 URL 与对外返回 URL 可分离，见 AppDeployConfig.publicDeployHost）
        String deployDirPath = appDeployConfig.getDeployDir() + File.separator + deployKey;
        String internalBase = StrUtil.removeSuffix(StrUtil.trim(appDeployConfig.getDeployHost()), "/");
        String publicBase = StrUtil.isNotBlank(appDeployConfig.getPublicDeployHost())
                ? StrUtil.removeSuffix(StrUtil.trim(appDeployConfig.getPublicDeployHost()), "/")
                : internalBase;
        String screenshotTargetUrl = internalBase + "/" + deployKey + "/";
        String appDeployUrl = publicBase + "/" + deployKey + "/";
        try {
            // 先清空旧部署目录，避免历史残留文件影响最新版本
            FileUtil.del(deployDirPath);
            FileUtil.mkdir(deployDirPath);
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "该应用部署失败");
        }

        // 7. 更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());

        BusinessAssert.requireTrue(this.updateById(updateApp),
                ErrorCode.BUSINESS_ERROR, "更新部署信息失败"
        );

        // 8. 异步更新应用封面
        generateAppScreenshotAsync(appId, screenshotTargetUrl);

        // 9. 返回URL
        return appDeployUrl;
    }
}
