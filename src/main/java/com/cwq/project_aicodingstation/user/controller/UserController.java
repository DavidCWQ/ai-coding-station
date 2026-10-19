package com.cwq.project_aicodingstation.user.controller;

import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.user.dto.UserBindEmailRequest;
import com.cwq.project_aicodingstation.user.dto.UserForgotPasswordRequest;
import com.cwq.project_aicodingstation.user.dto.UserLoginRequest;
import com.cwq.project_aicodingstation.user.dto.UserRegisterRequest;
import com.cwq.project_aicodingstation.user.dto.UserResetPasswordRequest;
import com.cwq.project_aicodingstation.user.dto.UserChangePasswordRequest;
import com.cwq.project_aicodingstation.user.dto.UserUpdateRequest;
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

    /**
     * 当前登录用户更新个人资料（用户名、头像、简介）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateMyProfile(@RequestBody UserUpdateRequest request,
                                                 HttpServletRequest httpRequest) {
        return ResultUtils.success(userService.updateMyProfile(request, httpRequest));
    }

    /**
     * 当前登录用户修改密码
     */
    @PostMapping("/change-password")
    public BaseResponse<Boolean> changePassword(@RequestBody UserChangePasswordRequest request,
                                                HttpServletRequest httpRequest) {
        return ResultUtils.success(userService.changePassword(request, httpRequest));
    }

    /**
     * 按账号申请重置邮件。账号不存在或未绑定邮箱时，返回与成功相同的提示。
     */
    @PostMapping("/password/forgot")
    public BaseResponse<String> forgotPassword(@RequestBody UserForgotPasswordRequest request,
                                               HttpServletRequest httpRequest) {
        return ResultUtils.success(userService.requestPasswordReset(request, httpRequest));
    }

    /**
     * 使用邮件中的令牌设置新密码。旧登录态会在下一次请求时失效。
     */
    @PostMapping("/password/reset")
    public BaseResponse<Boolean> resetPassword(@RequestBody UserResetPasswordRequest request,
                                               HttpServletRequest httpRequest) {
        return ResultUtils.success(userService.resetPassword(request, httpRequest));
    }

    /**
     * 当前用户绑定或更换邮箱。邮箱留空则解除绑定。
     */
    @PostMapping("/email/bind")
    public BaseResponse<Boolean> bindEmail(@RequestBody UserBindEmailRequest request,
                                           HttpServletRequest httpRequest) {
        return ResultUtils.success(userService.bindEmail(request, httpRequest));
    }
}
