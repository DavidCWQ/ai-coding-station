package com.cwq.project_aicodingstation.user.service;

import com.cwq.project_aicodingstation.user.dto.UserLoginRequest;
import com.cwq.project_aicodingstation.user.dto.UserRegisterRequest;
import com.cwq.project_aicodingstation.user.dto.UserChangePasswordRequest;
import com.cwq.project_aicodingstation.user.dto.UserUpdateRequest;
import com.cwq.project_aicodingstation.user.entity.SysUser;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.cwq.project_aicodingstation.user.vo.UserVO;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Set;

public interface UserService extends IService<SysUser> {

    Long userRegister(UserRegisterRequest request);

    UserLoginVO userLogin(UserLoginRequest request, HttpServletRequest httpRequest);

    boolean userLogout(HttpServletRequest request);

    UserLoginVO getUserLoginVO(HttpServletRequest request);

    boolean updateMyProfile(UserUpdateRequest request, HttpServletRequest httpRequest);

    boolean changePassword(UserChangePasswordRequest request, HttpServletRequest httpRequest);

    UserVO getVOById(Long id);

    List<UserVO> ListUserVOByIds(Set<Long> ids);
}
