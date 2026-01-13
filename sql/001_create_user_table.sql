-- =======================================================
-- File: 001_create_user_table.sql
-- Description: 数据库初始化，创建用户表
-- Author: CWQ
-- Date: 2026-01-08
-- Notes:   1. 统一放在项目根目录的`sql/`下。
--          2. 按"版本+动作+对象"命名。一个文件 = 一个明确目的。
--          3. 不放在resources，避免被打包进jar。
--          4. editTime表示用户编辑个人信息的时间（需业务代码来更新）
-- =======================================================
-- 创建库
CREATE DATABASE IF NOT EXISTS `ai_coding_station_memo`;

-- 切换库
USE `ai_coding_station_memo`;

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`            BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    `user_account`  VARCHAR(128)                            NOT NULL COMMENT '账号',
    `user_password` VARCHAR(256)                            NOT NULL COMMENT '密码（加密）',
    `user_name`     VARCHAR(128)                            NULL COMMENT '用户昵称',
    `user_avatar`   VARCHAR(1024)                           NULL COMMENT '用户头像',
    `user_profile`  VARCHAR(512)                            NULL COMMENT '用户简介',
    `user_role`     VARCHAR(64) DEFAULT 'user'              NOT NULL COMMENT '用户角色：user/admin',
    `edit_time`     DATETIME DEFAULT CURRENT_TIMESTAMP      NOT NULL COMMENT '编辑时间',
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP      NOT NULL COMMENT '创建时间',
    `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP      NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT  DEFAULT 0                      NOT NULL COMMENT '是否删除',
    UNIQUE KEY `uk_user_account` (`user_account`),
    KEY `idx_user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
