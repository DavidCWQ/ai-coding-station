package com.cwq.project_aicodingstation.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import com.cwq.project_aicodingstation.user.dto.UserQueryRequest;
import com.cwq.project_aicodingstation.user.enums.UserRoleEnum;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.cwq.project_aicodingstation.user.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.cwq.project_aicodingstation.user.entity.SysUser;
import com.cwq.project_aicodingstation.user.mapper.SysUserMapper;
import com.cwq.project_aicodingstation.user.service.SysUserService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @implNote Service Layer (基础用户能力 [DAO+通用方法])
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER =
            new BCryptPasswordEncoder();

    @Override
    public Long userRegister(String userAccount, String userPassword, String checkPassword) {

        // 1. 参数校验
        BusinessAssert.failIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword),
                ErrorCode.PARAMS_MISSING, "注册参数为空"
        );
        BusinessAssert.failIf(userAccount.length() < 4,
                ErrorCode.PARAMS_INVALID, "用户账号过短"
        );
        BusinessAssert.failIf(userPassword.length() < 8,
                ErrorCode.PARAMS_INVALID, "用户密码过短"
        );
        BusinessAssert.equals(userPassword, checkPassword,
                ErrorCode.PARAMS_ERROR, "两次密码不一致"
        );

        // 2. 账号唯一性校验
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("user_account", userAccount);

        BusinessAssert.failIf(this.count(queryWrapper) > 0,
                ErrorCode.PARAMS_ERROR, "账号已存在"
        );

        // 3. 密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 4. 持久化
        SysUser user = new SysUser();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(UserConstant.DEFAULT_USERNAME);
        user.setUserRole(UserRoleEnum.USER.getValue());

        BusinessAssert.requireTrue(this.save(user),
                ErrorCode.SYSTEM_ERROR, "注册失败"
        );

        return user.getId();
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        return PASSWORD_ENCODER.encode(userPassword);
    }

    @Override
    public UserLoginVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {

        // 1. 参数校验
        BusinessAssert.notNull(request,
                ErrorCode.PARAMS_MISSING, "用户登陆(http)请求为空"
        );
        BusinessAssert.failIf(StrUtil.hasBlank(userAccount, userPassword),
                ErrorCode.PARAMS_MISSING, "登陆参数为空"
        );
        BusinessAssert.failIf(userAccount.length() < 4,
                ErrorCode.PARAMS_INVALID, "用户账号过短"
        );
        BusinessAssert.failIf(userPassword.length() < 8,
                ErrorCode.PARAMS_INVALID, "用户密码过短"
        );

        // 2. 查询用户是否存在（仅按账号查询）
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("user_account", userAccount);
        SysUser user = this.mapper.selectOneByQuery(queryWrapper);
        BusinessAssert.notNull(user, ErrorCode.PARAMS_ERROR, "用户不存在");

        // 3. 使用BCrypt验证密码
        BusinessAssert.requireTrue(
                PASSWORD_ENCODER.matches(userPassword, user.getUserPassword()),
                ErrorCode.PARAMS_ERROR, "密码错误"
        );

        // 4. 记录用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

        // 5. 获得脱敏后的用户信息
        return this.getUserLoginVO(user);
    }

    @Override
    public UserLoginVO getUserLoginVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtil.copyProperties(user, userLoginVO);
        return userLoginVO;
    }

    @Override
    public SysUser getLoginUser(HttpServletRequest request) {

        // 0. 参数校验
        BusinessAssert.notNull(request,
                ErrorCode.PARAMS_MISSING, "获取用户(http)请求为空"
        );

        // 1. 先判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        SysUser currentUser = (SysUser) userObj;

        BusinessAssert.notNull(currentUser, ErrorCode.NOT_LOGIN, "当前用户未登录");
        BusinessAssert.notNull(currentUser.getId(), ErrorCode.NOT_LOGIN, "用户id不存在");

        // 2. 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        currentUser = this.getById(currentUser.getId());
        BusinessAssert.notNull(currentUser, ErrorCode.NOT_LOGIN, "当前用户未登录");

        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {

        // 0. 参数校验
        BusinessAssert.notNull(request, ErrorCode.PARAMS_ERROR, "登出(http)请求为空");

        // 1. 先判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        BusinessAssert.notNull(userObj, ErrorCode.SYSTEM_ERROR, "未登录");

        // 2. 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<SysUser> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {

        // 1. 参数校验
        BusinessAssert.notNull(userQueryRequest,
                ErrorCode.PARAMS_ERROR, "查询请求参数为空"
        );

        // 2. 获取请求信息
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        return QueryWrapper.create()
                .eq("id", id)
                .eq("user_role", userRole)
                .like("user_account", userAccount)
                .like("user_name", userName)
                .like("user_profile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

}
