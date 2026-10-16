package com.cwq.project_aicodingstation.mc.controller;

import com.cwq.project_aicodingstation.common.annotation.AuthCheck;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.mc.dto.McAllowlistUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McGameruleUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McOpResultVO;
import com.cwq.project_aicodingstation.mc.dto.McPermissionUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McSettingsUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McWorldActivateRequest;
import com.cwq.project_aicodingstation.mc.service.McBedrockService;
import com.cwq.project_aicodingstation.mc.vo.McConsoleVO;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/mc/admin")
@RequiredArgsConstructor
public class McBedrockAdminController {

    private final McBedrockService mcBedrockService;

    @GetMapping("/console")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McConsoleVO> console() {
        return ResultUtils.success(mcBedrockService.adminConsole());
    }

    @GetMapping("/world/icon")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseEntity<Resource> icon(@RequestParam String folder) {
        Resource resource = mcBedrockService.worldIcon(folder, true);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
                .body(resource);
    }

    @GetMapping("/world/download")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseEntity<Resource> download(@RequestParam String folder) {
        Resource resource = mcBedrockService.downloadWorld(folder);
        String filename = URLEncoder.encode(folder + ".mcworld.zip", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(resource);
    }

    @PostMapping("/settings")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McOpResultVO> settings(@RequestBody McSettingsUpdateRequest req) {
        return ResultUtils.success(mcBedrockService.updateSettings(req));
    }

    @PostMapping("/gamerules")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McOpResultVO> gamerules(@RequestBody McGameruleUpdateRequest req) {
        return ResultUtils.success(mcBedrockService.updateGamerules(req));
    }

    @PostMapping("/permission")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McOpResultVO> permission(@RequestBody McPermissionUpdateRequest req) {
        return ResultUtils.success(mcBedrockService.updatePermission(req));
    }

    @PostMapping("/allowlist")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McOpResultVO> allowlist(@RequestBody McAllowlistUpdateRequest req) {
        return ResultUtils.success(mcBedrockService.updateAllowlist(req));
    }

    @PostMapping("/world/activate")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McOpResultVO> activate(@RequestBody McWorldActivateRequest req) {
        return ResultUtils.success(mcBedrockService.activateWorld(req));
    }

    @PostMapping("/backup")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McOpResultVO> backup() {
        return ResultUtils.success(mcBedrockService.backupNow());
    }

    @PostMapping("/world/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McOpResultVO> deleteWorld(@RequestParam String folder) {
        return ResultUtils.success(mcBedrockService.deleteWorld(folder));
    }

    @PostMapping("/backup/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McOpResultVO> deleteBackup(@RequestParam String filename) {
        return ResultUtils.success(mcBedrockService.deleteBackup(filename));
    }

    @PostMapping("/server/start")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McOpResultVO> start() {
        return ResultUtils.success(mcBedrockService.startServer());
    }

    @PostMapping("/server/stop")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McOpResultVO> stop() {
        return ResultUtils.success(mcBedrockService.stopServer());
    }

    @GetMapping("/ops/{opId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<McOpResultVO> opStatus(@PathVariable String opId) {
        return ResultUtils.success(mcBedrockService.opStatus(opId));
    }
}
