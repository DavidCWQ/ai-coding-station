package com.cwq.project_aicodingstation.user.controller;

/*
 * Controller = 协议层，不是业务层
 * Controller 只做 5 件事:
 * - 接收参数（Request）
 * - 基础参数校验（null / id <= 0）
 * - 权限声明（注解）
 * - 调用 Service
 * - 返回 Response
 * Controller 不该做：
 * - 复杂业务判断；组装分页逻辑；密码加密；角色判断细节。
 * */

import com.cwq.project_aicodingstation.common.annotation.AuthCheck;
import com.cwq.project_aicodingstation.common.request.DeleteRequest;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import com.cwq.project_aicodingstation.common.utils.ResultUtils;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import com.cwq.project_aicodingstation.user.dto.*;
import com.cwq.project_aicodingstation.user.service.AdminUserService;
import com.cwq.project_aicodingstation.user.vo.UserVO;
import com.cwq.project_aicodingstation.user.entity.SysUser;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    @Resource
    private AdminUserService adminUserService;

    /**
     * 保存并添加 sysUser。
     *
     * @param userAddRequest 用户添加请求
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("/save")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> save(@RequestBody UserAddRequest userAddRequest) {
        return ResultUtils.success(adminUserService.save(userAddRequest));
    }

    /**
     * 根据 request 删除 sysUser。
     *
     * @param deleteRequest 删除请求
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("/remove/")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> remove(@PathVariable DeleteRequest deleteRequest) {
        return ResultUtils.success(adminUserService.remove(deleteRequest));
    }

    /**
     * 根据 request 更新 sysUser。
     *
     * @param userUpdateRequest 用户更新请求
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> update(@RequestBody UserUpdateRequest userUpdateRequest) {
        return ResultUtils.success(adminUserService.update(userUpdateRequest));
    }

    /**
     * 根据主键获取 sysUser。
     *
     * @param id sysUser 主键
     * @return sysUser 详情
     */
    @GetMapping("/get/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SysUser> getUser(@PathVariable Long id) {
        return ResultUtils.success(adminUserService.getById(id));
    }

    /**
     * 根据主键获取包装类 UserVO。
     *
     * @param id sysUser 主键
     * @return UserVO
     */
    @GetMapping("/get/vo/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<UserVO> getUserVO(@PathVariable Long id) {
        return ResultUtils.success(adminUserService.getVOById(id));
    }

    /**
     * 分页获取用户封装列表。
     *
     * @param request 查询请求参数
     * @return 分页对象 {@code Page<UserVO>}
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listByPage(@RequestBody UserQueryRequest request) {
        return ResultUtils.success(adminUserService.listUserVOByPage(request));
    }
}
