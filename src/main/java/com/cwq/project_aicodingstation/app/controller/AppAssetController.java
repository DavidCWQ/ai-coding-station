package com.cwq.project_aicodingstation.app.controller;

import com.cwq.project_aicodingstation.app.dto.AppReplaceImageRequest;
import com.cwq.project_aicodingstation.app.service.AppAssetService;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/app/assets")
public class AppAssetController {

    @Resource
    private AppAssetService appAssetService;

    @Resource
    private UserService userService;

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<String> uploadImage(@RequestParam Long appId,
                                            @RequestParam(required = false) Long sessionId,
                                            @RequestPart("file") MultipartFile file,
                                            HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        String webPath = appAssetService.uploadImage(appId, sessionId, file, userVO);
        return ResultUtils.success(webPath);
    }

    @PostMapping("/replace-image")
    public BaseResponse<Boolean> replaceImage(@RequestBody @Valid AppReplaceImageRequest req,
                                              HttpServletRequest request) {
        UserLoginVO userVO = userService.getUserLoginVO(request);
        return ResultUtils.success(appAssetService.replaceImage(req, userVO));
    }
}
