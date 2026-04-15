-- =======================================================
-- File: 005_create_agent_chat_tables.sql
-- Description: 智能体对话（与 App 对话隔离，仅关联用户）
-- Author: CWQ
-- Date: 2026-04-04
-- =======================================================

USE `ai_coding_station_memo`;

CREATE TABLE IF NOT EXISTS `agent_chat_session` (           -- 智能体会话（元信息）
    `id`            BIGINT                                  NOT NULL PRIMARY KEY COMMENT '主键（雪花）',
    `user_id`       BIGINT                                  NOT NULL COMMENT '用户 id',
    `agent_code`    VARCHAR(32)                             NOT NULL COMMENT '智能体编码（如 code_assistant）',
    `title`         VARCHAR(256)                            NULL COMMENT '会话标题',
    `last_msg_time` DATETIME                                NULL COMMENT '最后消息时间',
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP      NOT NULL COMMENT '创建时间',
    `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP      NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT DEFAULT 0                       NOT NULL COMMENT '是否删除',
    -- ================= 索引 =================
    KEY `idx_agent_session_user_id` (`user_id`),
    KEY `idx_agent_session_user_time` (`user_id`, `agent_code`, `last_msg_time`),
    -- ================= 外键 =================
    CONSTRAINT `fk_agent_chat_session_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `sys_user` (`id`)
                                                            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体会话表';

CREATE TABLE IF NOT EXISTS `agent_chat_message` (           -- 智能体对话消息
    `id`              BIGINT                                NOT NULL PRIMARY KEY COMMENT '主键（雪花）',
    `user_id`         BIGINT                                NOT NULL COMMENT '所属用户 id',
    `session_id`      BIGINT                                NOT NULL COMMENT '会话 id',
    `agent_code`      VARCHAR(32)                           NOT NULL COMMENT '智能体编码（冗余，便于审计与排查）',
    `message`         TEXT                                  NOT NULL COMMENT '消息正文',
    `message_type`    VARCHAR(32)                           NOT NULL COMMENT 'user / ai / system',
    `metadata`        JSON                                  NULL COMMENT '扩展元数据（可选）',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL COMMENT '创建时间',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT DEFAULT 0                     NOT NULL COMMENT '是否删除',
    -- ================= 索引设计 =================
    KEY `idx_agent_msg_user_id` (`user_id`),                -- 提升基于 用户ID 的查询性能
    KEY `idx_agent_msg_session_id` (`session_id`),          -- 提升基于 会话ID 的查询性能
    KEY `idx_agent_msg_session_time` (`session_id`, `create_time`),
    -- ================= 约束设计 =================
    CONSTRAINT `fk_agent_chat_message_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `sys_user` (`id`)
                                                            ON DELETE CASCADE,
    CONSTRAINT `fk_agent_chat_message_session`
    FOREIGN KEY (`session_id`)
    REFERENCES `agent_chat_session` (`id`)
                                                            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体消息表';
