package com.cwq.project_aicodingstation.app.controller;

import cn.hutool.json.JSONUtil;
import com.cwq.project_aicodingstation.app.dto.AppAddRequest;
import com.cwq.project_aicodingstation.app.dto.AppChatGenCodeRequest;
import com.cwq.project_aicodingstation.app.dto.AppDeployRequest;
import com.cwq.project_aicodingstation.app.dto.AppQueryRequest;
import com.cwq.project_aicodingstation.app.dto.AppUpdateRequest;
import com.cwq.project_aicodingstation.app.config.AppDeployConfig;
import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.service.AppService;
import com.cwq.project_aicodingstation.app.vo.AppVO;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.request.DeleteRequest;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.core.download.ProjectDownloadService;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Map;

/**
 * @apiNote App Control Layer (普通用户)
 * [调用链：Controller → Service (业务规则、Assert) → Mapper → DataBase；按业务域分包，按业务模块纵向拆]
 * Controller 只做「参数接收 + 转发」
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */

@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private AppDeployConfig appDeployConfig;

    @Resource
    private ProjectDownloadService projectDownloadService;

    /* ======================= 用户接口 ======================= */

    /**
     * 用户创建应用
     */
    @PostMapping("/create")
    public BaseResponse<Long> createApp(@RequestBody AppAddRequest req,
                                        HttpServletRequest request) {
        return ResultUtils.success(
                appService.createApp(req, userService.getUserLoginVO(request))
        );
    }

    /**
     * 用户更新应用
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest req,
                                           HttpServletRequest request) {
        return ResultUtils.success(
                appService.updateApp(req, userService.getUserLoginVO(request))
        );
    }

    /**
     * 用户删除应用
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest req,
                                           HttpServletRequest request) {
        return ResultUtils.success(
                appService.deleteApp(req, userService.getUserLoginVO(request))
        );
    }

    /**
     * 根据 id 获取应用详情
     */
    @GetMapping("/get/{id}")
    public BaseResponse<AppVO> getApp(@PathVariable Long id) {
        return ResultUtils.success(appService.getAppVOById(id));
    }

    /**
     * 分页获取 当前用户创建的应用列表
     */
    @PostMapping("/my/list")
    public BaseResponse<Page<AppVO>> listMyApps(@RequestBody AppQueryRequest req,
                                                HttpServletRequest request) {
        return ResultUtils.success(
                appService.listMyApps(req, userService.getUserLoginVO(request))
        );
    }

    /**
     * 分页获取 精选应用列表
     */
    @PostMapping("/featured/list")
    public BaseResponse<Page<AppVO>> listFeaturedApps(@RequestBody AppQueryRequest req) {
        return ResultUtils.success(appService.listFeaturedApps(req));
    }

    /* ======================= SSE接口 ======================= */

    /**
     * 应用聊天生成代码 (SSE流式)
     */
    @GetMapping(value = "/chat/genCode", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam Long sessionId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        return toSSE(appService // 调用 Service，封装 JSON + 追加 done 事件
                .chatToGenCode(appId, sessionId, message, userService.getUserLoginVO(request))
        );
    }

    /**
     * 应用聊天生成代码 (SSE流式，POST 防止长文本触发 431 ERROR)
     */
    @PostMapping(value = "/chat/genCode", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestBody @Valid AppChatGenCodeRequest req,
                                                       HttpServletRequest request) {
        return toSSE(appService
                .chatToGenCode(req.getAppId(), req.getSessionId(), req.getMessage(),
                        userService.getUserLoginVO(request))
        );
    }

    private Flux<ServerSentEvent<String>> toSSE(Flux<String> source) {
        return source
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String json = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(json)
                            .build();
                })
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    /**
     * 应用部署
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest req,
                                          HttpServletRequest request) {
        return ResultUtils.success(
                appService.deployApp(req, userService.getUserLoginVO(request))
        );
    }

    /**
     * 下载应用生成代码 (ZIP)
     *
     * @param appId    应用 ID
     * @param request  请求
     * @param response 响应
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId, HttpServletRequest request,
                                HttpServletResponse response) {

        // 1. 参数校验
        BusinessAssert.requireTrue(appId != null && appId > 0,
                ErrorCode.PARAMS_ERROR, "应用ID无效");

        App app = appService.getById(appId);
        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");

        UserLoginVO userVO = userService.getUserLoginVO(request);
        BusinessAssert.equals(app.getUserId(), userVO.getId(),
                ErrorCode.NO_PERMISSION, "无权限下载该应用代码：仅原作者可下载");

        String sourceDirName = app.getCodeGenType() + "_" + appId;
        String sourceDirPath = appDeployConfig.getOutputDir() + File.separator + sourceDirName;

        File sourceDir = new File(sourceDirPath);
        BusinessAssert.requireTrue(sourceDir.exists() && sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND, "应用代码不存在，请先生成代码");

        String downloadFileName = String.valueOf(appId);
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }
}
