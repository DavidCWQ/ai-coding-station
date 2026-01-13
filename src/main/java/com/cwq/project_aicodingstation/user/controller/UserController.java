package com.cwq.project_aicodingstation.user.controller;

import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.user.dto.UserLoginRequest;
import com.cwq.project_aicodingstation.user.dto.UserRegisterRequest;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @apiNote User Control Layer (普通用户：注册/登录/当前用户)
 * [调用链：Controller → Service (业务规则、Assert) → Mapper → DataBase；按业务域分包，按业务模块纵向拆]
 * Controller 只做「参数接收 + 转发」
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    // ============= 数据模型(注册/登陆) -> 服务开发 -> 接口开发 ============

    /**
     * 用户注册
     *
     * @param request 用户注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest request) {
        return ResultUtils.success(userService.userRegister(request));
    }

    /**
     * 用户登陆
     *
     * @param request 用户注册请求
     * @param httpRequest 客户端（如浏览器）向服务器发送的 HTTP 请求信息
     * @return 登陆结果
     */
    @PostMapping("/login")
    public BaseResponse<UserLoginVO> userLogin(@RequestBody UserLoginRequest request,
                                               HttpServletRequest httpRequest) {
        return ResultUtils.success(userService.userLogin(request, httpRequest));
    }

    /**
     * 用户注销
     *
     * @param httpRequest  客户端（如浏览器）向服务器发送的 HTTP 请求信息
     * @return 注销结果
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest httpRequest) {
        return ResultUtils.success(userService.userLogout(httpRequest));
    }

    /**
     * 获取当前登录用户
     *
     * @param httpRequest 客户端（如浏览器）向服务器发送的 HTTP 请求信息
     * @return 登陆结果
     */
    @GetMapping("/get/login")
    public BaseResponse<UserLoginVO> getLoginUser(HttpServletRequest httpRequest) {
        return ResultUtils.success(userService.getUserLoginVO(httpRequest));
    }
}
