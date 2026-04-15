-- =======================================================
-- File: 001_rag_schema.sql
-- Description: RAG / pgvector 表结构初始化（非兼容模式）
-- Author: CWQ
-- Date: 2026-04-08
-- Notes:   1. 适用于重建场景；会先 DROP 再 CREATE。
--          2. 文件唯一键使用 (file_name, file_dir)。
-- =======================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- 入库文件记录：file_name + file_dir 唯一标识文件（文件名 + 目录）
CREATE TABLE IF NOT EXISTS rag_ingested_file (
    id              BIGSERIAL                       PRIMARY KEY,
    file_name       TEXT                            NOT NULL,
    file_dir        TEXT                            NOT NULL DEFAULT '',
    content_hash    VARCHAR(64)                     NOT NULL DEFAULT '',
    ingested_at     TIMESTAMP                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_rag_ingested_file_name_dir
    ON rag_ingested_file (file_name, file_dir);

-- 向量表：metadata 中保存 file_name / file_dir / corpus
CREATE TABLE IF NOT EXISTS rag_embedding (
    embedding_id    UUID                            PRIMARY KEY,
    embedding       VECTOR(1024)                    NOT NULL,
    text            TEXT                            NOT NULL,
    metadata        JSONB
);

-- PostgreSQL/pgvector：近似最近邻搜索索引，通过聚类加速搜索
CREATE INDEX idx_rag_embedding_vector
    ON rag_embedding
    USING ivfflat (embedding vector_cosine_ops)     -- IVFFlat (Inverted File with Flat) 索引算法
    WITH (lists = 100);                             -- 分成 100个 聚类 (适合50k行文件)

CREATE INDEX idx_rag_embedding_meta_file_name
    ON rag_embedding ((metadata ->> 'file_name'));

CREATE INDEX idx_rag_embedding_meta_file_dir
    ON rag_embedding ((metadata ->> 'file_dir'));

ANALYZE rag_embedding;
