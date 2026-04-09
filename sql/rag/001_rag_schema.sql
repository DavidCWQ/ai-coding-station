-- =======================================================
-- File: 001_rag_schema.sql
-- Description: RAG / pgvector 表结构初始化与兼容升级脚本
-- Author: CWQ
-- Date: 2026-04-08
-- Notes:   1. 放在项目根目录 sql/rag/，由 compose 挂载自动执行。
--          2. rag_ingested_file 增加 content_hash，用于文档更新重建 embedding。
--          3. 保留旧列 file_name 作为主键，兼容历史数据。
-- =======================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- 入库文件记录：file_name 这里存“文件唯一键”（建议为 classpath）--
CREATE TABLE IF NOT EXISTS rag_ingested_file (
    file_name       TEXT                            PRIMARY KEY,
    content_hash    VARCHAR(64)                     NOT NULL DEFAULT '',
    ingested_at     TIMESTAMP                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 向量表：metadata 中保存 file_name（文件唯一键）等检索与清理信息 --
CREATE TABLE IF NOT EXISTS rag_embedding (
    id              BIGSERIAL                       PRIMARY KEY,
    embedding       VECTOR(1024)                    NOT NULL,
    text            TEXT                            NOT NULL,
    metadata        JSONB
);

-- 兼容升级：旧库无 content_hash/update_time 时自动补齐）--
ALTER TABLE rag_ingested_file
    ADD COLUMN IF NOT EXISTS
    content_hash VARCHAR(64)                        NOT NULL DEFAULT '';

ALTER TABLE rag_ingested_file
    ADD COLUMN IF NOT EXISTS
    update_time  TIMESTAMP                          NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- PostgreSQL/pgvector：近似最近邻搜索索引，通过聚类加速搜索 --
CREATE INDEX IF NOT EXISTS idx_rag_embedding_vector
    ON rag_embedding
    USING ivfflat (embedding vector_cosine_ops)     -- IVFFlat (Inverted File with Flat) 索引算法
    WITH (lists = 100);                             -- 分成 100个 聚类 (适合50k行文件)

CREATE INDEX IF NOT EXISTS idx_rag_embedding_meta_file_name
    ON rag_embedding ((metadata ->> 'file_name'));

ANALYZE rag_embedding;
