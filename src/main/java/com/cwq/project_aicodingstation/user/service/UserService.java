package com.cwq.project_aicodingstation.user.service;

import com.cwq.project_aicodingstation.user.dto.UserLoginRequest;
import com.cwq.project_aicodingstation.user.dto.UserRegisterRequest;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {

    Long userRegister(UserRegisterRequest request);

    UserLoginVO userLogin(UserLoginRequest request, HttpServletRequest httpRequest);

    boolean userLogout(HttpServletRequest request);

    UserLoginVO getUserLoginVO(HttpServletRequest request);
}

