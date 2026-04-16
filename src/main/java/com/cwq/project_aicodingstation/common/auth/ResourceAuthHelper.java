package com.cwq.project_aicodingstation.common.auth;

import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.mapper.AppMapper;
import com.cwq.project_aicodingstation.app.constant.AppConstant;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 资源级访问校验（属主、管理员、应用与会话组合规则）。
 * <p>
 * 「认证 + 粗粒度角色」可用 {@code AuthCheck}；
 * </p><p>
 * 「某条数据是否归当前用户」应在领域服务内、在加载实体后调用本类方法，保证与事务同事务边界。
 * </p>
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Component
public class ResourceAuthHelper {

    @Resource
    private AppMapper appMapper;

    /**
     * 要求已登录（存在登录用户对象）
     *
     * @param userVO 当前用户
     */
    public void requireLogin(UserLoginVO userVO) {
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");
    }

    /**
     * 校验应用存在且当前用户为应用创建者或管理员。
     *
     * @param appId  应用 id
     * @param userVO 当前用户
     */
    public void requireAppEditable(Long appId, UserLoginVO userVO) {
        requireLogin(userVO);
        BusinessAssert.notNull(appId, ErrorCode.PARAMS_MISSING, "应用 id 为空");
        QueryWrapper qw = QueryWrapper.create();
        qw.select("id", "user_id").eq("id", appId);
        App app = appMapper.selectOneByQuery(qw);
        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");
        requireOwnerOrAdmin(userVO, app.getUserId(), "无权限编辑该应用");
    }

    /**
     * 校验应用可访问：「精选应用」任何情况均可读取；
     * 登录后管理员可读取全部应用，应用创建者可读取所创建的应用；并返回应用实体（避免调用方重复查库）。
     *
     * @param appId  应用 id
     * @param userVO 当前用户
     * @return 应用实体（仅含 id / userId / priority 等基础字段）
     */
    public App requireAppReadable(Long appId, UserLoginVO userVO) {
        BusinessAssert.notNull(appId, ErrorCode.PARAMS_MISSING, "应用 id 为空");
        QueryWrapper qw = QueryWrapper.create();
        qw.select("id", "user_id", "priority").eq("id", appId);
        App app = appMapper.selectOneByQuery(qw);
        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");
        boolean featured = app.getPriority() != null && app.getPriority() >= AppConstant.GOOD_APP_PRIORITY;
        if (featured) { return app; } //「精选应用」任何情况均可读取
        requireOwnerOrAdmin(userVO, app.getUserId(), "无权限访问该应用");
        return app;
    }

    /**
     * 资源属主或管理员可操作（如会话行上的 user_id、智能体会话属主）。
     *
     * @param userVO              当前用户（须已登录）
     * @param resourceOwnerUserId 资源上的属主用户 id
     * @param noPermissionMsg     拒绝时的提示文案
     */
    public void requireOwnerOrAdmin(UserLoginVO userVO, Long resourceOwnerUserId, String noPermissionMsg) {
        requireLogin(userVO);
        boolean owner = resourceOwnerUserId != null && resourceOwnerUserId.equals(userVO.getId());
        boolean admin = UserConstant.ADMIN_ROLE.equals(userVO.getUserRole());
        BusinessAssert.requireTrue(owner || admin, ErrorCode.NO_PERMISSION, noPermissionMsg);
    }

}
