package com.cwq.project_aicodingstation.app.service;

/* 需求分析 -> 方案设计 -> 业务开发
 *
 * 应用生成平台核心需求：
 * -【用户】创建应用（须填写 initPrompt）
 * -【用户】根据 id 修改自己的应用（目前只支持修改应用名称）
 * -【用户】根据 id 删除自己的应用
 * -【用户】根据 id 查看应用详情
 * -【用户】分页查询自己的应用列表（支持根据名称查询，每页最多 20 个）
 * -【用户】分页查询精选的应用列表（支持根据名称查询，每页最多 20 个）
 * -【管理员】根据 id 删除任意应用
 * -【管理员】根据 id 更新任意应用（支持更新应用名称、应用封面、优先级）
 * -【管理员】分页查询应用列表（支持根据除时间外的任何字段查询，每页数量不限）
 * -【管理员】根据 id 查看应用详情
 * */

import com.cwq.project_aicodingstation.app.dto.*;
import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.vo.AppVO;
import com.cwq.project_aicodingstation.common.request.DeleteRequest;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AppService extends IService<App> {

    // ======================= 用户侧 ========================

    /**
     * 用户创建应用
     *
     * @param req       创建应用请求
     * @param userVO    已登录用户信息 (已脱敏)
     * @return          应用 id
     */
    Long createApp(AppAddRequest req, UserLoginVO userVO);

    /**
     * 用户更新应用（用户只能更新自己的应用名称）
     *
     * @param req       应用更新请求
     * @param userVO    已登录用户信息 (已脱敏)
     * @return          {@code true} 更新成功，{@code false} 更新失败
     */
    boolean updateApp(AppUpdateRequest req, UserLoginVO userVO);

    /**
     * 用户删除应用（用户只能删除自己的应用）
     *
     * @param req       删除请求
     * @param userVO    已登录用户信息 (已脱敏)
     * @return          {@code true} 删除成功，{@code false} 删除失败
     */
    boolean deleteApp(DeleteRequest req, UserLoginVO userVO);

    /**
     * 获取脱敏的 App 信息 (App->AppVO)
     *
     * @param app   App 信息
     * @return      App 信息 (已脱敏)
     */
    AppVO getAppVO(App app);

    /**
     * 根据 id 获取应用详情
     *
     * @param id    应用 id
     * @return      应用详情 (已脱敏)
     */
    AppVO getAppVOById(Long id);

    /**
     * 将查询请求转为 QueryWrapper Obj 来生成 SQL 查询
     * (<a href="https://mybatis-flex.com/zh/base/querywrapper.html">mybatis-flex</a>)
     *
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 分页获取当前用户创建的应用列表
     *
     * @param req       应用查询请求
     * @param userVO    请求
     * @return          应用列表
     */
    Page<AppVO> listMyApps(AppQueryRequest req, UserLoginVO userVO);

    /**
     * 分页获取精选应用列表
     *
     * @param req   查询请求
     * @return      精选应用列表
     */
    Page<AppVO> listFeaturedApps(AppQueryRequest req);

    /**
     * 批量构建 AppVO 列表（带创建者用户信息）
     *
     * <p>该方法用于将应用实体列表转换为前端展示用的 AppVO 列表，并在构建过程中
     * 一次性批量查询所有关联用户信息，避免逐条查询导致的 N+1 性能问题。</p>
     *
     * <p><b>性能特点：</b></p>
     * <ul>
     *     <li>时间复杂度 O(n)</li>
     *     <li>适用于分页列表 / 管理后台 / 精选应用等批量场景</li>
     * </ul>
     *
     * @param appList 应用实体列表
     * @return 包含用户信息的 AppVO 列表；若输入为空则返回空列表（不会返回 null）
     */
    List<AppVO> getAppVOList(List<App> appList);

    // ======================= 管理员侧 =======================

    /**
     * 管理员删除应用
     *
     * @param req 删除请求
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    boolean adminDeleteApp(DeleteRequest req);

    /**
     * 管理员更新应用
     *
     * @param req 更新请求
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    boolean adminUpdateApp(AppAdminUpdateRequest req);

    /**
     * 管理员根据 id 获取应用详情
     *
     * @param id    应用 id
     * @return      应用详情 (已脱敏)
     */
    AppVO adminGetAppVOById(Long id);

    /**
     * 管理员分页获取应用列表
     *
     * @param req   查询请求
     * @return      应用列表 (已脱敏)
     */
    Page<AppVO> adminListAppVOByPage(AppQueryRequest req);

    // ======================= 应用侧 ========================

    /**
     * 应用聊天生成代码（流式）
     *
     * <p>该方法是应用与 AI代码生成系统的核心集成入口。
     * 生成的代码目录格式：{@code codeGenType_appId}</p>
     *
     * @param appId     应用 ID
     * @param sessionId 会话 ID
     * @param message   用户输入消息
     * @param userVO    当前登录用户 VO
     * @return 流式代码生成结果 (SSE)
     */
    Flux<String> chatToGenCode(Long appId, Long sessionId, String message, UserLoginVO userVO);

    /**
     * 部署应用
     *
     * @param req       部署请求
     * @param userVO    当前登录用户 VO
     * @return 可访问URL
     */
    String deployApp(AppDeployRequest req, UserLoginVO userVO);
}
