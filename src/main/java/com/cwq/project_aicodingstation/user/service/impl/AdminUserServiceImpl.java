package com.cwq.project_aicodingstation.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.request.DeleteRequest;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import com.cwq.project_aicodingstation.user.dto.UserAddRequest;
import com.cwq.project_aicodingstation.user.dto.UserQueryRequest;
import com.cwq.project_aicodingstation.user.dto.UserUpdateRequest;
import com.cwq.project_aicodingstation.user.entity.SysUser;
import com.cwq.project_aicodingstation.user.service.AdminUserService;
import com.cwq.project_aicodingstation.user.service.SysUserService;
import com.cwq.project_aicodingstation.user.vo.UserVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class AdminUserServiceImpl implements AdminUserService {

    @Resource
    private SysUserService sysUserService;

    @Override
    public Long save(UserAddRequest request) {

        // 1. 参数校验
        BusinessAssert.notNull(request, ErrorCode.PARAMS_MISSING, "管理员添加请求为空");

        // 2. 复制信息
        SysUser user = new SysUser();
        BeanUtil.copyProperties(request, user);

        // 3. 密码加密
        String encryptPassword = sysUserService.getEncryptPassword(UserConstant.DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);

        // 4. 返回校验
        BusinessAssert.requireTrue(sysUserService.save(user),
                ErrorCode.SYSTEM_ERROR, "管理员保存用户失败"
        );
        return user.getId();
    }

    @Override
    public boolean remove(DeleteRequest request) {

        // 1. 参数校验
        BusinessAssert.notNull(request,
                ErrorCode.PARAMS_MISSING, "管理员删除请求为空"
        );
        BusinessAssert.requireTrue(request.getId() <= 0,
                ErrorCode.PARAMS_ERROR  , "管理员删除请求中id不合法"
        );

        return sysUserService.removeById(request.getId());
    }

    @Override
    public boolean update(UserUpdateRequest request) {

        // 1. 参数校验
        BusinessAssert.notNull(request,
                ErrorCode.PARAMS_MISSING, "管理员更新请求为空"
        );
        BusinessAssert.notNull(request.getId(),
                ErrorCode.PARAMS_MISSING  , "管理员更新请求中id为空"
        );

        // 2. 复制信息
        SysUser user = new SysUser();
        BeanUtil.copyProperties(request, user);

        // 3. 返回校验
        BusinessAssert.requireTrue(sysUserService.updateById(user),
                ErrorCode.SYSTEM_ERROR, "管理员更新用户失败"
        );
        return true;
    }

    @Override
    public SysUser getById(Long id) {
        BusinessAssert.requireTrue(id <= 0,
                ErrorCode.PARAMS_ERROR, "管理员获取id不合法"
        );
        SysUser user = sysUserService.getById(id);
        BusinessAssert.notNull(user, ErrorCode.NOT_FOUND, "获取用户不存在");
        return user;
    }

    @Override
    public UserVO getVOById(Long id) {
        return sysUserService.getUserVO(getById(id));
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest request) {

        // 1. 参数校验
        BusinessAssert.notNull(request, ErrorCode.PARAMS_MISSING, "获取分页请求为空");

        long pageNum = request.getPageNum();
        long pageSize = request.getPageSize();
        Page<SysUser> userPage = sysUserService.page(
                Page.of(pageNum, pageSize), sysUserService.getQueryWrapper(request)
        );

        // 2. 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        userVOPage.setRecords(sysUserService.getUserVOList(userPage.getRecords()));

        return userVOPage;
    }
}
