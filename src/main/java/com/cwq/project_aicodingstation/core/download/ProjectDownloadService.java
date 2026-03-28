package com.cwq.project_aicodingstation.core.download;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 项目目录打包下载（ZIP），供业务层复用。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
public interface ProjectDownloadService {

    /**
     * 将指定目录打包为 ZIP 并写入响应流。
     *
     * @param projectPath      项目根目录绝对路径
     * @param downloadFileName 下载文件名（不含 .zip 后缀）
     * @param response         HTTP 响应
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
