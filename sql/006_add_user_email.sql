-- =======================================================
-- File: 006_add_user_email.sql
-- Description: 用户表增加可空邮箱，供忘记密码使用。已有账号不强制补填。
-- Author: CWQ
-- Date: 2026-10-18
-- Notes: db-migrate 会重复执行本文件，因此用 information_schema 判断后再改表。
-- =======================================================

USE `ai_coding_station_memo`;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'user_email'
);
SET @ddl := IF(
    @col_exists = 0,
    'ALTER TABLE `sys_user` ADD COLUMN `user_email` VARCHAR(256) NULL COMMENT ''邮箱，用于找回密码'' AFTER `user_account`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND INDEX_NAME = 'uk_user_email'
);
SET @ddl := IF(
    @idx_exists = 0,
    'ALTER TABLE `sys_user` ADD UNIQUE KEY `uk_user_email` (`user_email`)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
