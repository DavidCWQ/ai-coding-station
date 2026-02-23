package com.cwq.project_aicodingstation.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.dto.UserLoginRequest;
import com.cwq.project_aicodingstation.user.dto.UserRegisterRequest;
import com.cwq.project_aicodingstation.user.entity.SysUser;
import com.cwq.project_aicodingstation.user.mapper.SysUserMapper;
import com.cwq.project_aicodingstation.user.service.SysUserService;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.cwq.project_aicodingstation.user.vo.UserVO;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

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

    @Override
    public UserVO getVOById(Long id) {
        return sysUserService.getUserVO(getById(id));
    }

    @Override
    public List<UserVO> ListUserVOByIds(Set<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return new ArrayList<>();
        }
        return this.listByIds(ids).stream()
                .map(sysUserService::getUserVO)
                .collect(Collectors.toList());
    }
}
