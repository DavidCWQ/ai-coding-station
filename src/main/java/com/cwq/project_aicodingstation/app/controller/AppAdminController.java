package com.cwq.project_aicodingstation.app.controller;

import com.cwq.project_aicodingstation.app.dto.AppAdminUpdateRequest;
import com.cwq.project_aicodingstation.app.dto.AppQueryRequest;
import com.cwq.project_aicodingstation.app.service.AppService;
import com.cwq.project_aicodingstation.app.vo.AppVO;
import com.cwq.project_aicodingstation.common.annotation.AuthCheck;
import com.cwq.project_aicodingstation.common.request.DeleteRequest;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @apiNote App Admin Control Layer (管理员)
 * [调用链：Controller → Service (业务规则、Assert) → Mapper → DataBase；按业务域分包，按业务模块纵向拆]
 * Controller 只做「参数接收 + 转发」
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */

@RestController
@RequestMapping("/app/admin")
@RequiredArgsConstructor
public class AppAdminController {

    @Resource
    private AppService appService;

    /* ====================== 管理员接口 ====================== */

    /**
     * 管理员删除应用
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminDeleteApp(@RequestBody DeleteRequest req) {
        Boolean result = appService.adminDeleteApp(req);
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新应用
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdateApp(@RequestBody AppAdminUpdateRequest req) {
        Boolean result = appService.adminUpdateApp(req);
        return ResultUtils.success(result);
    }

    /**
     * 管理员查看应用详情
     */
    @GetMapping("/get/vo/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVO(@PathVariable Long id) {
        AppVO vo = appService.adminGetAppVOById(id);
        return ResultUtils.success(vo);
    }

    /**
     * 管理员分页查询应用
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listApp(@RequestBody AppQueryRequest req) {
        Page<AppVO> page = appService.adminListAppVOByPage(req);
        return ResultUtils.success(page);
    }
}