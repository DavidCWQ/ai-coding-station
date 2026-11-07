package com.cwq.project_aicodingstation.mc.controller;

import com.cwq.project_aicodingstation.common.annotation.AuthCheck;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.mc.service.McBedrockService;
import com.cwq.project_aicodingstation.mc.vo.McConsoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/**
 * 登录用户：只能看当前世界（访客视角）。
 */
@RestController
@RequestMapping("/mc")
@RequiredArgsConstructor
public class McBedrockController {

    private final McBedrockService mcBedrockService;

    @GetMapping("/console")
    @AuthCheck
    public BaseResponse<McConsoleVO> console() {
        return ResultUtils.success(mcBedrockService.visitorConsole());
    }

    @GetMapping("/world/icon")
    @AuthCheck
    public ResponseEntity<Resource> icon(@RequestParam String folder) {
        Resource resource = mcBedrockService.worldIcon(folder, false);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=120")
                .body(resource);
    }

    @GetMapping("/pack/icon")
    @AuthCheck
    public ResponseEntity<Resource> packIcon(@RequestParam String id) {
        Resource resource = mcBedrockService.packIcon(id, false);
        String name = resource.getFilename() == null ? "" : resource.getFilename().toLowerCase(Locale.ROOT);
        MediaType type = name.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=120")
                .body(resource);
    }
}
