-- =======================================================
-- File: 009_message_feedback_and_agent_share.sql
-- Description: 消息评价，以及智能体对话的公开分享快照
-- Author: CWQ
-- Date: 2026-11-11
-- Notes: db-migrate 会重复执行本文件，因此使用 CREATE TABLE IF NOT EXISTS。
-- =======================================================

USE `ai_coding_station_memo`;

CREATE TABLE IF NOT EXISTS `message_feedback` (
    `id`          BIGINT                                NOT NULL PRIMARY KEY COMMENT '主键（雪花）',
    `user_id`     BIGINT                                NOT NULL COMMENT '评价人',
    `source`      VARCHAR(16)                           NOT NULL COMMENT 'app / agent',
    `message_id`  BIGINT                                NOT NULL COMMENT '被评价的消息 id',
    `vote`        TINYINT                               NOT NULL COMMENT '1 有帮助，-1 没帮助',
    `reason`      VARCHAR(200)                          NULL COMMENT '没帮助时的可选原因',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_feedback_user_source_msg` (`user_id`, `source`, `message_id`),
    KEY `idx_feedback_message` (`source`, `message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息评价';

CREATE TABLE IF NOT EXISTS `agent_chat_share` (
    `id`          BIGINT                                NOT NULL PRIMARY KEY COMMENT '主键（雪花）',
    `user_id`     BIGINT                                NOT NULL COMMENT '分享人',
    `session_id`  BIGINT                                NOT NULL COMMENT '来源会话',
    `agent_code`  VARCHAR(32)                           NOT NULL COMMENT '智能体编码',
    `token`       VARCHAR(32)                           NOT NULL COMMENT '公开链接令牌',
    `title`       VARCHAR(256)                          NULL COMMENT '分享标题',
    `revoked`     TINYINT DEFAULT 0                     NOT NULL COMMENT '1 已停止分享',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_agent_share_token` (`token`),
    KEY `idx_agent_share_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体对话分享';

CREATE TABLE IF NOT EXISTS `agent_chat_share_item` (
    `id`          BIGINT                                NOT NULL PRIMARY KEY COMMENT '主键（雪花）',
    `share_id`    BIGINT                                NOT NULL COMMENT '分享 id',
    `sort_order`  INT                                   NOT NULL COMMENT '展示顺序',
    `role`        VARCHAR(16)                           NOT NULL COMMENT 'user / assistant / system',
    `content`     MEDIUMTEXT                            NOT NULL COMMENT '创建当时的正文副本',
    KEY `idx_agent_share_item_share` (`share_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体分享内容快照';
