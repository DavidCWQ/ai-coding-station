package com.cwq.project_aicodingstation.stats.controller;

import com.cwq.project_aicodingstation.common.annotation.AuthCheck;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.stats.dto.VisitHitRequest;
import com.cwq.project_aicodingstation.stats.service.StatsService;
import com.cwq.project_aicodingstation.stats.vo.StatsOverviewVO;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 访问统计。上报接口不要求登录，管理端概览只对管理员开放。
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Resource
    private StatsService statsService;

    /**
     * 记录一次页面浏览或心跳。爬虫、重复会话和无效目标返回 false，不计入浏览量。
     */
    @PostMapping("/hit")
    public BaseResponse<Boolean> hit(@RequestBody VisitHitRequest request, HttpServletRequest httpRequest) {
        return ResultUtils.success(statsService.record(request, httpRequest));
    }

    /**
     * 今日全站浏览量、独立访客、在线人数、趋势和应用访问排行。
     */
    @GetMapping("/admin/overview")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<StatsOverviewVO> overview(@RequestParam(defaultValue = "7") int days) {
        return ResultUtils.success(statsService.overview(days));
    }
}
