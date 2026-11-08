-- =======================================================
-- File: 007_create_visit_stats.sql
-- Description: 访问明细（保留 90 天）与按日汇总（永久保留）
-- Author: CWQ
-- Date: 2026-11-08
-- Notes: db-migrate 会重复执行本文件，建表语句保持幂等。
-- =======================================================

USE `ai_coding_station_memo`;

CREATE TABLE IF NOT EXISTS `visit_log` (
    `id`            BIGINT                                  NOT NULL COMMENT '主键' PRIMARY KEY,
    `visited_at`    DATETIME                                NOT NULL COMMENT '访问时间',
    `stat_date`     DATE                                    NOT NULL COMMENT '统计日，Asia/Shanghai',
    `scope`         VARCHAR(8)                              NOT NULL COMMENT 'SITE 全站页面，APP 应用详情',
    `app_id`        BIGINT                                  NULL COMMENT '应用 id，全站访问为空',
    `path`          VARCHAR(256)                            NOT NULL COMMENT '页面路径',
    `visitor_hash`  CHAR(64)                                NOT NULL COMMENT 'IP 与 User-Agent 的 SHA-256',
    `session_hash`  CHAR(64)                                NOT NULL COMMENT '会话 id 的 SHA-256',
    KEY `idx_visit_log_visited_at` (`visited_at`),
    KEY `idx_visit_log_scope_date` (`stat_date`, `scope`, `app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问明细，保留 90 天';

CREATE TABLE IF NOT EXISTS `visit_daily` (
    `stat_date`     DATE                                    NOT NULL COMMENT '统计日，Asia/Shanghai',
    `scope`         VARCHAR(8)                              NOT NULL COMMENT 'SITE 或 APP',
    `app_id`        BIGINT        DEFAULT 0                 NOT NULL COMMENT '全站为 0',
    `pv`            BIGINT        DEFAULT 0                 NOT NULL COMMENT '浏览量',
    `uv`            BIGINT        DEFAULT 0                 NOT NULL COMMENT '独立访客',
    PRIMARY KEY (`stat_date`, `scope`, `app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问按日汇总，永久保留';
