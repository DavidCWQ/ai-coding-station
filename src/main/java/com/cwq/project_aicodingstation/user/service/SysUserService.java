package com.cwq.project_aicodingstation.user.service;

/* 需求分析 -> 方案设计 -> 业务开发
 *
 * 用户管理需求具体可以拆分为：
 * - 【管理员】创建用户
 * - 【管理员】根据 id 删除用户
 * - 【管理员】更新用户
 * - 【管理员】分页获取用户列表（需要脱敏）
 * - 【管理员】根据 id 获取用户（未脱敏）
 * - 【管理员】根据 id 获取用户（脱敏）
 * */

import com.cwq.project_aicodingstation.user.dto.UserQueryRequest;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.cwq.project_aicodingstation.user.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.cwq.project_aicodingstation.user.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * @apiNote User Service Layer (基础用户能力 [DAO+通用方法])
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    Long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 密码加密
     *
     * @param userPassword 用户密码
     * @return `加密后的`用户密码
     * */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request 客户端（如浏览器）向服务器发送的 HTTP 请求信息
     * @return 脱敏后的用户信息
     */
    UserLoginVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取`脱敏的`已登录用户信息
     *
     * @return userLoginVO
     */
    UserLoginVO getUserLoginVO(SysUser user);

    /**
     * 获取当前登录用户
     *
     * @param request 客户端（如浏览器）向服务器发送的 HTTP 请求信息
     * @return 当前登录用户
     */
    SysUser getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request 客户端（如浏览器）向服务器发送的 HTTP 请求信息
     * @return 注销成功返回 {@code true}，未成功返回 {@code false}
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取`脱敏的`用户信息
     *
     * @return userVO
     */
    UserVO getUserVO(SysUser user);

    /**
     * 获取`脱敏的`用户信息列表
     *
     * @return {@code List<UserVO>}
     */
    List<UserVO> getUserVOList(List<SysUser> userList);

    /**
     * 将查询请求转为 QueryWrapper Obj 来生成 SQL 查询
     * (<a href="https://mybatis-flex.com/zh/base/querywrapper.html">mybatis-flex</a>)
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    // ============ 上述功能都是样板代码，俗称`增删改查` ============

}
