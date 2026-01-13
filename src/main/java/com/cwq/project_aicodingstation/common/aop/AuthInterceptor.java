package com.cwq.project_aicodingstation.common.aop;

/*
 * 面向切面编程（AOP）作为一种编程范式，
 * 通过将与核心业务逻辑无关的横切关注点（如日志、安全、事务管理等）从主逻辑中分离出来，
 * 极大地提升了代码的整洁性和可管理性。
 * */

import com.cwq.project_aicodingstation.common.annotation.AuthCheck;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.entity.SysUser;
import com.cwq.project_aicodingstation.user.enums.UserRoleEnum;
import com.cwq.project_aicodingstation.user.service.SysUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class AuthInterceptor {

    @Resource
    private SysUserService userService;

    /**
     * 拦截所有标注了 @AuthCheck 的方法
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {

        String mustRole = authCheck.mustRole();

        // 1. 获取当前请求
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        BusinessAssert.notNull(attributes, ErrorCode.NOT_LOGIN, "请求为空");
        HttpServletRequest request = attributes.getRequest();

        // 2. 获取当前登录用户（你已有）
        SysUser loginUser = userService.getLoginUser(request);

        // 3. 不要求角色，说明：只要登录即可
        if (mustRole == null || mustRole.isBlank()) {
            return joinPoint.proceed();
        }

        // 4. 校验角色（必须有该权限才通过）
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());

        BusinessAssert.notNull(mustRoleEnum, ErrorCode.NO_PERMISSION, "无访问权限");
        BusinessAssert.notNull(userRoleEnum, ErrorCode.NO_PERMISSION, "无访问权限");

        // 5. 管理员校验（可扩展）
        boolean hasRole = UserRoleEnum.ADMIN.equals(mustRoleEnum);
        boolean noAuth = !UserRoleEnum.ADMIN.equals(userRoleEnum);
        BusinessAssert.requireTrue(hasRole && noAuth,
                ErrorCode.NO_PERMISSION, "要求必须有管理员权限，但用户没有管理员权限"
        );

        // 6. 通过权限校验，放行
        return joinPoint.proceed();
    }
}
