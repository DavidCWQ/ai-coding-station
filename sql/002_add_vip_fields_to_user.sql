-- =====================================================
-- File: 002_add_vip_fields_to_user.sql
-- Description: 用户表中，增加会员相关字段
-- Author: CWQ
-- Date: 2026-01-08
-- =====================================================
-- 修改表
ALTER TABLE `sys_user`
    ADD COLUMN `vip_code`           VARCHAR(128)        NULL COMMENT '会员兑换码',
    ADD COLUMN `vip_number`         BIGINT              NULL COMMENT '会员编号',
    ADD COLUMN `vip_expire_time`    DATETIME            NULL COMMENT '会员过期时间';
