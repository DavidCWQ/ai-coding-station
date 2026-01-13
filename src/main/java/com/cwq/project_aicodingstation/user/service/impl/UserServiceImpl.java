package com.cwq.project_aicodingstation.user.service.impl;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.dto.UserLoginRequest;
import com.cwq.project_aicodingstation.user.dto.UserRegisterRequest;
import com.cwq.project_aicodingstation.user.entity.SysUser;
import com.cwq.project_aicodingstation.user.service.SysUserService;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl implements UserService {

    @Resource
    private SysUserService sysUserService;

    @Override
    public Long userRegister(UserRegisterRequest request) {
        BusinessAssert.notNull(request, ErrorCode.PARAMS_MISSING, "用户注册请求为空");
        return sysUserService.userRegister(
                request.getUserAccount(),
                request.getUserPassword(),
                request.getCheckPassword()
        );
    }

    @Override
    public UserLoginVO userLogin(UserLoginRequest request, HttpServletRequest httpRequest) {
        BusinessAssert.notNull(request, ErrorCode.PARAMS_MISSING, "用户登陆请求为空");
        return sysUserService.userLogin(
                request.getUserAccount(),
                request.getUserPassword(),
                httpRequest
        );
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        return sysUserService.userLogout(request);
    }

    @Override
    public UserLoginVO getUserLoginVO(HttpServletRequest request) {
        SysUser user = sysUserService.getLoginUser(request);
        return sysUserService.getUserLoginVO(user);
    }


}
