package com.cwq.project_aicodingstation.app.service;

import com.cwq.project_aicodingstation.app.dto.AppReplaceImageRequest;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import org.springframework.web.multipart.MultipartFile;

public interface AppAssetService {

    /**
     * 上传应用图片资源并返回可访问相对路径（/img/...）。
     */
    String uploadImage(Long appId, Long sessionId, MultipartFile file, UserLoginVO userVO);

    /**
     * 直接替换应用页面中的目标图片。
     */
    boolean replaceImage(AppReplaceImageRequest req, UserLoginVO userVO);
}
