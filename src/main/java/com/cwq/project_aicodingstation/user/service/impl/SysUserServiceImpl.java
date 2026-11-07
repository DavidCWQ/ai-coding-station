package com.cwq.project_aicodingstation.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import com.cwq.project_aicodingstation.user.dto.UserQueryRequest;
import com.cwq.project_aicodingstation.user.enums.UserRoleEnum;
import com.cwq.project_aicodingstation.user.security.AuthThrottle;
import com.cwq.project_aicodingstation.user.security.ClientIpResolver;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.cwq.project_aicodingstation.user.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
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

    /** 账号不存在时仍做一次比对，避免响应时间暴露账号是否存在。 */
    private static final String DUMMY_PASSWORD_HASH = PASSWORD_ENCODER.encode("login-timing-placeholder");

    private static final String LOGIN_FAILED_MESSAGE = "账号或密码错误";

    @Resource
    private AuthThrottle authThrottle;

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
        userAccount = userAccount.trim();
        BusinessAssert.failIf(userAccount.length() < 4,
                ErrorCode.PARAMS_INVALID, "用户账号过短"
        );
        BusinessAssert.failIf(userPassword.length() < 8,
                ErrorCode.PARAMS_INVALID, "用户密码过短"
        );

        String accountKey = AuthThrottle.accountKey(userAccount);
        String clientIp = ClientIpResolver.resolve(request);
        authThrottle.assertLoginAllowed(accountKey, clientIp);

        // 2. 查询用户是否存在（仅按账号查询）。不存在和密码错误返回同一句话。
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("user_account", userAccount);
        SysUser user = this.mapper.selectOneByQuery(queryWrapper);
        boolean passwordOk = false;
        if (user == null) {
            PASSWORD_ENCODER.matches(userPassword, DUMMY_PASSWORD_HASH);
        } else {
            try {
                passwordOk = PASSWORD_ENCODER.matches(userPassword, user.getUserPassword());
            } catch (IllegalArgumentException ex) {
                passwordOk = false;
            }
        }
        if (!passwordOk) {
            if (authThrottle.recordLoginFailure(accountKey, clientIp)) {
                authThrottle.throwLoginLocked(accountKey, clientIp);
            }
            BusinessAssert.failIf(true, ErrorCode.PARAMS_ERROR, LOGIN_FAILED_MESSAGE);
        }
        authThrottle.clearLoginFailures(accountKey, clientIp);

        // 3. 记录用户的登录态（含密码哈希，用于发现密码被重置后作废旧会话）
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
        BeanUtil.copyProperties(user, userLoginVO, "userPassword");
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
        String sessionPassword = currentUser.getUserPassword();
        currentUser = this.getById(currentUser.getId());
        BusinessAssert.notNull(currentUser, ErrorCode.NOT_LOGIN, "当前用户未登录");

        // 密码重置或修改后，库中的哈希与会话里保存的不一致，旧登录态立即失效
        if (sessionPassword != null && !sessionPassword.equals(currentUser.getUserPassword())) {
            request.getSession().invalidate();
            BusinessAssert.failIf(true, ErrorCode.NOT_LOGIN, "登录已失效，请重新登录");
        }

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

        // 3. 原先无条件 .eq("id", id)，前端占位 id: 0 会变成 WHERE id = 0，结果集为空。
        //    现已经改为: iff id != null && id > 0 才加 id 条件，以此类推。
        QueryWrapper qw = QueryWrapper.create();
        if (id != null && id > 0) {
            qw.eq("id", id);
        }
        if (userRole != null) {
            qw.eq("user_role", userRole);
        }
        if (StrUtil.isNotBlank(userAccount)) {
            qw.like("user_account", userAccount);
        }
        if (StrUtil.isNotBlank(userName)) {
            qw.like("user_name", userName);
        }
        if (StrUtil.isNotBlank(userProfile)) {
            qw.like("user_profile", userProfile);
        }
        return qw.orderBy(sortField, "ascend".equals(sortOrder));
    }

}
