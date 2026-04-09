package com.cwq.project_aicodingstation.ai.rag.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "true")
public class RagFileRepository {

    private final JdbcTemplate jdbcTemplate;

    public RagFileRepository(@Qualifier("ragJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getContentHash(String fileKey) {
        return jdbcTemplate.query(
                "SELECT content_hash FROM rag_ingested_file WHERE file_name = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                fileKey
        );
    }

    public void upsert(String fileKey, String contentHash) {
        jdbcTemplate.update(
                """
                INSERT INTO rag_ingested_file (file_name, content_hash)
                VALUES (?, ?)
                ON CONFLICT (file_name)
                DO UPDATE SET
                    content_hash = EXCLUDED.content_hash,
                    update_time = CURRENT_TIMESTAMP
                """,
                fileKey,
                contentHash
        );
    }

    public void deleteNotIn(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            jdbcTemplate.update("TRUNCATE TABLE rag_ingested_file");
            return;
        }
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "DELETE FROM rag_ingested_file WHERE NOT (file_name = ANY (?))");
            Array fileKeyArray = con.createArrayOf("text", fileKeys.toArray());
            ps.setArray(1, fileKeyArray);
            return ps;
        });
    }

    public long countEmbeddingsByFileKey(String fileKey) {
        Long n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rag_embedding WHERE metadata ->> 'file_name' = ?",
                Long.class,
                fileKey
        );
        return n != null ? n : 0L;
    }

    public void deleteEmbeddingsByFileKey(String fileKey) {
        jdbcTemplate.update(
                "DELETE FROM rag_embedding WHERE metadata ->> 'file_name' = ?",
                fileKey
        );
    }

    public void deleteEmbeddingsNotIn(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            jdbcTemplate.update("TRUNCATE TABLE rag_embedding");
            return;
        }
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "DELETE FROM rag_embedding WHERE NOT ((metadata ->> 'file_name') = ANY (?))");
            Array fileKeyArray = con.createArrayOf("text", fileKeys.toArray());
            ps.setArray(1, fileKeyArray);
            return ps;
        });
    }
}
