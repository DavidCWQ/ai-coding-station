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

    public String getContentHash(String fileName, String fileDir) {
        return jdbcTemplate.query(
                "SELECT content_hash FROM rag_ingested_file WHERE file_name = ? AND file_dir = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                fileName,
                fileDir
        );
    }

    public void upsert(String fileName, String fileDir, String contentHash) {
        jdbcTemplate.update(
                """
                INSERT INTO rag_ingested_file (file_name, file_dir, content_hash)
                VALUES (?, ?, ?)
                ON CONFLICT (file_name, file_dir)
                DO UPDATE SET
                    content_hash = EXCLUDED.content_hash,
                    update_time = CURRENT_TIMESTAMP
                """,
                fileName,
                fileDir,
                contentHash
        );
    }

    public void deleteNotIn(List<String> fileCodes) {
        if (fileCodes == null || fileCodes.isEmpty()) {
            jdbcTemplate.update("TRUNCATE TABLE rag_ingested_file");
            return;
        }
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "DELETE FROM rag_ingested_file WHERE NOT (concat(file_dir, '/', file_name) = ANY (?))");
            Array fileKeyArray = con.createArrayOf("text", fileCodes.toArray());
            ps.setArray(1, fileKeyArray);
            return ps;
        });
    }

    public void deleteEmbeddingsByFile(String fileName, String fileDir) {
        jdbcTemplate.update(
                """
                DELETE FROM rag_embedding
                WHERE metadata ->> 'file_name' = ?
                  AND COALESCE(metadata ->> 'file_dir', '') = ?
                """,
                fileName,
                fileDir
        );
    }

    public void deleteEmbeddingsNotIn(List<String> fileCodes) {
        if (fileCodes == null || fileCodes.isEmpty()) {
            jdbcTemplate.update("TRUNCATE TABLE rag_embedding");
            return;
        }
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    """
                    DELETE FROM rag_embedding
                    WHERE NOT (concat(COALESCE(metadata ->> 'file_dir', ''), '/', metadata ->> 'file_name') = ANY (?))
                    """);
            Array fileKeyArray = con.createArrayOf("text", fileCodes.toArray());
            ps.setArray(1, fileKeyArray);
            return ps;
        });
    }
}
