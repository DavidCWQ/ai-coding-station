-- =======================================================
-- File: 003_create_app_table.sql
-- Description: 创建应用表（AI 网站应用生命周期）
-- Author: CWQ
-- Date: 2026-02-08
-- =======================================================

USE `ai_coding_station_memo`;

CREATE TABLE IF NOT EXISTS `app` (
    `id`              BIGINT AUTO_INCREMENT                 COMMENT '主键' PRIMARY KEY,
    `app_name`        VARCHAR(256)                          NULL COMMENT '应用名称',
    `cover`           VARCHAR(512)                          NULL COMMENT '应用封面',
    `init_prompt`     TEXT                                  NULL COMMENT '初始化提示词',
    `code_gen_type`   VARCHAR(64)                           NULL COMMENT '代码生成类型 HTML/MULTI_FILE',
    `deploy_key`      VARCHAR(16)                           NULL COMMENT '部署唯一标识（6位短key）',
    `deployed_time`   DATETIME                              NULL COMMENT '部署时间',
    `priority`        INT DEFAULT 0                         NOT NULL COMMENT '优先级（99=精选，999=置顶）',
    `user_id`         BIGINT                                NOT NULL COMMENT '创建用户id',
    `edit_time`       DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL COMMENT '编辑时间',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL COMMENT '创建时间',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT DEFAULT 0                     NOT NULL COMMENT '是否删除',
    UNIQUE KEY `uk_deploy_key` (`deploy_key`),              -- 保证部署标识唯一性
    KEY `idx_app_name` (`app_name`),                        -- 提升基于应用名称的查询性能
    KEY `idx_user_id` (`user_id`),                          -- 提升基于用户ID的查询性能
    CONSTRAINT `fk_app_user`
    FOREIGN KEY (`user_id`)                                 -- 保证数据一致性
    REFERENCES `sys_user` (`id`)
                                                            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI应用表';
