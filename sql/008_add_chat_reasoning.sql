-- =======================================================
-- File: 008_add_chat_reasoning.sql
-- Description: 应用对话单独保存思考过程，不进入下一轮模型上下文。
-- Author: CWQ
-- Date: 2026-11-08
-- Notes: db-migrate 会重复执行本文件，因此用 information_schema 判断后再改表。
-- =======================================================

USE `ai_coding_station_memo`;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'chat_history'
      AND COLUMN_NAME = 'reasoning'
);
SET @ddl := IF(
    @col_exists = 0,
    'ALTER TABLE `chat_history` ADD COLUMN `reasoning` MEDIUMTEXT NULL COMMENT ''模型思考过程，不写入下一轮上下文'' AFTER `message`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
