package com.cwq.project_aicodingstation.mc.service;

import com.cwq.project_aicodingstation.mc.dto.McAllowlistUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McGameruleUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McOpResultVO;
import com.cwq.project_aicodingstation.mc.dto.McPermissionUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McSettingsUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McWorldActivateRequest;
import com.cwq.project_aicodingstation.mc.vo.McConsoleVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface McBedrockService {

    McConsoleVO visitorConsole();

    McConsoleVO adminConsole();

    Resource worldIcon(String folder, boolean admin);

    Resource downloadWorld(String folder);

    McOpResultVO updateSettings(McSettingsUpdateRequest req);

    McOpResultVO updateGamerules(McGameruleUpdateRequest req);

    McOpResultVO updatePermission(McPermissionUpdateRequest req);

    McOpResultVO updateAllowlist(McAllowlistUpdateRequest req);

    McOpResultVO activateWorld(McWorldActivateRequest req);

    McOpResultVO backupNow();

    McOpResultVO deleteWorld(String folder);

    McOpResultVO uploadWorld(MultipartFile file);

    McOpResultVO deleteBackup(String filename);

    McOpResultVO startServer();

    McOpResultVO stopServer();

    McOpResultVO opStatus(String opId);
}
