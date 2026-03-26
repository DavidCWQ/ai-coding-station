-- =======================================================
-- File: 004_create_chat_history_table.sql
-- Description: 创建对话历史表（AI 应用对话上下文，会话含多轮对话）
-- Author: CWQ
-- Date: 2026-03-24
-- =======================================================

USE `ai_coding_station_memo`;

CREATE TABLE IF NOT EXISTS `chat_session` (               -- 对话容器（元信息）
    `id`            BIGINT AUTO_INCREMENT                 PRIMARY KEY COMMENT '主键（雪花ID）',
    `app_id`        BIGINT                                NOT NULL COMMENT '应用id',
    `user_id`       BIGINT                                NOT NULL COMMENT '用户id',
    `title`         VARCHAR(256)                          NULL COMMENT '会话标题（自动生成或用户修改）',
    `last_msg_time` DATETIME                              NULL COMMENT '最后消息时间（用于排序）',
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL COMMENT '创建时间',
    `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT DEFAULT 0                     NOT NULL COMMENT '是否删除',
    -- ================= 索引 =================
    KEY `idx_chat_session_app_id` (`app_id`),
    KEY `idx_chat_session_user_id` (`user_id`),
    KEY `idx_chat_session_user_time` (`user_id`, `last_msg_time`),
    -- ================= 外键 =================
    CONSTRAINT `fk_chat_session_app`
    FOREIGN KEY (`app_id`)
    REFERENCES `app` (`id`)
                                                        ON DELETE CASCADE,
    CONSTRAINT `fk_chat_session_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `sys_user` (`id`)
                                                        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话会话表';

CREATE TABLE IF NOT EXISTS `chat_history` (                 -- 对话内容（真实数据）
    `id`              BIGINT AUTO_INCREMENT                 PRIMARY KEY COMMENT '主键',
    `message`         TEXT                                  NOT NULL COMMENT '消息内容',
    `message_type`    VARCHAR(32)                           NOT NULL COMMENT '消息类型（user/ai/system）',
    `app_id`          BIGINT                                NOT NULL COMMENT '应用id',
    `user_id`         BIGINT                                NOT NULL COMMENT '创建用户id',
    `session_id`      BIGINT                                NOT NULL COMMENT '会话id',
    `parent_id`       BIGINT                                NULL COMMENT '父消息id（用于上下文关联 / 重试）',
    `file_list`       JSON                                  NULL COMMENT '代码文件列表（JSON数组，可选），DB只存[元数据+URL]，不存代码本体',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL COMMENT '创建时间',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT DEFAULT 0                     NOT NULL COMMENT '是否删除',
    -- ================= 索引设计 =================
    KEY `idx_app_id` (`app_id`),                            -- 提升基于 应用名称 的查询性能
    KEY `idx_user_id` (`user_id`),                          -- 提升基于 用户ID 的查询性能
    KEY `idx_session_id` (`session_id`),                    -- 提升基于 会话ID 的查询性能
    KEY `idx_create_time` (`create_time`),                  -- 提升基于 时间 的查询性能
    KEY `idx_app_id_create_time` (`app_id`, `create_time`), -- 核心游标分页索引
    KEY `idx_session_time` (`session_id`, `create_time`),   -- 核心游标分页索引
    KEY `idx_parent_id` (`parent_id`),
    -- ================= 约束设计 =================
    CONSTRAINT `fk_chat_history_app`
    FOREIGN KEY (`app_id`)
    REFERENCES `app` (`id`)
                                                            ON DELETE CASCADE,
    CONSTRAINT `fk_chat_history_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `sys_user` (`id`)
                                                            ON DELETE CASCADE,
    CONSTRAINT `fk_chat_history_session`
    FOREIGN KEY (`session_id`)
    REFERENCES `chat_session` (`id`)
                                                            ON DELETE CASCADE,
    CONSTRAINT `fk_chat_history_parent`
    FOREIGN KEY (`parent_id`)
    REFERENCES `chat_history` (`id`)
                                                            ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话历史表';
