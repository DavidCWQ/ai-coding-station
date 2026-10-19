package com.cwq.project_aicodingstation.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.dto.UserBindEmailRequest;
import com.cwq.project_aicodingstation.user.dto.UserChangePasswordRequest;
import com.cwq.project_aicodingstation.user.dto.UserForgotPasswordRequest;
import com.cwq.project_aicodingstation.user.dto.UserLoginRequest;
import com.cwq.project_aicodingstation.user.dto.UserRegisterRequest;
import com.cwq.project_aicodingstation.user.dto.UserResetPasswordRequest;
import com.cwq.project_aicodingstation.user.dto.UserUpdateRequest;
import com.cwq.project_aicodingstation.user.entity.SysUser;
import com.cwq.project_aicodingstation.user.mapper.SysUserMapper;
import com.cwq.project_aicodingstation.user.security.AuthThrottle;
import com.cwq.project_aicodingstation.user.security.ClientIpResolver;
import com.cwq.project_aicodingstation.user.security.EmailAddresses;
import com.cwq.project_aicodingstation.user.security.PasswordResetMailer;
import com.cwq.project_aicodingstation.user.security.PasswordResetTokenStore;
import com.cwq.project_aicodingstation.user.service.SysUserService;
import com.cwq.project_aicodingstation.user.service.UserService;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.cwq.project_aicodingstation.user.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    static final String FORGOT_PASSWORD_HINT = "如果该账号已绑定邮箱，重置邮件已发送，请查收";

    @Resource
    private SysUserService sysUserService;

    @Resource
    private AuthThrottle authThrottle;

    @Resource
    private PasswordResetTokenStore passwordResetTokenStore;

    @Resource
    private PasswordResetMailer passwordResetMailer;

    @Value("${app.user.registration-enabled:true}")
    private boolean registrationEnabled;

    @Override
    public Long userRegister(UserRegisterRequest request) {
        BusinessAssert.notNull(request, ErrorCode.PARAMS_MISSING, "用户注册请求为空");
        BusinessAssert.requireTrue(registrationEnabled, ErrorCode.NO_PERMISSION, "注册已关闭，请使用已有账号登录");
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
    public boolean updateMyProfile(UserUpdateRequest request, HttpServletRequest httpRequest) {

        // 1. 参数校验
        BusinessAssert.notNull(request, ErrorCode.PARAMS_MISSING, "更新请求为空");
        BusinessAssert.notBlank(request.getUserName(), ErrorCode.PARAMS_INVALID, "用户名不能为空");

        SysUser current = sysUserService.getLoginUser(httpRequest);
        BusinessAssert.failIf(
                request.getId() != null && !request.getId().equals(current.getId()),
                ErrorCode.PARAMS_ERROR, "无权修改其他用户资料"
        );

        // 2. 修改资料（昵称，头像，简介）
        current.setUserName(request.getUserName().trim());
        if (request.getUserAvatar() != null) {
            current.setUserAvatar(request.getUserAvatar());
        }
        if (request.getUserProfile() != null) {
            current.setUserProfile(request.getUserProfile());
        }
        current.setUpdateTime(LocalDateTime.now());
        BusinessAssert.requireTrue(
                sysUserService.updateById(current),
                ErrorCode.SYSTEM_ERROR, "更新用户资料失败"
        );

        return true;
    }

    @Override
    public boolean changePassword(UserChangePasswordRequest request, HttpServletRequest httpRequest) {
        BusinessAssert.notNull(request, ErrorCode.PARAMS_MISSING, "修改密码请求为空");
        BusinessAssert.failIf(
                StrUtil.hasBlank(request.getOldPassword(), request.getNewPassword(), request.getCheckNewPassword()),
                ErrorCode.PARAMS_MISSING, "密码参数不能为空"
        );
        BusinessAssert.failIf(request.getNewPassword().length() < 8,
                ErrorCode.PARAMS_INVALID, "新密码至少8位");
        BusinessAssert.equals(request.getNewPassword(), request.getCheckNewPassword(),
                ErrorCode.PARAMS_ERROR, "两次新密码不一致");

        SysUser current = sysUserService.getLoginUser(httpRequest);

        BusinessAssert.requireTrue(
                org.springframework.security.crypto.bcrypt.BCrypt.checkpw(
                        request.getOldPassword(), current.getUserPassword()
                ),
                ErrorCode.PARAMS_ERROR,
                "原密码错误"
        );

        current.setUserPassword(sysUserService.getEncryptPassword(request.getNewPassword()));
        current.setUpdateTime(LocalDateTime.now());
        BusinessAssert.requireTrue(sysUserService.updateById(current),
                ErrorCode.SYSTEM_ERROR, "修改密码失败");

        // 修改密码成功后，作废当前 HTTP 会话，避免旧会话继续携带登录态，强制用户重新登录
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return true;
    }

    @Override
    public String requestPasswordReset(UserForgotPasswordRequest request, HttpServletRequest httpRequest) {
        BusinessAssert.notNull(request, ErrorCode.PARAMS_MISSING, "重置密码请求为空");
        BusinessAssert.notNull(httpRequest, ErrorCode.PARAMS_MISSING, "重置密码请求为空");
        String userAccount = request.getUserAccount() == null ? "" : request.getUserAccount().trim();
        BusinessAssert.failIf(userAccount.length() < 4, ErrorCode.PARAMS_INVALID, "请输入正确的账号");
        BusinessAssert.requireTrue(passwordResetMailer.isReady(), ErrorCode.BUSINESS_ERROR, "邮件服务暂不可用");

        String clientIp = ClientIpResolver.resolve(httpRequest);
        authThrottle.assertForgotAllowed(AuthThrottle.accountKey(userAccount), clientIp);

        SysUser user = sysUserService.getOne(QueryWrapper.create().eq("user_account", userAccount));
        if (user == null || StrUtil.isBlank(user.getUserEmail())) {
            return FORGOT_PASSWORD_HINT;
        }
        String token = passwordResetTokenStore.issue(user.getId());
        try {
            passwordResetMailer.send(user.getUserEmail(), token);
        } catch (RuntimeException ex) {
            log.error("Password reset mail failed, userId={}", user.getId(), ex);
        }
        return FORGOT_PASSWORD_HINT;
    }

    @Override
    public boolean resetPassword(UserResetPasswordRequest request, HttpServletRequest httpRequest) {
        BusinessAssert.notNull(request, ErrorCode.PARAMS_MISSING, "重置密码请求为空");
        BusinessAssert.notNull(httpRequest, ErrorCode.PARAMS_MISSING, "重置密码请求为空");
        BusinessAssert.failIf(
                StrUtil.hasBlank(request.getToken(), request.getNewPassword(), request.getCheckNewPassword()),
                ErrorCode.PARAMS_MISSING, "密码参数不能为空"
        );
        BusinessAssert.failIf(request.getNewPassword().length() < 8,
                ErrorCode.PARAMS_INVALID, "新密码至少8位");
        BusinessAssert.equals(request.getNewPassword(), request.getCheckNewPassword(),
                ErrorCode.PARAMS_ERROR, "两次新密码不一致");

        authThrottle.assertResetSubmitAllowed(ClientIpResolver.resolve(httpRequest));
        Long userId = passwordResetTokenStore.consume(request.getToken());
        BusinessAssert.notNull(userId, ErrorCode.PARAMS_ERROR, "重置链接无效或已过期");

        SysUser user = sysUserService.getById(userId);
        BusinessAssert.notNull(user, ErrorCode.PARAMS_ERROR, "重置链接无效或已过期");
        user.setUserPassword(sysUserService.getEncryptPassword(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        BusinessAssert.requireTrue(sysUserService.updateById(user),
                ErrorCode.SYSTEM_ERROR, "重置密码失败");
        return true;
    }

    @Override
    public boolean bindEmail(UserBindEmailRequest request, HttpServletRequest httpRequest) {
        BusinessAssert.notNull(request, ErrorCode.PARAMS_MISSING, "绑定邮箱请求为空");
        BusinessAssert.failIf(StrUtil.isBlank(request.getPassword()),
                ErrorCode.PARAMS_MISSING, "请输入当前密码");

        SysUser current = sysUserService.getLoginUser(httpRequest);
        BusinessAssert.requireTrue(
                org.springframework.security.crypto.bcrypt.BCrypt.checkpw(
                        request.getPassword(), current.getUserPassword()
                ),
                ErrorCode.PARAMS_ERROR, "密码错误"
        );

        String email = null;
        if (StrUtil.isNotBlank(request.getUserEmail())) {
            email = EmailAddresses.normalize(request.getUserEmail());
            BusinessAssert.notNull(email, ErrorCode.PARAMS_INVALID, "邮箱格式不正确");
            SysUser occupied = sysUserService.getOne(QueryWrapper.create().eq("user_email", email));
            BusinessAssert.failIf(
                    occupied != null && !occupied.getId().equals(current.getId()),
                    ErrorCode.DATA_CONFLICT, "该邮箱已被使用"
            );
        }
        updateUserEmail(current.getId(), email);
        return true;
    }

    /**
     * 直接写列，这样留空可以解除绑定。updateById 会跳过 null 字段。
     */
    private void updateUserEmail(Long userId, String email) {
        try {
            int rows = Db.updateBySql(
                    "UPDATE sys_user SET user_email = ?, update_time = ? WHERE id = ? AND is_deleted = 0",
                    email, LocalDateTime.now(), userId
            );
            BusinessAssert.requireTrue(rows == 1, ErrorCode.SYSTEM_ERROR, "更新邮箱失败");
        } catch (RuntimeException ex) {
            if (isDuplicateKey(ex)) {
                throw new BusinessException(ErrorCode.DATA_CONFLICT, "该邮箱已被使用");
            }
            throw ex;
        }
    }

    private static boolean isDuplicateKey(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof DuplicateKeyException || current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("Duplicate entry")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
